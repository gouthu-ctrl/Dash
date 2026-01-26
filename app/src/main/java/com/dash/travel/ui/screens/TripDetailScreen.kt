package com.dash.travel.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.models.Trip
import com.dash.travel.ui.viewmodel.TripDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.*

// Data model for UI representation
data class TripMember(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String, // owner, editor, viewer
    val status: String // accepted, pending, declined
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    trips: List<Trip>,
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit,
    onAddItem: () -> Unit,
    onRefreshImage: (String) -> Unit,
    onEditTrip: (title: String, description: String, startDate: String, endDate: String, destination: String, origin: String) -> Unit = { _, _, _, _, _, _ -> },
    fetchTripMembers: suspend (String) -> List<TripMember>
) {
    val trip = trips.find { it.id == tripId }
    val items = viewModel.items
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    
    var showImageOptions by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val destinationName = remember(trip) {
        trip?.destinationData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
    }
    val originName = remember(trip) {
        trip?.originData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: ""
    }
    
    var tripMembers by remember { mutableStateOf(emptyList<TripMember>()) }
    
    LaunchedEffect(tripId) {
        tripMembers = fetchTripMembers(tripId)
    }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Innovative Hero Section
                item(key = "hero") {
                    TripHeroSection(
                        trip = trip,
                        originName = originName,
                        destinationName = destinationName,
                        onEditClick = { showEditDialog = true },
                        haptic = haptic,
                        showOptions = { showImageOptions = true }
                    )
                }

                // 2. Participants
                item(key = "details_section") {
                    TripDetailsSection(
                        members = tripMembers
                    )
                }

                // 3. Itinerary Timeline
                if (items.isEmpty()) {
                    item(key = "empty_itinerary") {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            EmptyItinerary()
                        }
                    }
                } else {
                    item(key = "timeline_header") {
                        Text(
                            "Timeline",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 16.dp)
                        )
                    }
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val isThisItemDragging = draggedItemId == item.id
                        
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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

            // Floating Navigation Controls
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            originName = originName,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, desc, start, end, dest, origin ->
                onEditTrip(title, desc, start, end, dest, origin)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripHeroSection(
    trip: Trip?,
    originName: String,
    destinationName: String,
    onEditClick: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    showOptions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        AsyncImage(
            model = trip?.tripImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showOptions()
                    }
                ),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            if (originName.isNotBlank() || destinationName.isNotBlank()) {
                Text(
                    text = "${originName.ifBlank { "START" }} → ${destinationName.ifBlank { "END" }}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = trip?.title ?: "Trip Details",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (trip?.startDate != null) {
                Text(
                    text = "${trip.startDate}${if (trip.endDate != null) " — ${trip.endDate}" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
        }
    }
}

@Composable
fun TripDetailsSection(
    members: List<TripMember>
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        TripParticipantsSection(members = members)
    }
}

@Composable
fun TripParticipantsSection(members: List<TripMember>) {
    if (members.isNotEmpty()) {
        Column {
            Text(
                text = "Trip Members (${members.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                members.take(6).forEach { member ->
                    MemberAvatar(member = member)
                }
                if (members.size > 6) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${members.size - 6}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberAvatar(member: TripMember) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val isOwner = member.role == "owner"
    val isAccepted = member.status == "accepted"
    val isDeclined = member.status == "declined"

    // FIX 2: Define Gold color outside the lambda for performance and clarity
    val Gold = Color(0xFFFFD700) 
    
    val indicatorColor = when {
        isOwner -> Gold 
        isAccepted -> MaterialTheme.colorScheme.primary
        isDeclined -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    
    val indicatorIcon = when {
        isOwner -> Icons.Default.Star
        isAccepted -> Icons.Default.Check
        isDeclined -> Icons.Default.Close
        else -> Icons.Default.AccessTime
    }

    Box(
        modifier = Modifier
            .size(56.dp) // Container size
            .combinedClickable(
                onClick = {
                    val statusText = when {
                        isOwner -> "Owner"
                        isAccepted -> "Accepted"
                        isDeclined -> "Declined"
                        else -> "Pending"
                    }
                    Toast.makeText(context, "${member.name} (${statusText})", Toast.LENGTH_SHORT).show()
                },
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Avatar Circle (48dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Initials (Fallback layer, z-index 0)
            Text(
                text = member.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // FIX 1: Avatar Image (Foreground, z-index 1) - loads on top of initials
            if (!member.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Status Indicator (Aligned BottomEnd, size 20dp, sitting over the edge)
        Surface(
            color = indicatorColor,
            shape = CircleShape,
            modifier = Modifier
                .size(20.dp) 
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                imageVector = indicatorIcon,
                contentDescription = null,
                tint = if (isOwner) Color.Black else MaterialTheme.colorScheme.onPrimary, // Star tint contrast
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripDialog(
    trip: Trip,
    destinationName: String,
    originName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(trip.title) }
    var description by remember { mutableStateOf(trip.description ?: "") }
    var start by remember { mutableStateOf(trip.startDate ?: "") }
    var end by remember { mutableStateOf(trip.endDate ?: "") }
    var dest by remember { mutableStateOf(destinationName) }
    var origin by remember { mutableStateOf(originName) }

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
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Departing From") },
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
                onClick = { onConfirm(title, description, start, end, dest, origin) },
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
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")
    val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateItem()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(elevation, shape = RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
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