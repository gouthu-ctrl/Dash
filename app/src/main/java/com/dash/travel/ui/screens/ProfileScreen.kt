package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.data.model.Profile
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.dash.travel.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: Profile?,
    tripCount: Int = 0,
    placeCount: Int = 0,
    onNavigateBack: () -> Unit,
    onEditProfile: () -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.content_desc_edit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground,
                    actionIconContentColor = OnBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                if (profile?.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = stringResource(R.string.content_desc_avatar),
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                profile?.fullName?.take(1) ?: profile?.username?.take(1) ?: "?",
                                style = MaterialTheme.typography.displayMedium,
                                color = Primary
                            )
                        }
                    }
                }
                
                // Pro Badge (Mock)
                Surface(
                    color = Color(0xFFFFD700), // Gold
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Background),
                    modifier = Modifier.size(24.dp) // adjusted for simplicity
                ) {
                     // small star or icon
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                profile?.fullName ?: stringResource(R.string.profile_unknown_user),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            
            Text(
                "@${profile?.username ?: "username"}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStat(stringResource(R.string.profile_stat_trips), tripCount.toString(), Icons.Default.FlightTakeoff)
                ProfileStat(stringResource(R.string.profile_stat_countries), placeCount.toString(), Icons.Default.Map)
                ProfileStat(stringResource(R.string.profile_stat_friends), "24", Icons.Default.Person)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Travel Style (Chips) - Mock content
            DashCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_travel_style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.style_nature)) })
                        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.style_foodie)) })
                        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.style_backpacker)) })
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = SurfaceContainerHigh,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    }
}
