package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.TripDao
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.model.MemberStatus
import com.dash.travel.data.model.TripWithMembership
import com.dash.travel.data.repository.TripRepository
import com.dash.travel.data.repository.ProfileRepository
import com.dash.travel.data.model.Profile
import com.dash.travel.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

class HomeViewModel(
    private val tripDao: TripDao,
    private val tripRepository: TripRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val trips = mutableStateListOf<TripEntity>()
    private var syncJob: Job? = null
    
    // Loading state for initial fetch
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _userProfile = MutableStateFlow<Profile?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        // Observe local database
        viewModelScope.launch {
            tripDao.getAllTrips().collectLatest { dbTrips ->
                trips.clear()
                trips.addAll(dbTrips)
            }
        }
        
        // Initial sync from cloud
        refreshTrips()
        
        // Fetch User Profile
        fetchUserProfile()
        
        // Listen for Realtime Invitations
        subscribeToInvitations()
    }

    private fun subscribeToInvitations() {
        viewModelScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                val userId = user?.id ?: return@launch
                
                val channel = SupabaseManager.client.channel("trip_invites_$userId")
                
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "trip_members"
                    filter(FilterOperation("user_id", FilterOperator.EQ, userId))
                }

                channel.subscribe()

                changes.collect { action ->
                    if (action is PostgresAction.Insert) {
                        // New invitation received! Refresh trips.
                        refreshTrips()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            repeat(3) { attempt ->
                try {
                    val user = SupabaseManager.client.auth.currentUserOrNull()
                    val userId = user?.id
                    val email = user?.email
                    
                    if (userId != null) {
                        _userProfile.value = profileRepository.getProfile(userId)
                        
                        if (email != null) {
                            val normalizedEmail = email.trim().lowercase()
                            tripRepository.linkInvitedMember(normalizedEmail, userId)
                            delay(1000) // Give DB a moment to link
                            refreshTrips()
                            return@launch // Success
                        }
                    }
                } catch (e: Exception) {
                   if (attempt == 2) e.printStackTrace()
                }
                delay(2000) // Wait before retry
            }
        }
    }

    /**
     * Fetch latest trips with membership status from Supabase and update Room
     */
    fun refreshTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch trips with membership status
                val tripsWithStatus = tripRepository.getTripsWithMembership()
                
                // FAILSAFE: Fetch owned trips to catch any where trip_members trigger lagged/failed
                val ownedTrips = tripRepository.getOwnedTrips()
                
                // Merge strategies
                val mergedTripsMap = tripsWithStatus.associateBy { it.trip.id }.toMutableMap()
                
                ownedTrips.forEach { ownedTrip ->
                    val id = ownedTrip.id ?: return@forEach
                    if (!mergedTripsMap.containsKey(id)) {
                        // User owns this trip but has no member record -> Assume OWNER/ACCEPTED
                        mergedTripsMap[id] = TripWithMembership(
                            trip = ownedTrip,
                            status = MemberStatus.ACCEPTED
                        )
                    }
                }
                
                // Map to Room entities
                val entities = mergedTripsMap.values.mapIndexed { index, item ->
                    val trip = item.trip
                    TripEntity(
                        id = trip.id ?: "",
                        title = trip.title,
                        description = trip.description,
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                        tripImageUrl = trip.tripImageUrl,
                        displayOrder = trip.displayOrder ?: index,
                        membershipStatus = item.status.name.lowercase()
                    )
                }
                
                // Update local DB
                tripDao.insertTrips(entities)
            } catch (e: Exception) {
                _error.value = "Failed to sync trips: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Accept a pending trip invitation
     */
    /**
     * Accept a pending trip invitation
     */
    fun acceptTrip(tripId: String) {
        viewModelScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                val userId = user?.id ?: return@launch
                val email = user.email ?: return@launch
                
                tripRepository.acceptTripInvitation(tripId, userId, email)
                refreshTrips()
            } catch (e: Exception) {
                _error.value = "Failed to accept trip: ${e.message}"
            }
        }
    }

    /**
     * Reject a pending trip invitation
     */
    fun rejectTrip(tripId: String) {
        viewModelScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                val userId = user?.id ?: return@launch
                val email = user.email ?: return@launch
                
                tripRepository.rejectTripInvitation(tripId, userId, email)
                
                // Locally remove it immediately for better UX
                tripDao.hardDeleteTrip(tripId)
                
                refreshTrips()
            } catch (e: Exception) {
                _error.value = "Failed to reject trip: ${e.message}"
            }
        }
    }

    /**
     * Handles the visual swapping of items for zero-latency UI.
     */
    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in trips.indices || toIndex !in trips.indices) return
        trips.apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }
    }

    /**
     * Persists the final order to Room immediately upon drop and schedules a debounced cloud sync.
     */
    fun onDrop() {
        val updatedOrders = trips.mapIndexed { index, trip ->
            trip.id to index
        }

        // Local Room persistence (Immediate)
        viewModelScope.launch {
            tripDao.updateAllOrders(updatedOrders)
        }

        // Debounced Supabase Sync (2.5s delay)
        scheduleRemoteSync()
    }

    private fun scheduleRemoteSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(2500)
            syncToSupabase()
        }
    }

    private suspend fun syncToSupabase() {
        try {
            trips.forEachIndexed { index, trip ->
                tripRepository.updateTripOrder(trip.id, index)
            }
        } catch (e: Exception) {
            _error.value = "Failed to sync order: ${e.message}"
        }
    }
}
