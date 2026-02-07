package com.dash.travel.data.ai

import com.dash.travel.data.model.GeneratePlanResponse
import kotlinx.serialization.Serializable

/**
 * Strategy interface for AI Generation
 * Allows switching between Cloud (Gemini Flash) and On-Device (Gemini Nano)
 */
interface AIStrategy {
    suspend fun isAvailable(): Boolean
    suspend fun generatePlan(
        prompt: String,
        tripId: String? = null,
        preferences: UserPreferences? = null
    ): Result<GeneratePlanResponse>
}

@Serializable
data class UserPreferences(
    val pace: String? = null,
    val interests: List<String>? = null,
    val dietary: List<String>? = null
)
