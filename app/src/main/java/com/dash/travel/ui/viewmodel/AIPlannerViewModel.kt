package com.dash.travel.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.AIMessage

import com.dash.travel.data.repository.AIRepository
import com.dash.travel.data.model.GeneratedItem
import com.dash.travel.ui.screens.ai.ChatMessage
import com.dash.travel.ui.screens.ai.MessageRole as UiMessageRole
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for AI Trip Planner
 */
class AIPlannerViewModel(
    private val tripId: String? = null,
    private val aiRepository: AIRepository
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    val isLoading = mutableStateOf(false)
    val generatedItems = mutableStateListOf<GeneratedItem>() // For displaying generated plan
    private var conversationId: String? = null

    init {
        // Load history if tripId is present
        if (tripId != null) {
            viewModelScope.launch {
                loadHistory()
            }
        }
    }

    private suspend fun loadHistory() {
        try {
            if (tripId == null) return
            val conversations = aiRepository.getConversationsForTrip(tripId)
            val latest = conversations.firstOrNull()
            
            if (latest != null) {
                conversationId = latest.id
                messages.clear()
                messages.addAll(latest.messages.map { 
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = if (it.role == "user") UiMessageRole.USER else UiMessageRole.ASSISTANT,
                        content = it.content
                    )
                })
            }
        } catch (e: Exception) {
            // Handle load error
            e.printStackTrace()
        }
    }

    fun sendMessage(content: String) {
        // UI Update - add user message immediately
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = UiMessageRole.USER,
            content = content
        )
        messages.add(userMessage)

        viewModelScope.launch {
            isLoading.value = true
            try {
                // Call the Edge Function via repository
                val response = aiRepository.generateTripPlan(
                    prompt = content,
                    tripId = tripId,
                    preferences = null // TODO: Get from user profile
                )
                
                val assistantContent = if (response.success && !response.items.isNullOrEmpty()) {
                    // Store generated items for UI display
                    generatedItems.clear()
                    generatedItems.addAll(response.items)
                    
                    // Create a summary message
                    buildString {
                        append("I've generated a plan with ${response.items.size} recommendations for you:\n\n")
                        response.items.take(5).forEachIndexed { index, item ->
                            append("${index + 1}. **${item.title}**")
                            item.locationName?.let { append(" in $it") }
                            append("\n")
                        }
                        if (response.items.size > 5) {
                            append("...and ${response.items.size - 5} more items.\n")
                        }
                        append("\nWould you like me to add these to your trip itinerary?")
                    }
                } else {
                    // Fallback to summary or error
                    response.summary ?: response.error ?: "I couldn't generate a plan. Please try again."
                }
                
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = UiMessageRole.ASSISTANT,
                    content = assistantContent
                )
                messages.add(assistantMessage)
                
                // Persist conversation
                persistConversation()
                
            } catch (e: Exception) {
                // Error handling
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = UiMessageRole.ASSISTANT,
                    content = "Sorry, I encountered an error: ${e.message}"
                )
                messages.add(errorMessage)
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }
    
    private suspend fun persistConversation() {
        if (tripId == null) return
        
        val dbMessages = messages.map {
            AIMessage(
                role = if (it.role == UiMessageRole.USER) "user" else "assistant",
                content = it.content
            )
        }
        
        try {
            if (conversationId == null) {
                // Create new
                val newConv = aiRepository.createConversation(tripId, dbMessages)
                conversationId = newConv.id
            } else {
                // Update existing
                aiRepository.updateConversationMessages(conversationId!!, dbMessages)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Apply generated items to the trip itinerary
     */
    fun applyGeneratedPlan() {
        // Items are already added by the Edge Function if tripId was provided
        // This can trigger a refresh or navigate to the itinerary view
        generatedItems.clear()
    }
    
    /**
     * Clear current generated items (user rejected the plan)
     */
    fun clearGeneratedPlan() {
        generatedItems.clear()
    }
}

