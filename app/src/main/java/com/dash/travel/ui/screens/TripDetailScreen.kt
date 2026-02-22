package com.dash.travel.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.ui.components.*
import com.dash.travel.ui.onboarding.ContextualGuide
import com.dash.travel.ui.onboarding.ContextualGuideStep
import com.dash.travel.ui.onboarding.GuideIds
import com.dash.travel.ui.onboarding.rememberContextualGuideState
import com.dash.travel.ui.viewmodel.TripDetailViewModel
import com.dash.travel.ui.viewmodel.AISuggestionsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.dash.travel.R

// Data model for UI representation
data class TripMember(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String, // owner, editor, viewer
    val status: String, // accepted, pending, declined
    val userId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit,
    onAddItem: () -> Unit,
    onRefreshImage: (String) -> Unit,
    onEditTripClicked: () -> Unit,
    onDeleteTrip: () -> Unit = {},
    onExportToPdf: () -> Unit = {},
    fetchTripMembers: suspend (String) -> List<TripMember>,
    onViewMaps: () -> Unit = {},
    onManageMembers: () -> Unit = {},
    onViewDocs: () -> Unit = {},
    onShare: () -> Unit = {},
    onViewChat: () -> Unit = {},
    onViewReminders: () -> Unit = {},
    onViewVoting: () -> Unit = {},
    onEditItem: (String) -> Unit = {},
    onDownloadAttachment: (String) -> Unit = {},
    aiSuggestionsViewModel: AISuggestionsViewModel? = null
) {
    // AI Suggestions Bottom Sheet state
    val scope = rememberCoroutineScope()
    val aiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAISheet by remember { mutableStateOf(false) }
    val trip by viewModel.trip.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()
    val items = viewModel.items
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    var showImageOptions by remember { mutableStateOf(false) }
    
    val destinationName = remember(trip) { trip?.destinationData?.name ?: "" }
    val originName = remember(trip) { trip?.originData?.name ?: "" }
    
    // Date parsing
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val tripStartDate = remember(trip) {
        trip?.startDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
    }
    val tripEndDate = remember(trip) {
        trip?.endDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
    }
    val totalDays = remember(tripStartDate, tripEndDate) {
        calculateTripDuration(tripStartDate, tripEndDate)
    }
    
    var tripMembers by remember { mutableStateOf(emptyList<TripMember>()) }
    var selectedDayIndex by remember { mutableStateOf(-1) } // -1 = All days
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }
    var showTripDeleteDialog by remember { mutableStateOf(false) }
    
    // Day formatter for timeline headers
    val dayFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    
    LaunchedEffect(tripId) {
        tripMembers = fetchTripMembers(tripId)
    }

    // [Fix] Calculate Edit Permission
    val currentUserId = remember { viewModel.currentUserId }
    val currentUserRole = remember(tripMembers) {
        tripMembers.find { it.userId == currentUserId }?.role
    }
    val canEdit = remember(currentUserRole) {
        currentUserRole == "owner" || currentUserRole == "editor"
    }

    // Refresh on resume (e.g. returning from Add Itinerary)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
                viewModel.refreshItinerary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            if (canEdit) {
                GradientButton(
                    onClick = onAddItem,
                    text = stringResource(R.string.trip_add_activity),
                    icon = Icons.Default.Add,
                    modifier = Modifier.width(160.dp) 
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // 1. Hero Section
                item(key = "hero") {
                        TripHeroSection(
                            trip = trip,
                            originName = originName,
                            destinationName = destinationName,
                            canEdit = canEdit,
                            onEditClick = onEditTripClicked,
                            onDeleteClick = { showTripDeleteDialog = true },
                            onExportPdfClick = onExportToPdf,
                            activeUsers = activeUsers,
                            showOptions = { if (canEdit) showImageOptions = true }
                        )
                }

                // Tools
                item(key = "tools") {
                    QuickActionsRow(
                        onManageMembers = onManageMembers,
                        onViewDocs = onViewDocs,
                        onShare = onShare,
                        onViewVoting = onViewVoting,
                        onAskAI = { 
                            if (aiSuggestionsViewModel != null) {
                                showAISheet = true
                            }
                        }
                    )
                }

                // Contextual guide for first-time users (after tools row)
                item(key = "trip_guide") {
                    val tripGuideState = rememberContextualGuideState(
                        guideId = GuideIds.TRIP_DETAIL_GUIDE,
                        steps = listOf(
                            ContextualGuideStep(
                                icon = Icons.Default.Timeline,
                                message = "This is your trip timeline — scroll to see each day",
                                emoji = "📋"
                            ),
                            ContextualGuideStep(
                                icon = Icons.Default.Add,
                                message = "Tap + to add flights, hotels, activities, or notes",
                                emoji = "✈️"
                            ),
                            ContextualGuideStep(
                                icon = Icons.Default.Groups,
                                message = "Invite friends from the share menu to plan together",
                                emoji = "🤝"
                            )
                        )
                    )
                    ContextualGuide(guideState = tripGuideState)
                }

                // 2. Summary & Stats
                item(key = "summary") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TripSummaryCard(
                                totalDays = totalDays,
                                totalMembers = tripMembers.count { it.status == "accepted" }.coerceAtLeast(1),
                                totalPlaces = items.size,
                                onTravelersClick = { onManageMembers() }
                            )
                    }
                }

                // 3. Day Chips
                if (totalDays > 1) {
                    item(key = "day_chips") {
                        DayChipsRow(
                            startDate = tripStartDate,
                            endDate = tripEndDate,
                            selectedDayIndex = selectedDayIndex,
                            onDaySelected = { index, _ -> selectedDayIndex = index },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // 4. Participants - Clickable to navigate to Members screen
                item(key = "details_section") {
                    // Filter to show only accepted members in the main view
                    val acceptedMembers = remember(tripMembers) { tripMembers.filter { it.status == "accepted" } }
                    TripParticipantsSection(
                        members = acceptedMembers,
                        onNavigateToMembers = onManageMembers
                    )
                }

                // 5. Itinerary Timeline
                val filteredItems = if (selectedDayIndex == -1 || tripStartDate == null) {
                    items
                } else {
                    val cal = Calendar.getInstance()
                    cal.time = tripStartDate
                    cal.add(Calendar.DAY_OF_MONTH, selectedDayIndex)
                    val datePrefix = isoFormatter.format(cal.time)
                    items.filter { it.startTime?.startsWith(datePrefix) == true }
                }

                if (filteredItems.isEmpty()) {
                    item(key = "empty_itinerary") {
                        EmptyItinerary()
                    }
                } else {
                    item(key = "timeline_header") {
                        SectionHeader(title = stringResource(R.string.trip_itinerary_title), modifier = Modifier.padding(horizontal = 24.dp))
                    }
                    
                    // Group items by day for day headers (computed outside remember since filteredItems changes)
                    val groupedItems = filteredItems.groupBy { item ->
                        item.startTime?.take(10) ?: "unscheduled"
                    }.toSortedMap()
                    
                    var overallIndex = 0
                    groupedItems.forEach { (dateKey, dayItems) ->
                        // Day header
                        item(key = "day_header_$dateKey") {
                            val displayDate = if (dateKey == "unscheduled") {
                                "Unscheduled"
                            } else {
                                try {
                                    isoFormatter.parse(dateKey)?.let { dayFormatter.format(it) } ?: dateKey
                                } catch (e: Exception) {
                                    dateKey
                                }
                            }
                            
                            DayHeaderSeparator(
                                date = displayDate,
                                itemCount = dayItems.size
                            )
                        }
                        
                        // Items for this day
                        itemsIndexed(dayItems, key = { _, item -> item.id }) { indexInDay, item ->
                            val globalIndex = overallIndex + indexInDay
                            val isThisItemDragging = draggedItemId == item.id
                            
                            // Wrapper box for padding/margins
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DraggableTimelineItemWrapper(
                                    item = item,
                                    isDragging = isThisItemDragging,
                                    dragOffset = if (isThisItemDragging) dragOffset else 0f,
                                    onDragStart = { 
                                        if (canEdit) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedItemId = item.id
                                        }
                                    },
                                    onDrag = { delta -> 
                                        dragOffset += delta
                                        val currentIndex = filteredItems.indexOfFirst { it.id == item.id }
                                        if (currentIndex != -1) {
                                            val targetIndex = findTargetIndexInList(lazyListState, item.id, dragOffset, filteredItems)
                                            if (targetIndex != null && targetIndex != currentIndex) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                // Find real indices in 'items' for moving
                                                val realFromIndex = items.indexOfFirst { it.id == item.id }
                                                val targetItem = filteredItems[targetIndex]
                                                val realToIndex = items.indexOfFirst { it.id == targetItem.id }
                                                if (realFromIndex != -1 && realToIndex != -1) {
                                                    viewModel.onMove(realFromIndex, realToIndex)
                                                    dragOffset = 0f 
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = { 
                                        draggedItemId = null
                                        dragOffset = 0f 
                                        viewModel.onDragEnd()
                                    }
                                ) {
                                    // ACTUAL CONTENT reusing TripDetailComponents
                                    TimelineItem(
                                        item = item,
                                        isFirst = indexInDay == 0,
                                        isLast = indexInDay == dayItems.lastIndex,
                                        currentUserVote = viewModel.userVotes[item.id],
                                        voteState = viewModel.voteCounts[item.id],
                                        onVote = { voteType -> viewModel.vote(item.id, voteType) },
                                        canEdit = canEdit,
                                        onClick = { if (canEdit) onEditItem(item.id) },
                                        onDelete = {
                                            itemToDelete = item.id
                                            showDeleteDialog = true
                                        },
                                        onDownloadAttachment = onDownloadAttachment
                                    )
                                }
                            }
                        }
                        overallIndex += dayItems.size
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom spacing for FAB
            }

            // Floating Navigation Back Button (Custom)
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.trip_delete_confirmation_title)) },
            text = { Text(stringResource(R.string.trip_delete_confirmation_text)) },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { viewModel.deleteItem(it) }
                    showDeleteDialog = false
                    itemToDelete = null
                }) { 
                    Text(stringResource(R.string.trip_delete), color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text(stringResource(R.string.trip_refresh_theme_title)) },
            text = { Text(stringResource(R.string.trip_refresh_theme_text)) },
            confirmButton = {
                Button(onClick = { 
                    onRefreshImage(trip?.title ?: "")
                    showImageOptions = false 
                }) { Text(stringResource(R.string.trip_refresh)) }
            },
            dismissButton = {
                TextButton(onClick = { showImageOptions = false }) { Text(stringResource(R.string.settings_cancel)) }
            }
        )
    }

    // Trip Delete Confirmation Dialog
    if (showTripDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showTripDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Trip?") },
            text = { 
                Text(
                    "Are you sure you want to delete \"${trip?.title ?: "this trip"}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTripDeleteDialog = false
                        onDeleteTrip()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { 
                    Text("Delete") 
                }
            },
            dismissButton = {
                TextButton(onClick = { showTripDeleteDialog = false }) { 
                    Text("Cancel") 
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // AI Suggestions Bottom Sheet
    if (showAISheet && aiSuggestionsViewModel != null) {
        // Get last itinerary item's location (or trip destination as fallback)
        val lastItineraryLocation = remember(items) {
            items.lastOrNull()?.locationName ?: ""
        }
        
        AISuggestionsBottomSheet(
            sheetState = aiSheetState,
            viewModel = aiSuggestionsViewModel,
            tripDestination = destinationName,
            lastItineraryLocation = lastItineraryLocation,
            onDismiss = { 
                showAISheet = false 
                aiSuggestionsViewModel.reset()
            },
            onItemAdded = {
                // Refresh the itinerary to show the newly added item
                viewModel.refreshItinerary()
                Toast.makeText(context, "Added to your itinerary!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TripParticipantsSection(
    members: List<TripMember>,
    onNavigateToMembers: () -> Unit = {}
) {
    if (members.isNotEmpty()) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable(onClick = onNavigateToMembers)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.trip_members_title)} (${members.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View all members",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                        Text("+${members.size - 6}", style = MaterialTheme.typography.titleSmall)
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
    val Gold = Color(0xFFFFD700) 
    
    val indicatorColor = when (member.role) {
        "owner" -> Gold 
        "editor" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .combinedClickable(
                onClick = {
                    Toast.makeText(context, "${member.name} (${member.role})", Toast.LENGTH_SHORT).show()
                },
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        contentAlignment = Alignment.Center
    ) {
        UserAvatar(
            imageUrl = member.avatarUrl,
            name = member.name,
            size = 48.dp
        )

        // Role Indicator
        if (member.role == "owner") {
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
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
    }
}

// Helper for drag logic
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

    // If target is a header, try to snap to the item immediately following it (start of that day)
    if (targetItem.key.toString().startsWith("day_header_")) {
        val headerIndex = items.indexOf(targetItem)
        if (headerIndex != -1 && headerIndex + 1 < items.size) {
            val nextItem = items[headerIndex + 1]
            val index = list.indexOfFirst { it.id == nextItem.key }
            if (index != -1) return index
        }
    }

    return list.indexOfFirst { it.id == targetItem.key }.takeIf { it != -1 }
}

private fun visibleItemsfirstOrNull(items: List<androidx.compose.foundation.lazy.LazyListItemInfo>, center: Float): androidx.compose.foundation.lazy.LazyListItemInfo? {
    return items.firstOrNull { item ->
        item.key is String && center >= item.offset && center <= (item.offset + item.size)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DraggableTimelineItemWrapper(
    item: ItineraryItemEntity,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
    val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")
    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")

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
        content()
    }
}