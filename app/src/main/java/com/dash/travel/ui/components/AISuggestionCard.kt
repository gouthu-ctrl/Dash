package com.dash.travel.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dash.travel.data.model.GeneratedItem
import com.dash.travel.ui.theme.*
import java.text.NumberFormat
import java.util.Currency

/**
 * Card for displaying a single AI-generated suggestion
 * 
 * Features:
 * - Type-based icon (activity/food/stay)
 * - Title with location name
 * - Time/cost info if available
 * - Prominent "Add to Trip" button (56dp touch target)
 * - Dismiss button
 */
@Composable
fun AISuggestionCard(
    item: GeneratedItem,
    isAdding: Boolean = false,
    onAddToTrip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeIcon = getTypeIcon(item.type)
    val typeColor = getTypeColor(item.type)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Icon + Title + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Type Icon
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = item.type,
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Title and Location
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.locationName.isNullOrBlank()) {
                        Text(
                            text = item.locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Dismiss Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Description (if available)
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Info Chips Row (Time, Cost)
            val hasTime = !item.startTime.isNullOrBlank()
            val hasCost = item.estimatedCost != null && item.estimatedCost > 0
            
            if (hasTime || hasCost) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasTime) {
                        InfoChip(
                            icon = Icons.Default.Schedule,
                            text = formatTime(item.startTime)
                        )
                    }
                    if (hasCost) {
                        InfoChip(
                            icon = Icons.Default.AttachMoney,
                            text = formatCost(item.estimatedCost, item.currency ?: "USD")
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add to Trip Button (56dp touch target)
            AddToTripButton(
                onClick = onAddToTrip,
                isLoading = isAdding,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddToTripButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "buttonScale"
    )
    
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(56.dp) // MD3 touch target
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading) "Adding..." else "Add to Trip",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Helper functions
private fun getTypeIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "activity" -> Icons.Default.DirectionsWalk
        "eat", "food", "restaurant" -> Icons.Default.Restaurant
        "stay", "hotel" -> Icons.Default.Hotel
        "transport", "flight" -> Icons.Default.Flight
        else -> Icons.Default.Place
    }
}

private fun getTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "activity" -> TimelineActivity
        "eat", "food", "restaurant" -> TimelineFood
        "stay", "hotel" -> TimelineStay
        "transport", "flight" -> TimelineFlight
        else -> TimelineNote
    }
}

private fun formatTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return ""
    return try {
        // Extract time portion from ISO string (e.g., "2026-05-01T09:00:00Z")
        val timePart = isoTime.substringAfter("T").substringBefore("Z").substringBefore("+")
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: return timePart.take(5)
            val minute = parts[1]
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$displayHour:$minute $amPm"
        } else {
            timePart.take(5)
        }
    } catch (e: Exception) {
        isoTime.take(5)
    }
}

private fun formatCost(cost: Double?, currency: String): String {
    if (cost == null || cost == 0.0) return ""
    return try {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(currency)
        format.format(cost)
    } catch (e: Exception) {
        "$${cost.toInt()}"
    }
}
