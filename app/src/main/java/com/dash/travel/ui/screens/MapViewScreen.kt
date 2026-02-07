package com.dash.travel.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.ui.theme.Background
import com.dash.travel.ui.theme.OnBackground
import com.dash.travel.ui.theme.OnSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    tripTitle: String,
    items: List<ItineraryItem>,
    onNavigateBack: () -> Unit
) {
    // Filter items with valid locations
    val validItems = remember(items) { items.filter { it.locationLat != null && it.locationLng != null } }

    // Generate Markers JSON
    val markersJson = remember(validItems) {
        validItems.joinToString(prefix = "[", postfix = "]") { item ->
            """
            {
                "lat": ${item.locationLat},
                "lng": ${item.locationLng},
                "title": "${item.title.replace("\"", "\\\"")}",
                "snippet": "${(item.locationName ?: "").replace("\"", "\\\"")}"
            }
            """
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tripTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Interactive Map", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                },
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (validItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No locations to display", color = OnSurfaceVariant)
                }
            } else {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            
                            val htmlContent = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                    <style>body { margin: 0; padding: 0; } #map { width: 100%; height: 100vh; }</style>
                                </head>
                                <body>
                                    <div id="map"></div>
                                    <script>
                                        // Initialize Map
                                        var map = L.map('map');
                                        
                                        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                            maxZoom: 19,
                                            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                                        }).addTo(map);

                                        var markersData = $markersJson;
                                        var bounds = L.latLngBounds();

                                        markersData.forEach(function(m) {
                                            var marker = L.marker([m.lat, m.lng]).addTo(map);
                                            marker.bindPopup("<b>" + m.title + "</b><br>" + m.snippet);
                                            bounds.extend([m.lat, m.lng]);
                                        });

                                        if (markersData.length > 0) {
                                            map.fitBounds(bounds, { padding: [50, 50] });
                                        } else {
                                            map.setView([0, 0], 2);
                                        }
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            
                            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
