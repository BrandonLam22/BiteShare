package org.example.biteshare.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random


@Serializable
data class Review(
    val id: String = Random.nextInt(0, 1000000).toString(),
    @SerialName("restaurant_name")
    val restaurantName: String,
    val tags: List<String>,

    val content: String,
    val rating: Int = 5,
    @SerialName("user_id")
    val userId: String? = null,
    val username: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("modified_at")
    val modifiedAt: String? = null,
)