package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*

data class DashboardWidget(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizationScreen(
    onNavigateBack: () -> Unit
) {
    var widgets by remember {
        mutableStateOf(
            listOf(
                DashboardWidget("hero", "Upcoming Trip Hero", "Large countdown and photo for next trip", true),
                DashboardWidget("quickdata", "Quick Actions", "Buttons for new trip, expense, etc.", true),
                DashboardWidget("weather", "Destination Weather", "Current weather at upcoming destination", true),
                DashboardWidget("currency", "Currency Converter", "Quick conversion for destination currency", false),
                DashboardWidget("explore", "Explore Suggestions", "Personalized travel ideas", true)
            )
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Customize Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
        ) {
            Surface(
                color = SurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.small
            ) {
                 Row(modifier = Modifier.padding(12.dp)) {
                      Icon(Icons.Default.Widgets, contentDescription = null, tint = Primary)
                      Spacer(modifier = Modifier.width(12.dp))
                      Text(
                          "Toggle widgets to show or hide them on your Home screen. Drag to reorder (Coming Soon).",
                          style = MaterialTheme.typography.bodySmall,
                          color = OnSurfaceVariant
                      )
                 }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(widgets) { widget ->
                    WidgetItem(
                        widget = widget,
                        onToggle = {
                            widgets = widgets.map { 
                                if (it.id == widget.id) it.copy(isEnabled = !it.isEnabled) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetItem(
    widget: DashboardWidget,
    onToggle: () -> Unit
) {
    DashCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (widget.isEnabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (widget.isEnabled) Primary else OnSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(widget.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(widget.description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            
            Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = OnSurfaceVariant.copy(alpha=0.3f))
        }
    }
}
