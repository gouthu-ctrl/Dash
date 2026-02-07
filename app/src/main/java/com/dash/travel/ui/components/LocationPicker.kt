package com.dash.travel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dash.travel.data.local.dao.RecentLocationsDao
import com.dash.travel.data.models.LocationResult
import com.dash.travel.data.search.LocalSearchService
import com.dash.travel.di.DiContainer
import com.dash.travel.ui.theme.*
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

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = Primary 
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainer,
                unfocusedContainerColor = SurfaceContainer,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Transparent
            )
        )
        // Overlay to capture clicks
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showBottomSheet = true }
        )
    }

    if (showBottomSheet) {
        // Access DiContainer dependencies ONLY when sheet is shown (lazy access)
        val dao = remember { DiContainer.recentLocationsDao }
        val localSearchService = remember { DiContainer.localSearchService }
        
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Background,
            contentColor = OnBackground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = OnSurfaceVariant) },
            modifier = Modifier.fillMaxHeight(0.9f)
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
                return LocationSearchViewModel(recentLocationsDao, localSearchService) as T
            }
        }
    }
    val viewModel: LocationSearchViewModel = viewModel(factory = factory)
    
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search cities or airports") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = OnSurfaceVariant
                ) 
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = OnSurfaceVariant)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerHigh,
                unfocusedContainerColor = SurfaceContainerHigh,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.Transparent
            )
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
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                }
                is LocationSearchUiState.Empty -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No results found", 
                                style = MaterialTheme.typography.bodyLarge,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
                is LocationSearchUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error: ${state.message}", color = Error)
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
                        item { HeaderText(text = "Popular Destinations") }
                        items(state.popular, key = { "popular-${it.placeId}" }) { result ->
                            LocationResultItem(result) {
                                viewModel.onLocationSelected(result, onLocationSelected)
                            }
                        }
                    }
                    if (state.recent.isNotEmpty()) {
                        item { HeaderText(text = "Recently Selected") }
                        items(state.recent, key = { "recent-${it.placeId}" }) { result ->
                            LocationResultItem(result) {
                                viewModel.onLocationSelected(result, onLocationSelected)
                            }
                        }
                    }
                    if (state.popular.isEmpty() && state.recent.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp), 
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Start typing to search...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 8.dp)
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
        result.isRecent -> Secondary
        result.isPopular -> Warning
        else -> OnSurfaceVariant
    }

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!result.country.isNullOrEmpty()) {
                    Text(
                        text = result.country,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    HorizontalDivider(color = SurfaceBorder.copy(alpha = 0.5f))
}
