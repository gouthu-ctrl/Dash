package com.dash.travel.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.dash.travel.ui.components.GradientButton
import com.dash.travel.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Premium Welcome Screen with animated elements and social sign-in
 * 
 * Design Features:
 * - Animated floating elements in background
 * - Gradient accent text
 * - Social sign-in buttons (Google primary)
 * - Alternative email sign-in
 * - Progress indicators during sign-in
 */
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
            Spacer(modifier = Modifier.height(60.dp))
            
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
                            .size(180.dp)
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
                            .size(120.dp)
                            .clip(RoundedCornerShape(28.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
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
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Subheadline
                Text(
                    text = "The smartest way to travel with\nfriends, family & AI",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnBackgroundSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Feature highlights
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureChip(text = "🤝 Collaborate")
                    FeatureChip(text = "🤖 AI Powered")
                    FeatureChip(text = "📴 Offline")
                }
            }
            
            // Sign in section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
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
                
                // Apple Sign In (if needed in future)
                // Currently hidden but ready for implementation
                
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
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

/**
 * Floating decorative elements for visual interest
 */
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

/**
 * Small feature chip for highlights
 */
@Composable
private fun FeatureChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant
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
