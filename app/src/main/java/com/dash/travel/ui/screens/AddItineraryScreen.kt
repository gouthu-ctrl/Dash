package com.dash.travel.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import android.provider.OpenableColumns
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dash.travel.data.model.ItineraryItem
import com.dash.travel.data.models.PhotonResponse
import com.dash.travel.ui.components.DashCard
import com.dash.travel.ui.components.GradientButton
import com.dash.travel.ui.components.SuggestionTextField
import com.dash.travel.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.dash.travel.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItineraryScreen(
    tripId: String,
    itemToEdit: ItineraryItem? = null,
    onNavigateBack: () -> Unit,
    onDownloadAttachment: (String) -> Unit = {},
    onSaveItem: (item: ItineraryItem, attachmentUri: Uri?, isShared: Boolean) -> Unit
) {
    // Start with null type and update when itemToEdit becomes available
    var selectedType by remember { mutableStateOf<ItineraryType?>(null) }
    
    // Update selectedType when itemToEdit loads asynchronously
    LaunchedEffect(itemToEdit) {
        if (itemToEdit != null) {
            selectedType = itineraryTypes.find { it.id == itemToEdit.type }
        }
    }
    
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { 
                    AnimatedContent(targetState = selectedType, label = "title") { type ->
                        if (type != null) {
                            Text(stringResource(R.string.new_item_title, stringResource(type.labelRes)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        } else {
                            Text(stringResource(R.string.add_to_journey), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedType != null) selectedType = null else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground,
                    navigationIconContentColor = OnBackground
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(Background)
        ) {
            AnimatedContent(
                targetState = selectedType,
                transitionSpec = {
                    if (targetState != null) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "content"
            ) { type ->
                if (type == null) {
                    TypeSelectionGrid(onTypeSelected = { selectedType = it })
                } else {
                    val defaultTitle = stringResource(type.labelRes)
                    DynamicItineraryForm(
                        type = type,
                        tripId = tripId,
                        initialFields = remember(itemToEdit) {
                            if (itemToEdit != null) {
                                val map = mutableMapOf<String, String>()
                                map["title"] = itemToEdit.title
                                map["location"] = itemToEdit.locationName ?: ""
                                map["startTime"] = itemToEdit.startTime ?: ""
                                map["bookingRef"] = itemToEdit.bookingRef ?: ""
                                itemToEdit.providerDetails?.forEach { k, v ->
                                     map[k] = v.jsonPrimitive.contentOrNull ?: v.toString()
                                }
                                map
                            } else emptyMap()
                        },
                        itemToEdit = itemToEdit,
                        onSave = { fields, uri, isShared, isProposal ->
                            val item = ItineraryItem(
                                tripId = tripId,
                                type = type.id, // e.g. "flight", "hotel"
                                title = fields["title"] ?: defaultTitle,
                                locationName = fields["location"],
                                status = if (isProposal) com.dash.travel.data.model.ItineraryStatus.PROPOSED else com.dash.travel.data.model.ItineraryStatus.CONFIRMED,
                                startTime = fields["startTime"] ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                bookingRef = fields["bookingRef"],
                                providerDetails = buildJsonObject {
                                    fields.filter { it.key !in listOf("title", "location", "startTime", "bookingRef") }
                                          .forEach { put(it.key, it.value) }
                                }
                            )
                            onSaveItem(item, uri, isShared)
                        },
                        onDownloadAttachment = onDownloadAttachment
                    )
                }
            }
        }
    }
}

@Composable
fun TypeSelectionGrid(onTypeSelected: (ItineraryType) -> Unit) {
    // Categories from ItineraryUtils.kt logic (Transportation, Accommodation, Activities, Planning)
    // We hardcode the order we want
    val categories = listOf("Transportation", "Accommodation", "Activities", "Planning")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            divider = {},
            containerColor = Background,
            contentColor = Primary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = Primary
                )
            }
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { 
                        Text(
                            stringResource(getCategoryLabelRes(category)), 
                            style = if (pagerState.currentPage == index) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = OnSurfaceVariant
                )
            }
        }
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val currentCategory = categories[pageIndex]
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Filter main list
                val types = itineraryTypes.filter { it.category == currentCategory }
                items(types) { type ->
                    TypeCard(type, onClick = { onTypeSelected(type) })
                }
            }
        }
    }
}

@Composable
fun TypeCard(type: ItineraryType, onClick: () -> Unit) {
    DashCard(
        onClick = onClick,
        modifier = Modifier.height(130.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        type.icon, 
                        contentDescription = null, 
                        modifier = Modifier.size(28.dp), 
                        tint = Primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(type.labelRes), 
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        }
    }
}

@Composable
fun DynamicItineraryForm(
    type: ItineraryType,
    tripId: String,
    onSave: (Map<String, String>, Uri?, Boolean, Boolean) -> Unit,
    onDownloadAttachment: (String) -> Unit,
    initialFields: Map<String, String> = emptyMap(),
    itemToEdit: ItineraryItem? = null
) {
    var fields by remember(initialFields) { mutableStateOf(initialFields) }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var isShared by remember { mutableStateOf(true) }
    var isProposal by remember(itemToEdit) { 
        mutableStateOf(itemToEdit?.status == com.dash.travel.data.model.ItineraryStatus.PROPOSED) 
    }
    
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Check file size < 10MB
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                if (it.length > 10 * 1024 * 1024) {
                    // Too large logic
                } else {
                    attachmentUri = uri
                }
            }
        }
    }

    // Auto-population logic for location/airline
    val client = remember {
        HttpClient(Android) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
    var locationSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(fields["location"]) {
        val q = fields["location"] ?: ""
        if (q.length > 2) {
            delay(300)
            try {
                val response: PhotonResponse = client.get("https://photon.komoot.io/api/?q=$q&limit=5").body()
                locationSuggestions = response.features.mapNotNull { feat ->
                    val p = feat.properties
                    // Construct detailed address: "123 Main St, City, State, Country"
                    val streetPart = listOfNotNull(p.housenumber, p.street).joinToString(" ")
                    listOfNotNull(
                        if (streetPart.isNotBlank()) streetPart else p.name, 
                        p.city, 
                        p.state, 
                        p.country
                    ).distinct().joinToString(", ")
                }
            } catch (e: Exception) { locationSuggestions = emptyList() }
        } else { locationSuggestions = emptyList() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(type.icon, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.details_section), 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
        }

        item {
            StyledTextField(
                value = fields["title"] ?: "",
                onValueChange = { fields = fields.toMutableMap().apply { put("title", it) } },
                label = stringResource(R.string.title_label)
            )
        }
        
        // Contextual Fields mapped to ItineraryUtils IDs
        when (type.id) {
            "flight" -> {
                item { StyledTextField(fields["airline"] ?: "", { fields = fields.toMutableMap().apply { put("airline", it) } }, stringResource(R.string.airline_label), Icons.Default.AirplanemodeActive) }
                item { StyledTextField(fields["flight_number"] ?: "", { fields = fields.toMutableMap().apply { put("flight_number", it) } }, stringResource(R.string.flight_number_label), Icons.Default.Numbers) }
                item { 
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { StyledTextField(fields["terminal"] ?: "", { fields = fields.toMutableMap().apply { put("terminal", it) } }, stringResource(R.string.terminal_label)) }
                        Box(Modifier.weight(1f)) { StyledTextField(fields["gate"] ?: "", { fields = fields.toMutableMap().apply { put("gate", it) } }, stringResource(R.string.gate_label)) }
                    }
                }
            }
            "train" -> {
                item { StyledTextField(fields["train_number"] ?: "", { fields = fields.toMutableMap().apply { put("train_number", it) } }, stringResource(R.string.train_number_label), Icons.Default.Numbers) }
                item { StyledTextField(fields["platform"] ?: "", { fields = fields.toMutableMap().apply { put("platform", it) } }, stringResource(R.string.platform_label), Icons.Default.MeetingRoom) }
                item { StyledTextField(fields["seat"] ?: "", { fields = fields.toMutableMap().apply { put("seat", it) } }, stringResource(R.string.seat_label), Icons.Default.EventSeat) }
            }
            "hotel", "airbnb", "hostel", "camping" -> {
                item {
                    SuggestionTextField(
                        value = fields["location"] ?: "",
                        onValueChange = { fields = fields.toMutableMap().apply { put("location", it) } },
                        label = stringResource(R.string.address_label),
                        suggestions = locationSuggestions,
                        onSuggestionClick = { 
                            fields = fields.toMutableMap().apply { put("location", it) }
                            locationSuggestions = emptyList()
                        },
                        icon = Icons.Default.Place
                    )
                }
                if (type.id != "camping") {
                    item { StyledTextField(fields["access_code"] ?: "", { fields = fields.toMutableMap().apply { put("access_code", it) } }, stringResource(R.string.access_code_label), Icons.Default.Key) }
                } else {
                    item { StyledTextField(fields["campsite"] ?: "", { fields = fields.toMutableMap().apply { put("campsite", it) } }, stringResource(R.string.campsite_label), Icons.Default.Numbers) }
                }
            }
            "restaurant", "sightseeing", "tour", "hike" -> {
                 item {
                    SuggestionTextField(
                        value = fields["location"] ?: "",
                        onValueChange = { fields = fields.toMutableMap().apply { put("location", it) } },
                        label = stringResource(R.string.location_label),
                        suggestions = locationSuggestions,
                        onSuggestionClick = { 
                            fields = fields.toMutableMap().apply { put("location", it) }
                            locationSuggestions = emptyList()
                        },
                        icon = Icons.Default.Place
                    )
                }
            }
        }

        // DateTime Selection
        item {
            DateTimeSelector(
                value = fields["startTime"] ?: "",
                onValueChange = { fields = fields.toMutableMap().apply { put("startTime", it) } }
            )
        }
        
        // Hide BookingRef for Notes
        if (type.id != "note") {
            item { StyledTextField(fields["bookingRef"] ?: "", { fields = fields.toMutableMap().apply { put("bookingRef", it) } }, stringResource(R.string.booking_ref_label), Icons.Default.ConfirmationNumber) }
        } else {
            item {
                StyledTextField(
                    value = fields["description"] ?: "",
                    onValueChange = { fields = fields.toMutableMap().apply { put("description", it) } },
                    label = stringResource(R.string.note_details_label),
                    singleLine = false,
                    minLines = 3
                )
            }
        }

        // File Upload Section
        item {
            AttachmentSection(
                uri = attachmentUri,
                existingAttachments = remember(tripId) { itemToEdit?.attachments ?: emptyList() },
                isShared = isShared,
                onPickFile = { launcher.launch("*/*") },
                onClearFile = { attachmentUri = null },
                onShareToggle = { isShared = it },
                onDownloadAttachment = onDownloadAttachment
            )
        }

        // Voting Proposal Toggle
        item {
            DashCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { isProposal = !isProposal },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = if (isProposal) Primary else OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.propose_voting_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            stringResource(R.string.propose_voting_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isProposal,
                        onCheckedChange = { isProposal = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimary,
                            checkedTrackColor = Primary
                        )
                    )
                }
            }
        }

        item {
            GradientButton(
                text = if (isProposal) stringResource(R.string.propose_activity_button) else stringResource(R.string.add_to_itinerary_button),
                onClick = { onSave(fields, attachmentUri, isShared, isProposal) },
                enabled = (fields["title"]?.isNotBlank() ?: false) || type.id == "note",
                icon = Icons.Default.Check
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = Primary) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = SurfaceBorder,
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer
        ),
        singleLine = singleLine,
        minLines = minLines
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelector(value: String, onValueChange: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState()
    
    val storageFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    val displayValue = remember(value) {
        if (value.isBlank()) ""
        else {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                // Try parsing current value (Order: ISO, Storage, Display)
                val date = try { 
                    isoFormat.parse(value) 
                } catch (e: Exception) {
                    try { storageFormat.parse(value) } catch (e2: Exception) { displayFormat.parse(value) }
                }
                
                if (date != null) displayFormat.format(date) else value
            } catch (e: Exception) { value }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.start_time_label)) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Primary) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = false, 
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = OnSurface,
                disabledBorderColor = SurfaceBorder,
                disabledLeadingIconColor = Primary,
                disabledLabelColor = OnSurfaceVariant,
                disabledContainerColor = SurfaceContainer
            )
        )
        
        // Overlay for click
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    showTimePicker = true 
                }) { Text(stringResource(R.string.next_button)) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceContainerHigh
            )
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    
                    // selectedDateMillis is UTC. We need Y/M/D from it.
                    dateState.selectedDateMillis?.let { millis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.timeInMillis = millis
                        cal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH))
                    }
                    
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    cal.set(Calendar.SECOND, 0)
                    
                    onValueChange(storageFormat.format(cal.time))
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok_button)) }
            },
            text = { TimePicker(state = timeState) },
            containerColor = SurfaceContainerHigh
        )
    }
}

@Composable
fun AttachmentSection(
    uri: Uri?, 
    existingAttachments: List<com.dash.travel.data.model.ItineraryAttachment> = emptyList(),
    isShared: Boolean, 
    onPickFile: () -> Unit, 
    onClearFile: () -> Unit, 
    onShareToggle: (Boolean) -> Unit,
    onDownloadAttachment: (String) -> Unit
) {
    val context = LocalContext.current
    var fileName by remember(uri) { mutableStateOf<String?>(null) }
    
    // Resolve filename from URI
    LaunchedEffect(uri) {
        if (uri != null) {
            if (uri.scheme == "content") {
               try {
                   context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                       if (cursor.moveToFirst()) {
                           val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                           if (index != -1) fileName = cursor.getString(index)
                       }
                   }
               } catch (e: Exception) { fileName = uri.lastPathSegment }
            } else {
                fileName = uri.lastPathSegment
            }
        } else {
            fileName = null
        }
    }

    DashCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.attachments_title), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onPickFile) {
                    Text(if (uri == null) stringResource(R.string.add_new_file) else stringResource(R.string.change_file))
                }
            }
            
            // Existing Attachments
            if (existingAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.saved_files_label), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                existingAttachments.forEach { attachment ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onDownloadAttachment(attachment.storagePath) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = OnSurface)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                attachment.fileName, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // New Upload
            if (uri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.new_upload_label), style = MaterialTheme.typography.labelSmall, color = Primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            fileName ?: stringResource(R.string.file_default_name), 
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearFile) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Primary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onShareToggle(!isShared) }
                ) {
                    Checkbox(
                        checked = isShared, 
                        onCheckedChange = onShareToggle,
                        colors = CheckboxDefaults.colors(checkedColor = Primary)
                    )
                    Column {
                        Text("Share with Trip members in this Trip", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                        Text("Attachments will not be shared outside of this trip.", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }
                }
            }
        }
    }
}
