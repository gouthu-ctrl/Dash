package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.remote.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TripDetailViewModel(
    private val tripId: String,
    private val itineraryDao: ItineraryDao
) : ViewModel() {

    // Using mutableStateListOf for optimized, zero-latency UI updates
    val items = mutableStateListOf<ItineraryItemEntity>()
    
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            itineraryDao.getItineraryForTrip(tripId).collectLatest { dbItems ->
                // Only update if not currently dragging to avoid visual jumps
                // In a production app, you might use a flag or separate states
                if (items.size != dbItems.size || items.map { it.id } != dbItems.map { it.id }) {
                    items.clear()
                    items.addAll(dbItems)
                }
            }
        }
    }

    /**
     * Handles the movement of items in the list.
     * Updates local state immediately and persists to Room.
     */
    fun onMove(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in items.indices || toIndex !in items.indices) return

        items.apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }

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
                SupabaseManager.client.postgrest.from("itinerary_items").update(buildJsonObject {
                    put("sorting_index", index.toDouble())
                }) {
                    filter { eq("id", item.id) }
                }
            }
        } catch (e: Exception) {
            // Silently handle sync errors or expose via a UI state for a "Sync Failed" indicator
        }
    }
}
