package com.dash.travel.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneratePlanResponse(
    val success: Boolean,
    val items: List<GeneratedItem>? = null,
    val summary: String? = null,
    val error: String? = null
)

@Serializable
data class GeneratedItem(
    val type: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
    val currency: String? = null
)
