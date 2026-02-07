package com.dash.travel.data.ai

import com.dash.travel.data.repository.PlanRequest
import com.dash.travel.data.model.GeneratePlanResponse
import com.dash.travel.data.model.GeneratedItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import kotlinx.serialization.json.Json

/**
 * Cloud Strategy: Uses Supabase Edge Function 'generate-plan'
 * which is configured to use Google Gemini 1.5 Flash.
 */
class CloudGeminiStrategy(private val supabase: SupabaseClient) : AIStrategy {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun isAvailable(): Boolean {
        return true
    }

    override suspend fun generatePlan(
        prompt: String,
        tripId: String?,
        preferences: UserPreferences?
    ): Result<GeneratePlanResponse> {
        return try {
            val request = PlanRequest(prompt, tripId, preferences)
            
            // Log payload for debugging
            val jsonBody = json.encodeToString(PlanRequest.serializer(), request)
            android.util.Log.d("CloudGemini", "Sending request payload: $jsonBody")

            // Invoke returns HttpResponse
            // Pass the object directly; SupabaseClient handles serialization via KotlinXSerializer
            val response = supabase.functions.invoke(
                function = "generate-plan",
                body = request, 
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            )
            
            val responseText = response.bodyAsText()
            val planResponse = json.decodeFromString<GeneratePlanResponse>(responseText)
            
            if (planResponse.success) {
                // Return the full response (items + summary)
                Result.success(planResponse)
            } else {
                Result.failure(Exception(planResponse.error ?: "Unknown AI error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
