package com.dash.travel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Types of itinerary optimization suggestions
 */
enum class OptimizationType {
    ROUTE_OPTIMIZATION,     // Reorder items to minimize travel
    TIME_CONFLICT,          // Alert about overlapping times
    BUDGET_SAVINGS,         // Suggest cheaper alternatives
    BETTER_ORDERING         // General ordering improvement
}

/**
 * An optimization suggestion for the itinerary
 */
data class OptimizationSuggestion(
    val id: String,
    val type: OptimizationType,
    val title: String,
    val description: String,
    val estimatedSavings: String? = null, // e.g., "Save 30 mins" or "Save $20"
    val affectedItemIds: List<String> = emptyList()
)

/**
 * Card showing an optimization suggestion
 * 
 * Note: For real AI-powered suggestions, connect to an AI API
 * Current implementation uses on-device heuristics
 */
@Composable
fun OptimizationSuggestionCard(
    suggestion: OptimizationSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, gradientColors) = when (suggestion.type) {
        OptimizationType.ROUTE_OPTIMIZATION -> Triple(
            Icons.Default.Route,
            Color(0xFF3B82F6),
            listOf(Color(0xFF3B82F6).copy(alpha = 0.15f), Color(0xFF8B5CF6).copy(alpha = 0.1f))
        )
        OptimizationType.TIME_CONFLICT -> Triple(
            Icons.Default.Schedule,
            Color(0xFFF59E0B),
            listOf(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF97316).copy(alpha = 0.1f))
        )
        OptimizationType.BUDGET_SAVINGS -> Triple(
            Icons.Default.Savings,
            Color(0xFF22C55E),
            listOf(Color(0xFF22C55E).copy(alpha = 0.15f), Color(0xFF14B8A6).copy(alpha = 0.1f))
        )
        OptimizationType.BETTER_ORDERING -> Triple(
            Icons.Default.SwapVert,
            Color(0xFF8B5CF6),
            listOf(Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF3B82F6).copy(alpha = 0.1f))
        )
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = iconColor.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI Suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = suggestion.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp)
                )

                if (suggestion.estimatedSavings != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = Color(0xFF22C55E).copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 44.dp)
                    ) {
                        Text(
                            text = suggestion.estimatedSavings,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Ignore")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iconColor
                        )
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Banner showing multiple optimization suggestions available
 */
@Composable
fun OptimizationBanner(
    suggestionCount: Int,
    onViewSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = suggestionCount > 0,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            onClick = onViewSuggestions,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$suggestionCount optimization suggestion${if (suggestionCount > 1) "s" else ""} available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "View →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Simple on-device optimization analyzer
 * For more advanced optimization, connect to an AI API
 */
object ItineraryOptimizer {

    /**
     * Analyze itinerary items and generate optimization suggestions
     * This is a simple heuristic-based analyzer (no API costs)
     */
    fun analyzeItinerary(
        items: List<ItineraryItemData>
    ): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()

        // Check for time conflicts
        val sortedItems = items.filter { it.startTime != null }.sortedBy { it.startTime }
        sortedItems.zipWithNext { a, b ->
            if (a.endTime != null && b.startTime != null && a.endTime > b.startTime) {
                suggestions.add(
                    OptimizationSuggestion(
                        id = "conflict_${a.id}_${b.id}",
                        type = OptimizationType.TIME_CONFLICT,
                        title = "Time Conflict Detected",
                        description = "${a.title} ends after ${b.title} starts. Consider adjusting times.",
                        affectedItemIds = listOf(a.id, b.id)
                    )
                )
            }
        }

        // Check for potential reordering (simple heuristic: group by type)
        val itemsByType = items.groupBy { it.type }
        if (itemsByType.size > 1) {
            val restaurantItems = items.filter { it.type == "restaurant" }
            val activityItems = items.filter { it.type == "activity" }
            
            // Suggest grouping if items are scattered
            if (restaurantItems.size > 1 && activityItems.size > 1) {
                // Check if items are interleaved
                var lastType: String? = null
                var typeChanges = 0
                items.forEach { item ->
                    if (lastType != null && lastType != item.type) {
                        typeChanges++
                    }
                    lastType = item.type
                }
                
                if (typeChanges > 4) {
                    suggestions.add(
                        OptimizationSuggestion(
                            id = "grouping_suggestion",
                            type = OptimizationType.BETTER_ORDERING,
                            title = "Optimize Item Order",
                            description = "Group similar activities together to reduce travel time between locations.",
                            estimatedSavings = "Save 15-30 mins"
                        )
                    )
                }
            }
        }

        return suggestions.take(3) // Limit suggestions
    }
}

/**
 * Simple data class for optimizer input
 */
data class ItineraryItemData(
    val id: String,
    val title: String,
    val type: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null
)
