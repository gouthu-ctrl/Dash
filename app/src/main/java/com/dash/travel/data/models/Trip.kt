package com.dash.travel.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class Trip(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val timezone: String = "UTC",
    @SerialName("custom_attributes") val customAttributes: JsonObject? = null,
    @SerialName("destination_data") val destinationData: JsonElement? = null,
    @SerialName("origin_data") val originData: JsonElement? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("trip_image_url") val tripImageUrl: String? = null,
    @SerialName("display_order") val displayOrder: Int = 0,
    var placesCount: Int = 0
)

