package com.dash.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
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
    onViewMaps: () -> Unit,
    onManageMembers: () -> Unit,
    onViewDocs: () -> Unit,
    onShare: () -> Unit,
    onViewChat: () -> Unit,
    onViewReminders: () -> Unit,
    onViewVoting: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        item { QuickActionChip(stringResource(R.string.action_map), Icons.Default.Map, onViewMaps) }
        item { QuickActionChip(stringResource(R.string.action_vote), Icons.Default.HowToVote, onViewVoting) }
        item { QuickActionChip(stringResource(R.string.action_docs), Icons.Default.Description, onViewDocs) }
        item { QuickActionChip(stringResource(R.string.action_reminders), Icons.Default.Checklist, onViewReminders) }

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
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
