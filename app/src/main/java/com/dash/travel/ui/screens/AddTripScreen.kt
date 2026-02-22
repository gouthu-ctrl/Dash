package com.dash.travel.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dash.travel.data.model.SupabaseTrip
import com.dash.travel.data.model.DestinationData
import com.dash.travel.data.model.OriginData
import com.dash.travel.ui.viewmodel.AddTripViewModel
import com.dash.travel.ui.components.*
import com.dash.travel.ui.onboarding.SmartTooltip
import com.dash.travel.ui.onboarding.TooltipIds
import com.dash.travel.ui.theme.*
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Premium Add/Edit Trip Screen
 * 
 * Features:
 * - Location picker with search
 * - Date range selection
 * - Custom fields and notes
 * - Timezone selection
 * - Collaborator invite
 * - Smart tooltips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onNavigateBack: () -> Unit,
    onSaveTrip: (
        tripName: String,
        description: String,
        startDate: String,
        endDate: String,
        timezone: String,
        customAttributes: JsonObject?,
        destination: JsonObject?,
        origin: JsonObject?,
        inviteEmails: List<String>
    ) -> Unit,
    getHomeLocation: suspend () -> String,
    isFirstTrip: Boolean = true,
    existingTrip: SupabaseTrip? = null,
    tripId: String? = null,
    viewModel: AddTripViewModel? = null
) {
    val isoFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // Load trip if ID provided
    LaunchedEffect(tripId) {
        if (tripId != null && viewModel != null) {
            viewModel.loadTrip(tripId)
        }
    }

    val loadedTripState = viewModel?.trip?.collectAsStateWithLifecycle()
    val loadedTrip = loadedTripState?.value
    val displayTrip = loadedTrip ?: existingTrip
    
    // Helper to convert data model to JsonObject for existing components (if needed)
    // or adapt components. For now we reconstruct JsonObjects from SupabaseTrip data.
    fun DestinationData?.toJson(): JsonObject? {
        if (this == null) return null
        return buildJsonObject {
            put("name", name ?: "")
            put("country", country ?: "")
            placeId?.let { put("place_id", it) }
            lat?.let { put("lat", it) }
            lng?.let { put("lng", it) }
        }
    }

    fun OriginData?.toJson(): JsonObject? {
        if (this == null) return null
        return buildJsonObject {
            put("name", name ?: "")
            put("country", country ?: "")
            placeId?.let { put("place_id", it) }
            lat?.let { put("lat", it) }
            lng?.let { put("lng", it) }
        }
    }

    // Location states - Initialize from displayTrip (which might be null initially then update)
    var fromLocationData by remember { mutableStateOf<JsonObject?>(displayTrip?.originData.toJson()) }
    var fromLocationName by remember { mutableStateOf(displayTrip?.originData?.name ?: "") }
    
    var destinationData by remember { mutableStateOf<JsonObject?>(displayTrip?.destinationData.toJson()) }
    var destinationName by remember { mutableStateOf(displayTrip?.destinationData?.name ?: "") }

    // Update state when displayTrip changes (async load)
    LaunchedEffect(displayTrip) {
        if (displayTrip != null) {
            fromLocationData = displayTrip.originData.toJson()
            fromLocationName = displayTrip.originData?.name ?: ""
            destinationData = displayTrip.destinationData.toJson()
            destinationName = displayTrip.destinationData?.name ?: ""
        }
    }
    
    // Derived Trip Title
    val tripName = remember(destinationName) {
        if (destinationName.isNotBlank()) "Trip to $destinationName" else "New Trip"
    }
    
    // Defaults for removed fields (Timezone, Notes, Custom Fields)
    val selectedTimezone = displayTrip?.timezone ?: "UTC"
    val notes = displayTrip?.description ?: ""
    
    // Invite emails
    var inviteEmails by remember { mutableStateOf("") }

    // Date states
    var startDate by remember { 
        mutableStateOf<Date?>(
            displayTrip?.startDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
        ) 
    }
    var endDate by remember { 
        mutableStateOf<Date?>(
            displayTrip?.endDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
        ) 
    }
    
    LaunchedEffect(displayTrip) {
        if (displayTrip != null) {
            startDate = displayTrip.startDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
            endDate = displayTrip.endDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
        }
    }

    var showDateRangePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    // Load home location on first launch
    LaunchedEffect(Unit) {
        if (displayTrip == null && fromLocationName.isBlank()) {
            fromLocationName = getHomeLocation()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (existingTrip != null) "Edit Trip" else "New Adventure",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Location Section
            item {
                SectionHeader(title = "Where & When")
                DashCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LocationPickerField(
                            label = "Destination",
                            initialValue = destinationName,
                            onLocationSelected = { name, json ->
                                destinationName = name
                                destinationData = json
                            }
                        )
                        
                        LocationPickerField(
                            label = "Departing From",
                            initialValue = fromLocationName,
                            onLocationSelected = { name, json ->
                                fromLocationName = name
                                fromLocationData = json
                            }
                        )
                        
                        // Date Picker Trigger
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDateRangePicker = true },
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (startDate != null && endDate != null) 
                                            "${dateFormatter.format(startDate!!)} - ${dateFormatter.format(endDate!!)}"
                                        else "Select Dates",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (startDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (startDate == null) {
                                        Text(
                                            text = "When are you going?",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            // Travel Companions (only for new trips)
            if (existingTrip == null) {
                item {
                    SectionHeader(title = "Travel Companions")
                    DashCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = inviteEmails,
                                onValueChange = { inviteEmails = it },
                                label = { Text("Invite via Email") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Email, 
                                        contentDescription = null,
                                        tint = OnSurfaceVariant 
                                    ) 
                                },
                                placeholder = { Text("friend@example.com, family@test.com") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GroupAdd, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "They'll get an invite to join this trip",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Primary Action Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                GradientButton(
                    text = if (existingTrip != null) "Update Trip" else "Start Planning",
                    onClick = {
                        val emails = inviteEmails.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        
                        val customAttrs: JsonObject? = null
                        
                        startDate?.let { sDate ->
                            endDate?.let { eDate ->
                                onSaveTrip(
                                    tripName,
                                    notes,
                                    isoFormatter.format(sDate),
                                    isoFormatter.format(eDate),
                                    selectedTimezone,
                                    customAttrs,
                                    destinationData,
                                    fromLocationData,
                                    emails
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = destinationName.isNotBlank() && 
                              startDate != null && 
                              endDate != null && 
                              destinationData != null && 
                              (fromLocationData != null || fromLocationName.isNotBlank())
                )
                
                // Tooltip for first time users
                if (isFirstTrip && existingTrip == null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SmartTooltip(
                            tooltipId = TooltipIds.ADD_TRIP_SAVE,
                            message = "Fill in the details to enable the button!",
                            position = com.dash.travel.ui.onboarding.TooltipPosition.TOP
                        )
                    }
                }
            }
        }
    }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate?.time,
            initialSelectedEndDateMillis = endDate?.time
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateRangePickerState.selectedStartDateMillis?.let { startMillis ->
                            dateRangePickerState.selectedEndDateMillis?.let { endMillis ->
                                val calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startMillis }
                                startDate = calStart.time

                                val calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endMillis }
                                endDate = calEnd.time
                            }
                        }
                        showDateRangePicker = false
                    }
                ) { 
                    Text("Confirm", fontWeight = FontWeight.Bold) 
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { 
                    Text("Cancel", color = OnSurfaceVariant) 
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                dayContentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = { 
                    Text(
                        "Select Trip Dates", 
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
