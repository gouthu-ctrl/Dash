package com.dash.travel.data.local.dao

import androidx.room.*
import com.dash.travel.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    
    // ==================== READ ====================
    
    @Query("SELECT * FROM trips WHERE isDeleted = 0 ORDER BY displayOrder ASC")
    fun getAllTrips(): Flow<List<TripEntity>>
    
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?
    
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripByIdFlow(tripId: String): Flow<TripEntity?>
    
    // ==================== WRITE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)
    
    @Query("UPDATE trips SET displayOrder = :order, isDirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun updateAllOrders(tripOrders: List<Pair<String, Int>>) {
        tripOrders.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }
    
    // ==================== DELETE ====================
    
    @Query("UPDATE trips SET isDeleted = 1, isDirty = 1, updatedAt = :now WHERE id = :tripId")
    suspend fun softDeleteTrip(tripId: String, now: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun hardDeleteTrip(tripId: String)
    
    @Query("DELETE FROM trips WHERE isDeleted = 1 AND isDirty = 0")
    suspend fun cleanupDeletedTrips()
    
    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()
    
    // ==================== SYNC ====================
    
    @Query("SELECT * FROM trips WHERE isDirty = 1")
    suspend fun getDirtyTrips(): List<TripEntity>
    
    @Query("UPDATE trips SET isDirty = 0, lastSyncedAt = :syncTime WHERE id = :tripId")
    suspend fun markSynced(tripId: String, syncTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE trips SET isDirty = 0, lastSyncedAt = :syncTime WHERE id IN (:tripIds)")
    suspend fun markAllSynced(tripIds: List<String>, syncTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM trips WHERE isDirty = 1")
    suspend fun getDirtyCount(): Int
}
