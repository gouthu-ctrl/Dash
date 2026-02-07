package com.dash.travel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.screens.collaborative.VoteableItem
import com.dash.travel.ui.screens.collaborative.VoteType
import com.dash.travel.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    tripId: String,
    items: List<VoteableItem>,
    onNavigateBack: () -> Unit,
    onVote: (String, VoteType) -> Unit
) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Group Voting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No proposed items to vote on yet.", color = OnSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(innerPadding)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    VotingCard(item = item, onVote = { type -> onVote(item.id, type) })
                }
            }
        }
    }
}

@Composable
fun VotingCard(
    item: VoteableItem,
    onVote: (VoteType) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (item.subtitle.isNotBlank()) {
                         Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                // Thumbs Up
                VoteButton(
                    icon = Icons.Default.ThumbUp,
                    count = item.voteState.upvotes,
                    isSelected = item.currentUserVote == VoteType.UPVOTE,
                    onClick = { onVote(VoteType.UPVOTE) }
                )
                
                // Thumbs Down
                VoteButton(
                    icon = Icons.Default.ThumbDown,
                    count = item.voteState.downvotes,
                    isSelected = item.currentUserVote == VoteType.DOWNVOTE,
                    onClick = { onVote(VoteType.DOWNVOTE) }
                )
                

            }
        }
    }
}

@Composable
fun VoteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}
