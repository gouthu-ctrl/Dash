package com.dash.travel.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PresenceUser(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val color: String = "#FFD700" // Default gold, can be randomized
)
