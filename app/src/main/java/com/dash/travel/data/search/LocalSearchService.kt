package com.dash.travel.data.search

import android.content.Context
import com.dash.travel.data.models.LocationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// In a real application, the search structure would be a Trie/B-Tree for sub-millisecond search.
// We use a simple list filter and ConcurrentHashMap for safe indexing/caching to simulate the local component.

class LocalSearchService(private val context: Context) {

    // Simulates the massive in-memory index
    private var locationIndex: List<LocationResult> = emptyList()

    // Used to cache index results for faster access
    private val indexCache = ConcurrentHashMap<String, LocationResult>()

    private val localIndexDataSource = listOf(
        LocationResult(name = "Paris", placeId = "local-par", latitude = 48.8566, longitude = 2.3522, country = "France", type = LocationResult.LocationType.CITY),
        LocationResult(name = "New York", placeId = "local-nyc", latitude = 40.7128, longitude = -74.0060, country = "United States", type = LocationResult.LocationType.CITY),
        LocationResult(name = "San Francisco", placeId = "local-sfo", latitude = 37.7749, longitude = -122.4194, country = "United States", type = LocationResult.LocationType.CITY),
        LocationResult(name = "Tokyo (NRT)", placeId = "local-nrt", latitude = 35.7635, longitude = 140.3860, country = "Japan", type = LocationResult.LocationType.AIRPORT),
        LocationResult(name = "London Heathrow (LHR)", placeId = "local-lhr", latitude = 51.4700, longitude = 0.4543, country = "United Kingdom", type = LocationResult.LocationType.AIRPORT),
        LocationResult(name = "Frankfurt", placeId = "local-fra", latitude = 50.1109, longitude = 8.6821, country = "Germany", type = LocationResult.LocationType.CITY),
        LocationResult(name = "Berlin", placeId = "local-ber", latitude = 52.5200, longitude = 13.4050, country = "Germany", type = LocationResult.LocationType.CITY),
        LocationResult(name = "Shanghai", placeId = "local-shg", latitude = 31.2304, longitude = 121.4737, country = "China", type = LocationResult.LocationType.CITY),
        // Simulate 50,000 entries by repeating a list of items to make search slower (and thus test threading)
    )

    suspend fun initializeIndex() = withContext(Dispatchers.Default) {
        // This simulates reading a 5MB JSON file and indexing it. Must be off Main thread.
        // The actual list is small here, but the context is that it's a heavy operation.
        locationIndex = localIndexDataSource.map { result ->
            // Simulate processing/normalization during indexing
            indexCache[result.name.lowercase()] = result
            result
        }
    }

    suspend fun search(query: String): List<LocationResult> = withContext(Dispatchers.Default) {
        if (query.length < 3) return@withContext emptyList()
        val lowerCaseQuery = query.lowercase()

        // High-performance search simulation (prefix matching)
        // This simulates traversing a Trie/Tree structure, which should be very fast.
        locationIndex
            .filter {
                it.name.lowercase().startsWith(lowerCaseQuery) ||
                (it.country?.lowercase()?.startsWith(lowerCaseQuery) == true)
            }
            .sortedBy { it.name.length } // Prioritize shorter, more precise matches
            .take(10) // Limit to top 10 local results
    }
}
