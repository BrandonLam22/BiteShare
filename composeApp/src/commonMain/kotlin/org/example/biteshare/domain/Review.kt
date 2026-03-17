package org.example.biteshare.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random


@Serializable
data class Review(
    val id: String = Random.nextInt(0, 1000000).toString(),
    val restaurantName: String,
    val tags: List<String>,
    val content: String,
    val rating: Int = 5,
    val userId: String? = null,
    val username: String? = null,
    val createdAt: String? = null,
)