package com.dash.travel.data.local.dao

import androidx.room.*
import com.dash.travel.data.local.entity.ItineraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {
    
    // ==================== READ ====================
    
    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId AND isDeleted = 0 ORDER BY displayOrder ASC")
    fun getItineraryForTrip(tripId: String): Flow<List<ItineraryItemEntity>>
    
    @Query("SELECT * FROM itinerary_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): ItineraryItemEntity?
    
    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId AND status = :status AND isDeleted = 0")
    suspend fun getItemsByStatus(tripId: String, status: String): List<ItineraryItemEntity>
    
    // ==================== WRITE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItineraryItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItineraryItemEntity>)

    @Update
    suspend fun updateItem(item: ItineraryItemEntity)
    
    @Query("UPDATE itinerary_items SET displayOrder = :order, isDirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun updateAllOrders(items: List<Pair<String, Int>>) {
        items.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }
    
    // ==================== DELETE ====================
    
    @Query("UPDATE itinerary_items SET isDeleted = 1, isDirty = 1, updatedAt = :now WHERE id = :itemId")
    suspend fun softDeleteItem(itemId: String, now: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun hardDeleteItem(itemId: String)
    
    @Query("DELETE FROM itinerary_items WHERE isDeleted = 1 AND isDirty = 0")
    suspend fun cleanupDeletedItems()
    
    // ==================== SYNC ====================
    
    @Query("SELECT * FROM itinerary_items WHERE isDirty = 1")
    suspend fun getDirtyItems(): List<ItineraryItemEntity>
    
    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId AND isDirty = 1")
    suspend fun getDirtyItemsForTrip(tripId: String): List<ItineraryItemEntity>
    
    @Query("UPDATE itinerary_items SET isDirty = 0, lastSyncedAt = :syncTime WHERE id = :itemId")
    suspend fun markSynced(itemId: String, syncTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE itinerary_items SET isDirty = 0, lastSyncedAt = :syncTime WHERE id IN (:itemIds)")
    suspend fun markAllSynced(itemIds: List<String>, syncTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM itinerary_items WHERE isDirty = 1")
    suspend fun getDirtyCount(): Int
}
