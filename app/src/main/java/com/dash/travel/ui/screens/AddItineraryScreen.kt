package com.dash.travel.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.data.models.ItineraryItem
import com.dash.travel.data.models.PhotonResponse
import com.dash.travel.ui.components.SuggestionTextField
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItineraryScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    onSaveItem: (item: ItineraryItem, attachmentUri: Uri?, isShared: Boolean) -> Unit
) {
    var selectedType by remember { mutableStateOf<ItineraryType?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    selectedType?.let { Text("New ${it.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } 
                        ?: Text("Add to Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedType != null) selectedType = null else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (selectedType == null) {
                TypeSelectionGrid(onTypeSelected = { selectedType = it })
            } else {
                DynamicItineraryForm(
                    type = selectedType!!,
                    onSave = { fields, uri, isShared ->
                        val item = ItineraryItem(
                            tripId = tripId,
                            type = selectedType!!.id,
                            title = fields["title"] ?: selectedType!!.label,
                            locationName = fields["location"],
                            startTime = fields["startTime"],
                            bookingRef = fields["bookingRef"],
                            providerDetails = buildJsonObject {
                                fields.filter { it.key !in listOf("title", "location", "startTime", "bookingRef") }
                                      .forEach { put(it.key, it.value) }
                            }
                        )
                        onSaveItem(item, uri, isShared)
                    }
                )
            }
        }
    }
}

@Composable
fun TypeSelectionGrid(onTypeSelected: (ItineraryType) -> Unit) {
    val categories = listOf("Transportation", "Accommodation", "Activities", "Planning")
    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()
    
    Column {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
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
                            category, 
                            style = if (pagerState.currentPage == index) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
                        ) 
                    }
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
                items(itineraryTypes.filter { it.category == currentCategory }) { type ->
                    TypeCard(type, onClick = { onTypeSelected(type) })
                }
            }
        }
    }
}

@Composable
fun TypeCard(type: ItineraryType, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(type.icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(type.label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun DynamicItineraryForm(
    type: ItineraryType, 
    onSave: (Map<String, String>, Uri?, Boolean) -> Unit
) {
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var isShared by remember { mutableStateOf(true) }
    
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
                    listOfNotNull(p.name, p.city, p.state, p.country).distinct().joinToString(", ")
                }
            } catch (e: Exception) { locationSuggestions = emptyList() }
        } else { locationSuggestions = emptyList() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "Enter details for your ${type.label.lowercase()}", 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = fields["title"] ?: "",
                onValueChange = { fields = fields.toMutableMap().apply { put("title", it) } },
                label = { Text("Title (e.g., Flight to NYC)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }
        
        // Contextual Fields
        when (type.id) {
            "flight" -> {
                item { FormField(fields, "airline", "Airline", Icons.Default.AirplanemodeActive) { fields = it } }
                item { FormField(fields, "flight_number", "Flight Number", Icons.Default.Numbers) { fields = it } }
                item { 
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormField(fields, "terminal", "Terminal", modifier = Modifier.weight(1f)) { fields = it }
                        FormField(fields, "gate", "Gate", modifier = Modifier.weight(1f)) { fields = it }
                    }
                }
            }
            "train" -> {
                item { FormField(fields, "train_number", "Train Number", Icons.Default.Numbers) { fields = it } }
                item { FormField(fields, "platform", "Platform", Icons.Default.MeetingRoom) { fields = it } }
                item { FormField(fields, "seat", "Car / Seat", Icons.Default.EventSeat) { fields = it } }
            }
            "hotel", "airbnb", "hostel", "camping" -> {
                item {
                    SuggestionTextField(
                        value = fields["location"] ?: "",
                        onValueChange = { fields = fields.toMutableMap().apply { put("location", it) } },
                        label = "Address / Location",
                        suggestions = locationSuggestions,
                        onSuggestionClick = { 
                            fields = fields.toMutableMap().apply { put("location", it) }
                            locationSuggestions = emptyList()
                        },
                        icon = Icons.Default.Place
                    )
                }
                if (type.id != "camping") {
                    item { FormField(fields, "access_code", "Access Code", Icons.Default.Key) { fields = it } }
                } else {
                    item { FormField(fields, "campsite", "Campsite #", Icons.Default.Numbers) { fields = it } }
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
            item { FormField(fields, "bookingRef", "Confirmation / PNR", Icons.Default.ConfirmationNumber) { fields = it } }
        }

        // File Upload Section
        item {
            AttachmentSection(
                uri = attachmentUri,
                isShared = isShared,
                onPickFile = { launcher.launch("*/*") },
                onShareToggle = { isShared = it }
            )
        }

        item {
            Button(
                onClick = { onSave(fields, attachmentUri, isShared) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = (fields["title"]?.isNotBlank() ?: false)
            ) {
                Text("Confirm and Add", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun FormField(
    fields: Map<String, String>,
    key: String,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onValueChange: (Map<String, String>) -> Unit
) {
    OutlinedTextField(
        value = fields[key] ?: "",
        onValueChange = { onValueChange(fields.toMutableMap().apply { put(key, it) }) },
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null) } },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelector(value: String, onValueChange: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState()
    
    val displayFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Start Time & Date") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            shape = RoundedCornerShape(16.dp),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
    
    // Transparent overlay for click
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).clickable { showDatePicker = true })

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    showDatePicker = false
                    showTimePicker = true 
                }) { Text("Next") }
            }
        ) { DatePicker(state = dateState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    dateState.selectedDateMillis?.let { cal.timeInMillis = it }
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    onValueChange(displayFormat.format(cal.time))
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
fun AttachmentSection(uri: Uri?, isShared: Boolean, onPickFile: () -> Unit, onShareToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Attachments", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onPickFile) {
                    Text(if (uri == null) "Upload" else "Change")
                }
            }
            
            if (uri != null) {
                Text("File selected: ${uri.lastPathSegment}", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isShared, onCheckedChange = onShareToggle)
                    Text("Share with trip members", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
