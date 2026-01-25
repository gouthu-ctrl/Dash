package com.dash.travel

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dash.travel.data.models.ItineraryItem
import com.dash.travel.data.models.Trip
import com.dash.travel.ui.screens.*
import com.dash.travel.ui.theme.DashTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.UUID

// Data classes for Supabase interaction
@Serializable
data class NewTrip(
    val title: String,
    val description: String?,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("destination_data") val destinationData: JsonObject,
    @SerialName("trip_image_url") val tripImageUrl: String? = null
)

@Serializable
data class TripAttachment(
    @SerialName("itinerary_id") val itineraryId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String?,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("is_shared") val isShared: Boolean
)

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class ItineraryItemSummary(
    @SerialName("trip_id") val tripId: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
            install(Storage)
        }

        setContent {
            DashTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val context = this
                val credentialManager = remember { CredentialManager.create(context) }
                
                val trips = remember { mutableStateListOf<Trip>() }
                val itineraryItems = remember { mutableStateListOf<ItineraryItem>() }
                var userName by remember { mutableStateOf("Traveler") }

                suspend fun fetchProfile() {
                    try {
                        val userId = supabase.auth.currentUserOrNull()?.id ?: return
                        val profile = supabase.postgrest.from("profiles").select { filter { eq("id", userId) } }.decodeSingleOrNull<Profile>()
                        profile?.fullName?.let { userName = it.split(" ").firstOrNull() ?: it }
                    } catch (e: Exception) { Log.e("DashData", "Profile fetch failed", e) }
                }

                suspend fun fetchTrips() {
                    try {
                        val session = supabase.auth.currentSessionOrNull()
                        if (session != null) {
                            val fetchedTrips = supabase.postgrest.from("trips").select().decodeList<Trip>()
                            val itinerarySummary = supabase.postgrest.from("itinerary_items").select(Columns.raw("trip_id")).decodeList<ItineraryItemSummary>()
                            val counts = itinerarySummary.groupBy { it.tripId }.mapValues { it.value.size }
                            fetchedTrips.forEach { trip -> trip.placesCount = counts[trip.id] ?: 0 }
                            trips.clear()
                            trips.addAll(fetchedTrips)
                        }
                    } catch (e: Exception) { Log.e("DashData", "Trips fetch failed", e) }
                }

                suspend fun fetchItinerary(tripId: String) {
                    try {
                        val items = supabase.postgrest.from("itinerary_items").select { filter { eq("trip_id", tripId) } }.decodeList<ItineraryItem>()
                        itineraryItems.clear()
                        itineraryItems.addAll(items)
                    } catch (e: Exception) { Log.e("DashData", "Itinerary fetch failed", e) }
                }

                LaunchedEffect(Unit) {
                    val session = supabase.auth.currentSessionOrNull()
                    if (session != null) {
                        coroutineScope.launch { fetchTrips(); fetchProfile() }
                        navController.navigate("home") { popUpTo("welcome") { inclusive = true } }
                    }
                }

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { 
                        WelcomeScreen(onGoogleLoginClicked = {
                            coroutineScope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(context.getString(R.string.google_client_id))
                                        .setAutoSelectEnabled(true)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential

                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        
                                        supabase.auth.signInWith(IDToken) {
                                            this.idToken = idToken
                                            this.provider = Google
                                        }
                                        
                                        fetchProfile()
                                        fetchTrips()
                                        navController.navigate("home") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Auth", "Google Login Failed", e)
                                    Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    }
                    composable("home") {
                        HomeScreen(
                            trips = trips,
                            userName = userName,
                            onNavigateToTrip = { navController.navigate("trip_detail/$it") },
                            onCreateTrip = { navController.navigate("add_trip") }
                        )
                    }
                    composable("add_trip") {
                        AddTripScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSaveTrip = { tripName, description, startDate, endDate, destination, inviteEmails ->
                                coroutineScope.launch {
                                    try {
                                        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                        val destinationData = buildJsonObject {
                                            put("name", destination)
                                        }
                                        val newTrip = NewTrip(
                                            title = tripName,
                                            description = description,
                                            startDate = startDate,
                                            endDate = endDate,
                                            createdBy = userId,
                                            destinationData = destinationData
                                        )
                                        supabase.postgrest.from("trips").insert(newTrip)
                                        fetchTrips()
                                        navController.popBackStack()
                                        Toast.makeText(context, "Trip created!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("AddTrip", "Failed", e)
                                        Toast.makeText(context, "Failed to create trip", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            getHomeLocation = { "San Francisco" },
                            isFirstTrip = trips.isEmpty()
                        )
                    }
                    composable("trip_detail/{tripId}", arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { backStackEntry ->
                        val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                        LaunchedEffect(tripId) { fetchItinerary(tripId) }
                        TripDetailScreen(
                            tripId = tripId, trips = trips, itineraryItems = itineraryItems,
                            onNavigateBack = { navController.popBackStack() },
                            onAddItem = { navController.navigate("add_itinerary/$tripId") },
                            onRefreshImage = { title ->
                                coroutineScope.launch {
                                    val encoded = URLEncoder.encode(title, "UTF-8")
                                    val newUrl = "https://loremflickr.com/1200/800/$encoded,travel,landscape/all?random=${System.currentTimeMillis()}"
                                    supabase.postgrest.from("trips").update(buildJsonObject { put("trip_image_url", newUrl) }) { filter { eq("id", tripId) } }
                                    fetchTrips()
                                    Toast.makeText(context, "Theme updated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    composable("add_itinerary/{tripId}", arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { backStackEntry ->
                        val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                        AddItineraryScreen(
                            tripId = tripId,
                            onNavigateBack = { navController.popBackStack() },
                            onSaveItem = { item, uri, isShared ->
                                coroutineScope.launch {
                                    try {
                                        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                        val insertedItem = supabase.postgrest.from("itinerary_items")
                                            .insert(item) { select() }.decodeSingle<ItineraryItem>()
                                        
                                        if (uri != null) {
                                            val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment}"
                                            val storagePath = "$tripId/$fileName"
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                                            
                                            // 1. Upload to Supabase Storage
                                            supabase.storage.from("itinerary-files").upload(storagePath, bytes)
                                            
                                            // 2. Save metadata to itinerary_attachments
                                            val attachment = TripAttachment(
                                                itineraryId = insertedItem.id!!,
                                                fileName = uri.lastPathSegment ?: "file",
                                                fileType = context.contentResolver.getType(uri),
                                                storagePath = storagePath,
                                                fileSize = bytes.size.toLong(),
                                                userId = userId,
                                                isShared = isShared
                                            )
                                            supabase.postgrest.from("itinerary_attachments").insert(attachment)
                                        }

                                        fetchItinerary(tripId)
                                        navController.popBackStack()
                                        Toast.makeText(context, "Added to itinerary", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("AddItinerary", "Failed", e)
                                        Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
