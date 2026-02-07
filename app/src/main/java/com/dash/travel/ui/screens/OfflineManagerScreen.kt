package com.dash.travel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*
import kotlinx.coroutines.delay

data class OfflineRegion(
    val id: String,
    val name: String,
    val size: String,
    val isDownloaded: Boolean,
    val downloadProgress: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineManagerScreen(
    onNavigateBack: () -> Unit
) {
    // Mock Data
    var regions by remember {
        mutableStateOf(
            listOf(
                OfflineRegion("1", "Tokyo, Japan", "125 MB", true),
                OfflineRegion("2", "Kyoto, Japan", "85 MB", false),
                OfflineRegion("3", "Osaka, Japan", "92 MB", false)
            )
        )
    }
    
    // Simulate Download
    LaunchedEffect(regions) {
        val index = regions.indexOfFirst { !it.isDownloaded && it.downloadProgress > 0f && it.downloadProgress < 1f }
        if (index != -1) {
            delay(100)
            val updated = regions.toMutableList()
            val current = updated[index]
            if (current.downloadProgress < 1f) {
                updated[index] = current.copy(downloadProgress = current.downloadProgress + 0.05f)
                regions = updated
            } else {
                 updated[index] = current.copy(isDownloaded = true, downloadProgress = 0f)
                 regions = updated
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Offline Maps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
            // Status Header
            DashCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("You are online", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Offline mode will start automatically when connection is lost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
            
            Text(
                "Available Regions",
                style = MaterialTheme.typography.titleSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(regions) { region ->
                    OfflineRegionCard(
                        region = region,
                        onDownload = {
                             regions = regions.map { 
                                 if (it.id == region.id) it.copy(downloadProgress = 0.05f) else it 
                             }
                        },
                        onDelete = {
                            regions = regions.map {
                                if (it.id == region.id) it.copy(isDownloaded = false) else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineRegionCard(
    region: OfflineRegion,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    DashCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(region.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(region.size, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                
                if (region.isDownloaded) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OnSurfaceVariant)
                    }
                } else if (region.downloadProgress > 0f) {
                    CircularProgressIndicator(
                        progress = { region.downloadProgress },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = Primary)
                    }
                }
            }
            
            if (region.isDownloaded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Downloaded", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D))
                }
            }
        }
    }
}
