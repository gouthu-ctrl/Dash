package com.dash.travel.ui.screens.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplaneTicket
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Price alert type
 */
enum class PriceAlertType {
    FLIGHT,
    HOTEL,
    ACTIVITY
}

/**
 * Price trend
 */
enum class PriceTrend {
    UP,
    DOWN,
    STABLE
}

/**
 * Price alert data
 */
data class PriceAlert(
    val id: String,
    val type: PriceAlertType,
    val title: String,
    val description: String,
    val currentPrice: Float,
    val originalPrice: Float,
    val targetPrice: Float,
    val trend: PriceTrend,
    val priceChange: Float, // percentage
    val bookingUrl: String,
    val isEnabled: Boolean = true,
    val lastChecked: String
)

/**
 * Price Tracking Alerts Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    alerts: List<PriceAlert>,
    onNavigateBack: () -> Unit,
    onAddAlert: () -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    onUpdateTargetPrice: (String, Float) -> Unit
) {
    var selectedAlert by remember { mutableStateOf<PriceAlert?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Price Alerts")
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
            ExtendedFloatingActionButton(
                onClick = onAddAlert,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Alert") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        if (alerts.isEmpty()) {
            EmptyAlertsContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAddAlert = onAddAlert
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Summary header
                item {
                    PriceAlertsSummary(alerts = alerts)
                }

                items(alerts, key = { it.id }) { alert ->
                    PriceAlertCard(
                        alert = alert,
                        onClick = { selectedAlert = alert },
                        onToggle = { onToggleAlert(alert.id, it) },
                        onOpenBooking = { onOpenBooking(alert.bookingUrl) },
                        onDelete = { onDeleteAlert(alert.id) }
                    )
                }
            }
        }
    }

    // Alert detail sheet
    selectedAlert?.let { alert ->
        PriceAlertDetailSheet(
            alert = alert,
            onDismiss = { selectedAlert = null },
            onUpdateTarget = { target ->
                onUpdateTargetPrice(alert.id, target)
                selectedAlert = null
            },
            onOpenBooking = { onOpenBooking(alert.bookingUrl) }
        )
    }
}

@Composable
private fun PriceAlertsSummary(alerts: List<PriceAlert>) {
    val activeAlerts = alerts.count { it.isEnabled }
    val priceDrops = alerts.count { it.trend == PriceTrend.DOWN }

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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                value = alerts.size.toString(),
                label = "Total Alerts",
                color = MaterialTheme.colorScheme.primary
            )
            SummaryItem(
                value = activeAlerts.toString(),
                label = "Active",
                color = Color(0xFF22C55E)
            )
            SummaryItem(
                value = priceDrops.toString(),
                label = "Price Drops",
                color = Color(0xFFF97316)
            )
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PriceAlertCard(
    alert: PriceAlert,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onOpenBooking: () -> Unit,
    onDelete: () -> Unit
) {
    val typeIcon = when (alert.type) {
        PriceAlertType.FLIGHT -> Icons.Default.AirplaneTicket
        PriceAlertType.HOTEL -> Icons.Default.Hotel
        PriceAlertType.ACTIVITY -> Icons.Default.LocalActivity
    }

    val typeColor = when (alert.type) {
        PriceAlertType.FLIGHT -> Color(0xFF3B82F6)
        PriceAlertType.HOTEL -> Color(0xFF8B5CF6)
        PriceAlertType.ACTIVITY -> Color(0xFFF97316)
    }

    val trendIcon = when (alert.trend) {
        PriceTrend.UP -> Icons.AutoMirrored.Filled.TrendingUp
        PriceTrend.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
        PriceTrend.STABLE -> Icons.Default.TrendingFlat
    }

    val trendColor = when (alert.trend) {
        PriceTrend.UP -> Color(0xFFEF4444)
        PriceTrend.DOWN -> Color(0xFF22C55E)
        PriceTrend.STABLE -> Color(0xFF6B7280)
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isEnabled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = typeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Toggle
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current price with trend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%.0f", alert.currentPrice)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (alert.currentPrice <= alert.targetPrice) {
                            Color(0xFF22C55E)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Original price (strikethrough if different)
                    if (alert.originalPrice != alert.currentPrice) {
                        Text(
                            text = "$${String.format("%.0f", alert.originalPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Trend indicator
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = trendColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (alert.priceChange >= 0) "+" else ""}${String.format("%.1f", alert.priceChange)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = trendColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Target price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.0f", alert.targetPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Alert when below target
            if (alert.currentPrice <= alert.targetPrice) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF22C55E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉 Price dropped below target!",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = onOpenBooking
                        ) {
                            Text(
                                "Book Now",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Last checked
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last checked: ${alert.lastChecked}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyAlertsContent(
    modifier: Modifier = Modifier,
    onAddAlert: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.2f),
                            Color(0xFFF97316).copy(alpha = 0.2f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "No Price Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Get notified when prices drop\nfor flights, hotels, and activities",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddAlert) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your First Alert")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceAlertDetailSheet(
    alert: PriceAlert,
    onDismiss: () -> Unit,
    onUpdateTarget: (Float) -> Unit,
    onOpenBooking: () -> Unit
) {
    var targetPrice by remember { mutableFloatStateOf(alert.targetPrice) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Target price slider
            Text(
                text = "Target Price: $${String.format("%.0f", targetPrice)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = targetPrice,
                onValueChange = { targetPrice = it },
                valueRange = (alert.currentPrice * 0.5f)..(alert.currentPrice * 1.5f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${String.format("%.0f", alert.currentPrice * 0.5f)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%.0f", alert.currentPrice * 1.5f)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onUpdateTarget(targetPrice) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update Target")
                }

                Button(
                    onClick = onOpenBooking,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Book")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
