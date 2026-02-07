package com.dash.travel.ui.screens.sharing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Share permission levels
 */
enum class SharePermission {
    VIEW_ONLY,
    CAN_COMMENT,
    CAN_EDIT
}

/**
 * Share link settings
 */
data class ShareLinkSettings(
    val isEnabled: Boolean = false,
    val permission: SharePermission = SharePermission.VIEW_ONLY,
    val requiresPassword: Boolean = false,
    val password: String? = null,
    val expiresAt: String? = null,
    val shareUrl: String? = null
)

/**
 * Shared user data
 */
data class SharedUser(
    val id: String,
    val email: String,
    val name: String?,
    val avatarUrl: String? = null,
    val permission: SharePermission,
    val isPending: Boolean = false
)

/**
 * Share Trip Screen - Configure sharing options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTripScreen(
    tripTitle: String,
    shareSettings: ShareLinkSettings,
    sharedUsers: List<SharedUser>,
    onNavigateBack: () -> Unit,
    onTogglePublicLink: (Boolean) -> Unit,
    onChangePermission: (SharePermission) -> Unit,
    onCopyLink: () -> Unit,
    onShowQrCode: () -> Unit,
    onInviteUser: (String, SharePermission) -> Unit,
    onRemoveUser: (String) -> Unit,
    onChangeUserPermission: (String, SharePermission) -> Unit,
    onRegenerateLink: () -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var linkCopied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Trip") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Trip header
            item {
                TripShareHeader(tripTitle = tripTitle)
            }

            // Public link section
            item {
                PublicLinkSection(
                    settings = shareSettings,
                    onToggle = onTogglePublicLink,
                    onChangePermission = onChangePermission,
                    onCopyLink = {
                        onCopyLink()
                        linkCopied = true
                    },
                    onShowQrCode = onShowQrCode,
                    onRegenerateLink = onRegenerateLink,
                    linkCopied = linkCopied
                )
            }

            // Shared users section
            if (sharedUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "Shared With",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                sharedUsers.forEach { user ->
                    item(key = user.id) {
                        SharedUserCard(
                            user = user,
                            onRemove = { onRemoveUser(user.id) },
                            onChangePermission = { perm -> onChangeUserPermission(user.id, perm) }
                        )
                    }
                }
            }

            // Invite prompt
            item {
                InvitePromptCard(
                    onInvite = { showInviteDialog = true }
                )
            }
        }
    }

    // Invite dialog
    if (showInviteDialog) {
        InviteUserSheet(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, permission ->
                onInviteUser(email, permission)
                showInviteDialog = false
            }
        )
    }
}

@Composable
private fun TripShareHeader(tripTitle: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = tripTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Configure sharing settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PublicLinkSection(
    settings: ShareLinkSettings,
    onToggle: (Boolean) -> Unit,
    onChangePermission: (SharePermission) -> Unit,
    onCopyLink: () -> Unit,
    onShowQrCode: () -> Unit,
    onRegenerateLink: () -> Unit,
    linkCopied: Boolean
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (settings.isEnabled) Icons.Default.Public else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (settings.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Public Link",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (settings.isEnabled) "Anyone with link can access"
                            else "Only invited people can access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            AnimatedVisibility(visible = settings.isEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission selector
                    Text(
                        text = "Access Level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SharePermission.entries.forEachIndexed { index, permission ->
                            SegmentedButton(
                                selected = settings.permission == permission,
                                onClick = { onChangePermission(permission) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = SharePermission.entries.size
                                )
                            ) {
                                Text(
                                    when (permission) {
                                        SharePermission.VIEW_ONLY -> "View"
                                        SharePermission.CAN_COMMENT -> "Comment"
                                        SharePermission.CAN_EDIT -> "Edit"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Share link display
                    settings.shareUrl?.let { url ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onCopyLink,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (linkCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (linkCopied) "Copied!" else "Copy Link")
                        }

                        FilledTonalButton(onClick = onShowQrCode) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FilledTonalButton(onClick = onRegenerateLink) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedUserCard(
    user: SharedUser,
    onRemove: () -> Unit,
    onChangePermission: (SharePermission) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user.name ?: user.email).first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name ?: user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.isPending) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (user.permission) {
                            SharePermission.VIEW_ONLY -> "Can view"
                            SharePermission.CAN_COMMENT -> "Can comment"
                            SharePermission.CAN_EDIT -> "Can edit"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    SharePermission.entries.forEach { permission ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (permission) {
                                        SharePermission.VIEW_ONLY -> "View only"
                                        SharePermission.CAN_COMMENT -> "Can comment"
                                        SharePermission.CAN_EDIT -> "Can edit"
                                    }
                                )
                            },
                            onClick = {
                                onChangePermission(permission)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    when (permission) {
                                        SharePermission.VIEW_ONLY -> Icons.Default.Visibility
                                        SharePermission.CAN_COMMENT -> Icons.Default.Edit
                                        SharePermission.CAN_EDIT -> Icons.Default.Edit
                                    },
                                    contentDescription = null
                                )
                            },
                            trailingIcon = if (user.permission == permission) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onRemove()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitePromptCard(onInvite: () -> Unit) {
    Surface(
        onClick = onInvite,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Invite Collaborators",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Add friends or family to plan together",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteUserSheet(
    onDismiss: () -> Unit,
    onInvite: (String, SharePermission) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var permission by remember { mutableStateOf(SharePermission.VIEW_ONLY) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Invite to Trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                placeholder = { Text("friend@example.com") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permission Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SharePermission.entries.forEachIndexed { index, perm ->
                    SegmentedButton(
                        selected = permission == perm,
                        onClick = { permission = perm },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SharePermission.entries.size
                        )
                    ) {
                        Text(
                            when (perm) {
                                SharePermission.VIEW_ONLY -> "View"
                                SharePermission.CAN_COMMENT -> "Comment"
                                SharePermission.CAN_EDIT -> "Edit"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onInvite(email, permission) },
                enabled = email.contains("@"),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Invite")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
