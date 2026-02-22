package com.dash.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dash.travel.R

@Composable
fun QuickActionsRow(
    onManageMembers: () -> Unit,
    onViewDocs: () -> Unit,
    onShare: () -> Unit,
    onViewVoting: () -> Unit,
    onAskAI: () -> Unit = {}
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // AI Suggestions chip FIRST for visibility
        item { PremiumAIChip(onAskAI) }
        
        // Prominent Action Chips
        item { QuickActionChip(stringResource(R.string.action_vote), Icons.Default.HowToVote, onViewVoting) }
        item { QuickActionChip(stringResource(R.string.action_docs), Icons.Default.Description, onViewDocs) }
        item { QuickActionChip(stringResource(R.string.action_members), Icons.Default.Group, onManageMembers) }
        item { QuickActionChip(stringResource(R.string.action_share), Icons.Default.Share, onShare) }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Make chip more prominent with elevation and border
    androidx.compose.material3.ElevatedAssistChip(
        onClick = onClick,
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            ) 
        },
        colors = AssistChipDefaults.elevatedAssistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = AssistChipDefaults.elevatedAssistChipElevation(elevation = 2.dp),
        border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
fun PremiumAIChip(
    onClick: () -> Unit
) {
    // Premium Gradient Style
    val brush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .wrapContentSize()
            // Add a subtle shadow/glow
            .shadow(4.dp, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(horizontal = 16.dp, vertical = 10.dp), // Slightly larger than standard chips
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.ai_ask),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
