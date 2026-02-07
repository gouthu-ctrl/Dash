package com.dash.travel.data.repository

import com.dash.travel.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Repository for User Profile operations with Supabase
 */
class ProfileRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "profiles"
    }

    /**
     * Get profile by ID
     */
    suspend fun getProfile(userId: String): Profile? {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull()
    }

    /**
     * Get current user's profile (convenience)
     * Assumes auth is handled and we pass the ID, or we could use auth.currentSession.user?.id if we had auth repo access
     */
    suspend fun getCurrentProfile(userId: String): Profile? {
        return getProfile(userId)
    }

    /**
     * Create or update profile
     */
    suspend fun upsertProfile(profile: Profile): Profile {
        return supabase.from(TABLE_NAME)
            .upsert(profile) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update specific fields of a profile
     */
    suspend fun updateProfile(userId: String, updates: Map<String, Any?>): Profile {
        return supabase.from(TABLE_NAME)
            .update(updates) {
                filter { eq("id", userId) }
                select()
            }
            .decodeSingle()
    }

    /**
     * Search profiles by username or name (for inviting friends)
     */
    suspend fun searchProfiles(query: String): List<Profile> {
        return supabase.from(TABLE_NAME)
            .select {
                filter {
                    or {
                        ilike("username", "%$query%")
                        ilike("full_name", "%$query%")
                    }
                }
                limit(10)
            }
            .decodeList()
    }
    /**
     * Get user stats (Trip Count, Place Count)
     * Calculates client-side as fallback for missing RPC
     */
    suspend fun getUserStats(userId: String): Pair<Int, Int> {
        return try {
            // 1. Count Trips (Status ACCEPTED)
            val trips = supabase.from("trip_members")
                .select(Columns.raw("trip_id")) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<JsonObject>()
            
            val tripCount = trips.size
            val tripIds = trips.mapNotNull { it["trip_id"]?.jsonPrimitive?.contentOrNull }

            if (tripIds.isEmpty()) return Pair(tripCount, 0)

            // 2. Count Places
            val uniquePlaces = mutableSetOf<String>()
            
            // Chunking to be safe
            tripIds.chunked(20).forEach { batchIds ->
                val items = supabase.from("itinerary_items")
                    .select(Columns.raw("location_name")) {
                        filter {
                            isIn("trip_id", batchIds)
                            neq("location_name", "null") 
                        }
                    }
                    .decodeList<JsonObject>()
                
                items.forEach { json ->
                     val loc = json["location_name"]?.jsonPrimitive?.contentOrNull
                     if (!loc.isNullOrBlank()) {
                         uniquePlaces.add(loc)
                     }
                }
            }
            
            Pair(tripCount, uniquePlaces.size)

        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }
}
