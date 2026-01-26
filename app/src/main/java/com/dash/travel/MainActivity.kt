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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.dash.travel.data.local.DashDatabase
import com.dash.travel.data.local.entity.ItineraryItemEntity
import com.dash.travel.data.local.entity.TripEntity
import com.dash.travel.data.models.ItineraryItem
import com.dash.travel.data.models.Trip
import com.dash.travel.data.remote.SupabaseManager
import com.dash.travel.ui.screens.*
import com.dash.travel.ui.theme.DashTheme
import com.dash.travel.ui.viewmodel.HomeViewModel
import com.dash.travel.ui.viewmodel.TripDetailViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.UUID

@Serializable
data class NewTrip(
    val title: String,
    val description: String?,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("destination_data") val destinationData: JsonObject,
    @SerialName("origin_data") val originData: JsonObject, // Added for edit screen
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
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class ItineraryItemSummary(
    @SerialName("trip_id") val tripId: String
)

class MainActivity : ComponentActivity() {
    private lateinit var db: DashDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = Room.databaseBuilder(
            applicationContext,
            DashDatabase::class.java, "dash-db"
        ).fallbackToDestructiveMigration().build()

        setContent {
            DashTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val context = this
                val credentialManager = remember { CredentialManager.create(context) }
                
                var userName by remember { mutableStateOf("Traveler") }
                val trips = remember { mutableStateListOf<Trip>() }

                suspend fun fetchProfile() {
                    try {
                        val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return
                        val profile = SupabaseManager.client.postgrest.from("profiles").select { filter { eq("id", userId) } }.decodeSingleOrNull<Profile>()
                        profile?.fullName?.let { userName = it.split(" ").firstOrNull() ?: it }
                    } catch (e: Exception) { Log.e("DashData", "Profile fetch failed", e) }
                }

                suspend fun fetchTrips() {
                    try {
                        val session = SupabaseManager.client.auth.currentSessionOrNull()
                        if (session != null) {
                            val fetchedTrips = try {
                                SupabaseManager.client.postgrest.from("trips").select {
                                    order("display_order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                                }.decodeList<Trip>()
                            } catch (e: Exception) {
                                Log.w("DashData", "Trips fetch with order failed, trying without order", e)
                                SupabaseManager.client.postgrest.from("trips").select().decodeList<Trip>()
                            }
                            
                            val entities = fetchedTrips.mapIndexed { index, trip ->
                                TripEntity(
                                    id = trip.id,
                                    title = trip.title,
                                    description = trip.description,
                                    startDate = trip.startDate,
                                    endDate = trip.endDate,
                                    tripImageUrl = trip.tripImageUrl,
                                    displayOrder = trip.displayOrder ?: index
                                )
                            }
                            db.tripDao().insertTrips(entities)
                            
                            // Re-fetch trip data from Room to ensure local state uses stable IDs/data
                            // NOTE: Full Trip objects are not being stored in Room. We rely on Supabase fetch + Room ordering.
                            // We need to re-fetch *all* trip data to match the UI state logic.
                            // For simplicity, we stick to updating the mutableStateListOf with the Supabase results.
                            trips.clear()
                            trips.addAll(fetchedTrips)
                        }
                    } catch (e: Exception) { Log.e("DashData", "Trips fetch failed", e) }
                }

                suspend fun fetchItinerary(tripId: String) {
                    try {
                        val items = try {
                            SupabaseManager.client.postgrest.from("itinerary_items").select { 
                                filter { eq("trip_id", tripId) } 
                                order("sorting_index", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                            }.decodeList<ItineraryItem>()
                        } catch (e: Exception) {
                            SupabaseManager.client.postgrest.from("itinerary_items").select { 
                                filter { eq("trip_id", tripId) } 
                            }.decodeList<ItineraryItem>()
                        }
                        
                        val entities = items.mapIndexed { index, item ->
                            ItineraryItemEntity(
                                id = item.id!!,
                                tripId = item.tripId,
                                type = item.type,
                                title = item.title,
                                startTime = item.startTime,
                                locationName = item.locationName,
                                bookingRef = item.bookingRef,
                                displayOrder = index
                            )
                        }
                        db.itineraryDao().insertItems(entities)
                    } catch (e: Exception) { Log.e("DashData", "Itinerary fetch failed", e) }
                }

                suspend fun fetchTripMembers(tripId: String): List<TripMember> {
                    return try {
                        val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id // Get current user ID
                        
                        val result = SupabaseManager.client.postgrest.from("trip_members").select {
                            filter { eq("trip_id", tripId) }
                            // Request full_name and avatar_url from the linked profiles table
                            select(Columns.list("user_id", "role", "status", "profiles(full_name, avatar_url)"))
                        }
                        
                        val rawBody = result.data 
                        val json = Json { ignoreUnknownKeys = true }
                        val response = json.parseToJsonElement(rawBody).jsonArray
                        
                        response.map { jsonElement ->
                            val obj = jsonElement.jsonObject
                            val profilesObj = obj["profiles"]?.jsonObject
                            val userId = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
                            
                            // Use full_name, fall back to a derived name, or 'You' if it's the current user
                            val rawName = profilesObj?.get("full_name")?.jsonPrimitive?.contentOrNull
                            val profileAvatarUrl = profilesObj?.get("avatar_url")?.jsonPrimitive?.contentOrNull

                            val memberName = when {
                                rawName.isNullOrBlank() -> if (userId == currentUserId) "You" else "Dash User"
                                else -> rawName
                            }
                            
                            TripMember(
                                id = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString(),
                                name = memberName,
                                avatarUrl = profileAvatarUrl,
                                role = obj["role"]?.jsonPrimitive?.contentOrNull ?: "viewer",
                                status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "pending"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("DashData", "Failed to fetch trip members: $e")
                        emptyList()
                    }
                }

                LaunchedEffect(Unit) {
                    val session = SupabaseManager.client.auth.currentSessionOrNull()
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
                                        
                                        SupabaseManager.client.auth.signInWith(IDToken) {
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
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return HomeViewModel(db.tripDao()) as T
                                }
                            }
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
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
                                        val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch
                                        val destinationData = buildJsonObject {
                                            put("name", destination)
                                        }
                                        val originData = buildJsonObject {
                                            put("name", "San Francisco") // Placeholder for now
                                        }
                                        val encodedDest = URLEncoder.encode(destination, "UTF-8")
                                        val imageUrl = "https://loremflickr.com/1280/720/$encodedDest,landmark,cityscape,famous/all?random=${System.currentTimeMillis()}"
                                        
                                        val newTrip = NewTrip(
                                            title = tripName,
                                            description = description,
                                            startDate = startDate,
                                            endDate = endDate,
                                            createdBy = userId,
                                            destinationData = destinationData,
                                            originData = originData,
                                            tripImageUrl = imageUrl
                                        )
                                        SupabaseManager.client.postgrest.from("trips").insert(newTrip)
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
                            isFirstTrip = false
                        )
                    }
                    composable("trip_detail/{tripId}", arguments = listOf(navArgument("tripId") { type = NavType.StringType })) { backStackEntry ->
                        val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                        LaunchedEffect(tripId) { fetchItinerary(tripId) }
                        
                        val tripDetailViewModel: TripDetailViewModel = viewModel(
                            key = tripId,
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return TripDetailViewModel(tripId, db.itineraryDao()) as T
                                }
                            }
                        )

                        TripDetailScreen(
                            tripId = tripId, 
                            trips = trips, 
                            viewModel = tripDetailViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onAddItem = { navController.navigate("add_itinerary/$tripId") },
                            onRefreshImage = { title ->
                                coroutineScope.launch {
                                    try {
                                        val trip = trips.find { it.id == tripId }
                                        val destinationName = trip?.destinationData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: title
                                        val encoded = URLEncoder.encode(destinationName, "UTF-8")
                                        val newUrl = "https://loremflickr.com/1280/720/$encoded,landmark,cityscape,famous/all?random=${System.currentTimeMillis()}"
                                        SupabaseManager.client.postgrest.from("trips").update(buildJsonObject { put("trip_image_url", newUrl) }) { 
                                            filter { eq("id", tripId) } 
                                        }
                                        fetchTrips()
                                        Toast.makeText(context, "Theme updated!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("RefreshImage", "Failed", e)
                                        Toast.makeText(context, "Failed to update theme", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEditTrip = { title, description, startDate, endDate, destination, origin ->
                                coroutineScope.launch {
                                    try {
                                        val destinationData = buildJsonObject { put("name", destination) }
                                        val originData = buildJsonObject { put("name", origin) }
                                        val updatedTripMap = buildJsonObject {
                                            put("title", title)
                                            put("description", description)
                                            put("start_date", startDate)
                                            put("end_date", endDate)
                                            put("destination_data", destinationData)
                                            put("origin_data", originData) // Update origin
                                        }
                                        SupabaseManager.client.postgrest.from("trips").update(updatedTripMap) {
                                            filter { eq("id", tripId) }
                                        }
                                        
                                        fetchTrips()
                                        Toast.makeText(context, "Trip updated!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("EditTrip", "Failed", e)
                                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            fetchTripMembers = { id -> fetchTripMembers(id) } // Pass the real fetch function
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
                                        val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch
                                        val insertedItem = SupabaseManager.client.postgrest.from("itinerary_items")
                                            .insert(item) { select() }.decodeSingle<ItineraryItem>()
                                        
                                        if (uri != null) {
                                            val fileName = "${UUID.randomUUID()}_${uri.lastPathSegment}"
                                            val storagePath = "$tripId/$fileName"
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                                            
                                            // 1. Upload to Supabase Storage
                                            SupabaseManager.client.storage.from("itinerary-files").upload(storagePath, bytes)
                                            
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
                                            SupabaseManager.client.postgrest.from("itinerary_attachments").insert(attachment)
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