package com.dash.travel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val tripImageUrl: String?,
    val displayOrder: Int = 0
)
