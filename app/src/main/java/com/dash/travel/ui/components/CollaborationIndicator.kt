package com.dash.travel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Represents an active editor currently viewing/editing the trip
 */
data class ActiveEditor(
    val userId: String,
    val userName: String,
    val avatarUrl: String? = null,
    val isEditing: Boolean = false,  // true if actively typing/editing
    val editingSection: String? = null // e.g., "itinerary", "details", etc.
)

/**
 * Banner showing active editors at the top of a screen
 */
@Composable
fun ActiveEditorsBanner(
    activeEditors: List<ActiveEditor>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeEditors.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stacked avatars
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    activeEditors.take(3).forEach { editor ->
                        EditorAvatar(
                            editor = editor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Text description
                val text = when {
                    activeEditors.size == 1 -> {
                        val editor = activeEditors.first()
                        if (editor.isEditing) {
                            "${editor.userName} is editing..."
                        } else {
                            "${editor.userName} is viewing"
                        }
                    }
                    activeEditors.any { it.isEditing } -> {
                        val editingCount = activeEditors.count { it.isEditing }
                        "$editingCount people editing"
                    }
                    else -> "${activeEditors.size} people viewing"
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )

                // Pulsing edit indicator if someone is editing
                if (activeEditors.any { it.isEditing }) {
                    PulsingEditIndicator()
                }
            }
        }
    }
}

/**
 * Small avatar with optional editing indicator
 */
@Composable
private fun EditorAvatar(
    editor: ActiveEditor,
    modifier: Modifier = Modifier
) {
    val borderColor = if (editor.isEditing) {
        Color(0xFF22C55E) // Green for editing
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(modifier = modifier) {
        if (editor.avatarUrl != null) {
            AsyncImage(
                model = editor.avatarUrl,
                contentDescription = editor.userName,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Initial avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(getEditorColor(editor.userId))
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = editor.userName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Small editing indicator
        if (editor.isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Pulsing indicator showing active editing
 */
@Composable
private fun PulsingEditIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = CircleShape,
        color = Color(0xFF22C55E).copy(alpha = 0.2f),
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Editing",
            tint = Color(0xFF22C55E),
            modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
        )
    }
}

/**
 * Inline indicator that shows on a specific item being edited
 */
@Composable
fun ItemEditingIndicator(
    editorName: String,
    editorAvatarUrl: String? = null,
    editorId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color(0xFF22C55E).copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Small avatar
            if (editorAvatarUrl != null) {
                AsyncImage(
                    model = editorAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(getEditorColor(editorId)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = editorName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "$editorName editing",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Medium
            )

            // Animated typing indicator dots
            TypingDots()
        }
    }
}

/**
 * Animated typing dots indicator
 */
@Composable
private fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typingDots")

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val delay = index * 150
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A).copy(alpha = alpha))
            )
        }
    }
}

/**
 * Toast-style notification for collaboration events
 */
@Composable
fun CollaborationToast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    durationMs: Long = 3000L,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 4.dp
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * Generate a consistent color for an editor based on their ID
 */
private fun getEditorColor(userId: String): Color {
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF97316), // Orange
        Color(0xFF22C55E), // Green
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFFF59E0B), // Amber
    )
    val hash = userId.hashCode().let { if (it < 0) -it else it }
    return colors[hash % colors.size]
}
