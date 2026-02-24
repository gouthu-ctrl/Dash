package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.repository.TripRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.dash.travel.data.model.*
import com.dash.travel.data.remote.SupabaseManager
import com.dash.travel.data.repository.DocumentRepository
import com.dash.travel.data.repository.ImageRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.builtins.ListSerializer

class TripDetailViewModel(
    private val tripId: String,
    private val tripDao: com.dash.travel.data.local.dao.TripDao,
    private val itineraryDao: ItineraryDao,
    private val tripRepository: TripRepository,
    private val documentRepository: DocumentRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    // Using mutableStateListOf for optimized, zero-latency UI updates
    val items = mutableStateListOf<ItineraryItemEntity>()
    
    // Track current user's votes: ItemId -> VoteType (up, down, heart)
    val userVotes = androidx.compose.runtime.mutableStateMapOf<String, String?>()
    
    // Track aggregate vote counts: ItemId -> VoteState(up, down, fave)
    val voteCounts = androidx.compose.runtime.mutableStateMapOf<String, com.dash.travel.ui.screens.collaborative.VoteState>()
    
    private var syncJob: Job? = null

    // Trip Metadata
    private val _trip = kotlinx.coroutines.flow.MutableStateFlow<com.dash.travel.data.model.SupabaseTrip?>(null)
    val trip = _trip.asStateFlow()

    // Error state (must be declared before init{} since init calls refreshItinerary which uses _error)
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Active Users (Presence)
    private val _activeUsers = MutableStateFlow<List<PresenceUser>>(emptyList())
    val activeUsers = _activeUsers.asStateFlow()

    // Expose current user ID for Role checks
    val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id

    init {
        // Fetch Trip Metadata
        viewModelScope.launch {
             val fetchedTrip = tripRepository.getTripById(tripId)
             _trip.value = fetchedTrip
        }

        // Observe local DB
        viewModelScope.launch {
            itineraryDao.getItineraryForTrip(tripId).collectLatest { dbItems ->
                // Fix: Always update items content properly to ensure reactivity
                items.clear()
                items.addAll(dbItems)
                
                // Fix: Ensure votes are aligned with the latest items
                fetchUserVotes()
            }
        }
        
        // Initial fetch
        refreshTrip()
        fetchUserVotes() 
        
        // Initial sync
        refreshItinerary()
        
        // Realtime Subscription: Itinerary Items
        viewModelScope.launch {
            try {
                tripRepository.subscribeToItineraryChanges(tripId).collect {
                    refreshItinerary()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Realtime Subscription: Votes (Ensures counts update when returning from Voting screen)
        viewModelScope.launch {
            try {
                if (tripId.isNotEmpty()) {
                    tripRepository.subscribeToVoteChanges(tripId).collect {
                        fetchUserVotes()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Realtime Presence
        viewModelScope.launch {
            try {
                val session = SupabaseManager.client.auth.currentSessionOrNull()
                val user = session?.user
                if (user != null) {
                    // Create PresenceUser. Try to get name/avatar from profile if available, 
                    // otherwise fall back or fetch. 
                    // Since we might not have the full profile loaded here easily without another call,
                    // we'll try to use what we know or just a placeholder logic that ProfileRepository could handle.
                    // Ideally we'd pass ProfileRepository to this VM.
                    // For now, let's create a basic presence user using ID.
                    val presenceUser = PresenceUser(
                        userId = user.id,
                        displayName = user.email?.split("@")?.firstOrNull() ?: "Traveler",
                        avatarUrl = null, // Can't easily get this without ProfileRepo here, but that's okay for MVP
                        color = "#" + user.id.take(6) // Deterministic color from ID
                    )
                    
                    tripRepository.subscribeToPresence(tripId, presenceUser).collect { users ->
                        _activeUsers.value = users
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun onResume() {
        fetchUserVotes()
        refreshTrip()
    }
    
    private fun refreshTrip() {
        viewModelScope.launch {
            try {
                val fetchedTrip = tripRepository.getTripById(tripId)
                _trip.value = fetchedTrip
            } catch (e: Exception) {
                // Ignore metadata fetch errors for now
            }
        }
    }

    fun refreshItinerary() {
        viewModelScope.launch {
            _error.value = null
            try {
                // Use repository to fetch items
                val remoteItems = tripRepository.getItineraryItems(tripId)
                
                // Map to Room entities
                val json = Json { ignoreUnknownKeys = true }
                val entities = remoteItems.mapIndexed { index, item ->
                    val providerJson = item.providerDetails?.let { json.encodeToString(JsonObject.serializer(), it) }
                    val attachmentsJson = if (item.attachments.isNotEmpty()) {
                        json.encodeToString(ListSerializer(ItineraryAttachment.serializer()), item.attachments)
                    } else null

                    ItineraryItemEntity(
                        id = item.id ?: "",
                        tripId = item.tripId,
                        type = item.type,
                        status = item.status.name.lowercase(),
                        title = item.title,
                        description = item.description,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        locationName = item.locationName,
                        bookingRef = item.bookingRef,
                        displayOrder = index, // Repository returns sorted list
                        attachmentCount = item.attachments.size,
                        attachmentsJson = attachmentsJson,
                        providerDetailsJson = providerJson
                    )
                }
                
                // Fetch votes to update UI
                fetchUserVotes()
                
                itineraryDao.insertItems(entities)
            } catch (e: Exception) {
                _error.value = "Failed to sync itinerary: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Handles the movement of items in the list.
     * Updates local state immediately.
     */
    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return

        val item = items[fromIndex]
        
        // Determine target date from neighbors
        // We look at the item currently at toIndex (which will be shifted)
        val targetItem = items[toIndex]
        
        // If moving to a different date group, update the date
        // We use the date part of the startTime (ISO format yyyy-MM-ddTHH:mm:ss)
        val currentDate = item.startTime?.substringBefore("T")
        val targetDate = targetItem.startTime?.substringBefore("T")
        
        var updatedItem = item
        if (currentDate != targetDate && targetDate != null) {
            // Update the date, keeping the time if possible or defaulting to 09:00
            // For now, simpler: just use targetDate + "T09:00:00" if original was null, or keep original time
            val originalTime = item.startTime?.substringAfter("T", "09:00:00") ?: "09:00:00"
            val newStartTime = "${targetDate}T$originalTime"
            
            // Also update end time if present
            val newEndTime = if (item.endTime != null) {
                 val originalEndTime = item.endTime.substringAfter("T", "10:00:00")
                 "${targetDate}T$originalEndTime"
            } else null
            
            updatedItem = item.copy(startTime = newStartTime, endTime = newEndTime, isDirty = true)
        }

        items.apply {
            removeAt(fromIndex)
            add(toIndex, updatedItem)
        }
    }

    /**
     * Called when drag ends. Persists the order to Room and schedules network sync.
     */
    fun onDragEnd() {
        viewModelScope.launch {
            // 1. Update display order in memory to match current list position
            //    and capture any date changes (already in 'items' from onMove)
            val updatedEntities = items.mapIndexed { index, item ->
                if (item.displayOrder != index) {
                    item.copy(displayOrder = index, isDirty = true) 
                } else item
            }
            
            // 2. Atomically persist ALL changes (Dates + Orders) to Room
            //    'insertItems' with OnConflictStrategy.REPLACE functions as an upsert
            itineraryDao.insertItems(updatedEntities)
            
            // 3. Update the mutable list to reflect the persisted state (sanity check)
            //    This effectively clears the 'isDirty' flag if we re-fetched, but here we just want to ensure
            //    memory matches what we just sent to Room.
            //    However, to keep it simple and avoid fighting the Flow, we just wait for the Flow to emit.
            //    But we DO need to ensure 'items' has 'isDirty=true' so the Sync job picks them up?
            //    Room flow will emit the items we just inserted.
            
            // 4. Trigger Remote Sync
            scheduleRemoteSync()
        }
    }

    private fun scheduleRemoteSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(2500) // 2.5-second debounce as requested
            syncToSupabase()
        }
    }

    private suspend fun syncToSupabase() {
        try {
            // Send the final state to the cloud
            items.forEachIndexed { index, item ->
                // We must update order AND dates because onMove changes dates locally
                val updates = kotlinx.serialization.json.buildJsonObject {
                    put("sorting_index", index)
                    
                    // Only send date/time if valid
                    if (item.startTime != null) {
                        put("start_time", item.startTime)
                    }
                    if (item.endTime != null) {
                        put("end_time", item.endTime)
                    }
                }
                
                tripRepository.updateItineraryItem(item.id, updates)
            }
        } catch (e: Exception) {
            _error.value = "Sync failed: ${e.message}"
        }
    }
    
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                // Delete from remote
                tripRepository.deleteItineraryItem(itemId)
                
                // Delete from local
                itineraryDao.hardDeleteItem(itemId)
                
                // Update in-memory list
                items.removeIf { it.id == itemId }
                
                // Refresh to be safe (optional)
                // refreshItinerary()
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Soft delete the entire trip
     */
    fun deleteTrip() {
        viewModelScope.launch {
            try {
                // Logical delete on Supabase
                tripRepository.deleteTrip(tripId)
                // Immediate local cleanup
                tripDao.hardDeleteTrip(tripId)
            } catch (e: Exception) {
                _error.value = "Delete failed: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun getAttachmentUrl(path: String, onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Check if it's already a full URL
                if (path.startsWith("http")) {
                    onUrlReady(path)
                    return@launch
                }
                
                // Generate signed URL
                val url = documentRepository.getSignedUrl(path)
                onUrlReady(url)
            } catch (e: Exception) {
                _error.value = "Failed to open attachment: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun fetchUserVotes() {
        viewModelScope.launch {
            if (tripId.isEmpty()) return@launch

            repeat(3) { attempt ->
                try {
                    val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: run {
                        if (attempt < 2) delay(1000)
                        return@run null
                    }
                    
                    if (userId != null) {
                        // FIX: Fetch votes for the ENTIRE trip, independent of loaded items
                        val votes = tripRepository.getVotesForTrip(tripId)
                        
                        // 1. Map current user's votes
                        val myVotes = votes.filter { it.userId == userId }
                            .associate { it.itineraryItemId to it.voteType }
                        
                        userVotes.clear()
                        userVotes.putAll(myVotes)
                        
                        // 2. Aggregate counts
                        val aggregated = votes.groupBy { it.itineraryItemId }.mapValues { (_, itemVotes) ->
                            com.dash.travel.ui.screens.collaborative.VoteState(
                                upvotes = itemVotes.count { it.voteType == "up" },
                                downvotes = itemVotes.count { it.voteType == "down" }
                            )
                        }
                        voteCounts.clear()
                        voteCounts.putAll(aggregated)
                        return@launch
                    }
                } catch (e: Exception) {
                    if (attempt == 2) e.printStackTrace()
                }
                if (attempt < 2) delay(1000)
            }
        }
    }

    fun vote(itemId: String, voteType: String) {
        viewModelScope.launch {
            try {
                val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch
                
                // Optimistic update
                userVotes[itemId] = voteType
                
                tripRepository.castVote(itemId, userId, voteType)
                delay(500) // Small delay to ensure DB consistency before refresh
                // Refresh to get latest counts and sync
                fetchUserVotes()
            } catch (e: Exception) {
                _error.value = "Vote failed"
                refreshItinerary() // Revert state
            }
        }
    }

    /**
     * Fetch a new random image from Pixabay and update the trip
     */
    fun refreshTripImage() {
        viewModelScope.launch {
            try {
                val currentTrip = _trip.value ?: run {
                    android.util.Log.w("TripDetailVM", "refreshTripImage: No current trip")
                    return@launch
                }
                val destination = currentTrip.destinationData?.name ?: currentTrip.title ?: "Travel"
                android.util.Log.d("TripDetailVM", "Refreshing image for destination: $destination")
                
                var newImageUrl = imageRepository.fetchRandomImage(destination)
                
                // Fallback to "Travel" if no hits for the specific destination
                if (newImageUrl == null && destination != "Travel") {
                    newImageUrl = imageRepository.fetchRandomImage("Travel")
                }

                if (newImageUrl != null) {
                    // Append a URL fragment (#t={timestamp}) to bust Coil's cache without breaking the Pixabay URL
                    // The # fragment is not sent to the server (HTTP 400 avoided) but Coil uses the full URL string as a cache key.
                    val cacheBusterUrl = "$newImageUrl#t=${System.currentTimeMillis()}"
                    
                    android.util.Log.d("TripDetailVM", "Got new image URL: $cacheBusterUrl")
                    
                    // Update Supabase
                    val updates = kotlinx.serialization.json.buildJsonObject {
                        put("trip_image_url", cacheBusterUrl)
                    }
                    tripRepository.updateTrip(tripId, updates)
                    
                    // Update local state (Flow)
                    _trip.value = _trip.value?.copy(tripImageUrl = cacheBusterUrl)
                    
                    // Update local DB (Room) so the Home screen and cache reflect the new image
                    val currentTripEntity = tripDao.getTripById(tripId)
                    if (currentTripEntity != null) {
                        tripDao.updateTrip(currentTripEntity.copy(tripImageUrl = cacheBusterUrl))
                    }
                    
                    android.util.Log.d("TripDetailVM", "Updated trip with new image locally and remotely")
                } else {
                    android.util.Log.w("TripDetailVM", "fetchRandomImage returned null")
                }
            } catch (e: Exception) {
                android.util.Log.e("TripDetailVM", "Failed to refresh image", e)
            }
        }
    }
}
