package com.dash.travel.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.dash.travel.ui.theme.*
import kotlinx.coroutines.delay

// =============================================================================
// DASH TRIP PLANNER - TOOLTIP MANAGER
// =============================================================================
// Manages first-time user tooltips that disappear after 2 uses

/**
 * Tooltip identifiers for tracking which tooltips have been shown
 */
object TooltipIds {
    const val HOME_TAP_TRIP = "tooltip_home_tap_trip"
    const val HOME_FAB_ADD = "tooltip_home_fab_add"
    const val TRIP_DETAIL_ADD_ITEM = "tooltip_trip_add_item"
    const val TRIP_DETAIL_TIMELINE = "tooltip_trip_timeline"
    const val AI_PLANNER_INTRO = "tooltip_ai_planner_intro"
    const val VOTING_SWIPE = "tooltip_voting_swipe"
    const val DOCUMENT_VAULT_ADD = "tooltip_document_add"
    const val SHARE_TRIP = "tooltip_share_trip"
    const val ADD_TRIP_SAVE = "tooltip_add_trip_save"
}

/**
 * Maximum number of times to show a tooltip
 */
const val MAX_TOOLTIP_SHOWS = 2

/**
 * Manages tooltip display state and persistence
 */
class TooltipManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "dash_tooltips",
        Context.MODE_PRIVATE
    )
    
    /**
     * Get the number of times a tooltip has been shown
     */
    fun getShowCount(tooltipId: String): Int {
        return prefs.getInt(tooltipId, 0)
    }
    
    /**
     * Check if a tooltip should be shown (hasn't exceeded max shows)
     */
    fun shouldShow(tooltipId: String): Boolean {
        return getShowCount(tooltipId) < MAX_TOOLTIP_SHOWS
    }
    
    /**
     * Mark a tooltip as shown (increment counter)
     */
    fun markShown(tooltipId: String) {
        val currentCount = getShowCount(tooltipId)
        prefs.edit().putInt(tooltipId, currentCount + 1).apply()
    }
    
    /**
     * Reset a specific tooltip
     */
    fun reset(tooltipId: String) {
        prefs.edit().remove(tooltipId).apply()
    }
    
    /**
     * Reset all tooltips (for testing or "show hints again" feature)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
    }
}

/**
 * Composable to provide TooltipManager through composition
 */
val LocalTooltipManager = compositionLocalOf<TooltipManager?> { null }

@Composable
fun rememberTooltipManager(): TooltipManager {
    val context = LocalContext.current
    return remember { TooltipManager(context) }
}

/**
 * Tooltip overlay with arrow pointing to target
 */
@Composable
fun TooltipOverlay(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.TouchApp,
    position: TooltipPosition = TooltipPosition.BOTTOM,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300) // Small delay before showing
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { if (position == TooltipPosition.BOTTOM) -20 else 20 }
        ),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SurfaceContainerHighest,
                            SurfaceContainerHigh
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

enum class TooltipPosition {
    TOP, BOTTOM, START, END
}

/**
 * Smart tooltip that auto-manages visibility based on show count
 */
@Composable
fun SmartTooltip(
    tooltipId: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.TouchApp,
    position: TooltipPosition = TooltipPosition.BOTTOM,
    delayMs: Long = 500
) {
    val tooltipManager = LocalTooltipManager.current ?: rememberTooltipManager()
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(tooltipId) {
        if (tooltipManager.shouldShow(tooltipId)) {
            delay(delayMs)
            visible = true
        }
    }
    
    if (visible) {
        TooltipOverlay(
            message = message,
            modifier = modifier,
            icon = icon,
            position = position,
            onDismiss = {
                tooltipManager.markShown(tooltipId)
                visible = false
            }
        )
    }
}

/**
 * Tooltip with pulse animation on target
 */
@Composable
fun PulseTooltipTarget(
    showPulse: Boolean,
    modifier: Modifier = Modifier,
    pulseColor: Color = Primary,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (showPulse) 1f else 0f,
        animationSpec = tween(500),
        label = "pulseAlpha"
    )
    
    Box(modifier = modifier) {
        content()
        
        if (showPulse) {
            // Pulse ring effect
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.alpha = alpha * 0.3f
                    }
                    .background(
                        color = pulseColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

/**
 * Feature tour step data
 */
data class TourStep(
    val tooltipId: String,
    val message: String,
    val icon: ImageVector = Icons.Default.TouchApp,
    val targetDescription: String = "" // For accessibility
)

/**
 * Feature tour manager state
 */
class FeatureTourState(
    private val steps: List<TourStep>,
    private val tooltipManager: TooltipManager
) {
    var currentStepIndex by mutableStateOf(0)
        private set
    
    val currentStep: TourStep?
        get() = steps.getOrNull(currentStepIndex)
    
    val isComplete: Boolean
        get() = currentStepIndex >= steps.size
    
    val progress: Float
        get() = if (steps.isEmpty()) 1f else currentStepIndex.toFloat() / steps.size
    
    fun next() {
        currentStep?.let { step ->
            tooltipManager.markShown(step.tooltipId)
        }
        currentStepIndex++
    }
    
    fun skip() {
        steps.forEach { step ->
            tooltipManager.markShown(step.tooltipId)
        }
        currentStepIndex = steps.size
    }
}

@Composable
fun rememberFeatureTourState(
    steps: List<TourStep>,
    tooltipManager: TooltipManager = rememberTooltipManager()
): FeatureTourState {
    return remember(steps) {
        FeatureTourState(steps, tooltipManager)
    }
}

/**
 * Full-screen tour overlay
 */
@Composable
fun FeatureTourOverlay(
    tourState: FeatureTourState,
    modifier: Modifier = Modifier
) {
    val currentStep = tourState.currentStep
    
    if (currentStep != null && !tourState.isComplete) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { tourState.next() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { tourState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Primary,
                    trackColor = SurfaceBorder
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Icon
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = PrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Message
                Text(
                    text = currentStep.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(onClick = { tourState.skip() }) {
                        Text(
                            text = "Skip tour",
                            color = OnSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = { tourState.next() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text(
                            text = if (tourState.currentStepIndex == 0) "Got it" 
                                   else "Next"
                        )
                    }
                }
            }
        }
    }
}
