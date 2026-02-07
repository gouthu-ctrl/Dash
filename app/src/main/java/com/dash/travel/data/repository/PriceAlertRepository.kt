package com.dash.travel.data.repository

import com.dash.travel.data.model.SupabasePriceAlert
import com.dash.travel.data.model.PriceAlertSearchParams
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * Repository for Price Alert operations with Supabase
 */
class PriceAlertRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "price_alerts"
    }

    /**
     * Get all active price alerts for current user
     */
    suspend fun getActiveAlerts(): List<SupabasePriceAlert> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("is_active", true) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get all price alerts (active and inactive)
     */
    suspend fun getAllAlerts(): List<SupabasePriceAlert> {
        return supabase.from(TABLE_NAME)
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    /**
     * Get alert by ID
     */
    suspend fun getAlertById(alertId: String): SupabasePriceAlert? {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("id", alertId) }
            }
            .decodeSingleOrNull()
    }

    /**
     * Get alerts by type (flight, hotel, activity)
     */
    suspend fun getAlertsByType(type: String): List<SupabasePriceAlert> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { 
                    eq("is_active", true)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<SupabasePriceAlert>()
            .filter { it.searchParams.type == type }
    }

    /**
     * Create new price alert
     */
    suspend fun createAlert(
        searchParams: PriceAlertSearchParams,
        targetPrice: Double?,
        currentPrice: Double?
    ): SupabasePriceAlert {
        val alert = SupabasePriceAlert(
            searchParams = searchParams,
            targetPrice = targetPrice,
            currentPrice = currentPrice,
            isActive = true
        )
        
        return supabase.from(TABLE_NAME)
            .insert(alert) {
                select()
            }
            .decodeSingle()
    }

    /**
     * Update alert target price
     */
    suspend fun updateTargetPrice(alertId: String, targetPrice: Double) {
        supabase.from(TABLE_NAME)
            .update(mapOf("target_price" to targetPrice)) {
                filter { eq("id", alertId) }
            }
    }

    /**
     * Update current price (called by background sync)
     */
    suspend fun updateCurrentPrice(alertId: String, currentPrice: Double) {
        supabase.from(TABLE_NAME)
            .update(mapOf("current_price" to currentPrice)) {
                filter { eq("id", alertId) }
            }
    }

    /**
     * Toggle alert active status
     */
    suspend fun toggleAlertActive(alertId: String, isActive: Boolean) {
        supabase.from(TABLE_NAME)
            .update(mapOf("is_active" to isActive)) {
                filter { eq("id", alertId) }
            }
    }

    /**
     * Delete alert
     */
    suspend fun deleteAlert(alertId: String) {
        supabase.from(TABLE_NAME)
            .delete {
                filter { eq("id", alertId) }
            }
    }

    /**
     * Get alerts that have hit target price
     */
    suspend fun getTriggeredAlerts(): List<SupabasePriceAlert> {
        return supabase.from(TABLE_NAME)
            .select {
                filter { eq("is_active", true) }
            }
            .decodeList<SupabasePriceAlert>()
            .filter { alert ->
                val current = alert.currentPrice
                val target = alert.targetPrice
                current != null && target != null && current <= target
            }
    }
}
