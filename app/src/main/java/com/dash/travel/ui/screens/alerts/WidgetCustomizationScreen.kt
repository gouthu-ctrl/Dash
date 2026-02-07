package com.dash.travel.ui.screens.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Widget size options
 */
enum class WidgetSize {
    SMALL,   // 2x2
    MEDIUM,  // 4x2
    LARGE    // 4x4
}

/**
 * Widget type
 */
enum class WidgetType {
    COUNTDOWN,
    UPCOMING_TRIP,
    DAILY_ITINERARY,
    FLIGHT_STATUS
}

/**
 * Widget configuration
 */
data class WidgetConfig(
    val id: String,
    val type: WidgetType,
    val size: WidgetSize,
    val tripId: String?,
    val tripTitle: String?,
    val isActive: Boolean
)

/**
 * Widget Customization Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizationScreen(
    widgets: List<WidgetConfig>,
    onNavigateBack: () -> Unit,
    onAddWidget: (WidgetType, WidgetSize) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onConfigureWidget: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Home Screen Widgets")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Introduction
            item {
                WidgetIntroCard()
            }

            // Widget preview section
            item {
                Text(
                    text = "Available Widgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Countdown widget
            item {
                WidgetPreviewSection(
                    title = "Trip Countdown",
                    description = "Days remaining until your trip",
                    icon = Icons.Default.Schedule,
                    onAddSmall = { onAddWidget(WidgetType.COUNTDOWN, WidgetSize.SMALL) },
                    onAddMedium = { onAddWidget(WidgetType.COUNTDOWN, WidgetSize.MEDIUM) }
                ) {
                    CountdownWidgetPreview()
                }
            }

            // Upcoming trip widget
            item {
                WidgetPreviewSection(
                    title = "Upcoming Trip",
                    description = "Quick view of your next adventure",
                    icon = Icons.Default.LocationOn,
                    onAddSmall = { onAddWidget(WidgetType.UPCOMING_TRIP, WidgetSize.SMALL) },
                    onAddMedium = { onAddWidget(WidgetType.UPCOMING_TRIP, WidgetSize.MEDIUM) }
                ) {
                    UpcomingTripWidgetPreview()
                }
            }

            // Daily itinerary widget
            item {
                WidgetPreviewSection(
                    title = "Today's Itinerary",
                    description = "Your schedule at a glance",
                    icon = Icons.Default.CalendarMonth,
                    onAddSmall = null,
                    onAddMedium = { onAddWidget(WidgetType.DAILY_ITINERARY, WidgetSize.MEDIUM) }
                ) {
                    DailyItineraryWidgetPreview()
                }
            }

            // Flight status widget
            item {
                WidgetPreviewSection(
                    title = "Flight Status",
                    description = "Live updates for your flights",
                    icon = Icons.Default.Flight,
                    onAddSmall = { onAddWidget(WidgetType.FLIGHT_STATUS, WidgetSize.SMALL) },
                    onAddMedium = { onAddWidget(WidgetType.FLIGHT_STATUS, WidgetSize.MEDIUM) }
                ) {
                    FlightStatusWidgetPreview()
                }
            }

            // Active widgets
            if (widgets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(widgets) { widget ->
                    ActiveWidgetCard(
                        widget = widget,
                        onConfigure = { onConfigureWidget(widget.id) },
                        onRemove = { onRemoveWidget(widget.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetIntroCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Widgets,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add widgets to your home screen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Long-press on your home screen to add Dash widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WidgetPreviewSection(
    title: String,
    description: String,
    icon: ImageVector,
    onAddSmall: (() -> Unit)?,
    onAddMedium: (() -> Unit)?,
    preview: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                preview()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAddSmall != null) {
                    Button(
                        onClick = onAddSmall,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Small", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (onAddMedium != null) {
                    Button(
                        onClick = onAddMedium,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Medium", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// Widget Previews

@Composable
private fun CountdownWidgetPreview() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.size(width = 160.dp, height = 80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "days",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Tokyo",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UpcomingTripWidgetPreview() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(width = 200.dp, height = 100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "Tokyo Adventure",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Mar 15 - Mar 22",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "5 places",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "12d",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyItineraryWidgetPreview() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.size(width = 200.dp, height = 120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            ItineraryWidgetItem(time = "9:00", title = "Senso-ji Temple", isNext = true)
            ItineraryWidgetItem(time = "12:30", title = "Ramen Lunch", isNext = false)
            ItineraryWidgetItem(time = "15:00", title = "Shibuya Crossing", isNext = false)
        }
    }
}

@Composable
private fun ItineraryWidgetItem(
    time: String,
    title: String,
    isNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (isNext) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isNext) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlightStatusWidgetPreview() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.size(width = 160.dp, height = 80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NH102",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Color(0xFF22C55E)
                ) {
                    Text(
                        text = "On Time",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SFO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "10:30",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    Icons.Default.Flight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "NRT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "14:45+1",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWidgetCard(
    widget: WidgetConfig,
    onConfigure: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onConfigure,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (widget.type) {
                        WidgetType.COUNTDOWN -> "Trip Countdown"
                        WidgetType.UPCOMING_TRIP -> "Upcoming Trip"
                        WidgetType.DAILY_ITINERARY -> "Daily Itinerary"
                        WidgetType.FLIGHT_STATUS -> "Flight Status"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${widget.size.name.lowercase().replaceFirstChar { it.uppercase() }} widget" +
                            (widget.tripTitle?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
