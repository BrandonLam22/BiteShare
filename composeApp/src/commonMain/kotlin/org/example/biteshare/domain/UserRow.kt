package org.example.biteshare.domain


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRow(
    val id: String,
    val username: String,
    val email: String,
    val bio: String = "",
    val preferences: List<String> = emptyList(),
    @SerialName("food_restrictions") val foodRestrictions: List<String> = emptyList(),
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("friend_count") val friendCount: Int = 0
)