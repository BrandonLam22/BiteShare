package org.example.biteshare

import kotlinx.serialization.Serializable

@Serializable
data class RestaurantImportRequest(
    val restaurantName: String,
    val address: String = "",
    val postCode: String = "",
)

@Serializable
data class RestaurantImportResponse(
    val success: Boolean,
    val created: Boolean = false,
    val message: String,
    val restaurantId: String? = null,
    val restaurantName: String? = null,
)
