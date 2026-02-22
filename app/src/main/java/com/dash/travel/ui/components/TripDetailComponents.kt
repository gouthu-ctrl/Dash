package com.dash.travel.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.model.SupabaseTrip
import com.dash.travel.data.model.ItineraryAttachment
import com.dash.travel.ui.theme.*
import com.dash.travel.data.model.PresenceUser
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.ui.res.stringResource
import com.dash.travel.R

val Gold = Color(0xFFFFD700) 

/**
 * Premium Hero Section for Trip Detail
 */
@Composable
fun TripHeroSection(
    trip: SupabaseTrip?,
    originName: String,
    destinationName: String,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onExportPdfClick: () -> Unit = {},
    activeUsers: List<PresenceUser> = emptyList(), // New param
    showOptions: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(Color(0xFF2D333B)) // Fallback dark background for text visibility
    ) {
        AsyncImage(
            model = trip?.tripImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = showOptions),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 0f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .padding(bottom = 16.dp)
        ) {
            if (originName.isNotBlank() || destinationName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${originName.ifBlank { stringResource(R.string.route_start) }} → ${destinationName.ifBlank { stringResource(R.string.route_end) }}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = trip?.title ?: stringResource(R.string.trip_details_default),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (trip?.startDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${trip.startDate}${if (trip.endDate != null) " — ${trip.endDate}" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            
            // Presence Row (Active Users)
            if (activeUsers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PresenceAvatarRow(users = activeUsers)
            }
        }


        // Trip Actions Menu
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Trip options", tint = Color.White)
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (canEdit) {
                    DropdownMenuItem(
                        text = { Text("Edit Trip") },
                        onClick = { 
                            showMenu = false
                            onEditClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Export to PDF") },
                    onClick = { 
                        showMenu = false
                        onExportPdfClick()
                    },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                )
                if (canEdit) {
                    DropdownMenuItem(
                        text = { Text("Delete Trip", color = MaterialTheme.colorScheme.error) },
                        onClick = { 
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
            }
        }
    }
}

/**
 * Trip summary card showing at-a-glance stats
 */
@Composable
fun TripSummaryCard(
    totalDays: Int,
    totalMembers: Int,
    totalPlaces: Int,
    estimatedBudget: String? = null,
    onTravelersClick: () -> Unit = {}, // New param
    modifier: Modifier = Modifier
) {
    DashCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp), // Reduced vertical padding
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStatItem(
                icon = Icons.Default.CalendarToday,
                value = totalDays.toString(),
                label = stringResource(if (totalDays == 1) R.string.stat_day_singular else R.string.stat_day_plural),
                iconColor = Primary
            )
            Divider(
                modifier = Modifier
                    .height(24.dp) // Reduced divider height
                    .width(1.dp),
                color = SurfaceBorder
            )
            SummaryStatItem(
                icon = Icons.Default.Group,
                value = totalMembers.toString(),
                label = stringResource(if (totalMembers == 1) R.string.stat_traveler_singular else R.string.stat_traveler_plural),
                iconColor = Secondary,
                onClick = onTravelersClick // Pass click handler
            )
            Divider(
                modifier = Modifier
                    .height(24.dp) // Reduced divider height
                    .width(1.dp),
                color = SurfaceBorder
            )
            SummaryStatItem(
                icon = Icons.Default.Place,
                value = totalPlaces.toString(),
                label = stringResource(if (totalPlaces == 1) R.string.stat_place_singular else R.string.stat_place_plural),
                iconColor = Tertiary
            )
        }
    }
}

@Composable
private fun SummaryStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    onClick: (() -> Unit)? = null // Optional click handler
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // Add padding inside clickable area if needed, or rely on parent padding
            .padding(8.dp) // Add touch target padding
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp) // Reduced icon size
        )
        Spacer(modifier = Modifier.height(2.dp)) // Reduced spacer
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
    }
}

/**
 * Horizontal scrollable day chips for quick navigation
 */
@Composable
fun DayChipsRow(
    startDate: Date?,
    endDate: Date?,
    selectedDayIndex: Int,
    onDaySelected: (Int, Date) -> Unit,
    modifier: Modifier = Modifier
) {
    if (startDate == null || endDate == null) return
    
    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    
    val days = remember(startDate, endDate) {
        val daysList = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        while (!calendar.time.after(endDate)) {
            daysList.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        daysList
    }
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedDayIndex == -1,
                onClick = { onDaySelected(-1, startDate) },
                label = { Text(stringResource(R.string.chip_all_trip)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = OnPrimary,
                    containerColor = SurfaceContainerHigh,
                    labelColor = OnSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedDayIndex == -1
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(60.dp)
            )
        }
        
        // Day chips
        itemsIndexed(days) { index: Int, date: Date ->
            val isSelected = selectedDayIndex == index
            Surface(
                onClick = { onDaySelected(index, date) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Primary else SurfaceContainerHigh,
                modifier = Modifier.size(width = 56.dp, height = 60.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = dayFormatter.format(date).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) OnPrimary.copy(alpha = 0.8f) else OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) OnPrimary else OnSurface
                    )
                }
            }
        }
    }
}

/**
 * Timeline Item Layout
 */
/**
 * Timeline Item Layout - Compact Version
 */
@Composable
fun TimelineItem(
    item: ItineraryItemEntity,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    currentUserVote: String? = null,
    voteState: com.dash.travel.ui.screens.collaborative.VoteState? = null,
    onVote: (String) -> Unit = {},
    canEdit: Boolean = false,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDownloadAttachment: (String) -> Unit = {}
) {
    val tbdString = stringResource(R.string.date_tbd)
    fun formatDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return tbdString
        return try {
            val validFormats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm"
            )
            val outputFormat = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            
            var parsedDate: java.util.Date? = null
            for (format in validFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                    parsedDate = sdf.parse(isoString)
                    if (parsedDate != null) break
                } catch (e: Exception) {
                    continue
                }
            }
            if (parsedDate != null) outputFormat.format(parsedDate) else tbdString
        } catch (e: Exception) {
            tbdString
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline Line Column (Reduced width)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Top Line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Primary.copy(alpha = 0.3f))
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dot (Smaller)
            TimelineDot(icon = getIconForType(item.type), isCompact = true)

            // Bottom Line
            if (!isLast) {
                 Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Primary.copy(alpha = 0.3f))
                )
            } else {
                 Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Content Card
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp, top = 4.dp, end = 16.dp)
        ) {
            DashCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dateTimeStr = formatDateTime(item.startTime)
                                Text(
                                    text = dateTimeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.attachmentCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Parse attachments
                                    val attachments: List<ItineraryAttachment> = remember(item.attachmentsJson) {
                                        if (!item.attachmentsJson.isNullOrBlank()) {
                                            try {
                                                Json { ignoreUnknownKeys = true }.decodeFromString(
                                                    ListSerializer(ItineraryAttachment.serializer()),
                                                    item.attachmentsJson
                                                )
                                            } catch (e: Exception) { emptyList() }
                                        } else emptyList()
                                    }
                                    
                                    var showAttachmentsMenu by remember { mutableStateOf(false) }
                                    
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { 
                                                    if (attachments.size == 1) {
                                                        onDownloadAttachment(attachments.first().storagePath)
                                                    } else if (attachments.isNotEmpty()) {
                                                        showAttachmentsMenu = true
                                                    }
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = item.attachmentCount.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showAttachmentsMenu,
                                            onDismissRequest = { showAttachmentsMenu = false }
                                        ) {
                                            attachments.forEach { file ->
                                                DropdownMenuItem(
                                                    text = { Text(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                    onClick = {
                                                        onDownloadAttachment(file.storagePath)
                                                        showAttachmentsMenu = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Delete Action
                        if (canEdit) {
                            IconButton(
                                onClick = onDelete, 
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.trip_delete), 
                                    tint = OnSurfaceVariant.copy(alpha=0.6f), 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    // --- Extended Details Section ---
                    if (!item.providerDetailsJson.isNullOrBlank()) {
                        val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }
                        val details = remember(item.providerDetailsJson) {
                            try {
                                json.parseToJsonElement(item.providerDetailsJson).let { 
                                    if (it is kotlinx.serialization.json.JsonObject) it else null 
                                }
                            } catch (e: Exception) { null }
                        }
                        
                        if (details != null && details.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simple horizontal list of key details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Extract and show important bits based on type
                                val importantKeys = when(item.type) {
                                    "flight" -> listOf("airline", "flight_number", "gate", "terminal")
                                    "train" -> listOf("train_number", "platform", "seat")
                                    "hotel", "airbnb" -> listOf("access_code", "room")
                                    else -> details.keys.take(3)
                                }
                                
                                importantKeys.forEach { key ->
                                    val value = details[key]?.let { 
                                        if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString() 
                                    }
                                    if (!value.isNullOrBlank()) {
                                        DetailBadge(
                                            icon = getIconForKey(key),
                                            text = value
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!item.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!item.locationName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.locationName,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // --- Voting Controls for Proposed Items ---
                    if (item.status == "proposed") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoteButton(
                                icon = Icons.Default.ArrowUpward,
                                count = voteState?.upvotes ?: 0,
                                isSelected = currentUserVote == "up",
                                onClick = { onVote("up") },
                                color = Primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            VoteButton(
                                icon = Icons.Default.ArrowDownward,
                                count = voteState?.downvotes ?: 0,
                                isSelected = currentUserVote == "down",
                                onClick = { onVote("down") },
                                color = MaterialTheme.colorScheme.error
                            )

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoteButton(
    icon: ImageVector,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) color else OnSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) color else OnSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmptyItinerary() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SurfaceContainerHigh,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.trip_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.trip_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

@Composable
fun TimelineDot(icon: ImageVector, isCompact: Boolean = false) {
    val size = if (isCompact) 24.dp else 32.dp
    val iconSize = if (isCompact) 12.dp else 16.dp
    
    Surface(
        shape = CircleShape,
        color = SurfaceContainer, // Background behind dot
        border = BorderStroke(2.dp, Primary),
        modifier = Modifier.size(size),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private fun getIconForType(type: String): ImageVector {
    return when (type.lowercase()) {
        "flight" -> Icons.Default.Flight
        "hotel" -> Icons.Default.Hotel
        "food" -> Icons.Default.Restaurant
        "activity" -> Icons.Default.LocalActivity
        else -> Icons.Default.Place
    }
}

// Renamed from calculateTripDays to avoid conflict/provide implementation
fun calculateTripDuration(startDate: Date?, endDate: Date?): Int {
    if (startDate == null || endDate == null) return 0
    val diffInMillis = endDate.time - startDate.time
    return (TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1).toInt()
}

@Composable
private fun DetailBadge(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Primary.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Row of overlapping avatars for active users
 */
@Composable
fun PresenceAvatarRow(users: List<PresenceUser>) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Viewing now:", 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Box {
            users.take(5).forEachIndexed { index, user ->
                PresenceAvatar(
                    user = user,
                    modifier = Modifier
                        .padding(start = (index * 24).dp) // Overlap
                        .zIndex(5f - index) // Stack order
                )
            }
            if (users.size > 5) {
                Box(
                    modifier = Modifier
                        .padding(start = (5 * 24).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+${users.size - 5}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun PresenceAvatar(user: PresenceUser, modifier: Modifier = Modifier) {
    val color = try { Color(android.graphics.Color.parseColor(user.color)) } catch(e: Exception) { Gold }
    
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (user.avatarUrl != null) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = user.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


private fun getIconForKey(key: String): ImageVector {
    return when (key.lowercase()) {
        "flight_number", "train_number", "seat" -> Icons.Default.Numbers
        "airline" -> Icons.Default.AirplanemodeActive
        "gate", "terminal", "platform" -> Icons.Default.MeetingRoom
        "access_code" -> Icons.Default.VpnKey
        "room" -> Icons.Default.DoorSliding
        else -> Icons.Default.Info
    }
}

/**
 * Day header separator for the timeline - visually separates items by day
 */
@Composable
fun DayHeaderSeparator(
    date: String,
    itemCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OnSurfaceVariant.copy(alpha = 0.3f))
        )
        
        // Date badge
        Surface(
            color = Primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Primary
                )
                Text(
                    text = date,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Text(
                    text = "($itemCount)",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
        
        // Right line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(OnSurfaceVariant.copy(alpha = 0.3f))
        )
    }
}
