package com.dash.travel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class ItineraryType(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val category: String
)

val itineraryTypes = listOf(
    // Transportation
    ItineraryType("flight", "Flight", Icons.Default.AirplanemodeActive, "Transportation"),
    ItineraryType("train", "Train", Icons.Default.Train, "Transportation"),
    ItineraryType("bus", "Bus", Icons.Default.DirectionsBus, "Transportation"),
    ItineraryType("rental_car", "Rental Car", Icons.Default.DirectionsCar, "Transportation"),
    
    // Accommodation
    ItineraryType("hotel", "Hotel", Icons.Default.Hotel, "Accommodation"),
    ItineraryType("airbnb", "Airbnb / Rental", Icons.Default.HomeWork, "Accommodation"),
    ItineraryType("hostel", "Hostel", Icons.Default.Bed, "Accommodation"),
    ItineraryType("camping", "Camping", Icons.Default.Terrain, "Accommodation"),
    
    // Activities
    ItineraryType("restaurant", "Dining", Icons.Default.Restaurant, "Activities"),
    ItineraryType("sightseeing", "Sightseeing", Icons.Default.CameraAlt, "Activities"),
    ItineraryType("tour", "Guided Tour", Icons.Default.Tour, "Activities"),
    ItineraryType("hike", "Hiking", Icons.Default.Hiking, "Activities"),
    
    // Planning
    ItineraryType("note", "Note", Icons.AutoMirrored.Filled.Notes, "Planning"),
    ItineraryType("ticket", "Event Ticket", Icons.Default.ConfirmationNumber, "Planning")
)

fun getIconForType(type: String): ImageVector {
    return itineraryTypes.find { it.id == type }?.icon ?: Icons.Default.Event
}

@Composable
fun EmptyItinerary() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your journey is a blank canvas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add flights, stays, or hidden gems to start planning your perfect trip.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
