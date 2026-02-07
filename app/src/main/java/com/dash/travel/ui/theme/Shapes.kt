package com.dash.travel.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// DASH TRIP PLANNER - SHAPE SYSTEM
// =============================================================================
// Consistent corner radii for a cohesive visual language

/**
 * Shape definitions for the Dash app
 * 
 * Usage Guidelines:
 * - extraSmall (4dp): Tiny elements like badges, indicators
 * - small (8dp): Chips, small buttons, tags
 * - medium (12dp): Cards, dialogs, input fields
 * - large (16dp): Bottom sheets, large cards
 * - extraLarge (24dp): Full-screen modals, hero sections
 */
val DashShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Additional custom shapes for specific use cases
val BottomSheetShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

val TopBarShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp
)

val PillShape = RoundedCornerShape(50)

val CardShape = RoundedCornerShape(16.dp)

val ButtonShape = RoundedCornerShape(12.dp)

val ChipShape = RoundedCornerShape(8.dp)

val AvatarShape = RoundedCornerShape(50)

val TimelineDotShape = RoundedCornerShape(50)
