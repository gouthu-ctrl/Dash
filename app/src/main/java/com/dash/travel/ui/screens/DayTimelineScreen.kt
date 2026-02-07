package com.dash.travel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import com.dash.travel.data.models.ItineraryItem
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Per-day detailed itinerary view showing chronological events
 * with drag-and-drop reordering and color-coded item types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTimelineScreen(
    tripId: String,
    dayNumber: Int,
    date: Date,
    items: List<ItineraryItem>,
    onNavigateBack: () -> Unit,
    onItemClick: (ItineraryItem) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    // Reorderable state for drag-and-drop
    val reorderableItems = remember(items) { items.toMutableList() }
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            onReorder(from.index - 1, to.index - 1) // -1 for header offset
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Day $dayNumber",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateFormatter.format(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItem,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyDayContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAddItem = onAddItem
            )
        } else {
            LazyColumn(
                state = reorderState.listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState)
            ) {
                // Day summary header
                item {
                    DayHeaderCard(
                        itemCount = items.size,
                        totalHours = calculateDayDuration(items)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Timeline items
                itemsIndexed(items, key = { _, item -> item.id ?: item.hashCode() }) { index, item ->
                    ReorderableItem(
                        reorderableState = reorderState,
                        key = item.id ?: item.hashCode()
                    ) { isDragging ->
                        TimelineItemCard(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == items.lastIndex,
                            isDragging = isDragging,
                            timeFormatter = timeFormatter,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.detectReorderAfterLongPress(reorderState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeaderCard(
    itemCount: Int,
    totalHours: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$itemCount ${if (itemCount == 1) "event" else "events"} planned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = totalHours,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun TimelineItemCard(
    item: ItineraryItem,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean,
    timeFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor = getItemTypeColor(item.type)
    val itemIcon = getItemTypeIcon(item.type)
    
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Timeline connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Top connector line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Type indicator dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(itemColor)
            )
            
            // Bottom connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f, fill = true)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        
        // Item card
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 8.dp else 1.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) 
                    MaterialTheme.colorScheme.surfaceContainerHigh
                else 
                    MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Type icon with colored background
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(itemColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = itemIcon,
                            contentDescription = null,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.startTime != null) {
                            Text(
                                text = formatTime(item.startTime, item.endTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Drag handle
                    Icon(
                        Icons.Default.DragIndicator,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Location and details
                if (item.locationName != null || item.bookingRef != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item.locationName?.let { location ->
                            Text(
                                text = "📍 $location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item.bookingRef?.let { ref ->
                            Text(
                                text = "Ref: $ref",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Status badge
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(status = item.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status.lowercase()) {
        "confirmed" -> Triple(
            Color(0xFF22C55E).copy(alpha = 0.15f),
            Color(0xFF16A34A),
            "Confirmed"
        )
        "proposed" -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFD97706),
            "Proposed"
        )
        "draft" -> Triple(
            Color(0xFF6B7280).copy(alpha = 0.15f),
            Color(0xFF4B5563),
            "Draft"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status.replaceFirstChar { it.uppercase() }
        )
    }
    
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = backgroundColor,
        modifier = Modifier
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyDayContent(
    modifier: Modifier = Modifier,
    onAddItem: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📅",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No plans for this day yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first activity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper functions

@Composable
private fun getItemTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "flight" -> Color(0xFF3B82F6) // Blue
        "hotel" -> Color(0xFF8B5CF6) // Purple
        "restaurant", "food" -> Color(0xFFF97316) // Orange
        "activity", "attraction" -> Color(0xFF22C55E) // Green
        "note" -> Color(0xFF6B7280) // Gray
        "transport" -> Color(0xFF0EA5E9) // Sky blue
        else -> Color(0xFF6B7280) // Default gray
    }
}

@Composable
private fun getItemTypeIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "flight" -> Icons.Default.Flight
        "hotel" -> Icons.Default.Hotel
        "restaurant", "food" -> Icons.Default.Restaurant
        "activity", "attraction" -> Icons.Default.LocalActivity
        "note" -> Icons.Default.Notes
        else -> Icons.Default.Schedule
    }
}

private fun formatTime(startTime: String?, endTime: String?): String {
    if (startTime == null) return ""
    return if (endTime != null) {
        "$startTime - $endTime"
    } else {
        startTime
    }
}

private fun calculateDayDuration(items: List<ItineraryItem>): String {
    // For now, return a simple count-based string
    // In production, this would calculate actual time spans
    val flights = items.count { it.type == "flight" }
    val activities = items.count { it.type != "flight" && it.type != "hotel" }
    
    val parts = mutableListOf<String>()
    if (flights > 0) parts.add("$flights ${if (flights == 1) "flight" else "flights"}")
    if (activities > 0) parts.add("$activities ${if (activities == 1) "activity" else "activities"}")
    
    return if (parts.isEmpty()) "No scheduled items" else parts.joinToString(" • ")
}
