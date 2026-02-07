package com.dash.travel.data.repository

import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.dao.TripDao
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.local.toEntity
import com.dash.travel.data.local.toSupabaseItem
import com.dash.travel.data.local.toSupabaseTrip
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.data.model.SupabaseTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository that orchestrates offline-first data flow.
 * Reads from Room (local), writes to Both (local + Supabase when online).
 */
class SyncRepository(
    private val tripDao: TripDao,
    private val itineraryDao: ItineraryDao,
    private val tripRepository: TripRepository
) {
    
    // ==================== TRIPS ====================
    
    /**
     * Get all trips - reads from local Room database.
     * Call refreshTrips() separately to sync with server.
     */
    fun getTripsOfflineFirst(): Flow<List<TripEntity>> {
        return tripDao.getAllTrips()
    }
    
    /**
     * Get a single trip by ID
     */
    fun getTripById(tripId: String): Flow<TripEntity?> {
        return tripDao.getTripByIdFlow(tripId)
    }
    
    /**
     * Pull trips from Supabase and store in Room.
     * This is the "pull" part of sync.
     */
    suspend fun refreshTrips() {
        try {
            val remoteTrips = tripRepository.getTrips()
            val existingTrips = tripDao.getAllTrips().first()
            val existingIds = existingTrips.map { it.id }.toSet()
            
            // Convert and insert, preserving local display order if exists
            // Convert and insert, but conflict strategy: LOCAL WINS
            // If a local trip is dirty, do NOT overwrite it with remote data.
            val entities = remoteTrips.mapIndexedNotNull { index, trip ->
                val localTrip = existingTrips.find { it.id == trip.id }
                
                if (localTrip != null && localTrip.isDirty) {
                    // Local changes exist. Skip overwriting.
                    return@mapIndexedNotNull null
                }

                val existingOrder = localTrip?.displayOrder ?: index
                trip.toEntity(displayOrder = existingOrder)
            }
            
            if (entities.isNotEmpty()) {
                tripDao.insertTrips(entities)
            }
        } catch (e: Exception) {
            // Network error - continue with local data
            e.printStackTrace()
        }
    }
    
    /**
     * Create or update a trip locally (marks as dirty for sync)
     */
    suspend fun saveTrip(trip: TripEntity) {
        tripDao.insertTrip(trip.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Delete a trip (soft delete, will sync later)
     */
    suspend fun deleteTrip(tripId: String) {
        tripDao.softDeleteTrip(tripId)
    }
    
    /**
     * Push dirty trips to Supabase.
     * This is the "push" part of sync.
     */
    suspend fun pushDirtyTrips() {
        val dirtyTrips = tripDao.getDirtyTrips()
        
        for (trip in dirtyTrips) {
            try {
                if (trip.isDeleted) {
                    // Delete from server
                    tripRepository.deleteTrip(trip.id)
                    tripDao.hardDeleteTrip(trip.id)
                } else {
                    // Upsert to server
                    val supabaseTrip = trip.toSupabaseTrip()
                    tripRepository.upsertTrip(supabaseTrip)
                    tripDao.markSynced(trip.id)
                }
            } catch (e: Exception) {
                // Keep as dirty for retry
                e.printStackTrace()
            }
        }
    }
    
    // ==================== ITINERARY ITEMS ====================
    
    /**
     * Get itinerary items for a trip - reads from local.
     */
    fun getItineraryOfflineFirst(tripId: String): Flow<List<ItineraryItemEntity>> {
        return itineraryDao.getItineraryForTrip(tripId)
    }
    
    /**
     * Pull itinerary items from Supabase for a trip.
     */
    suspend fun refreshItinerary(tripId: String) {
        try {
            val remoteItems = tripRepository.getItineraryItems(tripId)
            val existingItems = itineraryDao.getItineraryForTrip(tripId).first()
            
            val entities = remoteItems.mapIndexedNotNull { index, item: ItineraryItem ->
                val localItem = existingItems.find { it.id == item.id }
                
                if (localItem != null && localItem.isDirty) {
                    return@mapIndexedNotNull null
                }

                val existingOrder = localItem?.displayOrder ?: index
                item.toEntity().copy(displayOrder = existingOrder)
            }
            
            if (entities.isNotEmpty()) {
                itineraryDao.insertItems(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Save an itinerary item locally (marks as dirty)
     */
    suspend fun saveItineraryItem(item: ItineraryItemEntity) {
        itineraryDao.insertItem(item.copy(isDirty = true, updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Delete an itinerary item (soft delete)
     */
    suspend fun deleteItineraryItem(itemId: String) {
        itineraryDao.softDeleteItem(itemId)
    }
    
    /**
     * Push dirty itinerary items to Supabase.
     */
    suspend fun pushDirtyItems() {
        val dirtyItems = itineraryDao.getDirtyItems()
        
        for (item in dirtyItems) {
            try {
                if (item.isDeleted) {
                    tripRepository.deleteItem(item.id)
                    itineraryDao.hardDeleteItem(item.id)
                } else {
                    val supabaseItem = item.toSupabaseItem()
                    tripRepository.upsertItem(supabaseItem)
                    itineraryDao.markSynced(item.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ==================== SYNC STATUS ====================
    
    /**
     * Check if there are pending changes to sync
     */
    suspend fun hasPendingChanges(): Boolean {
        return tripDao.getDirtyCount() > 0 || itineraryDao.getDirtyCount() > 0
    }
    
    /**
     * Full sync: push local changes, then pull remote updates
     */
    suspend fun sync() {
        // Push first (to avoid overwriting local changes)
        pushDirtyTrips()
        pushDirtyItems()
        
        // Then pull
        refreshTrips()
        
        // For items, we'd need to refresh each trip
        // This could be optimized with a "modified since" query
    }
    
    /**
     * Cleanup deleted records that have been synced
     */
    suspend fun cleanup() {
        tripDao.cleanupDeletedTrips()
        itineraryDao.cleanupDeletedItems()
    }
}
