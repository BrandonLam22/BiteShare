package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail

interface PickRepository {
    suspend fun friends(): List<Friend>
    suspend fun locations(): List<String>
    suspend fun restaurants(): List<Restaurant>
    suspend fun getRestaurantDetailById(id: String): RestaurantDetail?
    suspend fun userPreferences(): List<String>
    suspend fun userRestrictions(): List<String>
    suspend fun currentUserId(): String? = null
    suspend fun restaurantSelectionsByUserIds(userIds: Set<String>): Map<String, Set<String>> = emptyMap()
    suspend fun setRestaurantSelection(userId: String, restaurantId: String, selected: Boolean) {}
}
