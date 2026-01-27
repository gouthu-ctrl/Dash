package com.dash.travel

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Currency
import java.util.Locale

@Serializable
data class IpApiResponse(
    @SerialName("country_code") val countryCode: String? = null,
    val currency: String? = null
)

@Serializable
data class ProfileUpdate(
    val home_country_code: String?,
    val base_currency: String?,
    val last_seen: String?
)

private val jsonParser = Json { ignoreUnknownKeys = true }

suspend fun updateProfile(supabase: SupabaseClient) {
    try {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) return

        val userId = session.user?.id ?: return

        // Fetch location info from an HTTPS API
        val client = HttpClient()
        var countryCode: String? = null
        var currency: String? = null

        try {
            val responseText = client.get("https://ipapi.co/json/").bodyAsText()
            val ipData = jsonParser.decodeFromString<IpApiResponse>(responseText)
            countryCode = ipData.countryCode
            currency = ipData.currency
        } catch (e: Exception) {
            Log.w("ProfileUpdate", "Failed to fetch IP location: ${e.message}")
        }

        // Fallback to Locale
        if (countryCode == null) {
            countryCode = Locale.getDefault().country
        }
        if (currency == null) {
            currency = try {
                Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: Exception) {
                "USD"
            }
        }

        // Generate timestamp compatible with older Android versions
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val timestamp = dateFormat.format(java.util.Date())

        val updateData = ProfileUpdate(
            home_country_code = countryCode,
            base_currency = currency,
            last_seen = timestamp
        )

        supabase.from("profiles").update(updateData) {
            filter {
                eq("id", userId)
            }
        }
        Log.d("ProfileUpdate", "Profile updated successfully")

    } catch (e: Exception) {
        Log.e("ProfileUpdate", "Error updating profile: ${e.message}")
    }
}
