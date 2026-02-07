package com.dash.travel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.dash.travel.ui.theme.*

/**
 * Text field with drop-down suggestions
 */
@Composable
fun SuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    placeholder: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions) {
        expanded = focused && suggestions.isNotEmpty()
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = Primary) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { 
                    focused = it.isFocused
                    expanded = it.isFocused && suggestions.isNotEmpty()
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceBorder,
                focusedContainerColor = SurfaceContainer,
                unfocusedContainerColor = SurfaceContainer
            ),
            singleLine = true
        )

        AnimatedVisibility(visible = expanded && suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = SurfaceContainerHigh,
                shadowElevation = 4.dp
            ) {
                Column {
                    suggestions.take(5).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionClick(suggestion)
                                    expanded = false
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface
                            )
                        }
                        if (suggestion != suggestions.last()) {
                            HorizontalDivider(color = SurfaceBorder)
                        }
                    }
                }
            }
        }
    }
}
