package com.dash.travel.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.models.Trip
import com.dash.travel.ui.viewmodel.TripDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    trips: List<Trip>,
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit,
    onAddItem: () -> Unit,
    onRefreshImage: (String) -> Unit,
    onEditTrip: (title: String, description: String, startDate: String, endDate: String, destination: String) -> Unit = { _, _, _, _, _ -> }
) {
    val trip = trips.find { it.id == tripId }
    val items = viewModel.items
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    var showImageOptions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val destinationName = remember(trip) {
        trip?.destinationData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
    }
    
    // Drag state management
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Auto-scroll effect
    LaunchedEffect(draggedItemId, dragOffset) {
        if (draggedItemId != null) {
            val layoutInfo = lazyListState.layoutInfo
            val draggedItem = layoutInfo.visibleItemsInfo.find { it.key == draggedItemId } ?: return@LaunchedEffect
            
            val viewPortTop = layoutInfo.viewportStartOffset
            val viewPortBottom = layoutInfo.viewportEndOffset
            val itemTop = draggedItem.offset + dragOffset
            val itemBottom = itemTop + draggedItem.size

            val scrollThreshold = 100f
            if (itemTop < viewPortTop + scrollThreshold) {
                while (draggedItemId != null) {
                    lazyListState.scrollBy(-15f)
                    delay(10)
                }
            } else if (itemBottom > viewPortBottom - scrollThreshold) {
                while (draggedItemId != null) {
                    lazyListState.scrollBy(15f)
                    delay(10)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            trip?.title ?: "Trip Details",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        trip?.startDate?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Item") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showImageOptions = true
                        }
                    )
            ) {
                key(trip?.tripImageUrl) {
                    AsyncImage(
                        model = trip?.tripImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Hold to change theme", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                // Trip Info Section
                item {
                    TripInfoCard(
                        description = trip?.description,
                        destination = destinationName,
                        startDate = trip?.startDate,
                        endDate = trip?.endDate
                    )
                }

                if (items.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                            EmptyItinerary()
                        }
                    }
                } else {
                    item {
                        Text(
                            "Itinerary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                        )
                    }
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val isThisItemDragging = draggedItemId == item.id
                        
                        DraggableTimelineItem(
                            item = item,
                            isDragging = isThisItemDragging,
                            dragOffset = if (isThisItemDragging) dragOffset else 0f,
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggedItemId = item.id
                            },
                            onDrag = { delta ->
                                dragOffset += delta

                                val currentIndex = items.indexOfFirst { it.id == item.id }
                                if (currentIndex != -1) {
                                    val targetIndex = findTargetIndexInList(lazyListState, item.id, dragOffset, items)

                                    if (targetIndex != null && targetIndex != currentIndex) {
                                        // Subtle haptic \"tick\" on slot swap
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.onMove(currentIndex, targetIndex)
                                        dragOffset = 0f
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedItemId = null
                                dragOffset = 0f
                            }
                        )
                    }
                }
            }
        }
    }

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text("Refresh Trip Theme") },
            text = { Text("Search for a new scenic image for this destination?") },
            confirmButton = {
                Button(onClick = { 
                    onRefreshImage(trip?.title ?: "")
                    showImageOptions = false 
                }) { Text("Refresh") }
            },
            dismissButton = {
                TextButton(onClick = { showImageOptions = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && trip != null) {
        EditTripDialog(
            trip = trip,
            destinationName = destinationName,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, desc, start, end, dest ->
                onEditTrip(title, desc, start, end, dest)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun TripInfoCard(
    description: String?,
    destination: String,
    startDate: String?,
    endDate: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!destination.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destination,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (startDate != null && endDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$startDate - $endDate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (destination.isBlank() && startDate == null) {
                Text(
                    text = "No additional details provided.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripDialog(
    trip: Trip,
    destinationName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(trip.title) }
    var description by remember { mutableStateOf(trip.description ?: "") }
    var start by remember { mutableStateOf(trip.startDate ?: "") }
    var end by remember { mutableStateOf(trip.endDate ?: "") }
    var dest by remember { mutableStateOf(destinationName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Trip Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dest,
                    onValueChange = { dest = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Date") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("YYYY-MM-DD") }
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Date") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("YYYY-MM-DD") }
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, start, end, dest) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun findTargetIndexInList(
    state: androidx.compose.foundation.lazy.LazyListState,
    draggedItemKey: String,
    offset: Float,
    list: List<ItineraryItemEntity>
): Int? {
    val items = state.layoutInfo.visibleItemsInfo
    val draggedItem = items.find { it.key == draggedItemKey } ?: return null
    val center = draggedItem.offset + draggedItem.size / 2 + offset
    
    val targetItem = visibleItemsfirstOrNull(items, center) ?: return null

    return list.indexOfFirst { it.id == targetItem.key }.takeIf { it != -1 }
}

private fun visibleItemsfirstOrNull(items: List<androidx.compose.foundation.lazy.LazyListItemInfo>, center: Float): androidx.compose.foundation.lazy.LazyListItemInfo? {
    return items.firstOrNull { item ->
        item.key is String && center >= item.offset && center <= (item.offset + item.size)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DraggableTimelineItem(
    item: ItineraryItemEntity,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    // Lift effect animations
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")
    val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateItem() // Fluid sliding of other items
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(elevation, shape = RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                // Long press to initiate dragging
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
        TimelineItemContent(item)
    }
}

@Composable
fun TimelineItemContent(item: ItineraryItemEntity) {
    Row(modifier = Modifier.height(IntrinsicSize.Min).background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.width(48.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Card(
            modifier = Modifier
                .padding(bottom = 20.dp, start = 8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.startTime ?: "Time TBD",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!item.bookingRef.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            shape = CircleShape
                        ) {
                            Text(
                                item.bookingRef,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold
                )
                item.locationName?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp), 
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = it, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
