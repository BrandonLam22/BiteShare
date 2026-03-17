package org.example.biteshare.data

import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User

interface BiteShareRepository {
    // Login/Signup
    suspend fun login(username: String, password: String): User?
    suspend fun signup(username: String, password: String, email: String): User?

    suspend fun getHomeFeed(userName: String): HomeFeed
    suspend fun browseRestaurants(): List<Restaurant>
    suspend fun getRestaurantsByTag(tag: String): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
    suspend fun getRestaurantDetailById(id: String): RestaurantDetail?
    suspend fun getReviewsForRestaurant(restaurantName: String): List<Review>
    suspend fun submitReview(review: Review)
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
    suspend fun friends(): List<Friend>
    suspend fun locations(): List<String>
    suspend fun restaurants(): List<Restaurant>
    suspend fun userPreferences(): List<String>
    suspend fun userRestrictions(): List<String>
    suspend fun getPassword(): String
    suspend fun updatePassword(newPassword: String)
}
