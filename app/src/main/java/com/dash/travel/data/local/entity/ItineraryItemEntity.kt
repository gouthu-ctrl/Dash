package com.dash.travel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity for Itinerary Items with sync metadata.
 * Maps to Supabase `itinerary_items` table.
 */
@Entity(
    tableName = "itinerary_items",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class ItineraryItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val type: String, // activity, eat, stay, transport
    val status: String = "draft", // draft, confirmed, proposed, archived
    val title: String,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val estimatedCost: Double = 0.0,
    val currency: String = "USD",
    val bookingRef: String? = null,
    val displayOrder: Int = 0,
    val attachmentCount: Int = 0,
    val attachmentsJson: String? = null,
    val providerDetailsJson: String? = null,
    
    // Sync metadata
    val lastSyncedAt: Long? = null,
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
