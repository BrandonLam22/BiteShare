package org.example.biteshare.domain

import kotlinx.serialization.SerialName

data class FeaturedItem(
    val name: String,
    val price: String,
    val description: String = "",
)

data class RestaurantLocation(
    val city: String,
    val address: String,
    val distance: String = "",
)

data class RestaurantDetail(
    val restaurantId: String,
    @SerialName("restaurant_name")
    val name: String,
    val images: List<String> = emptyList(),
    val featuredItems: List<FeaturedItem> = emptyList(),
    val location: RestaurantLocation,
    val reviews: List<Review> = emptyList(),
    val description: String = "",
    val rating: Double = 0.0,
    val attributes: List<String> = emptyList(),
    val googleMapsLink: String? = null,
    val restaurantWebsite: String? = null,
    val priceRange: String = "$$",
)
