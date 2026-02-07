package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightAlertsScreen(
    onNavigateBack: () -> Unit
) {
    // Mock Data
    val flightStatus = FlightStatus(
        flightNumber = "UA 452",
        airline = "United Airlines",
        origin = "SFO",
        destination = "JFK",
        departureTime = "10:30 AM",
        arrivalTime = "06:45 PM",
        status = "Delayed",
        gate = "F12 -> F15",
        alertMessage = "Departure delayed by 45 mins. Gate changed to F15."
    )

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Flight Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Today's Flights",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            FlightStatusCard(flightStatus)
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun FlightStatusCard(flight: FlightStatus) {
    DashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.AirplanemodeActive, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(flight.airline, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text(flight.flightNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusChip(flight.status)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(flight.origin, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text(flight.departureTime, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
                
                // Flight Path Graphic
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("5h 15m", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    HorizontalDivider(thickness = 2.dp, color = SurfaceContainerHigh, modifier = Modifier.padding(vertical = 4.dp))
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(flight.destination, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text(flight.arrivalTime, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
            }
            
            // Alert Box
            if (flight.alertMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Flight Update", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(flight.alertMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            
            // Gate Info
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gate:", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(flight.gate, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = OnSurface)
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bgColor, contentColor) = when (status) {
        "On Time" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "Delayed" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        "Cancelled" -> Color(0xFFF3F4F6) to Color(0xFF374151)
        else -> SurfaceContainer to OnSurface
    }
    
    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

data class FlightStatus(
    val flightNumber: String,
    val airline: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val status: String,
    val gate: String,
    val alertMessage: String? = null
)
