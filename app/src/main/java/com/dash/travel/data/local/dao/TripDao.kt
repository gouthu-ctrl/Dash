package com.dash.travel.data.local.dao

import androidx.room.*
import com.dash.travel.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY displayOrder ASC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("UPDATE trips SET displayOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: String, order: Int)

    @Transaction
    suspend fun updateAllOrders(tripOrders: List<Pair<String, Int>>) {
        tripOrders.forEach { (id, order) ->
            updateOrder(id, order)
        }
    }
}
