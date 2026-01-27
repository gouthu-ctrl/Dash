package com.dash.travel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dash.travel.data.models.Trip
import com.dash.travel.ui.components.BeginnerTooltip
import com.dash.travel.ui.components.LocationPickerField
import com.dash.travel.ui.components.SectionHeader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onNavigateBack: () -> Unit,
    onSaveTrip: (tripName: String, description: String, startDate: String, endDate: String, destination: JsonObject?, origin: JsonObject?, inviteEmails: List<String>) -> Unit,
    getHomeLocation: suspend () -> String,
    isFirstTrip: Boolean = true,
    existingTrip: Trip? = null
) {
    val isoFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    // State to hold the selected location's JSON object
    var fromLocationData by remember { 
        mutableStateOf<JsonObject?>(existingTrip?.originData?.jsonObject) 
    }
    
    // State to hold the displayed name of the location
    var fromLocationName by remember { 
        mutableStateOf(existingTrip?.originData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "")
    }

    var destinationData by remember { 
        mutableStateOf<JsonObject?>(existingTrip?.destinationData?.jsonObject) 
    }
    var destinationName by remember { 
        mutableStateOf(existingTrip?.destinationData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "") 
    }
    
    // Derived Trip Title
    val tripName = remember(destinationName) {
        if (destinationName.isNotBlank()) "Trip to $destinationName" else "New Trip"
    }
    
    var inviteEmails by remember { mutableStateOf("") } 

    var startDate by remember { 
        mutableStateOf<Date?>(
            existingTrip?.startDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
        ) 
    }
    var endDate by remember { 
        mutableStateOf<Date?>(
            existingTrip?.endDate?.let { try { isoFormatter.parse(it) } catch (e: Exception) { null } }
        ) 
    }

    var showDateRangePicker by remember { mutableStateOf(false) }

    var showTooltip by remember { mutableStateOf(isFirstTrip && existingTrip == null) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    LaunchedEffect(Unit) {
        if (existingTrip == null && fromLocationName.isBlank()) {
            fromLocationName = getHomeLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (existingTrip != null) "Edit Trip" else "Plan a New Trip",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    BeginnerTooltip(
                        text = "Pick a destination and we'll handle the rest.",
                        onDismiss = { showTooltip = false }
                    )
                }
            }
            
            // Location Section - Moved to Top
            item {
                SectionHeader("Where to?")
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                }
            }

            // Dates Section - Improved UI
            item {
                SectionHeader("Dates")
                Surface(
                    onClick = { showDateRangePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface, // Using surface with border as a safe default
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (startDate != null && endDate != null) 
                                    "${dateFormatter.format(startDate!!)} - ${dateFormatter.format(endDate!!)}"
                                else "Select Dates",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (startDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (startDate == null) {
                                Text(
                                    text = "When are you going?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.CalendarMonth, 
                            contentDescription = "Select Dates",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (existingTrip == null) {
                item {
                    SectionHeader("Travel Companions")
                    OutlinedTextField(
                        value = inviteEmails,
                        onValueChange = { inviteEmails = it },
                        label = { Text("Invite via Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        placeholder = { Text("friend@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )
                    Text(
                        text = "Separate emails with commas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val emails = inviteEmails.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        startDate?.let { sDate ->
                            endDate?.let { eDate ->
                                onSaveTrip(
                                    tripName, 
                                    "", // Description removed
                                    isoFormatter.format(sDate), 
                                    isoFormatter.format(eDate), 
                                    destinationData, 
                                    fromLocationData, 
                                    emails
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(56.dp),
                    enabled = destinationName.isNotBlank() && startDate != null && endDate != null && destinationData != null && fromLocationData != null,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (existingTrip != null) "Update Trip" else "Start Planning", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate?.time,
            initialSelectedEndDateMillis = endDate?.time
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { startMillis ->
                        dateRangePickerState.selectedEndDateMillis?.let { endMillis ->
                            val calStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calStart.timeInMillis = startMillis
                            startDate = calStart.time

                            val calEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calEnd.timeInMillis = endMillis
                            endDate = calEnd.time
                        }
                    }
                    showDateRangePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = { Text("Select Trip Dates", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false
            )
        }
    }
}
