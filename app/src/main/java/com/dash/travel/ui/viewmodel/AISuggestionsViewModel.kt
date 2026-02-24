package com.dash.travel.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.model.GeneratedItem
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.data.model.ItineraryStatus
import com.dash.travel.data.repository.AIRepository
import com.dash.travel.data.repository.TripRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for AI Suggestions in TripDetailScreen
 * 
 * Provides a lightweight, focused interface for generating AI suggestions
 * and adding them to the current trip itinerary.
 */
class AISuggestionsViewModel(
    private val tripId: String,
    private val aiRepository: AIRepository,
    private val tripRepository: TripRepository,
    private val itineraryDao: ItineraryDao
) : ViewModel() {

    companion object {
        private const val TAG = "AISuggestionsVM"
        private const val PAGE_SIZE = 10
    }

    // UI State
    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data object LoadingMore : UiState()
        data class Success(val items: List<GeneratedItem>, val hasMore: Boolean) : UiState()
        data class Error(val message: String) : UiState()
    }

    val uiState = mutableStateOf<UiState>(UiState.Idle)
    val suggestions = mutableStateListOf<GeneratedItem>()
    
    // Track items being added (for loading states on individual cards)
    val addingItems = mutableStateOf<Set<String>>(emptySet())
    
    // Location context for suggestions
    val currentLocation = mutableStateOf("")
    
    // Store all items for pagination
    private var allGeneratedItems = listOf<GeneratedItem>()
    private var displayedCount = 0
    private var lastPrompt = ""

    /**
     * Initialize location from trip data
     * Priority: Last itinerary item location > Trip destination
     */
    fun initializeLocation(tripDestination: String?, lastItineraryLocation: String?) {
        currentLocation.value = lastItineraryLocation?.takeIf { it.isNotBlank() }
            ?: tripDestination?.takeIf { it.isNotBlank() }
            ?: ""
    }

    /**
     * Update location manually
     */
    fun updateLocation(location: String) {
        currentLocation.value = location
    }

    /**
     * Generate AI suggestions based on user prompt
     * @param prompt The user's request (e.g., "What to do tomorrow?", "Food nearby")
     */
    fun generateSuggestions(prompt: String) {
        if (prompt.isBlank()) return
        
        lastPrompt = prompt
        
        viewModelScope.launch {
            uiState.value = UiState.Loading
            
            try {
                // Build strict prompt with guardrails
                val systemGuardrail = "You are a travel assistant for the Dash Trip Planner app. " +
                        "Your ONLY purpose is to generate travel itinerary items (flights, hotels, activities, food, shopping, groceries, etc.). " +
                        "Generate a diverse list of exactly 10 suggestions. " +
                        "Each item MUST include a valid 'location_lat' and 'location_lng' if a specific location is identified. " +
                        "If the user's request is NOT related to travel, tourism, or exploring a location (including shopping and essentials), " +
                        "you must return an empty list of items. " +
                        "Landmarks, history, culture, and local attractions are all valid travel topics. " +
                        "Do not answer general knowledge questions, write code, or do math unless it's related to travel costs."

                val locationContext = if (currentLocation.value.isNotBlank()) "I am visiting ${currentLocation.value}." else ""
                
                val finalPrompt = "$systemGuardrail\n\nContext: $locationContext\nUser Request: $prompt"
                
                Log.d(TAG, "Generating suggestions with constrained prompt: $finalPrompt")
                
                val response = aiRepository.generateTripPlan(
                    prompt = finalPrompt,
                    tripId = null, // Don't auto-add; user will select items to add
                    preferences = null
                )

                
                if (response.success && !response.items.isNullOrEmpty()) {
                    allGeneratedItems = response.items
                    displayedCount = minOf(PAGE_SIZE, allGeneratedItems.size)
                    
                    suggestions.clear()
                    suggestions.addAll(allGeneratedItems.take(displayedCount))
                    
                    val hasMore = displayedCount < allGeneratedItems.size
                    uiState.value = UiState.Success(suggestions.toList(), hasMore)
                    
                    Log.d(TAG, "Generated ${allGeneratedItems.size} items, showing $displayedCount")
                } else {
                    Log.e(TAG, "No suggestions returned: ${response.error}")
                    val errorMessage = response.error ?: "I couldn't find any travel suggestions for that. Try asking about places to visit, food, or activities."
                    uiState.value = UiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating suggestions", e)
                uiState.value = UiState.Error(
                    e.message ?: "Failed to generate suggestions"
                )
            }
        }
    }

    /**
     * Load more suggestions (pagination)
     */
    fun loadMore() {
        if (displayedCount >= allGeneratedItems.size) return
        
        val previousState = uiState.value
        uiState.value = UiState.LoadingMore
        
        viewModelScope.launch {
            val newCount = minOf(displayedCount + PAGE_SIZE, allGeneratedItems.size)
            val newItems = allGeneratedItems.subList(displayedCount, newCount)
            
            suggestions.addAll(newItems)
            displayedCount = newCount
            
            val hasMore = displayedCount < allGeneratedItems.size
            uiState.value = UiState.Success(suggestions.toList(), hasMore)
            
            Log.d(TAG, "Loaded more: now showing $displayedCount of ${allGeneratedItems.size}")
        }
    }

    /**
     * Add a generated suggestion to the trip itinerary
     * @param item The suggestion to add
     * @param onSuccess Callback when item is successfully added
     */
    fun addToItinerary(item: GeneratedItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // Track loading state for this item
            addingItems.value = addingItems.value + item.title
            
            try {
                // Map AI type to valid App type
                val mappedType = mapToAppType(item.type)
                
                // Convert GeneratedItem to ItineraryItem for saving
                // NOTE: Do NOT use AI-generated dates - they are often in the past
                // Leave dates null so user can set them when editing the item
                // Fetch trip to get start date for default time
                val trip = tripRepository.getTripById(tripId)
                val defaultDate = trip?.startDate?.substringBefore("T") 
                    ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                val defaultStartTime = "${defaultDate}T09:00:00"
                
                val userNote = "\n\n(Added by AI to first day at 9:00 AM. Please edit or drag to reschedule.)"

                val itineraryItem = ItineraryItem(
                    tripId = tripId,
                    type = mappedType,
                    status = ItineraryStatus.PROPOSED, // AI-generated items start as proposed
                    title = item.title,
                    description = (item.description ?: "") + userNote,
                    startTime = defaultStartTime, // Default to 9:00 AM on first day
                    endTime = null,
                    locationName = item.locationName,
                    locationLat = item.locationLat,
                    locationLng = item.locationLng,
                    estimatedCost = item.estimatedCost ?: 0.0,
                    currency = item.currency ?: "USD"
                )
                
                val createdItem = tripRepository.createItineraryItem(itineraryItem)
                Log.d(TAG, "Successfully created itinerary item: ${createdItem.id} with type: $mappedType")
                
                // Sync to local Room database immediately for edit functionality
                val entity = ItineraryItemEntity(
                    id = createdItem.id ?: "",
                    tripId = createdItem.tripId,
                    type = createdItem.type,
                    status = createdItem.status.name.lowercase(),
                    title = createdItem.title,
                    description = createdItem.description,
                    startTime = createdItem.startTime,
                    endTime = createdItem.endTime,
                    locationName = createdItem.locationName,
                    locationLat = createdItem.locationLat,
                    locationLng = createdItem.locationLng,
                    estimatedCost = createdItem.estimatedCost,
                    currency = createdItem.currency,
                    bookingRef = createdItem.bookingRef,
                    displayOrder = 0,
                    isDirty = false,
                    lastSyncedAt = System.currentTimeMillis()
                )
                itineraryDao.insertItem(entity)
                Log.d(TAG, "Synced item to Room: ${entity.id}")
                
                // Remove from suggestions list (already added)
                suggestions.removeIf { it.title == item.title }
                allGeneratedItems = allGeneratedItems.filter { it.title != item.title }
                
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add item to itinerary", e)
                // Could show error toast via callback
            } finally {
                addingItems.value = addingItems.value - item.title
            }
        }
    }

    /**
     * Maps raw AI-generated types to valid App types
     * defined in ItineraryUtils.kt
     */
    private fun mapToAppType(rawType: String): String {
        val normalized = rawType.trim().lowercase()
        return when {
            // Food
            normalized in listOf("food", "dinner", "lunch", "breakfast", "cafe", "meal", "eat", "dining") -> "restaurant"
            
            // Shopping / Groceries (Map to sightseeing as generic 'place to visit')
            normalized in listOf("shopping", "shop", "store", "mall", "market", "grocery", "groceries", "supermarket") -> "sightseeing"
            
            // Sightseeing / Activity
            normalized in listOf("activity", "visit", "attraction", "place", "spot", "location", "view", "park", "museum") -> "sightseeing"
            normalized in listOf("tour", "guide") -> "tour"
            normalized in listOf("hike", "walk", "trek", "trail") -> "hike"
            
            // Accommodation
            normalized in listOf("stay", "accommodation", "lodging") -> "hotel"
            normalized in listOf("home", "house", "rental_home") -> "airbnb"
            normalized in listOf("camp") -> "camping"
            
            // Transport
            normalized in listOf("plane", "fly") -> "flight"
            normalized in listOf("rail", "subway", "metro") -> "train"
            normalized in listOf("drive", "car", "rental", "taxi", "uber") -> "rental_car"
            
            // Exact matches
            normalized in listOf("flight", "train", "bus", "rental_car", "hotel", "airbnb", "hostel", "camping", "restaurant", "sightseeing", "tour", "hike", "note", "ticket") -> normalized
            
            // Fallbacks
            normalized.contains("food") || normalized.contains("restaurant") -> "restaurant"
            normalized.contains("stay") || normalized.contains("hotel") -> "hotel"
            normalized.contains("flight") || normalized.contains("air") -> "flight"
            else -> "sightseeing" // Safe default
        }
    }

    /**
     * Dismiss a suggestion without adding it
     */
    fun dismissSuggestion(item: GeneratedItem) {
        suggestions.removeIf { it.title == item.title }
        allGeneratedItems = allGeneratedItems.filter { it.title != item.title }
        
        // If all dismissed, go back to idle
        if (suggestions.isEmpty() && uiState.value is UiState.Success) {
            uiState.value = UiState.Idle
        }
    }

    /**
     * Reset to initial state
     */
    fun reset() {
        suggestions.clear()
        allGeneratedItems = emptyList()
        displayedCount = 0
        lastPrompt = ""
        uiState.value = UiState.Idle
    }
}
