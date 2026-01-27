package com.dash.travel.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.BuildConfig
import com.dash.travel.data.local.dao.RecentLocationsDao
import com.dash.travel.data.local.entity.toEntity
import com.dash.travel.data.models.LocationResult
import com.dash.travel.data.search.LocalSearchService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.double
import kotlinx.serialization.json.contentOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- UI State Redefinition for Enhanced UX ---
sealed class LocationSearchUiState {
    object Idle : LocationSearchUiState()
    object Loading : LocationSearchUiState()
    object Empty : LocationSearchUiState()
    data class Success(val results: List<LocationResult>) : LocationSearchUiState()
    data class InitialSuggestions(
        val popular: List<LocationResult>,
        val recent: List<LocationResult>
    ) : LocationSearchUiState()
    data class Error(val message: String) : LocationSearchUiState()
}

class LocationSearchViewModel(
    private val recentLocationsDao: RecentLocationsDao,
    private val localSearchService: LocalSearchService
) : ViewModel() {

    // --- Local Data Sources (Instant Access) ---
    private val popularDestinations = listOf(
        LocationResult(name = "Tokyo", placeId = "local-pop-tokyo", latitude = 35.6895, longitude = 139.6917, country = "Japan", type = LocationResult.LocationType.CITY, isPopular = true),
        LocationResult(name = "London", placeId = "local-pop-london", latitude = 51.5074, longitude = 0.1278, country = "United Kingdom", type = LocationResult.LocationType.CITY, isPopular = true),
        LocationResult(name = "New York City", placeId = "local-pop-nyc", latitude = 40.7128, longitude = -74.0060, country = "United States", type = LocationResult.LocationType.CITY, isPopular = true)
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _recentLocations = MutableStateFlow<List<LocationResult>>(emptyList())

    // --- State Management ---
    private val _uiState = MutableStateFlow<LocationSearchUiState>(
        LocationSearchUiState.InitialSuggestions(popularDestinations, emptyList())
    )
    val uiState = _uiState.asStateFlow()

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        // One-time heavy lifting off Main thread
        viewModelScope.launch(Dispatchers.IO) {
            localSearchService.initializeIndex()
        }
        loadRecentLocations()
        observeSearchQuery()
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }

    private fun loadRecentLocations() {
        viewModelScope.launch(Dispatchers.IO) { // Database access off Main thread
            recentLocationsDao.getRecentLocations()
                .map { list -> 
                    list.map { it.toLocationResult().copy(isRecent = true) } 
                }
                .collect { recent ->
                    _recentLocations.value = recent
                    if (_searchQuery.value.isEmpty()) {
                        _uiState.value = LocationSearchUiState.InitialSuggestions(
                            popular = popularDestinations,
                            recent = recent
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.value = LocationSearchUiState.InitialSuggestions(
                popular = popularDestinations,
                recent = _recentLocations.value
            )
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearchQuery() {
        _searchQuery
            .filter { it.isNotBlank() }
            // Switch to a hybrid flow when query is 3+ chars
            .flatMapLatest { query ->
                Log.d("LocationSearch", "Processing query: $query")
                if (query.length < 3) {
                    return@flatMapLatest flowOf(
                        LocationSearchUiState.InitialSuggestions(
                            popularDestinations,
                            _recentLocations.value
                        )
                    )
                }
                
                // --- HYBRID SEARCH: Local Search (Instant) + Remote Search (Debounced/Cancellable) ---
                
                // 1. Local Search (Instant)
                val localFlow = flow {
                    val localResults = localSearchService.search(query)
                    if (localResults.isNotEmpty()) {
                        emit(LocationSearchUiState.Success(localResults))
                    } else {
                        emit(LocationSearchUiState.Loading) // Show loading while waiting for remote
                    }
                }.flowOn(Dispatchers.Default) // Local search is CPU-bound, use Default dispatcher
                
                // 2. Remote Search (Debounced, Fallback/Deep Search)
                val remoteFlow = flow {
                    emit(LocationSearchUiState.Loading) // Emit initial state to unblock combine
                    
                    // Introduce debouncing here, applied AFTER local search has run
                    kotlinx.coroutines.delay(300)
                    
                    try {
                        val remoteResults = searchPlaces(query)
                        Log.d("LocationSearch", "Remote results count: ${remoteResults.size}")
                        emit(LocationSearchUiState.Success(remoteResults))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("LocationSearch", "Error searching places", e)
                        emit(LocationSearchUiState.Error(e.message ?: "Network error"))
                    }
                }.flowOn(Dispatchers.IO) // Network operation off Main thread
                
                // Combine and prioritize local results
                localFlow.combine(remoteFlow) { localState, remoteState ->
                    when {
                        // Priority 1: Remote Success (Richer data, replaces local) IF it has results
                        remoteState is LocationSearchUiState.Success && remoteState.results.isNotEmpty() -> remoteState
                        
                        // Priority 2: Local Success (Instant feedback) - if remote is loading, error, or empty
                        localState is LocationSearchUiState.Success -> localState
                        
                        // Priority 3: Remote Error (Show error message if no local results)
                        remoteState is LocationSearchUiState.Error -> remoteState
                        
                        // Priority 4: Both Remote and Local are Empty -> show Empty
                        remoteState is LocationSearchUiState.Success -> LocationSearchUiState.Empty
                        
                        else -> LocationSearchUiState.Loading
                    }
                }.catch { e ->
                    if (e is CancellationException) throw e
                    // Catch unexpected errors in the combining flow
                    Log.e("LocationSearch", "Flow error", e)
                    emit(LocationSearchUiState.Error(e.message ?: "Flow processing error"))
                }
                
            }
            .onEach { state ->
                // State updates on Main thread
                _uiState.value = state
            }
            .launchIn(viewModelScope)
    }

    private suspend fun searchPlaces(query: String): List<LocationResult> {
        val accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (accessToken.isBlank()) {
            Log.e("LocationSearch", "Mapbox Access Token is missing or empty")
            throw Exception("Mapbox Access Token is missing")
        }

        // Encode the query to handle spaces and special characters
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json"
        
        Log.d("LocationSearch", "Fetching URL: $url with token ending in ...${accessToken.takeLast(4)}")

        try {
            val response = httpClient.get(url) {
                parameter("access_token", accessToken)
                // Removed "airport" as it was causing API errors. 
                // Available types: country, region, place, district, locality, postcode, neighborhood
                parameter("types", "place,locality,country")
                parameter("autocomplete", "true")
            }
            
            val responseBody = response.bodyAsText()
            Log.d("LocationSearch", "Raw API Response: $responseBody")

            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(responseBody)
            
            val features = jsonResponse["features"]?.jsonArray ?: return emptyList()
            
            return features.map { feature ->
                val fObj = feature.jsonObject
                val id = fObj["id"]?.jsonPrimitive?.content ?: ""
                val text = fObj["text"]?.jsonPrimitive?.content ?: ""
                val placeName = fObj["place_name"]?.jsonPrimitive?.content ?: ""
                val center = fObj["center"]?.jsonArray
                val long = center?.get(0)?.jsonPrimitive?.double ?: 0.0
                val lat = center?.get(1)?.jsonPrimitive?.double ?: 0.0
                
                val context = fObj["context"]?.jsonArray
                val country = context?.find { 
                    it.jsonObject["id"]?.jsonPrimitive?.content?.startsWith("country") == true 
                }?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                val type = if (id.startsWith("airport")) {
                    LocationResult.LocationType.AIRPORT
                } else {
                    LocationResult.LocationType.CITY
                }

                LocationResult(
                    name = text, // Use main text for list
                    placeId = "remote-$id",
                    latitude = lat,
                    longitude = long,
                    country = if (country.isNotBlank()) country else placeName, // Fallback to full name if country not parsed
                    type = type,
                    isRecent = false,
                    isPopular = false
                )
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("LocationSearch", "API call failed", e)
            throw e 
        }
    }

    fun onLocationSelected(
        result: LocationResult,
        onSelectionComplete: (String, kotlinx.serialization.json.JsonObject) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Mapbox search results already include coordinates, so we don't need a detail fetch usually.
                // However, for consistency with previous implementation, we just pass the data through.
                
                // For remote results, we already have Lat/Long from searchPlaces
                val finalResult = result.copy(isRecent = false, isPopular = false)

                // Save to history 
                recentLocationsDao.insert(finalResult.toEntity())

                // Callback with Supabase JSON format
                val json = buildJsonObject {
                    put("name", finalResult.name)
                    put("place_id", finalResult.placeId)
                    put("lat", finalResult.latitude)
                    put("long", finalResult.longitude)
                    put("country", finalResult.country)
                    put("source", "mapbox")
                }
                onSelectionComplete(finalResult.name, json)
            } catch (e: Exception) {
                // Fallback
                 val json = buildJsonObject {
                    put("name", result.name)
                    put("place_id", result.placeId)
                    put("lat", result.latitude)
                    put("long", result.longitude)
                    put("country", result.country)
                }
                onSelectionComplete(result.name, json)
            }
        }
    }
}
