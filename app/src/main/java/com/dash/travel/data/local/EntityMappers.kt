package com.dash.travel.data.local

import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.model.DestinationData
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.data.model.ItineraryStatus
import com.dash.travel.data.model.SupabaseTrip

/**
 * Mappers between Room entities and Supabase models
 */

// ==================== TRIP MAPPERS ====================

fun TripEntity.toSupabaseTrip(): SupabaseTrip = SupabaseTrip(
    id = this.id,
    createdBy = this.createdBy,
    title = this.title,
    description = this.description,
    startDate = this.startDate,
    endDate = this.endDate,
    timezone = this.timezone,
    destinationData = if (destinationName != null || destinationLat != null) {
        DestinationData(
            name = destinationName,
            country = destinationCountry,
            lat = destinationLat,
            lng = destinationLng
        )
    } else null,
    visibility = this.visibility,
    tripImageUrl = this.tripImageUrl
)

fun SupabaseTrip.toEntity(
    displayOrder: Int = 0,
    isDirty: Boolean = false,
    lastSyncedAt: Long? = System.currentTimeMillis()
): TripEntity = TripEntity(
    id = this.id ?: throw IllegalArgumentException("Trip ID required"),
    title = this.title,
    description = this.description,
    startDate = this.startDate,
    endDate = this.endDate,
    timezone = this.timezone,
    tripImageUrl = this.tripImageUrl,
    coverImageUrl = null, // Not in Supabase schema
    visibility = this.visibility ?: "private",
    createdBy = this.createdBy,
    displayOrder = displayOrder,
    destinationName = this.destinationData?.name,
    destinationCountry = this.destinationData?.country,
    destinationLat = this.destinationData?.lat,
    destinationLng = this.destinationData?.lng,
    lastSyncedAt = lastSyncedAt,
    isDirty = isDirty,
    isDeleted = false,
    updatedAt = System.currentTimeMillis()
)

// ==================== ITINERARY ITEM MAPPERS ====================

fun ItineraryItemEntity.toSupabaseItem(): ItineraryItem = ItineraryItem(
    id = this.id,
    tripId = this.tripId,
    type = this.type,
    status = try { 
        ItineraryStatus.valueOf(this.status.uppercase())
    } catch (e: Exception) { 
        ItineraryStatus.DRAFT 
    },
    title = this.title,
    description = this.description,
    startTime = this.startTime,
    endTime = this.endTime,
    locationName = this.locationName,
    locationLat = this.locationLat,
    locationLng = this.locationLng,
    estimatedCost = this.estimatedCost,
    currency = this.currency,
    bookingRef = this.bookingRef,
    sortingIndex = this.displayOrder
)

fun ItineraryItem.toEntity(
    isDirty: Boolean = false,
    lastSyncedAt: Long? = System.currentTimeMillis()
): ItineraryItemEntity = ItineraryItemEntity(
    id = this.id ?: throw IllegalArgumentException("Item ID required"),
    tripId = this.tripId,
    type = this.type,
    status = this.status.name.lowercase(),
    title = this.title,
    description = this.description,
    startTime = this.startTime,
    endTime = this.endTime,
    locationName = this.locationName,
    locationLat = this.locationLat,
    locationLng = this.locationLng,
    estimatedCost = this.estimatedCost,
    currency = this.currency,
    bookingRef = this.bookingRef,
    displayOrder = this.sortingIndex ?: 0,
    lastSyncedAt = lastSyncedAt,
    isDirty = isDirty,
    isDeleted = false,
    updatedAt = System.currentTimeMillis()
)
