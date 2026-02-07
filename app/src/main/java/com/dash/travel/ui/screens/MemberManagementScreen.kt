package com.dash.travel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.components.MemberManagementSection
import com.dash.travel.ui.components.MemberRole
import com.dash.travel.ui.components.TripMemberData
import com.dash.travel.ui.theme.Background
import com.dash.travel.ui.theme.OnBackground
import com.dash.travel.ui.theme.OnSurfaceVariant
import com.dash.travel.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    tripId: String,
    members: List<TripMemberData>,
    isCurrentUserOwner: Boolean,
    onNavigateBack: () -> Unit,
    onInviteMember: (String, MemberRole) -> Unit,
    onChangeRole: (String, MemberRole) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Team", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Reusing the Section Logic but wrapped in a screen
            // or implementing a vertical list which is better for "Management"
            
            MemberManagementSection(
                members = members,
                isCurrentUserOwner = isCurrentUserOwner,
                onInviteMember = { email, role -> onInviteMember(email, role) },
                onChangeRole = onChangeRole,
                onRemoveMember = onRemoveMember,
                onCopyInviteLink = { 
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Join my trip on Dash! Link: https://dash.travel/trip/$tripId")
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Trip")
                    context.startActivity(shareIntent)
                },
                onGenerateInviteLink = { 
                     // Same for now
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Join my trip on Dash! Link: https://dash.travel/trip/$tripId")
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Trip")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Helpful text
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(
                    "Editors can add and modify itinerary items. Viewers can only see the trip and vote.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
