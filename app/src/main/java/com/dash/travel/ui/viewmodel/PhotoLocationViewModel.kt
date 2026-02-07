package com.dash.travel.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.ui.screens.ai.ExtractedLocation
import com.dash.travel.ui.screens.ai.LocationType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ViewModel for extracting locations from photos using ML Kit
 * 
 * Uses on-device text recognition - NO API COSTS
 * Google ML Kit Text Recognition is completely free
 */
class PhotoLocationViewModel(
    private val tripId: String,
    private val context: Context
) : ViewModel() {

    val extractedLocations = mutableStateListOf<ExtractedLocation>()
    val isProcessing = mutableStateOf(false)

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Process an image to extract location information
     */
    fun processImage(imageUri: Uri) {
        viewModelScope.launch {
            isProcessing.value = true
            extractedLocations.clear()

            try {
                val extractedText = recognizeText(imageUri)
                val locations = parseLocationsFromText(extractedText)
                extractedLocations.addAll(locations)
            } catch (e: Exception) {
                // Handle error - could add error state
            } finally {
                isProcessing.value = false
            }
        }
    }

    /**
     * Use ML Kit to recognize text in the image
     */
    private suspend fun recognizeText(imageUri: Uri): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                textRecognizer.process(image)
                    .addOnSuccessListener { result ->
                        continuation.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Parse extracted text to find location-like strings
     * Uses heuristics to identify places, addresses, etc.
     */
    private fun parseLocationsFromText(text: String): List<ExtractedLocation> {
        val locations = mutableListOf<ExtractedLocation>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // Patterns for different location types
        val addressPattern = Regex("""\d+\s+[\w\s]+(?:St|Street|Ave|Avenue|Rd|Road|Blvd|Boulevard|Dr|Drive|Ln|Lane)""", RegexOption.IGNORE_CASE)
        val restaurantKeywords = listOf("restaurant", "cafe", "bistro", "bar", "grill", "kitchen", "eatery", "diner")
        val hotelKeywords = listOf("hotel", "inn", "resort", "suites", "lodge", "motel", "hostel", "airbnb")
        val flightPattern = Regex("""([A-Z]{2,3})\s*(\d{3,4})""") // AA123, UA1234
        val timePattern = Regex("""\d{1,2}:\d{2}\s*(AM|PM)?""", RegexOption.IGNORE_CASE)
        val datePattern = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\w+\s+\d{1,2},?\s*\d{4}""")

        // Track found dates/times for context
        var lastDateTime: String? = null

        for (line in lines) {
            // Skip very short lines
            if (line.length < 4) continue

            // Check for dates/times (context for next location)
            datePattern.find(line)?.let { match ->
                lastDateTime = match.value
            }
            timePattern.find(line)?.let { match ->
                lastDateTime = if (lastDateTime != null) "$lastDateTime ${match.value}" else match.value
            }

            // Check for flight numbers
            flightPattern.find(line)?.let { match ->
                locations.add(
                    ExtractedLocation(
                        id = UUID.randomUUID().toString(),
                        name = "Flight ${match.value}",
                        type = LocationType.FLIGHT,
                        dateTime = lastDateTime,
                        confidence = 0.9f
                    )
                )
                lastDateTime = null
                return@let
            }

            // Check for address patterns
            addressPattern.find(line)?.let { match ->
                // Find a name before the address or use address as name
                val addressStartIndex = line.indexOf(match.value)
                val namePart = if (addressStartIndex > 3) {
                    line.substring(0, addressStartIndex).trim()
                } else null

                locations.add(
                    ExtractedLocation(
                        id = UUID.randomUUID().toString(),
                        name = namePart ?: match.value,
                        address = match.value,
                        type = determineLocationType(namePart ?: ""),
                        dateTime = lastDateTime,
                        confidence = 0.85f
                    )
                )
                lastDateTime = null
                return@let
            }

            // Check for restaurants
            if (restaurantKeywords.any { line.contains(it, ignoreCase = true) }) {
                locations.add(
                    ExtractedLocation(
                        id = UUID.randomUUID().toString(),
                        name = line.take(50), // Limit name length
                        type = LocationType.RESTAURANT,
                        dateTime = lastDateTime,
                        confidence = 0.8f
                    )
                )
                lastDateTime = null
                continue
            }

            // Check for hotels
            if (hotelKeywords.any { line.contains(it, ignoreCase = true) }) {
                locations.add(
                    ExtractedLocation(
                        id = UUID.randomUUID().toString(),
                        name = line.take(50),
                        type = LocationType.HOTEL,
                        dateTime = lastDateTime,
                        confidence = 0.8f
                    )
                )
                lastDateTime = null
                continue
            }

            // Check for capitalized proper nouns (potential place names)
            if (line.first().isUpperCase() && line.length in 5..40 && 
                !line.contains("@") && !line.contains("http")) {
                val words = line.split(" ")
                val capitalizedWords = words.count { it.isNotEmpty() && it.first().isUpperCase() }
                if (capitalizedWords >= 2 && capitalizedWords <= 5) {
                    locations.add(
                        ExtractedLocation(
                            id = UUID.randomUUID().toString(),
                            name = line,
                            type = LocationType.PLACE,
                            dateTime = lastDateTime,
                            confidence = 0.6f
                        )
                    )
                    lastDateTime = null
                }
            }
        }

        // Sort by confidence and remove duplicates
        return locations
            .distinctBy { it.name.lowercase() }
            .sortedByDescending { it.confidence }
            .take(10) // Limit to top 10
    }

    private fun determineLocationType(name: String): LocationType {
        val lowerName = name.lowercase()
        return when {
            listOf("restaurant", "cafe", "bistro", "bar", "grill").any { lowerName.contains(it) } -> 
                LocationType.RESTAURANT
            listOf("hotel", "inn", "resort", "suites").any { lowerName.contains(it) } -> 
                LocationType.HOTEL
            listOf("museum", "park", "monument", "tower", "castle").any { lowerName.contains(it) } -> 
                LocationType.ATTRACTION
            else -> LocationType.PLACE
        }
    }

    /**
     * Toggle selection of a location
     */
    fun toggleLocation(locationId: String) {
        val index = extractedLocations.indexOfFirst { it.id == locationId }
        if (index != -1) {
            extractedLocations[index] = extractedLocations[index].copy(
                isSelected = !extractedLocations[index].isSelected
            )
        }
    }

    /**
     * Remove a location from the list
     */
    fun removeLocation(locationId: String) {
        extractedLocations.removeAll { it.id == locationId }
    }

    /**
     * Add selected locations to the trip as itinerary items
     */
    fun addSelectedLocationsToTrip() {
        val selected = extractedLocations.filter { it.isSelected }
        
        viewModelScope.launch {
            // TODO: Create itinerary items in Supabase for each selected location
            // For each location, create an ItineraryItem with:
            // - type: based on LocationType
            // - title: location name
            // - location_name: address
            // - start_time: dateTime if available
            
            // After adding, remove them from the extracted list
            extractedLocations.removeAll { it.isSelected }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}
