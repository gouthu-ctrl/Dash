package com.dash.travel.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dash.travel.data.local.entity.RecentLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentLocationsDao {
    @Query("SELECT * FROM recent_locations ORDER BY timestamp DESC LIMIT 5")
    fun getRecentLocations(): Flow<List<RecentLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: RecentLocationEntity)
}
