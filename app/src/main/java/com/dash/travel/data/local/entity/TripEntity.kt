package com.dash.travel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity for Trips with sync metadata.
 * Maps to Supabase `trips` table.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val timezone: String = "UTC",
    val tripImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val visibility: String = "private", // private, public, friends
    val createdBy: String? = null,
    val displayOrder: Int = 0,
    
    // Destination data (flattened from JSON)
    val destinationName: String? = null,
    val destinationCountry: String? = null,
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    
    // Sync metadata
    val lastSyncedAt: Long? = null,  // Epoch millis when last synced from server
    val isDirty: Boolean = false,     // True if local changes need to be pushed
    val isDeleted: Boolean = false,   // Soft delete for pending sync
    val membershipStatus: String = "accepted", // pending, accepted, declined
    val updatedAt: Long = System.currentTimeMillis()
)
