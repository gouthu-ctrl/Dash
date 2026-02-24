package com.dash.travel.data.repository

import com.dash.travel.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.*
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
// Removed explicit presence imports to rely on wildcard if possible
import io.github.jan.supabase.auth.auth

/**
 * Repository for Trip operations with Supabase
 */
class TripRepository(private val supabase: SupabaseClient) {

    // ==================== TRIPS ====================

    /**
     * Get all trips for current user (owned or member)
     */
    suspend fun getTrips(): List<SupabaseTrip> {
        return supabase.from("trips")
            .select {
                filter {
                    filter("deleted_at", FilterOperator.IS, null)
                }
            }
            .decodeList()
    }

    /**
     * Get trips ordered by display_order
     */
    suspend fun getTripsWithMembership(): List<TripWithMembership> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        
        return supabase.from("trip_members")
            .select(Columns.raw("status, trips!inner(*)")) {
                filter {
                    eq("user_id", userId)
                    filter("trips.deleted_at", FilterOperator.IS, null)
                }
            }
            .decodeList<TripMemberWithTrip>()
            .map { TripWithMembership(it.trip, it.status) }
    }

    /**
     * Get trips created by the current user (failsafe for missing membership rows)
     */
    suspend fun getOwnedTrips(): List<SupabaseTrip> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.from("trips")
            .select {
                filter {
                    eq("created_by", userId)
                    filter("deleted_at", FilterOperator.IS, null)
                }
            }
            .decodeList()
    }

    /**
     * Get trips ordered by display_order
     */
    suspend fun getTripsOrdered(): List<SupabaseTrip> {
        return supabase.from("trips")
            .select {
                filter {
                    filter("deleted_at", FilterOperator.IS, null)
                }
                order("display_order", Order.ASCENDING)
            }
            .decodeList()
    }

    /**
     * Get single trip by ID
     */
    suspend fun getTripById(tripId: String): SupabaseTrip? {
        return supabase.from("trips")
            .select {
                filter { 
                    eq("id", tripId) 
                    filter("deleted_at", FilterOperator.IS, null)
                }
            }
            .decodeSingleOrNull()
    }

    /**
     * Create new trip
     */
    suspend fun createTrip(trip: NewTripPayload): SupabaseTrip {
        return supabase.from("trips")
            .insert(trip) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update existing trip
     */
    suspend fun updateTrip(tripId: String, updates: kotlinx.serialization.json.JsonObject): SupabaseTrip {
        return supabase.from("trips")
            .update(updates) {
                filter { eq("id", tripId) }
                select()
            }
            .decodeSingle()
    }

    /**
     * Soft delete trip - sets deleted_at timestamp instead of removing the row
     */
    suspend fun deleteTrip(tripId: String) {
        supabase.from("trips")
            .update(mapOf("deleted_at" to java.time.Instant.now().toString())) {
                filter { eq("id", tripId) }
            }
    }
    
    /**
     * Upsert trip (insert or update)
     * Used by SyncRepository for offline sync
     */
    suspend fun upsertTrip(trip: SupabaseTrip): SupabaseTrip {
        return supabase.from("trips")
            .upsert(trip) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update trip display order (for drag-drop reordering)
     */
    suspend fun updateTripOrder(tripId: String, newOrder: Int) {
        supabase.from("trips")
            .update(mapOf("display_order" to newOrder)) {
                filter { eq("id", tripId) }
            }
    }

    /**
     * Get public templates
     */
    suspend fun getPublicTemplates(): List<SupabaseTrip> {
        return supabase.from("trips")
            .select {
                filter {
                    eq("is_template", true)
                    eq("is_public", true)
                }
            }
            .decodeList()
    }

    /**
     * Get trip by share token
     */
    suspend fun getTripByShareToken(token: String): SupabaseTrip? {
        return supabase.from("trips")
            .select {
                filter { eq("share_token", token) }
            }
            .decodeSingleOrNull()
    }

    // ==================== TRIP MEMBERS ====================

    /**
     * Get members for a trip
     */
    suspend fun getTripMembers(tripId: String): List<TripMember> {
        return supabase.from("trip_members")
            .select {
                filter { eq("trip_id", tripId) }
            }
            .decodeList()
    }

    /**
     * Get members with profile info
     */
    suspend fun getTripMembersWithProfiles(tripId: String): List<TripMemberWithProfile> {
        return supabase.from("trip_members")
            .select(Columns.raw("*, profiles(*)")) {
                filter { eq("trip_id", tripId) }
            }
            .decodeList()
    }

    /**
     * Invite user to trip
     */
    suspend fun inviteMember(tripId: String, email: String, role: TripRole): TripMember {
        val member = TripMember(
            tripId = tripId,
            invitedEmail = email,
            role = role,
            status = MemberStatus.PENDING
        )
        // Omit id from insert to let Supabase generate it
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val memberJson = json.encodeToJsonElement(TripMember.serializer(), member) as kotlinx.serialization.json.JsonObject
        val insertMap = memberJson.filter { it.key != "id" }

        return supabase.from("trip_members")
            .insert(insertMap) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Link any pending invitations for an email to a verified user profile
     * (Initial link without accepting yet)
     */
    suspend fun linkInvitedMember(email: String, userId: String) {
        supabase.from("trip_members")
            .update(mapOf(
                "user_id" to userId
            )) {
                filter {
                    eq("invited_email", email)
                    eq("status", "pending")
                }
            }
    }

    /**
     * Explicitly accept a trip invitation
     */
    /**
     * Explicitly accept a trip invitation
     */
    suspend fun acceptTripInvitation(tripId: String, userId: String, email: String) {
        // We also set user_id here to ensure linking happens if it hasn't already
        supabase.from("trip_members")
            .update(mapOf(
                "status" to "accepted",
                "user_id" to userId
            )) {
                filter {
                    eq("trip_id", tripId)
                    or {
                        eq("user_id", userId)
                        eq("invited_email", email)
                    }
                }
            }
    }

    /**
     * Explicitly reject a trip invitation
     */
    /**
     * Explicitly reject a trip invitation
     * Tries to match by user_id OR invited_email to ensure it works even if linking lagged
     */
    suspend fun rejectTripInvitation(tripId: String, userId: String, email: String) {
        supabase.from("trip_members")
            .update(mapOf("status" to "declined")) {
                filter {
                    eq("trip_id", tripId)
                    or {
                        eq("user_id", userId)
                        eq("invited_email", email)
                    }
                }
            }
    }

    /**
     * Update member role
     */
    suspend fun updateMemberRole(memberId: String, newRole: TripRole) {
        supabase.from("trip_members")
            .update(mapOf("role" to newRole.name.lowercase())) {
                filter { eq("id", memberId) }
            }
    }

    /**
     * Accept/decline invitation
     */
    suspend fun updateMemberStatus(memberId: String, status: MemberStatus) {
        supabase.from("trip_members")
            .update(mapOf("status" to status.name.lowercase())) {
                filter { eq("id", memberId) }
            }
    }

    /**
     * Remove member from trip
     */
    suspend fun removeMember(memberId: String) {
        supabase.from("trip_members")
            .delete {
                filter { eq("id", memberId) }
            }
    }

    // ==================== ITINERARY ITEMS ====================

    /**
     * Get single itinerary item
     */
    suspend fun getItineraryItem(itemId: String): ItineraryItem {
        return supabase.from("itinerary_items")
            .select(Columns.raw("*, attachments:itinerary_attachments(*)")) {
                filter { eq("id", itemId) }
            }
            .decodeSingle()
    }

    /**
     * Get all itinerary items for a trip
     */
    suspend fun getItineraryItems(tripId: String): List<ItineraryItem> {
        return supabase.from("itinerary_items")
            .select(Columns.raw("*, attachments:itinerary_attachments(*)")) {
                filter { eq("trip_id", tripId) }
                order("sorting_index", Order.ASCENDING)
            }
            .decodeList()
    }

    /**
     * Get items by status (e.g., proposed for voting)
     */
    suspend fun getItemsByStatus(tripId: String, status: ItineraryStatus): List<ItineraryItem> {
        return supabase.from("itinerary_items")
            .select {
                filter {
                    eq("trip_id", tripId)
                    eq("status", status.name.lowercase())
                }
                order("sorting_index", Order.ASCENDING)
            }
            .decodeList()
    }

    /**
     * Get items for a specific day
     */
    suspend fun getItemsForDay(tripId: String, date: String): List<ItineraryItem> {
        return supabase.from("itinerary_items")
            .select {
                filter {
                    eq("trip_id", tripId)
                    gte("start_time", "${date}T00:00:00")
                    lt("start_time", "${date}T23:59:59")
                }
                order("start_time", Order.ASCENDING)
            }
            .decodeList()
    }

    /**
     * Create itinerary item
     */
    suspend fun createItineraryItem(item: ItineraryItem): ItineraryItem {
        // Exclude virtual fields and server-generated fields from the insert payload
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val itemJson = json.encodeToJsonElement(ItineraryItem.serializer(), item) as kotlinx.serialization.json.JsonObject
        // Filter out: attachments (virtual), created_at (server-generated)
        // We ALLOW id to be passed if generated on client
        val insertMap = itemJson.filter { entry -> 
            entry.key != "attachments" && 
            entry.key != "created_at" &&
            !(entry.key == "id" && entry.value.toString() == "null")
        }
        
        return supabase.from("itinerary_items")
            .insert(insertMap) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update itinerary item
     */
    suspend fun updateItineraryItem(itemId: String, updates: kotlinx.serialization.json.JsonObject) {
        supabase.from("itinerary_items")
            .update(updates) {
                filter { eq("id", itemId) }
            }
    }

    suspend fun updateItineraryItem(item: ItineraryItem) {
        // Ensure ID is present
        val id = item.id ?: throw IllegalArgumentException("Item ID cannot be null for update")
        
        // Exclude virtual fields like 'attachments' from the update payload
        val json = kotlinx.serialization.json.Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val itemJson = json.encodeToJsonElement(ItineraryItem.serializer(), item) as kotlinx.serialization.json.JsonObject
        val updateMap = itemJson.filter { it.key != "attachments" && it.key != "id" }
        
        supabase.from("itinerary_items")
            .update(updateMap) {
                filter { eq("id", id) }
            }
    }

    /**
     * Update item status (for voting workflow)
     */
    suspend fun updateItemStatus(itemId: String, status: ItineraryStatus) {
        supabase.from("itinerary_items")
            .update(mapOf("status" to status.name.lowercase())) {
                filter { eq("id", itemId) }
            }
    }

    /**
     * Update sorting index (for drag-drop)
     */
    suspend fun updateItemSortingIndex(itemId: String, newIndex: Double) {
        supabase.from("itinerary_items")
            .update(mapOf("sorting_index" to newIndex)) {
                filter { eq("id", itemId) }
            }
    }

    /**
     * Delete itinerary item
     */
    suspend fun deleteItineraryItem(itemId: String) {
        supabase.from("itinerary_items")
            .delete {
                filter { eq("id", itemId) }
            }
    }
    
    /**
     * Alias for SyncRepository compatibility
     */
    suspend fun deleteItem(itemId: String) = deleteItineraryItem(itemId)
    
    /**
     * Upsert itinerary item (insert or update)
     * Used by SyncRepository for offline sync
     */
    suspend fun upsertItem(item: ItineraryItem): ItineraryItem {
        return supabase.from("itinerary_items")
            .upsert(item) {
                select()
            }
            .decodeSingle()
    }

    // ==================== VOTING ====================

    /**
     * Get votes for an itinerary item
     */
    suspend fun getVotes(itemId: String): List<ItineraryVote> {
        return supabase.from("itinerary_votes")
            .select {
                filter { eq("itinerary_item_id", itemId) }
            }
            .decodeList()
    }

    /**
     * Get ALL votes for a specific trip (more efficient than item-by-item)
     */
    suspend fun getVotesForTrip(tripId: String): List<ItineraryVote> {
        // Use inner join to filter votes by the trip_id of their parent item
        return supabase.from("itinerary_votes")
            .select(Columns.raw("*, itinerary_items!inner(trip_id)")) {
                filter {
                    eq("itinerary_items.trip_id", tripId)
                }
            }
            .decodeList()
    }

    /**
     * Get all votes for a list of itinerary items
     */
    suspend fun getVotesForItems(itemIds: List<String>): List<ItineraryVote> {
        if (itemIds.isEmpty()) return emptyList()
        
        return supabase.from("itinerary_votes")
            .select() {
                filter {
                    isIn("itinerary_item_id", itemIds)
                }
            }
            .decodeList()
    }



    /**
     * Cast a vote
     */
    suspend fun castVote(itemId: String, userId: String, voteType: String): ItineraryVote {
        val vote = ItineraryVote(
            itineraryItemId = itemId,
            userId = userId,
            voteType = voteType
        )
        
        // Use RPC for reliable atomic upsert
        return supabase.postgrest.rpc(
            "cast_vote",
            mapOf(
                "_item_id" to itemId,
                "_vote_type" to voteType
            )
        ).decodeSingle()
    }

    /**
     * Delete a vote (Revoke)
     */
    suspend fun deleteVote(voteId: String) {
         supabase.from("itinerary_votes")
            .delete {
                filter { eq("id", voteId) }
            }
    }
    
    /**
     * Delete vote by item and user (if we don't have voteId handy)
     * Note: Requires RLS to allow user to delete their own vote
     */
    suspend fun deleteMyVote(itemId: String) {
        // We rely on RLS 'delete using (user_id = auth.uid())'
        supabase.from("itinerary_votes")
            .delete {
                filter { eq("itinerary_item_id", itemId) }
            }
    }

    // ==================== ATTACHMENTS ====================

    /**
     * Link an attachment to an itinerary item
     */
    suspend fun addItineraryAttachment(attachment: ItineraryAttachment): ItineraryAttachment {
        return supabase.from("itinerary_attachments")
            .insert(attachment) {
                select()
            }
            .decodeSingle()
    }

    // ==================== REALTIME ====================

    /**
     * Subscribe to trip changes
     * NOTE: Filtering client-side by tripId as SDK filter API varies by version
     */
    fun subscribeToTripChanges(tripId: String): Flow<PostgresAction> {
        return callbackFlow {
            val channel = supabase.channel("trip-$tripId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "trips"
            }
            
            channel.subscribe()
            
            try {
                flow.collect { action ->
                    trySend(action)
                }
            } finally {
                channel.unsubscribe()
            }
            
            awaitClose {
                runBlocking { channel.unsubscribe() }
            }
        }
    }

    /**
     * Subscribe to itinerary item changes
     * NOTE: Filtering client-side by tripId  
     */
    fun subscribeToItineraryChanges(tripId: String): Flow<PostgresAction> {
        return callbackFlow {
            val channel = supabase.channel("itinerary-$tripId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "itinerary_items"
            }
            
            // Subscribe the channel
            channel.subscribe()
            
            // Collect and emit
            try {
                flow.collect { action ->
                    trySend(action)
                }
            } finally {
                channel.unsubscribe()
            }
            
            awaitClose {
                runBlocking { channel.unsubscribe() }
            }
        }
    }
    
    /**
     * Subscribe to vote changes
     */
    fun subscribeToVoteChanges(tripId: String): Flow<PostgresAction> {
        return callbackFlow {
            // We listen to all votes and filter in business logic or listen to specific channel
            val channel = supabase.channel("votes-$tripId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "itinerary_votes"
            }
            
            channel.subscribe()
            
            try {
                flow.collect { action ->
                    trySend(action)
                }
            } finally {
                channel.unsubscribe()
            }
            
            awaitClose {
                runBlocking { channel.unsubscribe() }
            }
        }
    }

    // ==================== PRESENCE ====================
    
    /**
     * Subscribe to presence changes and track current user
     */
    fun subscribeToPresence(tripId: String, user: PresenceUser): Flow<List<PresenceUser>> {
        // Presence temporarily disabled due to library import issues
        return kotlinx.coroutines.flow.flowOf(emptyList())
        /*
        return callbackFlow {
            // Use a specific topic for presence to avoid collisions with db changes if needed, 
            // though reusing 'trip-presence-$tripId' is fine.
            val channel = supabase.channel("presence-$tripId")
            
            // Get the flow BEFORE subscribing
            // val flow = channel.presenceDataFlow<PresenceUser>()
            
            channel.subscribe()
            
            // Track the user after subscribing
            launch {
                try {
                    // Randomize color if needed or rely on user's color
                    // channel.presence.track(user)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            try {
                // flow.collect { users ->
                    // Distinct users by ID to avoid duplicates if multiple tabs/devices for same user
                    // trySend(users.distinctBy { it.userId }) 
                // }
            } finally {
                channel.unsubscribe()
            }
            
            awaitClose {
                runBlocking { channel.unsubscribe() }
            }
        }
        */
    }
}

/**
 * Trip member with embedded profile data
 */
@kotlinx.serialization.Serializable
data class TripMemberWithProfile(
    val id: String,
    @kotlinx.serialization.SerialName("trip_id") val tripId: String,
    @kotlinx.serialization.SerialName("user_id") val userId: String?,
    @kotlinx.serialization.SerialName("invited_email") val invitedEmail: String?,
    val role: TripRole,
    val status: MemberStatus,
    val profiles: Profile?
)
