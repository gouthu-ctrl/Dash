package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dash.travel.data.model.SupabasePriceAlert
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsScreen(
    alerts: List<SupabasePriceAlert>,
    onNavigateBack: () -> Unit,
    onAddAlert: () -> Unit,
    onToggleActive: (SupabasePriceAlert, Boolean) -> Unit,
    onDeleteAlert: (SupabasePriceAlert) -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Price Watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddAlert) {
                        Icon(Icons.Default.Add, contentDescription = "Add Alert")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground,
                    actionIconContentColor = OnBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlert,
                containerColor = Primary,
                contentColor = OnPrimary
            ) {
                Icon(Icons.Default.AddAlert, contentDescription = "Add Alert")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (alerts.isEmpty()) {
                item {
                    EmptyAlertsState()
                }
            } else {
                items(alerts) { alert ->
                    PriceAlertCard(
                        alert = alert,
                        onToggle = { onToggleActive(alert, it) },
                        onDelete = { onDeleteAlert(alert) }
                    )
                }
            }
        }
    }
}

@Composable
fun PriceAlertCard(
    alert: SupabasePriceAlert,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    DashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    getPriceAlertIcon(alert.searchParams.type),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        alert.searchParams.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    alert.searchParams.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = alert.isActive,
                    onCheckedChange = onToggle,
                    thumbContent = {
                        Icon(
                            imageVector = if (alert.isActive) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SurfaceContainerHigh)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Current Price", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(
                        "$${alert.currentPrice?.toInt() ?: "--"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target Price", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(
                        "$${alert.targetPrice?.toInt() ?: "--"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }
            
            if (alert.currentPrice != null && alert.targetPrice != null && alert.currentPrice <= alert.targetPrice) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFDCFCE7), // Green 100
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Target Hit! Book Now",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D) // Green 700
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                 TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                     Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                     Spacer(modifier = Modifier.width(4.dp))
                     Text("Delete")
                 }
            }
        }
    }
}

@Composable
fun EmptyAlertsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AddAlert,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OnSurfaceVariant.copy(alpha=0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Active Alerts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Text(
            "Track flight and hotel prices to book at the best time.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun getPriceAlertIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "flight" -> Icons.Default.Flight
        "hotel" -> Icons.Default.Hotel
        else -> Icons.Default.LocalActivity
    }
}
