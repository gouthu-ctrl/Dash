package com.dash.travel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dash.travel.data.models.PhotonResponse
import com.dash.travel.ui.components.BeginnerTooltip
import com.dash.travel.ui.components.SectionHeader
import com.dash.travel.ui.components.SuggestionTextField
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onNavigateBack: () -> Unit,
    onSaveTrip: (tripName: String, description: String, startDate: String, endDate: String, destination: String, inviteEmails: List<String>) -> Unit,
    getHomeLocation: suspend () -> String,
    isFirstTrip: Boolean = true
) {
    var tripName by remember { mutableStateOf("") }
    var tripDescription by remember { mutableStateOf("") }
    var fromLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var inviteEmails by remember { mutableStateOf("") }

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    var showDateRangePicker by remember { mutableStateOf(false) }

    var fromSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var destSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    var showTooltip by remember { mutableStateOf(isFirstTrip) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    val isoFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val client = remember {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    LaunchedEffect(Unit) {
        fromLocation = getHomeLocation()
    }

    // Auto-population for "From Location"
    LaunchedEffect(fromLocation) {
        if (fromLocation.length > 2) {
            delay(500)
            try {
                val response: PhotonResponse = client.get("https://photon.komoot.io/api/?q=$fromLocation&limit=5&lang=en").body()
                fromSuggestions = response.features.mapNotNull { feat ->
                    val p = feat.properties
                    listOfNotNull(p.name, p.city, p.state, p.country).distinct().joinToString(", ")
                }
            } catch (e: Exception) {
                fromSuggestions = emptyList()
            }
        } else {
            fromSuggestions = emptyList()
        }
    }

    // Auto-population for "Destination"
    LaunchedEffect(destination) {
        if (destination.length > 2) {
            delay(500)
            try {
                val response: PhotonResponse = client.get("https://photon.komoot.io/api/?q=$destination&limit=5&lang=en").body()
                destSuggestions = response.features.mapNotNull { feat ->
                    val p = feat.properties
                    listOfNotNull(p.name, p.city, p.state, p.country).distinct().joinToString(", ")
                }
            } catch (e: Exception) {
                destSuggestions = emptyList()
            }
        } else {
            destSuggestions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Plan Your Journey",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    BeginnerTooltip(
                        text = "Fill in your trip details to get personalized recommendations later.",
                        onDismiss = { showTooltip = false }
                    )
                }
            }

            item {
                SectionHeader("Trip Details")
                OutlinedTextField(
                    value = tripName,
                    onValueChange = { tripName = it },
                    label = { Text("Trip Title") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("e.g., Summer in Japan") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                
                OutlinedTextField(
                    value = tripDescription,
                    onValueChange = { tripDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    placeholder = { Text("What\u0027s this trip about?") },
                    minLines = 2,
                    maxLines = 4,
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium
                )
            }

            item {
                SectionHeader("When are you going?")
                val dateRangeText = if (startDate != null && endDate != null) {
                    "${dateFormatter.format(startDate!!)} - ${dateFormatter.format(endDate!!)}"
                } else {
                    ""
                }
                
                Box(modifier = Modifier.fillMaxWidth().clickable { showDateRangePicker = true }) {
                    OutlinedTextField(
                        value = dateRangeText,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Travel Dates") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("Pick your travel window") },
                        trailingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Dates")
                        },
                        shape = MaterialTheme.shapes.medium,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            item {
                SectionHeader("Location")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SuggestionTextField(
                        value = fromLocation,
                        onValueChange = { fromLocation = it },
                        label = "Departing From",
                        suggestions = fromSuggestions,
                        onSuggestionClick = { 
                            fromLocation = it
                            fromSuggestions = emptyList()
                        },
                        icon = Icons.Default.LocationOn
                    )
                    
                    SuggestionTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = "Destination",
                        suggestions = destSuggestions,
                        onSuggestionClick = { 
                            destination = it
                            destSuggestions = emptyList()
                        },
                        icon = Icons.Default.Map,
                        placeholder = "Where to?"
                    )
                }
            }

            item {
                SectionHeader("Travelers")
                OutlinedTextField(
                    value = inviteEmails,
                    onValueChange = { inviteEmails = it },
                    label = { Text("Invite via Email") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    placeholder = { Text("friend@example.com, ...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.medium
                )
            }

            item {
                Button(
                    onClick = {
                        val emails = inviteEmails.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        startDate?.let { sDate ->
                            endDate?.let { eDate ->
                                onSaveTrip(tripName, tripDescription, isoFormatter.format(sDate), isoFormatter.format(eDate), destination, emails)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    enabled = tripName.isNotBlank() && startDate != null && endDate != null && destination.isNotBlank(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("Create Trip", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
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
