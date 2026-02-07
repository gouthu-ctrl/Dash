package com.dash.travel.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.ui.theme.*

// =============================================================================
// DASH TRIP PLANNER - CORE UI COMPONENTS
// =============================================================================

/**
 * Premium gradient button with press animation
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradientColors: List<Color> = PrimaryGradient
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(ButtonShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) gradientColors else listOf(
                        OnBackgroundTertiary,
                        OnBackgroundTertiary
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) OnPrimary else OnSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) OnPrimary else OnSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Secondary outlined button
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Primary
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(PrimaryGradient)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Glassmorphism card with blur effect (simulated)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant.copy(alpha = 0.7f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    GlassBorder,
                    GlassBorder.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Premium surface card with elevation
 */
@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = CardShape,
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = Surface
        )
    ) {
        Column(content = content)
    }
}

/**
 * Pro badge pill
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(
                brush = Brush.horizontalGradient(ProGradient)
            )
            .padding(
                horizontal = if (small) 6.dp else 10.dp,
                vertical = if (small) 2.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!small) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Black
                )
            }
            Text(
                text = "PRO",
                style = if (small) MaterialTheme.typography.labelSmall 
                       else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

/**
 * Status chip with icon
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Primary,
    containerColor: Color = color.copy(alpha = 0.15f)
) {
    Surface(
        modifier = modifier,
        shape = ChipShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Avatar with optional online indicator
 */
@Composable
fun UserAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    name: String = "",
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false
) {
    Box(modifier = modifier) {
        if (imageUrl.isNullOrEmpty()) {
            // Fallback to initials
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(PrimaryGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        // Online indicator
        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(size / 4)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) StatusOnline else StatusOffline)
            )
        }
    }
}

/**
 * Avatar stack for showing multiple members
 */
@Composable
fun AvatarStack(
    avatars: List<Pair<String?, String>>, // Pair of (imageUrl, name)
    modifier: Modifier = Modifier,
    avatarSize: Dp = 32.dp,
    maxDisplay: Int = 4,
    overlap: Dp = 10.dp
) {
    val displayAvatars = avatars.take(maxDisplay)
    val remaining = avatars.size - maxDisplay
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            displayAvatars.forEachIndexed { index, (url, name) ->
                Box(
                    modifier = Modifier
                        .padding(start = (index * (avatarSize - overlap).value).dp)
                        .border(2.dp, Background, CircleShape)
                ) {
                    UserAvatar(
                        imageUrl = url,
                        name = name,
                        size = avatarSize
                    )
                }
            }
            
            // +N indicator
            if (remaining > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = (displayAvatars.size * (avatarSize - overlap).value).dp)
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Timeline dot indicator with type-based colors
 */
@Composable
fun TimelineDot(
    type: String,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    showPulse: Boolean = false
) {
    val color = when (type.lowercase()) {
        "flight" -> TimelineFlight
        "stay", "hotel" -> TimelineStay
        "food", "eat", "restaurant" -> TimelineFood
        "activity" -> TimelineActivity
        "transport" -> TimelineTransport
        else -> TimelineNote
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (showPulse) {
            // Inner pulse dot
            Box(
                modifier = Modifier
                    .size(size / 2)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * Section header with optional action
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.SemiBold
        )
        if (action != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary
                )
            }
        }
    }
}

/**
 * Empty state placeholder
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = PrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}

/**
 * Shimmer loading placeholder
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    // Simple placeholder - in production, add shimmer animation
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardShape)
            .background(SurfaceVariant.copy(alpha = 0.5f))
    )
}

/**
 * Divider with text
 */
@Composable
fun DividerWithText(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SurfaceBorder
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SurfaceBorder
        )
    }
}

/**
 * Count badge (for notifications, etc.)
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    color: Color = Error
) {
    if (count <= 0) return
    
    val displayText = if (count > maxCount) "$maxCount+" else count.toString()
    
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Info banner
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    type: BannerType = BannerType.INFO,
    onDismiss: (() -> Unit)? = null
) {
    val (containerColor, contentColor) = when (type) {
        BannerType.INFO -> InfoContainer to Info
        BannerType.SUCCESS -> SuccessContainer to Success
        BannerType.WARNING -> WarningContainer to Warning
        BannerType.ERROR -> ErrorContainer to Error
    }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class BannerType {
    INFO, SUCCESS, WARNING, ERROR
}
