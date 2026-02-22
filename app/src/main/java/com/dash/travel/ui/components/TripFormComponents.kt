package com.dash.travel.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dash.travel.ui.theme.*
import java.util.UUID

/**
 * Data class representing a custom field with name and value
 */
data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: String
)

/**
 * Custom Fields Section with progressive disclosure
 */
@Composable
fun CustomFieldsSection(
    fields: List<CustomField>,
    onAddField: (name: String, value: String) -> Unit,
    onRemoveField: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(fields.isNotEmpty()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Expandable Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { isExpanded = !isExpanded },
            color = SurfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Custom Fields",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                    Text(
                        text = if (fields.isEmpty()) "Add hotel confirmations, notes, etc." 
                               else "${fields.size} field${if (fields.size > 1) "s" else ""} added",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = OnSurfaceVariant
                )
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Existing fields
                fields.forEach { field ->
                    CustomFieldItem(
                        field = field,
                        onRemove = { onRemoveField(field.id) }
                    )
                }

                // Add field button
                SecondaryButton(
                    text = "Add Custom Field",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Add field dialog
    if (showAddDialog) {
        AddCustomFieldDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, value ->
                onAddField(name, value)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CustomFieldItem(
    field: CustomField,
    onRemove: () -> Unit
) {
    DashCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = field.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                Text(
                    text = field.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove field",
                    tint = Error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCustomFieldDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String) -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var fieldValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Booking Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    label = { Text("Value") },
                    placeholder = { Text("e.g. XYZ-123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fieldName, fieldValue) },
                enabled = fieldName.isNotBlank() && fieldValue.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
            ) {
                Text("Cancel")
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

/**
 * Timezone selector dropdown
 */
@Composable
fun TimezoneSelector(
    selectedTimezone: String,
    onTimezoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val commonTimezones = remember {
        listOf(
            // Americas
            "America/New_York" to "New York (ET)",
            "America/Chicago" to "Chicago (CT)",
            "America/Denver" to "Denver (MT)",
            "America/Los_Angeles" to "Los Angeles (PT)",
            "America/Anchorage" to "Anchorage (AKT)",
            "Pacific/Honolulu" to "Honolulu (HST)",
            "America/Toronto" to "Toronto (ET)",
            "America/Vancouver" to "Vancouver (PT)",
            "America/Mexico_City" to "Mexico City (CST)",
            "America/Sao_Paulo" to "São Paulo (BRT)",
            "America/Buenos_Aires" to "Buenos Aires (ART)",
            "America/Lima" to "Lima (PET)",
            "America/Bogota" to "Bogotá (COT)",
            
            // Europe
            "Europe/London" to "London (GMT)",
            "Europe/Paris" to "Paris (CET)",
            "Europe/Berlin" to "Berlin (CET)",
            "Europe/Rome" to "Rome (CET)",
            "Europe/Madrid" to "Madrid (CET)",
            "Europe/Amsterdam" to "Amsterdam (CET)",
            "Europe/Zurich" to "Zurich (CET)",
            "Europe/Vienna" to "Vienna (CET)",
            "Europe/Stockholm" to "Stockholm (CET)",
            "Europe/Athens" to "Athens (EET)",
            "Europe/Moscow" to "Moscow (MSK)",
            "Europe/Istanbul" to "Istanbul (TRT)",
            
            // Asia
            "Asia/Dubai" to "Dubai (GST)",
            "Asia/Kolkata" to "India (IST)",
            "Asia/Bangkok" to "Bangkok (ICT)",
            "Asia/Singapore" to "Singapore (SGT)",
            "Asia/Hong_Kong" to "Hong Kong (HKT)",
            "Asia/Shanghai" to "Shanghai (CST)",
            "Asia/Tokyo" to "Tokyo (JST)",
            "Asia/Seoul" to "Seoul (KST)",
            "Asia/Manila" to "Manila (PHT)",
            "Asia/Jakarta" to "Jakarta (WIB)",
            "Asia/Kuala_Lumpur" to "Kuala Lumpur (MYT)",
            
            // Oceania
            "Australia/Sydney" to "Sydney (AEST)",
            "Australia/Melbourne" to "Melbourne (AEST)",
            "Australia/Perth" to "Perth (AWST)",
            "Australia/Brisbane" to "Brisbane (AEST)",
            "Pacific/Auckland" to "Auckland (NZST)",
            "Pacific/Fiji" to "Fiji (FJT)",
            
            // Africa & Middle East
            "Africa/Johannesburg" to "Johannesburg (SAST)",
            "Africa/Cairo" to "Cairo (EET)",
            "Africa/Lagos" to "Lagos (WAT)",
            "Africa/Nairobi" to "Nairobi (EAT)",
            "Asia/Jerusalem" to "Jerusalem (IST)",
            "Asia/Riyadh" to "Riyadh (AST)",
            
            // UTC
            "UTC" to "UTC (Universal)"
        )
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = SurfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Timezone",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = commonTimezones.find { it.first == selectedTimezone }?.second 
                               ?: selectedTimezone,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Select timezone",
                    tint = OnSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(SurfaceContainerHigh)
        ) {
            commonTimezones.forEach { (tzId, displayName) ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface
                        ) 
                    },
                    onClick = {
                        onTimezoneSelected(tzId)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Notes text field
 */
@Composable
fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("Trip Notes") },
        placeholder = { Text("Any important details...") },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
            focusedBorderColor = Primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}
