package com.dash.travel.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.models.Trip
import com.dash.travel.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class TripCardSize {
    COMPACT, STANDARD, LARGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String = "Traveler",
    onNavigateToTrip: (String) -> Unit = {},
    onCreateTrip: () -> Unit = {}
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var globalCardSize by rememberSaveable { mutableStateOf(TripCardSize.STANDARD) }
    
    val items = listOf("Trips", "Discover", "Vault", "Profile")
    val icons = listOf(
        Icons.Filled.CardTravel to Icons.Outlined.CardTravel,
        Icons.Filled.Explore to Icons.Outlined.Explore,
        Icons.Filled.Lock to Icons.Outlined.Lock,
        Icons.Filled.Person to Icons.Outlined.Person
    )

    Scaffold(
        topBar = {
            if (viewModel.trips.isNotEmpty()) {
                TopAppBar(
                    title = { Text("My Journey", fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onCreateTrip,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Trip", modifier = Modifier.size(36.dp))
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) icons[index].first else icons[index].second,
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (viewModel.trips.isEmpty()) {
            EmptyHomeContent(
                modifier = Modifier.padding(innerPadding),
                onCreateTrip = onCreateTrip
            )
        } else {
            HomeContent(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                cardSize = globalCardSize,
                onTripClick = onNavigateToTrip,
                onCardSizeChange = { globalCardSize = it }
            )
        }
    }
}

@Composable
fun EmptyHomeContent(
    modifier: Modifier = Modifier,
    onCreateTrip: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Ready for Adventure?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Plan your first trip with Dash and let us handle the details.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onCreateTrip,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Start Planning", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    cardSize: TripCardSize,
    onTripClick: (String) -> Unit,
    onCardSizeChange: (TripCardSize) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val trips = viewModel.trips

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Auto-scroll logic for a premium reordering experience
    LaunchedEffect(draggedItemId, dragOffset) {
        if (draggedItemId != null) {
            val layoutInfo = lazyListState.layoutInfo
            val draggedItem = layoutInfo.visibleItemsInfo.find { it.key == draggedItemId } ?: return@LaunchedEffect
            
            val viewPortTop = layoutInfo.viewportStartOffset
            val viewPortBottom = layoutInfo.viewportEndOffset
            val itemTop = draggedItem.offset + dragOffset
            val itemBottom = itemTop + draggedItem.size

            val scrollThreshold = 120f
            if (itemTop < viewPortTop + scrollThreshold) {
                while (draggedItemId != null) {
                    lazyListState.scrollBy(-20f)
                    delay(10)
                }
            } else if (itemBottom > viewPortBottom - scrollThreshold) {
                while (draggedItemId != null) {
                    lazyListState.scrollBy(20f)
                    delay(10)
                }
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        itemsIndexed(trips, key = { _, trip -> trip.id }) { index, trip ->
            val isThisItemDragging = draggedItemId == trip.id
            
            TripCardDraggable(
                trip = trip,
                cardSize = cardSize,
                isDragging = isThisItemDragging,
                dragOffset = if (isThisItemDragging) dragOffset else 0f,
                onDragStart = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    draggedItemId = trip.id
                },
                onDrag = { delta -> 
                    dragOffset += delta
                    val currentIndex = trips.indexOfFirst { it.id == trip.id }
                    if (currentIndex != -1) {
                        val targetIndex = findTargetIndexInList(lazyListState, trip.id, dragOffset, trips)
                        if (targetIndex != null && targetIndex != currentIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onMove(currentIndex, targetIndex)
                            dragOffset = 0f 
                        }
                    }
                },
                onDragEnd = { 
                    viewModel.onDrop()
                    draggedItemId = null
                    dragOffset = 0f 
                },
                onClick = { onTripClick(trip.id) },
                onSizeChangeRequested = { onCardSizeChange(it) }
            )
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

private fun findTargetIndexInList(
    state: androidx.compose.foundation.lazy.LazyListState,
    draggedItemKey: String,
    offset: Float,
    list: List<TripEntity>
): Int? {
    val items = state.layoutInfo.visibleItemsInfo
    val draggedItem = items.find { it.key == draggedItemKey } ?: return null
    val center = draggedItem.offset + draggedItem.size / 2 + offset
    
    val targetItem = items.firstOrNull { item ->
        item.key is String && center >= item.offset && center <= (item.offset + item.size)
    } ?: return null

    return list.indexOfFirst { it.id == targetItem.key }.takeIf { it != -1 }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.TripCardDraggable(
    trip: TripEntity,
    cardSize: TripCardSize,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit,
    onSizeChangeRequested: (TripCardSize) -> Unit
) {
    // Lift effect with Google Weather aesthetics
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")
    val elevation by animateDpAsState(if (isDragging) 12.dp else 2.dp, label = "elevation")
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateItem() // Fluid slide effect for non-dragged items
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(elevation, shape = RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                // Long press to initiate reordering
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        UpcomingTripCard(
            trip = trip,
            onClick = onClick,
            cardSize = cardSize,
            onSizeChangeRequested = onSizeChangeRequested
        )
    }
}

@Composable
fun UpcomingTripCard(
    trip: TripEntity,
    onClick: () -> Unit, 
    cardSize: TripCardSize,
    onSizeChangeRequested: (TripCardSize) -> Unit
) {
    val displaySdf = SimpleDateFormat("MMM dd", Locale.US)
    
    val daysLeft = trip.startDate?.let {
        val start = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it) } catch (e: Exception) { null }
        val diff = if (start != null) start.time - Date().time else -1L
        if (diff > 0) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) else 0
    }

    val height by animateDpAsState(
        targetValue = when (cardSize) {
            TripCardSize.COMPACT -> 90.dp
            TripCardSize.STANDARD -> 170.dp
            TripCardSize.LARGE -> 250.dp
        }, label = "height"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(24.dp),
        // Elevation handled by Draggable container
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = trip.tripImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = if (cardSize == TripCardSize.COMPACT) 0f else 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (cardSize == TripCardSize.COMPACT) 12.dp else 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cardSize != TripCardSize.COMPACT && daysLeft != null && daysLeft >= 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = if (daysLeft == 0L) "Today" else "In $daysLeft days",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Resize handle (secondary interaction)
                    var dragY by remember { mutableStateOf(0f) }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Resize",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragY += dragAmount.y
                                        if (dragY > 50) {
                                            if (cardSize == TripCardSize.COMPACT) onSizeChangeRequested(TripCardSize.STANDARD)
                                            else if (cardSize == TripCardSize.STANDARD) onSizeChangeRequested(TripCardSize.LARGE)
                                            dragY = 0f
                                        } else if (dragY < -50) {
                                            if (cardSize == TripCardSize.LARGE) onSizeChangeRequested(TripCardSize.STANDARD)
                                            else if (cardSize == TripCardSize.STANDARD) onSizeChangeRequested(TripCardSize.COMPACT)
                                            dragY = 0f
                                        }
                                    },
                                    onDragEnd = { dragY = 0f }
                                )
                            }
                    )
                }
                
                Column {
                    Text(
                        text = trip.title,
                        style = when (cardSize) {
                            TripCardSize.COMPACT -> MaterialTheme.typography.titleMedium
                            TripCardSize.STANDARD -> MaterialTheme.typography.titleLarge
                            TripCardSize.LARGE -> MaterialTheme.typography.headlineSmall
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    val dateRange = if (trip.startDate != null && trip.endDate != null) {
                        val start = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trip.startDate) } catch (e: Exception) { null }
                        val end = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(trip.endDate) } catch (e: Exception) { null }
                        if (start != null && end != null) {
                            "${displaySdf.format(start)} - ${displaySdf.format(end)}"
                        } else {
                            trip.description ?: ""
                        }
                    } else {
                        trip.description ?: ""
                    }
                    
                    if (dateRange.isNotBlank()) {
                        Text(
                            text = dateRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
