package com.dash.travel.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
private data class PixabayResponse(
    val total: Int,
    val totalHits: Int,
    val hits: List<PixabayHit>
)

@Serializable
private data class PixabayHit(
    val id: Int,
    val webformatURL: String,
    val largeImageURL: String,
    val previewURL: String
)

/**
 * Repository for fetching images from Pixabay
 */
class ImageRepository(private val client: HttpClient) {

    private val apiKey = "54576553-76da325eebc78dee5a333fe77"
    private val baseUrl = "https://pixabay.com/api/"

    /**
     * Fetch a random image for a given query (e.g. city name)
     * Optimized for mobile using webformatURL (640px wide)
     */
    suspend fun fetchRandomImage(query: String): String? {
        return try {
            val response: PixabayResponse = client.get(baseUrl) {
                parameter("key", apiKey)
                parameter("q", query)
                parameter("image_type", "photo")
                parameter("orientation", "horizontal")
                parameter("safesearch", "true")
                parameter("per_page", 20) // Get top 20 and pick random
            }.body()

            if (response.hits.isNotEmpty()) {
                // Return a random hit's webformatURL (640px) for speed on mobile
                val randomHit = response.hits[Random.nextInt(response.hits.size)]
                randomHit.webformatURL
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching image for query: $query", e)
            null
        }
    }
}
