package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.data.models.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    trips: List<Trip> = emptyList(),
    userName: String = "Traveler",
    onNavigateToTrip: (String) -> Unit = {},
    onCreateTrip: () -> Unit = {}
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Discover", "Vault", "Profile")
    val icons = listOf(
        Icons.Filled.Home to Icons.Outlined.Home,
        Icons.Filled.Explore to Icons.Outlined.Explore,
        Icons.Filled.Lock to Icons.Outlined.Lock,
        Icons.Filled.Person to Icons.Outlined.Person
    )

    Scaffold(
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onCreateTrip,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Trip", modifier = Modifier.size(36.dp))
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) icons[index].first else icons[index].second,
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (trips.isEmpty()) {
            EmptyHomeContent(
                modifier = Modifier.padding(innerPadding),
                userName = userName,
                onCreateTrip = onCreateTrip
            )
        } else {
            HomeContent(
                modifier = Modifier.padding(innerPadding),
                userName = userName,
                trips = trips,
                onTripClick = onNavigateToTrip
            )
        }
    }
}

@Composable
fun EmptyHomeContent(
    modifier: Modifier = Modifier,
    userName: String,
    onCreateTrip: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Welcome, $userName!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your world awaits. Plan your first trip with Dash, collaborate with friends, and let AI handle the details.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onCreateTrip,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Start Planning", modifier = Modifier.padding(vertical = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Navigation Guide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GuideItem(icon = Icons.Default.Explore, label = "Explore", description = "Find templates")
            GuideItem(icon = Icons.Default.Lock, label = "Vault", description = "Secure docs")
            GuideItem(icon = Icons.Default.NotificationsNone, label = "Alerts", description = "Price drops")
        }
    }
}

@Composable
fun GuideItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    userName: String,
    trips: List<Trip>,
    onTripClick: (String) -> Unit
) {
    val today = Date()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    val upcomingTrips = trips.filter { 
        val startDate = it.startDate?.let { d -> try { sdf.parse(d) } catch (e: Exception) { null } }
        startDate == null || !startDate.before(today)
    }.sortedBy { it.startDate }

    val pastTrips = trips.filter { 
        val startDate = it.startDate?.let { d -> try { sdf.parse(d) } catch (e: Exception) { null } }
        startDate != null && startDate.before(today)
    }.sortedByDescending { it.startDate }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeaderSection(userName)
        }

        if (upcomingTrips.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Trips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(upcomingTrips) { trip ->
                UpcomingTripCard(trip = trip, onClick = { onTripClick(trip.id) })
            }
        }

        if (pastTrips.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Past Adventures",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(pastTrips) { trip ->
                PastTripItem(trip = trip, onClick = { onTripClick(trip.id) })
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
        }
    }
}

@Composable
fun HeaderSection(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UpcomingTripCard(trip: Trip, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displaySdf = SimpleDateFormat("MMM dd", Locale.US)
    
    val daysLeft = trip.startDate?.let {
        val start = try { sdf.parse(it) } catch (e: Exception) { null }
        val diff = if (start != null) start.time - Date().time else -1L
        if (diff > 0) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) else 0
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // Fixed height for consistency
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Destination Image
            AsyncImage(
                model = trip.tripImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (daysLeft != null && daysLeft > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "In $daysLeft days",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${trip.placesCount} places",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Column {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    val dateRange = if (trip.startDate != null && trip.endDate != null) {
                        val start = try { sdf.parse(trip.startDate) } catch (e: Exception) { null }
                        val end = try { sdf.parse(trip.endDate) } catch (e: Exception) { null }
                        if (start != null && end != null) {
                            "${displaySdf.format(start)} - ${displaySdf.format(end)}"
                        } else {
                            trip.description ?: "Ready to explore"
                        }
                    } else {
                        trip.description ?: "Ready to explore"
                    }
                    
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun PastTripItem(trip: Trip, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = trip.tripImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${trip.placesCount} places visited",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
