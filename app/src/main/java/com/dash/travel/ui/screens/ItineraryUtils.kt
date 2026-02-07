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

import com.dash.travel.R

data class ItineraryType(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val category: String
)

val itineraryTypes = listOf(
    // Transportation
    ItineraryType("flight", R.string.type_flight, Icons.Default.AirplanemodeActive, "Transportation"),
    ItineraryType("train", R.string.type_train, Icons.Default.Train, "Transportation"),
    ItineraryType("bus", R.string.type_bus, Icons.Default.DirectionsBus, "Transportation"),
    ItineraryType("rental_car", R.string.type_rental_car, Icons.Default.DirectionsCar, "Transportation"),
    
    // Accommodation
    ItineraryType("hotel", R.string.type_hotel, Icons.Default.Hotel, "Accommodation"),
    ItineraryType("airbnb", R.string.type_airbnb, Icons.Default.HomeWork, "Accommodation"),
    ItineraryType("hostel", R.string.type_hostel, Icons.Default.Bed, "Accommodation"),
    ItineraryType("camping", R.string.type_camping, Icons.Default.Terrain, "Accommodation"),
    
    // Activities
    ItineraryType("restaurant", R.string.type_restaurant, Icons.Default.Restaurant, "Activities"),
    ItineraryType("sightseeing", R.string.type_sightseeing, Icons.Default.CameraAlt, "Activities"),
    ItineraryType("tour", R.string.type_tour, Icons.Default.Tour, "Activities"),
    ItineraryType("hike", R.string.type_hike, Icons.Default.Hiking, "Activities"),
    
    // Planning
    ItineraryType("note", R.string.type_note, Icons.AutoMirrored.Filled.Notes, "Planning"),
    ItineraryType("ticket", R.string.type_ticket, Icons.Default.ConfirmationNumber, "Planning")
)

fun getCategoryLabelRes(category: String): Int {
    return when(category) {
        "Transportation" -> R.string.category_transportation
        "Accommodation" -> R.string.category_accommodation
        "Activities" -> R.string.category_activities
        "Planning" -> R.string.category_planning
        else -> R.string.category_activities // Fallback
    }
}

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
        Text(androidx.compose.ui.res.stringResource(R.string.trip_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            androidx.compose.ui.res.stringResource(R.string.trip_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
