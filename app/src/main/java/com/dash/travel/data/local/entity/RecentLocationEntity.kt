package com.dash.travel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dash.travel.data.models.LocationResult

@Entity(tableName = "recent_locations")
data class RecentLocationEntity(
    @PrimaryKey val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val type: String, // Store enum as String
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toLocationResult(): LocationResult {
        return LocationResult(
            name = name,
            placeId = placeId,
            latitude = latitude,
            longitude = longitude,
            country = country,
            type = try {
                LocationResult.LocationType.valueOf(type)
            } catch (e: Exception) {
                LocationResult.LocationType.UNKNOWN
            }
        )
    }
}

fun LocationResult.toEntity(): RecentLocationEntity {
    return RecentLocationEntity(
        placeId = placeId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        country = country,
        type = type.name,
        timestamp = System.currentTimeMillis()
    )
}
