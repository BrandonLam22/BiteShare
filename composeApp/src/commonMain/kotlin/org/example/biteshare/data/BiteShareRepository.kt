package org.example.biteshare.data

import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant

interface BiteShareRepository : PickRepository {
    suspend fun getHomeFeed(userName: String): HomeFeed
    suspend fun browseRestaurants(): List<Restaurant>
    suspend fun getRestaurantsByTag(tag: String): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
    suspend fun getSavedRestaurants(): List<Restaurant>
    suspend fun toggleSaved(restaurantId: String)
    suspend fun getProfile(): ProfileData
    suspend fun updateNotificationPreference(enabled: Boolean)
    suspend fun logout()
    suspend fun getEditableProfile(): EditableProfile
    suspend fun updateProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>,
    )
    suspend fun getPassword(): String
    suspend fun updatePassword(newPassword: String)

    suspend fun verifyCurrentPassword(password: String): Boolean
}
