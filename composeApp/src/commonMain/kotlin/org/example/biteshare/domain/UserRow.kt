package org.example.biteshare.domain


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class UserRow(
    val id: String,
    val username: String,
    val email: String,
    val bio: String = "",
    val preferences: List<String> = emptyList(),
    @SerialName("food_restrictions") val foodRestrictions: List<String> = emptyList(),
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @Transient val friendCount: Int = 0
)
{
    //var friendCount: Int = 0  // set manually after fetching
}
