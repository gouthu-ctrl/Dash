package com.dash.travel.data.repository

import com.dash.travel.BuildConfig
import com.dash.travel.data.local.dao.RecentLocationsDao
import com.dash.travel.data.local.entity.toEntity
import com.dash.travel.data.models.LocationResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LocationRepository(
    private val recentLocationsDao: RecentLocationsDao
) {
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun getRecentLocations(): Flow<List<LocationResult>> {
        return recentLocationsDao.getRecentLocations().map { list ->
            list.map { it.toLocationResult() }
        }
    }

    suspend fun saveLocation(location: LocationResult) {
        recentLocationsDao.insert(location.toEntity())
    }

    suspend fun searchLocations(query: String): List<LocationResult> {
        val accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (accessToken.isBlank()) {
            return emptyList()
        }

        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$query.json"

        return try {
            val response = httpClient.get(url) {
                parameter("access_token", accessToken)
                parameter("types", "place,locality,airport,country")
                parameter("autocomplete", "true")
            }.body<JsonObject>()

            val features = response["features"]?.jsonArray ?: return emptyList()

            features.map { feature ->
                val fObj = feature.jsonObject
                val id = fObj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                val text = fObj["text"]?.jsonPrimitive?.contentOrNull ?: ""
                val placeName = fObj["place_name"]?.jsonPrimitive?.contentOrNull ?: ""
                val center = fObj["center"]?.jsonArray
                val long = center?.get(0)?.jsonPrimitive?.double ?: 0.0
                val lat = center?.get(1)?.jsonPrimitive?.double ?: 0.0

                val context = fObj["context"]?.jsonArray
                val country = context?.find {
                    it.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.startsWith("country") == true
                }?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

                val type = if (id.startsWith("airport")) {
                    LocationResult.LocationType.AIRPORT
                } else {
                    LocationResult.LocationType.CITY
                }

                LocationResult(
                    name = text,
                    placeId = "remote-$id",
                    latitude = lat,
                    longitude = long,
                    country = if (country.isNotBlank()) country else placeName,
                    type = type
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): LocationResult? {
        val cleanId = placeId.removePrefix("remote-")
        val accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        if (accessToken.isBlank()) return null

        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$cleanId.json"

        return try {
            val response = httpClient.get(url) {
                parameter("access_token", accessToken)
            }.body<JsonObject>()

            val features = response["features"]?.jsonArray
            val feature = features?.firstOrNull()?.jsonObject ?: return null

            val id = feature["id"]?.jsonPrimitive?.contentOrNull ?: ""
            val text = feature["text"]?.jsonPrimitive?.contentOrNull ?: ""
            val placeName = feature["place_name"]?.jsonPrimitive?.contentOrNull ?: ""
            val center = feature["center"]?.jsonArray
            val long = center?.get(0)?.jsonPrimitive?.double ?: 0.0
            val lat = center?.get(1)?.jsonPrimitive?.double ?: 0.0

            val context = feature["context"]?.jsonArray
            val country = context?.find {
                it.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.startsWith("country") == true
            }?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

            val type = if (id.startsWith("airport")) {
                LocationResult.LocationType.AIRPORT
            } else {
                LocationResult.LocationType.CITY
            }

            LocationResult(
                name = text,
                placeId = "remote-$id",
                latitude = lat,
                longitude = long,
                country = if (country.isNotBlank()) country else placeName,
                type = type
            )
        } catch (e: Exception) {
            null
        }
    }
}
