package com.dash.travel.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// DASH TRIP PLANNER - PREMIUM COLOR PALETTE
// =============================================================================
// A sophisticated dark theme with teal/mint gradients and premium accents

// -----------------------------------------------------------------------------
// PRIMARY GRADIENT (Teal to Mint)
// -----------------------------------------------------------------------------
val PrimaryGradientStart = Color(0xFF00BFA6)   // Teal - main brand color
val PrimaryGradientEnd = Color(0xFF64FFDA)     // Mint - gradient end
val Primary = Color(0xFF00BFA6)                 // Primary actions
val PrimaryVariant = Color(0xFF00897B)          // Pressed/darker state
val PrimaryContainer = Color(0xFF1A3D38)        // Container backgrounds
val OnPrimary = Color(0xFF003731)               // Text on primary
val OnPrimaryContainer = Color(0xFF70F7DB)      // Text on primary container

// Secondary (Complementary Purple for AI features)
val Secondary = Color(0xFFBB86FC)               // AI/smart features accent
val SecondaryVariant = Color(0xFF9A67EA)        // Pressed state
val SecondaryContainer = Color(0xFF2D2040)      // AI feature containers
val OnSecondary = Color(0xFF1F1A24)             // Text on secondary
val OnSecondaryContainer = Color(0xFFE8DEF8)    // Text on secondary container

// Tertiary (Warm accent for hearts/favorites)
val Tertiary = Color(0xFFFF6B9D)                // Hearts, favorites, love
val TertiaryVariant = Color(0xFFE91E63)         // Pressed state
val TertiaryContainer = Color(0xFF3D1F2A)       // Favorite containers
val OnTertiary = Color.White
val OnTertiaryContainer = Color(0xFFFFD9E2)

// -----------------------------------------------------------------------------
// SURFACE HIERARCHY (GitHub-dark inspired)
// -----------------------------------------------------------------------------
val Background = Color.Black                // Amoled Black
val Surface = Color(0xFF161B22)                 // Card backgrounds
val SurfaceVariant = Color(0xFF21262D)          // Elevated cards, modals
val SurfaceBright = Color(0xFF2D333B)           // Highlighted surfaces
val SurfaceContainer = Color(0xFF1C2128)        // Input fields, chips
val SurfaceContainerHigh = Color(0xFF262C34)    // Elevated containers
val SurfaceContainerHighest = Color(0xFF30363D) // Highest elevation
val SurfaceBorder = Color(0xFF30363D)           // Subtle borders
val InverseSurface = Color(0xFFF0F6FC)          // Light mode fallback

// -----------------------------------------------------------------------------
// TEXT COLORS
// -----------------------------------------------------------------------------
val OnBackground = Color(0xFFF0F6FC)            // Primary text
val OnBackgroundSecondary = Color(0xFF8B949E)   // Secondary text
val OnBackgroundTertiary = Color(0xFF6E7681)    // Disabled/hint text
val OnSurface = Color(0xFFF0F6FC)               // Text on surfaces
val OnSurfaceVariant = Color(0xFF8B949E)        // Secondary on surfaces
val InverseOnSurface = Color(0xFF0D1117)        // Text on inverse surface

// -----------------------------------------------------------------------------
// SEMANTIC COLORS
// -----------------------------------------------------------------------------
// Success (Confirmations, completed items)
val Success = Color(0xFF3FB950)                 // Green for success
val SuccessContainer = Color(0xFF1A3226)        // Success backgrounds
val OnSuccess = Color.White
val OnSuccessContainer = Color(0xFF7EE787)

// Warning (Alerts, expiring items)
val Warning = Color(0xFFFFAB40)                 // Orange for warnings
val WarningContainer = Color(0xFF3D2E1A)        // Warning backgrounds
val OnWarning = Color(0xFF1F1A24)
val OnWarningContainer = Color(0xFFFFD180)

// Error (Failures, cancellations)
val Error = Color(0xFFF85149)                   // Red for errors
val ErrorContainer = Color(0xFF3D1A1A)          // Error backgrounds
val OnError = Color.White
val OnErrorContainer = Color(0xFFFFB4AB)

// Info (Informational messages)
val Info = Color(0xFF58A6FF)                    // Blue for info
val InfoContainer = Color(0xFF1A2A3D)           // Info backgrounds
val OnInfo = Color.White
val OnInfoContainer = Color(0xFF79C0FF)

// -----------------------------------------------------------------------------
// SPECIAL PURPOSE COLORS
// -----------------------------------------------------------------------------
// Pro Badge (Premium features)
val ProGold = Color(0xFFFFD700)                 // Gold for Pro badge
val ProGradientStart = Color(0xFFFFD700)        // Pro gradient start
val ProGradientEnd = Color(0xFFFFA500)          // Pro gradient end
val ProContainer = Color(0xFF3D351A)            // Pro feature containers

// Voting Indicators
val VoteUp = Color(0xFF3FB950)                  // Upvote green
val VoteDown = Color(0xFFF85149)                // Downvote red
val VoteHeart = Color(0xFFFF6B9D)               // Heart/love pink
val VoteLike = Color(0xFF58A6FF)                // Like blue

// Status Indicators
val StatusOnline = Color(0xFF3FB950)            // Online presence
val StatusOffline = Color(0xFF6E7681)           // Offline
val StatusBusy = Color(0xFFFFAB40)              // Away/busy
val StatusTyping = Color(0xFF58A6FF)            // Typing indicator

// Timeline Dot Colors
val TimelineFlight = Color(0xFF58A6FF)          // Flights - blue
val TimelineStay = Color(0xFFBB86FC)            // Hotels - purple
val TimelineFood = Color(0xFFFFAB40)            // Restaurants - orange
val TimelineActivity = Color(0xFF3FB950)        // Activities - green
val TimelineTransport = Color(0xFF8B949E)       // Transport - gray
val TimelineNote = Color(0xFFF0F6FC)            // Notes - white

// -----------------------------------------------------------------------------
// GLASSMORPHISM EFFECTS
// -----------------------------------------------------------------------------
val GlassSurface = Color(0x33FFFFFF)            // 20% white overlay
val GlassBorder = Color(0x4DFFFFFF)             // 30% white border
val GlassShadow = Color(0x1A000000)             // 10% black shadow

// -----------------------------------------------------------------------------
// GRADIENT DEFINITIONS (as pairs for use with Brush.linearGradient)
// -----------------------------------------------------------------------------
val PrimaryGradient = listOf(PrimaryGradientStart, PrimaryGradientEnd)
val ProGradient = listOf(ProGradientStart, ProGradientEnd)
val SurfaceGradient = listOf(Surface, SurfaceVariant)
val HeroOverlayGradient = listOf(
    Color.Transparent,  // Transparent at top
    Color.Black.copy(alpha = 0.8f)   // 80% black at bottom
)

// -----------------------------------------------------------------------------
// LEGACY COMPAT (for existing code that uses old color names)
// -----------------------------------------------------------------------------
val DarkBlue = Background
val Purple = Secondary
val Pink = Tertiary
val LightPink = Color(0xFFFF8AD8)
val TextFieldBackground = SurfaceContainer
val PlaceholderText = OnBackgroundTertiary
