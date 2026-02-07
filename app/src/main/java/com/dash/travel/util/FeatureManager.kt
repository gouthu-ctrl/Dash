package com.dash.travel.util

import com.dash.travel.data.model.Profile
import com.dash.travel.data.model.SubscriptionTier

object FeatureManager {

    fun canAccessChat(profile: Profile?): Boolean {
        return profile?.subscriptionTier == SubscriptionTier.PRO
    }

    fun canAccessOfflineMode(profile: Profile?): Boolean {
        return profile?.subscriptionTier == SubscriptionTier.PRO
    }

    fun canCreateTrip(currentTripCount: Int, profile: Profile?): Boolean {
        if (profile?.subscriptionTier == SubscriptionTier.PRO) return true
        return currentTripCount < 3
    }
    
    fun canAccessFlightAlerts(profile: Profile?): Boolean {
        return profile?.subscriptionTier == SubscriptionTier.PRO
    }
}
