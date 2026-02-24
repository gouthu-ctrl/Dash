package com.dash.travel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dash.travel.data.model.GeneratedItem
import com.dash.travel.ui.viewmodel.AISuggestionsViewModel

/**
 * Bottom sheet for AI-powered trip suggestions
 * 
 * Features:
 * - Editable location chip (prefilled from last itinerary or trip destination)
 * - Quick prompt chips for common queries
 * - Custom text input for personalized queries
 * - Loading state with shimmer
 * - Results list with AISuggestionCard items (paginated to 5)
 * - Load more button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISuggestionsBottomSheet(
    sheetState: SheetState,
    viewModel: AISuggestionsViewModel,
    tripDestination: String = "",
    lastItineraryLocation: String = "",
    onDismiss: () -> Unit,
    onItemAdded: () -> Unit = {}
) {
    val uiState by viewModel.uiState
    val suggestions = viewModel.suggestions
    val addingItems by viewModel.addingItems
    val currentLocation by viewModel.currentLocation
    
    var promptText by remember { mutableStateOf("") }
    var showLocationEditor by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Initialize location on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeLocation(tripDestination, lastItineraryLocation)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Location Chip (Editable)
            LocationChip(
                location = currentLocation,
                isEditing = showLocationEditor,
                editingLocation = editingLocation,
                onStartEdit = {
                    editingLocation = currentLocation
                    showLocationEditor = true
                },
                onLocationChange = { editingLocation = it },
                onConfirmEdit = {
                    viewModel.updateLocation(editingLocation)
                    showLocationEditor = false
                },
                onCancelEdit = {
                    showLocationEditor = false
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Prompt Chips
            Text(
                text = "Quick prompts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuickPromptChips(
                onPromptSelected = { prompt ->
                    promptText = prompt
                    viewModel.generateSuggestions(prompt)
                    focusManager.clearFocus()
                },
                enabled = uiState !is AISuggestionsViewModel.UiState.Loading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Custom Input
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What would you like to do?") },
                singleLine = true,
                enabled = uiState !is AISuggestionsViewModel.UiState.Loading,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (promptText.isNotBlank()) {
                                viewModel.generateSuggestions(promptText)
                                focusManager.clearFocus()
                            }
                        },
                        enabled = promptText.isNotBlank() && uiState !is AISuggestionsViewModel.UiState.Loading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (promptText.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (promptText.isNotBlank()) {
                            viewModel.generateSuggestions(promptText)
                            focusManager.clearFocus()
                        }
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            ) {
                when (val state = uiState) {
                    is AISuggestionsViewModel.UiState.Idle -> {
                        EmptyStateContent()
                    }
                    is AISuggestionsViewModel.UiState.Loading -> {
                        LoadingStateContent()
                    }
                    is AISuggestionsViewModel.UiState.LoadingMore -> {
                        SuggestionsListContent(
                            suggestions = suggestions,
                            addingItems = addingItems,
                            hasMore = true,
                            isLoadingMore = true,
                            onAddToTrip = { item ->
                                viewModel.addToItinerary(item) {
                                    onItemAdded()
                                }
                            },
                            onDismiss = { item ->
                                viewModel.dismissSuggestion(item)
                            },
                            onLoadMore = { viewModel.loadMore() }
                        )
                    }
                    is AISuggestionsViewModel.UiState.Success -> {
                        if (suggestions.isEmpty()) {
                            AllAddedStateContent()
                        } else {
                            SuggestionsListContent(
                                suggestions = suggestions,
                                addingItems = addingItems,
                                hasMore = state.hasMore,
                                isLoadingMore = false,
                                onAddToTrip = { item ->
                                    viewModel.addToItinerary(item) {
                                        onItemAdded()
                                    }
                                },
                                onDismiss = { item ->
                                    viewModel.dismissSuggestion(item)
                                },
                                onLoadMore = { viewModel.loadMore() }
                            )
                        }
                    }
                    is AISuggestionsViewModel.UiState.Error -> {
                        ErrorStateContent(message = state.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationChip(
    location: String,
    isEditing: Boolean,
    editingLocation: String,
    onStartEdit: () -> Unit,
    onLocationChange: (String) -> Unit,
    onConfirmEdit: () -> Unit,
    onCancelEdit: () -> Unit
) {
    if (isEditing) {
        // Editing mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = editingLocation,
                onValueChange = onLocationChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter location") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onConfirmEdit) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCancelEdit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Display mode
        Surface(
            modifier = Modifier.clickable { onStartEdit() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = location.ifBlank { "Set location" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (location.isBlank()) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPromptChips(
    onPromptSelected: (String) -> Unit,
    enabled: Boolean
) {
    data class QuickPrompt(val label: String, val prompt: String, val icon: ImageVector)
    
    val prompts = listOf(
        // Row 1: Popular/Essential
        QuickPrompt("Must-see", "Must-see attractions and landmarks", Icons.Default.PhotoCamera),
        QuickPrompt("Food", "Best local food and restaurants", Icons.Default.Restaurant),
        QuickPrompt("Cafes", "Cozy cafes and coffee shops", Icons.Default.LocalCafe),
        // Row 2: Discovery
        QuickPrompt("Hidden gems", "Hidden gems and local secrets", Icons.Default.Explore),
        QuickPrompt("Free", "Free things to do", Icons.Default.Savings),
        QuickPrompt("Nightlife", "Bars and nightlife spots", Icons.Default.MusicNote),
        // Row 3: Activities
        QuickPrompt("Outdoor", "Outdoor activities and nature", Icons.Default.Park),
        QuickPrompt("Shopping", "Best shopping spots and markets", Icons.Default.ShoppingBag),
        QuickPrompt("Culture", "Museums, galleries, and culture", Icons.Default.Museum)
    )
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                // Consume horizontal gestures so they don't propagate to bottom sheet
                detectHorizontalDragGestures { _, _ -> }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true
    ) {
        items(prompts) { prompt ->
            FilterChip(
                onClick = { onPromptSelected(prompt.prompt) },
                label = { Text(prompt.label) },
                selected = false,
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        imageVector = prompt.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ask AI for suggestions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Use a quick prompt or type your own",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingStateContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Thinking...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Finding the best suggestions for you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SuggestionsListContent(
    suggestions: List<GeneratedItem>,
    addingItems: Set<String>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onAddToTrip: (GeneratedItem) -> Unit,
    onDismiss: (GeneratedItem) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = suggestions,
            key = { it.title }
        ) { item ->
            AISuggestionCard(
                item = item,
                isAdding = addingItems.contains(item.title),
                onAddToTrip = { onAddToTrip(item) },
                onDismiss = { onDismiss(item) }
            )
        }
        
        // Load More button
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        OutlinedButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load More")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAddedStateContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All done!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "All suggestions have been added to your trip",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorStateContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
