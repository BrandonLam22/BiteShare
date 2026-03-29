package org.example.biteshare.data

import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User
import org.example.biteshare.viewmodel.FriendDetails

interface BiteShareRepository : PickRepository {

    // Login/Signup
    suspend fun login(username: String, password: String): User?
    suspend fun signup(username: String, password: String, email: String): User?

    suspend fun getHomeFeed(userName: String): HomeFeed
    suspend fun browseRestaurants(): List<Restaurant>
    suspend fun getRestaurantsByTag(tag: String): List<Restaurant>
    suspend fun searchRestaurants(query: String, source: List<Restaurant> = emptyList()): List<Restaurant>
    suspend fun getRestaurantById(id: String): Restaurant?
    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail?
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
    suspend fun getPassword(): String
    suspend fun updatePassword(newPassword: String)

    suspend fun verifyCurrentPassword(password: String): Boolean
    suspend fun getUserReviews(): List<Review>
    suspend fun deleteReview(reviewId: String)
    suspend fun getFriendDetails(friendId: String): FriendDetails
    suspend fun editReview(reviewId: String, newRating: Int, newComment: String)
}