package com.dash.travel.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// =============================================================================
// DASH TRIP PLANNER - ANIMATION CONSTANTS
// =============================================================================
// Consistent animation specs for smooth, premium feel

/**
 * Duration Constants (in milliseconds)
 */
object AnimationDuration {
    const val Instant = 0
    const val Fast = 150
    const val Medium = 300
    const val Slow = 500
    const val Stagger = 50  // Delay between staggered items
}

/**
 * Spring Animation Specs
 * Use for natural, physics-based animations (recommended for most cases)
 */
object SpringSpecs {
    // Quick, responsive spring for small UI feedback
    val Quick: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    // Standard spring for general animations
    val Standard: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Gentle spring for larger movements
    val Gentle: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    // No bounce spring for subtle movements
    val NoBounce: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Tween Animation Specs
 * Use for precise timing control (e.g., fades, delays)
 */
object TweenSpecs {
    val Fast = tween<Float>(
        durationMillis = AnimationDuration.Fast,
        easing = FastOutSlowInEasing
    )
    
    val Medium = tween<Float>(
        durationMillis = AnimationDuration.Medium,
        easing = FastOutSlowInEasing
    )
    
    val Slow = tween<Float>(
        durationMillis = AnimationDuration.Slow,
        easing = FastOutSlowInEasing
    )
}

/**
 * Stagger Delay Calculator
 * For animating lists with cascading effect
 */
fun staggerDelay(index: Int, baseDelay: Int = AnimationDuration.Stagger): Int {
    return index * baseDelay
}

/**
 * Common animation values
 */
object AnimationValues {
    // Scale
    const val ScalePressed = 0.95f
    const val ScaleNormal = 1.0f
    const val ScalePop = 1.05f
    
    // Opacity
    const val OpacityDisabled = 0.5f
    const val OpacityDimmed = 0.7f
    const val OpacityFull = 1.0f
    
    // Elevation
    const val ElevationPressed = 2f
    const val ElevationNormal = 4f
    const val ElevationRaised = 8f
    
    // Rotation
    const val RotationChevronClosed = 0f
    const val RotationChevronOpen = 180f
}
