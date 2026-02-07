package com.dash.travel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dash.travel.data.local.dao.TripDao
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.dao.RecentLocationsDao
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.local.entity.RecentLocationEntity

@Database(
    entities = [
        TripEntity::class, 
        ItineraryItemEntity::class,
        RecentLocationEntity::class
    ], 
    version = 6, 
    exportSchema = false
)
abstract class DashDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun itineraryDao(): ItineraryDao
    abstract fun recentLocationsDao(): RecentLocationsDao
}
