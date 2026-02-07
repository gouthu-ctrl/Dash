package com.dash.travel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.ui.components.*
import com.dash.travel.ui.onboarding.LocalTooltipManager
import com.dash.travel.ui.onboarding.SmartTooltip
import com.dash.travel.ui.onboarding.TooltipIds
import com.dash.travel.ui.theme.*
import com.dash.travel.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Premium Home Screen with trip dashboard
 * 
 * Features:
 * - Personalized greeting
 * - Upcoming trip hero card
 * - Quick action widgets
 * - All trips list with stagger animation
 * - First-time user tooltips
 */
import androidx.compose.ui.res.stringResource
import com.dash.travel.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String = "Traveler",
    onNavigateToTrip: (String) -> Unit = {},
    onCreateTrip: () -> Unit = {},
    onNavigateToAIPlanner: () -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null
) {
    val allTrips = viewModel.trips
    val acceptedTrips = allTrips.filter { it.membershipStatus == "accepted" }
    val pendingInvitations = allTrips.filter { it.membershipStatus == "pending" }
    val userProfile by viewModel.userProfile.collectAsState()
    
    val upcomingTrip = acceptedTrips.firstOrNull { trip ->
        trip.startDate?.let { dateStr ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                date?.after(Date()) == true
            } catch (e: Exception) { false }
        } ?: false
    }
    
    var showContent by remember { mutableStateOf(false) }
    
    // Lifecycle-aware refresh
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTrips()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        showContent = true
    }

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTrip,
                shape = RoundedCornerShape(16.dp),
                containerColor = Primary,
                contentColor = OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trip")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (allTrips.isEmpty()) {
                EmptyHomeContent(
                    onCreateTrip = onCreateTrip,
                    onNavigateToAIPlanner = onNavigateToAIPlanner
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    // Header
                    item {
                        HomeHeader(
                            userName = userName,
                            userAvatarUrl = userProfile?.avatarUrl,
                            onSettingsClick = onNavigateToSettings,
                            onProfileClick = onNavigateToProfile
                        )
                    }

                    // Pending Invitations
                    if (pendingInvitations.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.home_new_invitations),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(pendingInvitations) { trip ->
                            InvitationCard(
                                trip = trip,
                                onAccept = { viewModel.acceptTrip(trip.id) },
                                onReject = { viewModel.rejectTrip(trip.id) }
                            )
                        }
                    }

                    
                    // Upcoming trip hero (if exists)
                    if (upcomingTrip != null) {
                        item {
                            AnimatedVisibility(
                                visible = showContent,
                                enter = fadeIn(tween(600)) + slideInVertically(
                                    initialOffsetY = { 50 }
                                )
                            ) {
                                UpcomingTripHero(
                                    trip = upcomingTrip,
                                    onClick = { onNavigateToTrip(upcomingTrip.id) }
                                )
                            }
                        }
                    }
                    
                    // Quick widgets
                    item {
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(600, delayMillis = 100))
                        ) {
                            QuickWidgets(
                                onNavigateToAIPlanner = onNavigateToAIPlanner
                            )
                        }
                    }
                    
                    // All trips header
                    item {
                        SectionHeader(
                            title = stringResource(R.string.home_all_trips),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            action = if (acceptedTrips.size > 3) stringResource(R.string.home_see_all) else null,
                            onActionClick = { /* Navigate to trips list */ }
                        )
                    }
                    
                    // Trip cards with stagger animation
                    itemsIndexed(acceptedTrips, key = { _, trip -> trip.id }) { index, trip ->
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(400, delayMillis = 150 + (index * 50))) +
                                    slideInVertically(
                                        initialOffsetY = { 30 },
                                        animationSpec = tween(400, delayMillis = 150 + (index * 50))
                                    )
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                TripCard(
                                    trip = trip,
                                    onClick = { onNavigateToTrip(trip.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                if (index == 0) {
                                    SmartTooltip(
                                        tooltipId = TooltipIds.HOME_TAP_TRIP,
                                        message = "👆 Tap to view plan",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(y = 20.dp),
                                        delayMs = 1000
                                    )
                                }
                            }
                        }
                    }
                }
            }
            

        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    userAvatarUrl: String?,
    onSettingsClick: (() -> Unit)?,
    onProfileClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = GreetingText(),
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundSecondary
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onProfileClick != null) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                ) {
                    if (userAvatarUrl != null) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = OnSurfaceVariant
                        )
                    }
                }
            }

            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainer)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> stringResource(R.string.home_greeting_morning)
        hour < 17 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
}

/**
 * Hero card for upcoming trip
 */
@Composable
private fun UpcomingTripHero(
    trip: TripEntity,
    onClick: () -> Unit
) {
    val daysLeft = calculateDaysLeft(trip.startDate)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Box {
            // Background image
            if (!trip.tripImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = trip.tripImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(HeroOverlayGradient)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.linearGradient(PrimaryGradient.map { it.copy(alpha = 0.3f) })
                        )
                )
            }
            
            // Content overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Days countdown badge
                if (daysLeft != null && daysLeft >= 0) {
                    StatusChip(
                        text = when {
                            daysLeft == 0L -> stringResource(R.string.time_today)
                            daysLeft == 1L -> stringResource(R.string.time_tomorrow)
                            else -> stringResource(R.string.time_days_format, daysLeft)
                        },
                        icon = Icons.Default.FlightTakeoff,
                        color = if (daysLeft <= 7) Warning else Primary
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Trip title
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Date range
                Text(
                    text = formatDateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // View button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.home_view_trip))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick action widgets row
 */
@Composable
private fun QuickWidgets(
    onNavigateToAIPlanner: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        item {
            QuickWidget(
                icon = Icons.Filled.AutoAwesome,
                label = stringResource(R.string.widget_ai_planner),
                gradient = listOf(Color(0xFF9C27B0), Color(0xFFE040FB)),
                onClick = onNavigateToAIPlanner
            )
        }
        item {
            QuickWidget(
                icon = Icons.Outlined.Checklist,
                label = stringResource(R.string.widget_packing),
                gradient = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
                onClick = { /* TODO */ }
            )
        }
        item {
            QuickWidget(
                icon = Icons.Outlined.WbSunny,
                label = stringResource(R.string.widget_weather),
                gradient = listOf(Color(0xFF2196F3), Color(0xFF03A9F4)),
                onClick = { /* TODO */ }
            )
        }
        item {
            QuickWidget(
                icon = Icons.Outlined.FolderOpen,
                label = stringResource(R.string.widget_documents),
                gradient = listOf(Secondary, Tertiary),
                onClick = { /* TODO */ }
            )
        }
        item {
            QuickWidget(
                icon = Icons.Outlined.TrendingUp,
                label = stringResource(R.string.widget_price_alerts),
                gradient = listOf(Warning, Color(0xFFFF9800)),
                isPro = true,
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun QuickWidget(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    isPro: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)  // Increased for thumb-friendly touch
            .height(100.dp) // Increased for thumb-friendly touch
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)  // Larger icon container
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)  // Larger icon
                    )
                }
                if (isPro) {
                    ProBadge(
                        small = true,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-4).dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Trip card for list view
 */
@Composable
fun TripCard(
    trip: TripEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysLeft = calculateDaysLeft(trip.startDate)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (trip.tripImageUrl.isNullOrEmpty()) {
                            Brush.linearGradient(PrimaryGradient.map { it.copy(alpha = 0.3f) })
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
            ) {
                if (!trip.tripImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = trip.tripImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = Primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Trip info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatDateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            
            // Days badge
            if (daysLeft != null && daysLeft >= 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                daysLeft == 0L -> Success.copy(alpha = 0.15f)
                                daysLeft <= 7 -> Warning.copy(alpha = 0.15f)
                                else -> Primary.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            daysLeft == 0L -> stringResource(R.string.time_today)
                            daysLeft == 1L -> "1 day"
                            else -> stringResource(R.string.time_days_short_format, daysLeft)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            daysLeft == 0L -> Success
                            daysLeft <= 7 -> Warning
                            else -> Primary
                        }
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Empty state when no trips exist
 */
@Composable
fun EmptyHomeContent(
    modifier: Modifier = Modifier,
    onCreateTrip: () -> Unit,
    onNavigateToAIPlanner: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconScale"
        )
        
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.linearGradient(
                        PrimaryGradient.map { it.copy(alpha = 0.2f) }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlightTakeoff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Primary
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = OnBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = OnBackgroundSecondary
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        GradientButton(
            text = stringResource(R.string.home_create_trip),
            onClick = onCreateTrip,
            icon = Icons.Default.Add
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToAIPlanner,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.widget_ai_planner), color = Primary)
        }
    }
}

// Helper functions
private fun calculateDaysLeft(startDate: String?): Long? {
    if (startDate == null) return null
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDate)
        if (date != null) {
            val diff = date.time - Date().time
            if (diff >= 0) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) else null
        } else null
    } catch (e: Exception) { null }
}

private fun formatDateRange(startDate: String?, endDate: String?): String {
    val displaySdf = SimpleDateFormat("MMM dd", Locale.US)
    val parseSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    return try {
        val start = startDate?.let { parseSdf.parse(it) }
        val end = endDate?.let { parseSdf.parse(it) }
        when {
            start != null && end != null -> "${displaySdf.format(start)} - ${displaySdf.format(end)}"
            start != null -> "Starting ${displaySdf.format(start)}"
            else -> "No dates set"
        }
    } catch (e: Exception) {
        "No dates set"
    }
}

@Composable
private fun InvitationCard(
    trip: TripEntity,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHighest)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MailOutline, contentDescription = null, tint = Primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_trip_invite_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary
                    )
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject) {
                    Text(stringResource(R.string.home_trip_decline), color = OnSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.home_trip_join))
                }
            }
        }
    }
}
