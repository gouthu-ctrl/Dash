package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.TripDao
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HomeViewModel(
    private val tripDao: TripDao
) : ViewModel() {

    val trips = mutableStateListOf<TripEntity>()
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            tripDao.getAllTrips().collectLatest { dbTrips ->
                // Sync logic: only update if the list has changed externally (e.g., initial load)
                // We avoid updating during a drag session to prevent flickering
                if (trips.size != dbTrips.size || trips.map { it.id } != dbTrips.map { it.id }) {
                    trips.clear()
                    trips.addAll(dbTrips)
                }
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
                SupabaseManager.client.postgrest.from("trips").update(buildJsonObject {
                    put("display_order", index)
                }) {
                    filter { eq("id", trip.id) }
                }
            }
        } catch (e: Exception) {
            // Silently fail or track for retry
        }
    }
}
