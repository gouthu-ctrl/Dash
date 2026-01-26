package com.dash.travel.data.local.dao

import androidx.room.*
import com.dash.travel.data.local.entity.ItineraryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {
    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY displayOrder ASC")
    fun getItineraryForTrip(tripId: String): Flow<List<ItineraryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItineraryItemEntity>)

    @Query("UPDATE itinerary_items SET displayOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Transaction
    suspend fun updateAllOrders(items: List<Pair<String, Int>>) {
        items.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }
}
