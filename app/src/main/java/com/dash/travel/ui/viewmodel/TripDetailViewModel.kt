package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.repository.TripRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dash.travel.data.model.SupabaseTrip
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.dash.travel.data.model.ItineraryAttachment
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

import com.dash.travel.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import com.dash.travel.data.repository.DocumentRepository

class TripDetailViewModel(
    private val tripId: String,
    private val itineraryDao: ItineraryDao,
    private val tripRepository: TripRepository,
    private val documentRepository: DocumentRepository
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

        items.apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
    }

    /**
     * Called when drag ends. Persists the order to Room and schedules network sync.
     */
    fun onDragEnd() {
        // Update display orders locally
        val updatedOrders = items.mapIndexed { index, item ->
            item.id to index
        }

        // Immediate persistence to Room (Local-First)
        viewModelScope.launch {
            itineraryDao.updateAllOrders(updatedOrders)
        }

        // Debounced remote sync (Google Weather pattern)
        scheduleRemoteSync()
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
                tripRepository.updateItemSortingIndex(item.id, index.toDouble())
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
}
