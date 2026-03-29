package org.example.biteshare.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class User (
    val id: String = Random.nextInt(0, 1000000).toString(),
    val username: String,
    val email: String,
    val password: String,
    val bio: String = "",
    val reviews: List<Review> = emptyList(),       
    val friends: List<Friend> = emptyList(),        
    val preferences: List<String> = emptyList(),
    val savedRestaurantIds: List<String> = emptyList(),
    @SerialName("food_restrictions")
    val foodRestrictions: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0

)