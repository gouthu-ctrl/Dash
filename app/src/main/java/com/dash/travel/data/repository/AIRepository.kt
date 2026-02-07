package com.dash.travel.data.repository

import com.dash.travel.data.ai.CloudGeminiStrategy
import com.dash.travel.data.ai.OnDeviceGeminiStrategy
import com.dash.travel.data.ai.UserPreferences
import com.dash.travel.data.model.AIConversation
import com.dash.travel.data.model.AIMessage
import com.dash.travel.data.model.GeneratePlanResponse
import com.dash.travel.data.model.GeneratedItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json

/**
 * Repository for AI Conversation persistence with Supabase
 */
class AIRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "ai_conversations"
        private const val GENERATE_PLAN_FUNCTION_NAME = "generate-plan"
    }

    /**
     * Get all conversations for a specific trip
     */
    suspend fun getConversationsForTrip(tripId: String): List<AIConversation> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("trip_id", tripId) }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get a specific conversation by ID
     */
    suspend fun getConversationById(conversationId: String): AIConversation? {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("id", conversationId) }
            }
            .decodeSingleOrNull()
    }

    /**
     * Create a new conversation
     */
    suspend fun createConversation(tripId: String, initialMessages: List<AIMessage> = emptyList()): AIConversation {
        // Supabase will handle ID generation and user_id via default/RLS if configured, 
        // but we pass what we can. 
        // Note: storage of user_id depends on RLS policies usually defaulting to auth.uid() 
        // or passed explicitly if table allows.
        
        val conversation = AIConversation(
            tripId = tripId,
            messages = initialMessages
        )
        
        return supabase.from(TABLE_NAME)
            .insert(conversation) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update conversation messages (append new message)
     */
    suspend fun updateConversationMessages(conversationId: String, messages: List<AIMessage>) {
        // We update the entire jsonb array or append. 
        // For simplicity, we'll replace the list with the updated full history 
        // or whatever the client provides.
        
        val updates = mapOf(
            "messages" to messages,
            "updated_at" to java.time.Instant.now().toString()
        )
        
        supabase.from(TABLE_NAME)
            .update(updates) {
                filter { eq("id", conversationId) }
            }
    }

    /**
     * Delete a conversation
     */
    suspend fun deleteConversation(conversationId: String) {
        supabase.from(TABLE_NAME)
            .delete {
                filter { eq("id", conversationId) }
            }
    }
    
    /**
     * Generate a trip plan using AI (calls Supabase Edge Function)
     * 
     * NOTE: Edge Function integration pending SDK version verification.
     * Currently returns a placeholder response.
     * 
     * @param prompt e.g. "3 days in Kyoto, food + temples, budget $800"
     * @param tripId optional - if provided, items will be added directly to this trip
     * @param preferences user preferences (pace, interests, dietary)
     * @return PlanResponse with generated items
     */
    // Strategies
    private val cloudStrategy = com.dash.travel.data.ai.CloudGeminiStrategy(supabase)
    private val onDeviceStrategy = com.dash.travel.data.ai.OnDeviceGeminiStrategy()

    /**
     * Generate a trip plan using Hybrid AI Strategy:
     * 1. Try On-Device (Gemini Nano) if available (Fast, Private, Free).
     * 2. Fallback to Cloud (Gemini Flash) via Supabase Edge Function (Cheap/Free tier).
     */
    suspend fun generateTripPlan(
        prompt: String,
        tripId: String? = null,
        preferences: com.dash.travel.data.ai.UserPreferences? = null
    ): GeneratePlanResponse {
        
        val strategy = if (onDeviceStrategy.isAvailable()) {
            println("AIRepository: Using On-Device Gemini Nano Strategy")
            onDeviceStrategy
        } else {
            println("AIRepository: Using Cloud Gemini Flash Strategy via Supabase Edge Function")
            cloudStrategy
        }
        
        val result = strategy.generatePlan(prompt, tripId, preferences)
        
        return result.getOrElse { e ->
            GeneratePlanResponse(
                success = false,
                error = e.message ?: "Unknown AI error"
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class PlanRequest(
    val prompt: String,
    val tripId: String? = null,
    val preferences: UserPreferences? = null
)

/**
 * Response from generate-plan Edge Function
 */

