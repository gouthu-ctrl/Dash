package com.dash.travel.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dash.travel.R
import com.dash.travel.ui.components.DividerWithText
import com.dash.travel.ui.theme.*
import kotlinx.coroutines.delay

// =============================================================================
// DASH TRIP PLANNER - WELCOME SCREEN
// =============================================================================
// Premium welcome with swipable feature carousel + social sign-in
// Design principles:
//   - One Primary Action: "Continue with Google"
//   - Feature value shown BEFORE sign-in (builds trust)
//   - Auto-advancing carousel with manual swipe support

/**
 * Feature card data for the welcome carousel
 */
private data class WelcomeFeature(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val isPro: Boolean = false
)

private val welcomeFeatures = listOf(
    WelcomeFeature(
        icon = Icons.Default.Explore,
        title = "Organize Your Trips",
        subtitle = "Flights, hotels, activities — all in one beautiful timeline",
        gradient = listOf(PrimaryGradientStart, PrimaryGradientEnd)
    ),
    WelcomeFeature(
        icon = Icons.Default.Groups,
        title = "Plan Together",
        subtitle = "Invite friends & family to collaborate in real-time",
        gradient = listOf(Color(0xFF667eea), Color(0xFF764ba2))
    ),
    WelcomeFeature(
        icon = Icons.Default.AutoAwesome,
        title = "AI-Powered Planning",
        subtitle = "Describe your dream trip, get a full itinerary instantly",
        gradient = listOf(Secondary, Color(0xFFE040FB))
    ),
    WelcomeFeature(
        icon = Icons.Default.CloudDone,
        title = "Works Offline",
        subtitle = "Download trips before you go — no Wi-Fi needed",
        gradient = listOf(Success, Color(0xFF4CAF50))
    ),
    WelcomeFeature(
        icon = Icons.Default.HowToVote,
        title = "Vote Together",
        subtitle = "Can't decide on a restaurant? Let the group vote!",
        gradient = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
    ),
    WelcomeFeature(
        icon = Icons.Default.RocketLaunch,
        title = "More Coming Soon",
        subtitle = "We're always adding new features to make your travels easier ✨",
        gradient = listOf(Warning, Color(0xFFFF6B9D))
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onGoogleLoginClicked: () -> Unit,
    onEmailLoginClicked: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateAngle"
    )
    
    // Entry animation
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800),
        label = "contentAlpha"
    )
    
    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentScale"
    )
    
    // Pager state for feature carousel
    val pagerState = rememberPagerState(pageCount = { welcomeFeatures.size })
    
    // Auto-advance carousel every 4 seconds
    LaunchedEffect(pagerState.currentPage) {
        delay(4000)
        val nextPage = (pagerState.currentPage + 1) % welcomeFeatures.size
        pagerState.animateScrollToPage(nextPage)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Background,
                        Surface,
                        Background
                    )
                )
            )
    ) {
        // Floating decorative elements in background
        FloatingElements(floatOffset, rotateAngle)
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alpha(contentAlpha)
                .scale(contentScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Hero section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Logo with glow effect
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Glow background
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Primary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.dash_logo),
                        contentDescription = "Dash Logo",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Headline with gradient
                Text(
                    text = buildAnnotatedString {
                        append("Plan trips ")
                        withStyle(
                            style = SpanStyle(
                                brush = Brush.horizontalGradient(PrimaryGradient)
                            )
                        ) {
                            append("together")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subheadline
                Text(
                    text = "The smartest way to travel with\nfriends, family & AI",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackgroundSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // ─── Feature Carousel ───
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) { page ->
                    FeatureCarouselCard(feature = welcomeFeatures[page])
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(welcomeFeatures.size) { index ->
                        val selected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (selected) 20.dp else 6.dp,
                            animationSpec = tween(300),
                            label = "indicatorWidth"
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        Brush.horizontalGradient(welcomeFeatures[pagerState.currentPage].gradient)
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(SurfaceBorder, SurfaceBorder)
                                        )
                                    }
                                )
                        )
                    }
                }
            }
            
            // Sign in section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // Google Sign In (Primary action)
                Button(
                    onClick = onGoogleLoginClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Gray,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Email option
                if (onEmailLoginClicked != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DividerWithText(text = "or")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onEmailLoginClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OnBackground
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Email",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Terms
                Text(
                    text = "By continuing, you agree to our Terms of Service\nand Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// =============================================================================
// Feature Carousel Card
// =============================================================================

@Composable
private fun FeatureCarouselCard(feature: WelcomeFeature) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient accent stripe at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(feature.gradient))
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(feature.gradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = feature.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        if (feature.isPro) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ProGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = feature.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// Floating Decorative Elements
// =============================================================================

@Composable
private fun FloatingElements(floatOffset: Float, rotateAngle: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top right floating circle
        Box(
            modifier = Modifier
                .offset(x = 280.dp, y = (80 + floatOffset).dp)
                .size(100.dp)
                .rotate(rotateAngle)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.1f),
                            Secondary.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Bottom left floating circle
        Box(
            modifier = Modifier
                .offset(x = (-30).dp, y = (600 - floatOffset).dp)
                .size(150.dp)
                .rotate(-rotateAngle)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Tertiary.copy(alpha = 0.08f),
                            Primary.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Small accent
        Box(
            modifier = Modifier
                .offset(x = 50.dp, y = (200 + floatOffset * 0.5f).dp)
                .size(40.dp)
                .background(
                    color = PrimaryGradientEnd.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun WelcomeScreenPreview() {
    DashTheme {
        WelcomeScreen(
            onGoogleLoginClicked = {},
            onEmailLoginClicked = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun WelcomeScreenLoadingPreview() {
    DashTheme {
        WelcomeScreen(
            onGoogleLoginClicked = {},
            isLoading = true
        )
    }
}
