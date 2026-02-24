package com.dash.travel.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dash.travel.ui.components.GradientButton
import com.dash.travel.ui.components.ProBadge
import com.dash.travel.ui.theme.*
import kotlinx.coroutines.launch

// =============================================================================
// DASH TRIP PLANNER - ONBOARDING SCREEN
// =============================================================================
// First-launch only guided tour after sign-in
// Warm, conversational tone — respects user's time

/**
 * Onboarding page content with optional Pro badge
 */
data class OnboardingPage(
    val icon: ImageVector,
    val emoji: String,
    val title: String,
    val description: String,
    val gradient: List<Color>,
    val isPro: Boolean = false
)

/**
 * Enhanced Onboarding Screen - First-time user experience
 * 
 * Design:
 * - Minimal, warm, conversational copy
 * - Swipeable horizontal pager
 * - Animated page indicators
 * - Pro feature badges
 * - Skip and Next navigation
 * - Only shown on first launch (managed by TooltipManager / SharedPrefs)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Explore,
            emoji = "👋",
            title = "Welcome aboard!",
            description = "We'll keep all your trips, flights, hotels & plans\nperfectly organized in one place.",
            gradient = listOf(PrimaryGradientStart, PrimaryGradientEnd)
        ),
        OnboardingPage(
            icon = Icons.Default.Groups,
            emoji = "🤝",
            title = "Better together",
            description = "Invite your travel crew — everyone can\nadd, vote, and edit in real-time.",
            gradient = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        ),
        OnboardingPage(
            icon = Icons.Default.AutoAwesome,
            emoji = "🤖",
            title = "Your AI travel buddy",
            description = "Just describe your dream trip and we'll\nbuild the itinerary for you.",
            gradient = listOf(Secondary, Color(0xFFE040FB))
        ),
        OnboardingPage(
            icon = Icons.Default.CloudDone,
            emoji = "📴",
            title = "No Wi-Fi? No problem.",
            description = "Download your trips before you go.\nEverything works offline.",
            gradient = listOf(Success, Color(0xFF4CAF50))
        ),
        OnboardingPage(
            icon = Icons.Default.RocketLaunch,
            emoji = "🎉",
            title = "You're all set!",
            description = "Let's plan your first adventure.\nWe'll guide you through it.",
            gradient = PrimaryGradient
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Main pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // Top skip button
        AnimatedVisibility(
            visible = !isLastPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = OnSurfaceVariant
                )
            ) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Bottom navigation
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(pages[pagerState.currentPage].gradient)
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            SurfaceBorder,
                                            SurfaceBorder
                                        )
                                    )
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation button (full width on last page)
            if (isLastPage) {
                GradientButton(
                    text = "Let's Go!",
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    gradientColors = pages[pagerState.currentPage].gradient
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[pagerState.currentPage].gradient[0]
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Next",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "pageScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Emoji above the icon
        Text(
            text = page.emoji,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(page.gradient)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Pro badge (if applicable)
        if (page.isPro) {
            ProBadge()
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = OnBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = OnBackgroundSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Extra spacer for bottom nav
        Spacer(modifier = Modifier.height(120.dp))
    }
}
