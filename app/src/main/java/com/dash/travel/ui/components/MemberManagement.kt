package com.dash.travel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Member roles for trip collaboration
 */
enum class MemberRole {
    OWNER,
    EDITOR,
    VIEWER
}

/**
 * Member invitation status
 */
enum class MemberStatus {
    ACCEPTED,
    PENDING,
    DECLINED
}

/**
 * Data class for a trip member
 */
data class TripMemberData(
    val id: String,
    val name: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val role: MemberRole,
    val status: MemberStatus
)

/**
 * Enhanced member management section with role display and actions
 */
@Composable
fun MemberManagementSection(
    members: List<TripMemberData>,
    isCurrentUserOwner: Boolean,
    inviteLink: String? = null,
    onInviteMember: (email: String, role: MemberRole) -> Unit,
    onChangeRole: (memberId: String, newRole: MemberRole) -> Unit,
    onRemoveMember: (memberId: String) -> Unit,
    onCopyInviteLink: () -> Unit,
    onGenerateInviteLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showMemberDetails by remember { mutableStateOf<TripMemberData?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trip Members (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isCurrentUserOwner) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Share link button
                    FilledTonalButton(
                        onClick = {
                            if (inviteLink != null) onCopyInviteLink() else onGenerateInviteLink()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (inviteLink != null) "Copy Link" else "Share Link")
                    }

                    // Invite button
                    FilledTonalButton(
                        onClick = { showInviteDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Member list
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members, key = { it.id }) { member ->
                MemberCard(
                    member = member,
                    isCurrentUserOwner = isCurrentUserOwner,
                    onClick = { showMemberDetails = member }
                )
            }
        }
    }

// Invite dialog
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, role ->
                onInviteMember(email, role)
                showInviteDialog = false
            }
        )
    }

    // Member details dialog
    showMemberDetails?.let { member ->
        MemberDetailsDialog(
            member = member,
            isCurrentUserOwner = isCurrentUserOwner,
            onDismiss = { showMemberDetails = null },
            onChangeRole = { newRole ->
                if (isCurrentUserOwner) {
                    onChangeRole(member.id, newRole)
                }
                showMemberDetails = null
            },
            onRemove = {
                onRemoveMember(member.id)
                showMemberDetails = null
            }
        )
    }
}
@Composable
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (String, MemberRole) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(MemberRole.VIEWER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Role Selector
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Role: ${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }}")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editor (Can edit trip)") },
                            onClick = { 
                                selectedRole = MemberRole.EDITOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Viewer (Read only)") },
                            onClick = { 
                                selectedRole = MemberRole.VIEWER
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (email.isNotBlank()) onInvite(email, selectedRole) },
                enabled = email.isNotBlank()
            ) {
                Text("Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MemberCard(
    member: TripMemberData,
    isCurrentUserOwner: Boolean,
    onClick: () -> Unit
) {
    val roleColor = when (member.role) {
        MemberRole.OWNER -> Color(0xFFFFD700) // Gold
        MemberRole.EDITOR -> MaterialTheme.colorScheme.primary
        MemberRole.VIEWER -> MaterialTheme.colorScheme.secondary
    }

    val statusColor = when (member.status) {
        MemberStatus.ACCEPTED -> Color(0xFF22C55E)
        MemberStatus.PENDING -> Color(0xFFF59E0B)
        MemberStatus.DECLINED -> Color(0xFFEF4444)
    }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            // Avatar with role indicator
            Box {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, roleColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.2f))
                            .border(2.dp, roleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }
                }

                // Role badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(roleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (member.role) {
                            MemberRole.OWNER -> Icons.Default.Star
                            MemberRole.EDITOR -> Icons.Default.Edit
                            MemberRole.VIEWER -> Icons.Default.Visibility
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = member.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Status badge
            if (member.status != MemberStatus.ACCEPTED) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = member.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Member") },
        text = {
            Column {
                Text(
                    "Enter the email address of the person you want to invite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email) },
                enabled = email.contains("@")
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MemberDetailsDialog(
    member: TripMemberData,
    isCurrentUserOwner: Boolean,
    onDismiss: () -> Unit,
    onChangeRole: (MemberRole) -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.name) },
        text = {
            Column {
                // Email
                member.email?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current role
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Role selection (only for owners)
                if (isCurrentUserOwner && member.role != MemberRole.OWNER) {
                    // Show chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleChip(
                            label = "Editor",
                            isSelected = member.role == MemberRole.EDITOR,
                            onClick = { onChangeRole(MemberRole.EDITOR) }
                        )
                        RoleChip(
                            label = "Viewer",
                            isSelected = member.role == MemberRole.VIEWER,
                            onClick = { onChangeRole(MemberRole.VIEWER) }
                        )
                    }
                } else {
                    // Read-only text
                    Text(
                        text = member.role.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Status
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Status: ${member.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (isCurrentUserOwner && member.role != MemberRole.OWNER) {
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    )
}

@Composable
private fun RoleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Compact member avatars row for inline display
 */
@Composable
fun MemberAvatarsRow(
    members: List<TripMemberData>,
    maxVisible: Int = 5,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-12).dp)
    ) {
        members.take(maxVisible).forEachIndexed { index, member ->
            val roleColor = when (member.role) {
                MemberRole.OWNER -> Color(0xFFFFD700)
                MemberRole.EDITOR -> MaterialTheme.colorScheme.primary
                MemberRole.VIEWER -> MaterialTheme.colorScheme.secondary
            }

            if (member.avatarUrl != null) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(roleColor.copy(alpha = 0.2f))
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = roleColor
                    )
                }
            }
        }

        // Overflow indicator
        if (members.size > maxVisible) {
            Surface(
                onClick = onViewAll,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+${members.size - maxVisible}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
