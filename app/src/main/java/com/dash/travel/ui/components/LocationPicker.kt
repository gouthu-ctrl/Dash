package com.dash.travel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.dash.travel.data.local.DashDatabase
import com.dash.travel.data.local.dao.RecentLocationsDao
import com.dash.travel.data.models.LocationResult
import com.dash.travel.data.search.LocalSearchService
import com.dash.travel.ui.viewmodel.LocationSearchUiState
import com.dash.travel.ui.viewmodel.LocationSearchViewModel
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerField(
    label: String,
    initialValue: String = "",
    onLocationSelected: (String, JsonObject) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(initialValue) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val context = LocalContext.current
    
    // WARNING: In a production application, these dependencies (Room DB) 
    // should be initialized at the Application level and injected here (e.g., using Hilt) 
    // to prevent potential leaks or jank on first access.
    val database = remember {
        Room.databaseBuilder(
            context.applicationContext,
            DashDatabase::class.java, "dash_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    val dao = remember { database.recentLocationsDao() }
    val localSearchService = remember { LocalSearchService(context) } 

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors()
        )
        // Overlay to capture clicks
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showBottomSheet = true }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxHeight() 
        ) {
            LocationSearchSheetContent(
                recentLocationsDao = dao,
                localSearchService = localSearchService, 
                onLocationSelected = { name, json ->
                    selectedText = name
                    onLocationSelected(name, json)
                    showBottomSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheetContent(
    recentLocationsDao: RecentLocationsDao,
    localSearchService: LocalSearchService, 
    onLocationSelected: (String, JsonObject) -> Unit
) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // NEW: Pass dependencies without PlacesClient
                return LocationSearchViewModel(recentLocationsDao, localSearchService) as T
            }
        }
    }
    val viewModel: LocationSearchViewModel = viewModel(factory = factory)
    
    // Read state once
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search cities or airports") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f), 
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (val state = uiState) {
                is LocationSearchUiState.Loading -> {
                    item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is LocationSearchUiState.Empty -> {
                    item(key = "empty") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No results found for \"$searchQuery\"", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is LocationSearchUiState.Error -> {
                    item(key = "error") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is LocationSearchUiState.Success -> {
                    items(state.results, key = { it.placeId }) { result ->
                        LocationResultItem(result) {
                            viewModel.onLocationSelected(result, onLocationSelected)
                        }
                    }
                }
                is LocationSearchUiState.InitialSuggestions -> {
                    if (state.popular.isNotEmpty()) {
                        item(key = "header-popular") { HeaderText(text = "Popular Destinations") }
                        items(state.popular, key = { it.placeId }) { result ->
                            LocationResultItem(result) {
                                viewModel.onLocationSelected(result, onLocationSelected)
                            }
                        }
                    }
                    if (state.recent.isNotEmpty()) {
                        item(key = "header-recent") { HeaderText(text = "Recently Selected") }
                        items(state.recent, key = { it.placeId }) { result ->
                            LocationResultItem(result) {
                                viewModel.onLocationSelected(result, onLocationSelected)
                            }
                        }
                    }
                    if (state.popular.isEmpty() && state.recent.isEmpty()) {
                        item(key = "empty-suggestions") {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Start typing to search for a destination.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                else -> { /* Idle state, nothing to show */ }
            }
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun LocationResultItem(
    result: LocationResult,
    onClick: () -> Unit
) {
    val icon = when {
        result.isRecent -> Icons.Default.History
        result.isPopular -> Icons.Default.Star
        result.type == LocationResult.LocationType.AIRPORT -> Icons.Default.AirplanemodeActive
        else -> Icons.Default.LocationCity
    }
    
    val tint = when {
        result.isRecent -> MaterialTheme.colorScheme.secondary
        result.isPopular -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column {
        ListItem(
            headlineContent = { Text(result.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { 
                Text(
                    result.country ?: "", 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        )
        HorizontalDivider()
    }
}
