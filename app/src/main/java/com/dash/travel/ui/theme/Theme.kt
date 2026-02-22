package com.dash.travel.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// =============================================================================
// DASH TRIP PLANNER - MATERIAL 3 THEME
// =============================================================================

// Dark Color Scheme (Primary)
private val DashDarkColorScheme = darkColorScheme(
    // Primary Colors
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = PrimaryGradientEnd,
    
    // Secondary Colors
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    
    // Tertiary Colors
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    
    // Background & Surface
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Primary,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    
    // Error Colors
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    
    // Other
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.5f)
)

// Light Color Scheme (Optional - for future light mode support)
private val DashLightColorScheme = lightColorScheme(
    // Primary Colors
    primary = PrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = PrimaryGradientEnd.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryVariant,
    inversePrimary = PrimaryGradientStart,
    
    // Secondary Colors
    secondary = SecondaryVariant,
    onSecondary = Color.White,
    secondaryContainer = Secondary.copy(alpha = 0.2f),
    onSecondaryContainer = SecondaryVariant,
    
    // Tertiary Colors
    tertiary = TertiaryVariant,
    onTertiary = Color.White,
    tertiaryContainer = Tertiary.copy(alpha = 0.2f),
    onTertiaryContainer = TertiaryVariant,
    
    // Background & Surface
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1F2937),
    surface = Color.White,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceTint = Primary,
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color.White,
    
    // Containers
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    
    // Error Colors
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error,
    
    // Other
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9),
    scrim = Color.Black.copy(alpha = 0.3f)
)

/**
 * Main theme composable for Dash Trip Planner
 * 
 * @param darkTheme Whether to use dark theme (default: true for this app)
 * @param dynamicColor Whether to use dynamic colors from Android 12+ (default: false to maintain brand consistency)
 * @param content The content to be themed
 */
@Composable
fun DashTheme(
    darkTheme: Boolean = true, // Default to dark theme
    dynamicColor: Boolean = false, // Keep brand colors consistent
    content: @Composable () -> Unit
) {
    // Force Dark Theme
    val colorScheme = DashDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Background.toArgb()
            
            // Force light status/nav icons only if background is light, but here background is dark
            // So we want light content (white icons) -> isAppearanceLight... = false
            val wic = WindowCompat.getInsetsController(window, view)
            wic.isAppearanceLightStatusBars = false
            wic.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DashShapes,
        content = content
    )
}
