package com.dash.travel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "itinerary_items")
data class ItineraryItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val type: String,
    val title: String,
    val startTime: String?,
    val locationName: String?,
    val bookingRef: String?,
    val displayOrder: Int = 0
)
