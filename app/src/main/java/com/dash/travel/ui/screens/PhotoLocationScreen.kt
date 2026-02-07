package com.dash.travel.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.components.GradientButton
import com.dash.travel.ui.theme.Background
import com.dash.travel.ui.theme.OnBackground
import com.dash.travel.ui.theme.OnSurface
import com.dash.travel.ui.theme.OnSurfaceVariant
import com.dash.travel.ui.theme.Primary
import com.dash.travel.ui.theme.SurfaceContainer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLocationScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onLocationFound: (locationName: String, lat: Double, lng: Double) -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var foundLocation by remember { mutableStateOf<FoundLocation?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            foundLocation = null // reset
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Photo Detective", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer)
                    .clickable { launcher.launch("image/*") }
                    .border(2.dp, if (imageUri == null) OnSurfaceVariant.copy(alpha=0.3f) else Color.Transparent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(imageUri).build(),
                        contentDescription = "Uploaded Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Scanning Animation Overlay
                    if (isAnalyzing) {
                        ScanLinesOverlay()
                    }
                    
                    if (foundLocation != null && !isAnalyzing) {
                         Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(32.dp)
                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tap to upload a photo",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceVariant
                        )
                        Text(
                            "We'll identify the location for you",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
            
            // Actions
            AnimatedVisibility(visible = imageUri != null && foundLocation == null) {
                GradientButton(
                    text = if (isAnalyzing) "Analyzing..." else "Analyze Photo",
                    onClick = {
                        isAnalyzing = true
                        // Simulate AI Analysis
                        // In real app, upload to Gemini Vision
                    },
                    enabled = !isAnalyzing,
                    icon = Icons.Default.AutoAwesome
                )
            }
            
            // Effect to simulate analysis
            LaunchedEffect(isAnalyzing) {
                if (isAnalyzing) {
                    delay(3000) // Fake processing time
                    isAnalyzing = false
                    foundLocation = FoundLocation(
                        name = "Eiffel Tower, Paris",
                        description = "Iconic wrought-iron lattice tower on the Champ de Mars.",
                        lat = 48.8584,
                        lng = 2.2945
                    )
                }
            }
            
            // Result
            AnimatedVisibility(visible = foundLocation != null) {
                foundLocation?.let { loc ->
                    DashCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    loc.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                loc.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onLocationFound(loc.name, loc.lat, loc.lng) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Add to Trip")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanLinesOverlay() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    // Animation logic would go here. For now, just a simple overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
    ) {
         CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

data class FoundLocation(
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double
)
