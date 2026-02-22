package com.dash.travel.data.remote

import com.dash.travel.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.engine.android.Android
import kotlinx.serialization.json.Json

import io.github.jan.supabase.annotations.SupabaseInternal

@OptIn(SupabaseInternal::class)
object SupabaseManager {
    
    // Shared JSON configuration for all modules
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        // Set default serializer for all modules
        defaultSerializer = KotlinXSerializer(json)
        
        install(Auth)
        install(Postgrest)
        install(Functions)
        install(Storage)
        install(Realtime) // Required for subscribeToItineraryChanges

        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    /**
     * Shared HttpClient for other repositories (like ImageRepository)
     */
    /**
     * Shared HttpClient for other repositories (like ImageRepository)
     */
    val httpClient = io.ktor.client.HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }
}
