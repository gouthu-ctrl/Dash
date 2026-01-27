package com.dash.travel.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationResult(
    val name: String,
    val placeId: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val type: LocationType,
    // New fields for UI state/context tracking
    val isRecent: Boolean = false,
    val isPopular: Boolean = false
) {
    enum class LocationType {
        CITY, AIRPORT, UNKNOWN
    }
}
