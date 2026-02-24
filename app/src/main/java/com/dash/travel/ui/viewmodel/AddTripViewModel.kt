package com.dash.travel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.model.*
import com.dash.travel.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class AddTripViewModel(
    private val tripRepository: TripRepository,
    private val profileRepository: ProfileRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _trip = MutableStateFlow<com.dash.travel.data.model.SupabaseTrip?>(null)
    val trip = _trip.asStateFlow()

    fun loadTrip(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedTrip = tripRepository.getTripById(tripId)
                _trip.value = fetchedTrip
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTrip(
        tripName: String,
        startDate: String,
        endDate: String,
        timezone: String,
        destination: JsonObject?,
        origin: JsonObject?,
        userId: String,
        inviteEmails: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Feature: Ensure User Profile exists (Backwards Compatibility / Self-Healing)
                val existingProfile = profileRepository.getProfile(userId)
                if (existingProfile == null) {
                   // Create basic profile if missing (referencing constraint fix)
                   val newProfile = Profile(
                       id = userId,
                       username = "Traveler_${userId.take(4)}"
                   )
                   profileRepository.upsertProfile(newProfile)
                }

                // Generate random image from Pixabay based on destination
                val destinationName = destination?.get("name")?.jsonPrimitive?.content?.replace("\"", "") ?: "Unknown"
                val imageUrl = imageRepository.fetchRandomImage(destinationName)

                val destData = destination?.let {
                    DestinationData(
                        name = it["name"]?.jsonPrimitive?.content,
                        country = it["country"]?.jsonPrimitive?.content,
                        placeId = it["place_id"]?.jsonPrimitive?.content,
                        lat = it["lat"]?.jsonPrimitive?.doubleOrNull,
                        lng = it["lng"]?.jsonPrimitive?.doubleOrNull
                    )
                } ?: DestinationData(name = "Unknown")

                val originData = origin?.let {
                    OriginData(
                        name = it["name"]?.jsonPrimitive?.content,
                        country = it["country"]?.jsonPrimitive?.content,
                        placeId = it["place_id"]?.jsonPrimitive?.content,
                        lat = it["lat"]?.jsonPrimitive?.doubleOrNull,
                        lng = it["lng"]?.jsonPrimitive?.doubleOrNull
                    )
                } ?: OriginData(name = "Unknown")

                val newTrip = NewTripPayload(
                    title = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    createdBy = userId,
                    destinationData = destData,
                    originData = originData,
                    tripImageUrl = imageUrl
                )
                
                val createdTrip = tripRepository.createTrip(newTrip)
                
                // Handle Invitations
                createdTrip.id?.let { tripId ->
                    inviteEmails.forEach { email ->
                        if (email.isNotBlank()) {
                            try {
                                tripRepository.inviteMember(
                                    tripId = tripId,
                                    email = email.trim(),
                                    role = com.dash.travel.data.model.TripRole.EDITOR 
                                )
                            } catch (e: Exception) {
                                // Log error but don't fail the whole trip creation
                                e.printStackTrace()
                            }
                        }
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create trip")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTrip(
        tripId: String,
        title: String,
        description: String?,
        startDate: String,
        endDate: String,
        timezone: String,
        customAttributes: JsonObject?,
        destination: JsonObject?,
        origin: JsonObject?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var updatedImageUrl: String? = null
                
                // If the destination is being updated, fetch a new corresponding image
                if (destination != null) {
                    val destinationName = destination["name"]?.jsonPrimitive?.content?.replace("\"", "")
                    if (!destinationName.isNullOrBlank()) {
                        val rawImageUrl = imageRepository.fetchRandomImage(destinationName)
                            ?: imageRepository.fetchRandomImage("Travel") // Fallback
                            
                        // Append cache-busting fragment
                        if (rawImageUrl != null) {
                            updatedImageUrl = "$rawImageUrl#t=${System.currentTimeMillis()}"
                        }
                    }
                }

                // Construct proper update payload
                val updates = kotlinx.serialization.json.buildJsonObject {
                    put("title", title)
                    if (description != null) put("description", description)
                    put("start_date", startDate)
                    put("end_date", endDate)
                    put("timezone", timezone)
                    if (customAttributes != null) put("custom_attributes", customAttributes)
                    if (destination != null) put("destination_data", destination)
                    if (origin != null) put("origin_data", origin)
                    if (updatedImageUrl != null) put("trip_image_url", updatedImageUrl)
                }

                tripRepository.updateTrip(tripId, updates)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update trip")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
