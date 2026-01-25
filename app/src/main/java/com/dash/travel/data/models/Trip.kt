package com.dash.travel.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Trip(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("destination_data") val destinationData: JsonElement? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("trip_image_url") val tripImageUrl: String? = null,
    var placesCount: Int = 0 // Derived or fetched separately
)
