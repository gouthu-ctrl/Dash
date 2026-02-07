package com.dash.travel.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PhotonFeature(val properties: PhotonProperties)

@Serializable
data class PhotonProperties(
    val name: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null
)

@Serializable
data class PhotonResponse(val features: List<PhotonFeature>)
