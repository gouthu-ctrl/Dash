package com.dash.travel.data.model

import kotlinx.serialization.json.JsonObject 
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Trip role enum matching Supabase
 */
@Serializable
enum class TripRole {
    @SerialName("owner") OWNER,
    @SerialName("editor") EDITOR,
    @SerialName("viewer") VIEWER
}

/**
 * Itinerary status enum matching Supabase
 */
@Serializable
enum class ItineraryStatus {
    @SerialName("draft") DRAFT,
    @SerialName("confirmed") CONFIRMED,
    @SerialName("proposed") PROPOSED,
    @SerialName("archived") ARCHIVED
}

/**
 * Document category enum matching Supabase
 */
@Serializable
enum class DocCategory {
    @SerialName("passport") PASSPORT,
    @SerialName("visa") VISA,
    @SerialName("ticket") TICKET,
    @SerialName("insurance") INSURANCE,
    @SerialName("other") OTHER
}

/**
 * Member status enum matching Supabase
 */
@Serializable
enum class MemberStatus {
    @SerialName("pending") PENDING,
    @SerialName("accepted") ACCEPTED,
    @SerialName("declined") DECLINED
}

/**
 * User profile matching Supabase profiles table
 */
@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("theme_preference") val themePreference: String? = "system",
    @SerialName("language_preference") val languagePreference: String? = "en",
    @SerialName("birth_date") val birthDate: String? = null,
    val gender: String? = null,
    @SerialName("home_country_code") val homeCountryCode: String? = null,
    @SerialName("base_currency") val baseCurrency: String = "USD",
    @SerialName("last_seen") val lastSeen: String? = null,
    val preferences: ProfilePreferences? = null,
    @SerialName("calendar_sync_settings") val calendarSyncSettings: CalendarSyncSettings? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("subscription_tier") val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
)

@Serializable
enum class SubscriptionTier {
    @SerialName("free") FREE,
    @SerialName("pro") PRO
}

@Serializable
data class ProfilePreferences(
    val pace: String = "medium",
    val interests: List<String> = emptyList(),
    val dietary: List<String> = emptyList()
)

@Serializable
data class CalendarSyncSettings(
    @SerialName("google_sync") val googleSync: Boolean = false,
    @SerialName("device_sync") val deviceSync: Boolean = false
)

/**
 * Trip matching Supabase trips table
 */
@Serializable
data class SupabaseTrip(
    val id: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val timezone: String = "UTC",
    @SerialName("destination_data") val destinationData: DestinationData? = null,
    @SerialName("origin_data") val originData: OriginData? = null,
    @SerialName("is_template") val isTemplate: Boolean = false,
    @SerialName("is_public") val isPublic: Boolean = false,
    val visibility: String? = "private", // private, friends, public (matches trip_visibility enum)
    @SerialName("template_price") val templatePrice: Double = 0.0,
    @SerialName("template_tags") val templateTags: List<String>? = null,
    @SerialName("share_token") val shareToken: String? = null,
    @SerialName("widgets_config") val widgetsConfig: List<WidgetConfig>? = null,
    @SerialName("trip_image_url") val tripImageUrl: String? = null,
    @SerialName("custom_attributes") val customAttributes: CustomAttributes? = null,
    @SerialName("budget_limit") val budgetLimit: Double? = null,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DestinationData(
    val name: String? = null,
    val country: String? = null,
    @SerialName("place_id") val placeId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

@Serializable
data class OriginData(
    val name: String? = null,
    val country: String? = null,
    @SerialName("place_id") val placeId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

@Serializable
data class CustomAttributes(
    val notes: String? = null,
    @SerialName("custom_fields") val customFields: List<CustomField>? = null
)

@Serializable
data class CustomField(
    val label: String,
    val value: String
)

@Serializable
data class WidgetConfig(
    val type: String,
    val size: String,
    val enabled: Boolean = true
)

/**
 * Trip member matching Supabase trip_members table
 */
@Serializable
data class TripMember(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("invited_email") val invitedEmail: String? = null,
    val role: TripRole = TripRole.VIEWER,
    val status: MemberStatus = MemberStatus.PENDING,
    @SerialName("member_origin_data") val memberOriginData: OriginData? = null
)

/**
 * Itinerary item matching Supabase itinerary_items table
 */
@Serializable
data class ItineraryItem(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    val type: String,
    val status: ItineraryStatus = ItineraryStatus.CONFIRMED,
    val title: String,
    val description: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_lat") val locationLat: Double? = null,
    @SerialName("location_lng") val locationLng: Double? = null,
    @SerialName("location_google_place_id") val locationGooglePlaceId: String? = null,
    @SerialName("estimated_cost") val estimatedCost: Double = 0.0,
    val currency: String = "USD",
    @SerialName("booking_ref") val bookingRef: String? = null,
    @SerialName("provider_details") val providerDetails: JsonObject? = null,
    @SerialName("sorting_index") val sortingIndex: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val attachments: List<ItineraryAttachment> = emptyList()
)

@Serializable
data class ItineraryAttachment(
    val id: String,
    @SerialName("itinerary_id") val itineraryId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_type") val fileType: String? = null,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("file_size") val fileSize: Int? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("is_shared") val isShared: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProviderDetails(
    val name: String? = null,
    val url: String? = null,
    val phone: String? = null,
    @SerialName("confirmation_number") val confirmationNumber: String? = null
)

/**
 * Document vault item matching Supabase document_vault table
 */
@Serializable
data class DocumentVaultItem(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("file_name") val fileName: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("doc_type") val docType: DocCategory = DocCategory.OTHER,
    @SerialName("is_encrypted") val isEncrypted: Boolean = true,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Price alert matching Supabase price_alerts table
 */
@Serializable
data class SupabasePriceAlert(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("search_params") val searchParams: PriceAlertSearchParams,
    @SerialName("target_price") val targetPrice: Double? = null,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PriceAlertSearchParams(
    val type: String, // "flight", "hotel", "activity"
    val title: String,
    val description: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val dates: AlertDateRange? = null,
    @SerialName("booking_url") val bookingUrl: String? = null
)

@Serializable
data class AlertDateRange(
    val start: String? = null,
    val end: String? = null
)

/**
 * AI Conversation matching Supabase ai_conversations table
 */
@Serializable
data class AIConversation(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val messages: List<AIMessage> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class AIMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: String? = null
)

/**
 * Expense matching Supabase expenses table
 */
@Serializable
data class Expense(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("paid_by") val paidBy: String? = null,
    val amount: Double,
    val currency: String = "USD",
    val description: String? = null,
    val category: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Expense split matching Supabase expense_splits table
 */
@Serializable
data class ExpenseSplit(
    val id: String? = null,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double? = null,
    @SerialName("is_settled") val isSettled: Boolean = false
)

/**
 * New trip creation payload
 */
@Serializable
data class NewTripPayload(
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val timezone: String = "UTC",
    @SerialName("origin_data") val originData: OriginData? = null,
    @SerialName("destination_data") val destinationData: DestinationData? = null,
    @SerialName("trip_image_url") val tripImageUrl: String? = null,
    @SerialName("custom_attributes") val customAttributes: CustomAttributes? = null,
    @SerialName("budget_limit") val budgetLimit: Double? = null,
    @SerialName("created_by") val createdBy: String? = null
)

/**
 * Itinerary vote matching Supabase itinerary_votes table
 */
@Serializable
data class ItineraryVote(
    val id: String? = null,
    @SerialName("itinerary_item_id") val itineraryItemId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("vote_type") val voteType: String, // 'up', 'down', 'heart', 'like'
    val weight: Int = 1,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Trip with membership status info
 */
@Serializable
data class TripWithMembership(
    val trip: SupabaseTrip,
    val status: MemberStatus = MemberStatus.ACCEPTED
)

@Serializable
internal data class TripMemberWithTrip(
    val status: MemberStatus,
    val trips: SupabaseTrip
) {
    val trip: SupabaseTrip get() = trips
}

