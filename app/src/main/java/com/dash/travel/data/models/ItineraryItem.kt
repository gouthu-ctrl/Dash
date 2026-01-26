package com.dash.travel.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ItineraryItem(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    val type: String, // flight, hotel, etc.
    val status: String = "confirmed",
    val title: String,
    val description: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("booking_ref") val bookingRef: String? = null,
    @SerialName("provider_details") val providerDetails: JsonObject? = null,
    @SerialName("estimated_cost") val estimatedCost: Double = 0.0,
    val currency: String = "USD",
    @SerialName("sorting_index") val sortingIndex: Double? = null
)
