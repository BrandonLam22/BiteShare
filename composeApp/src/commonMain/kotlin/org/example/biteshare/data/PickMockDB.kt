package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail

class PickMockDB(
    friends: List<Friend> = MockDB.fakeFriends,
    restaurants: List<Restaurant> = MockDB.allRestaurants,
    restaurantDetails: Map<String, RestaurantDetail> = MockDB.restaurantDetails,
    preferences: List<String> = emptyList(),
    restrictions: List<String> = emptyList(),
) : PickRepository {
    private val friendsData = friends.toList()
    private val restaurantsData = restaurants.toList()
    private val detailsData = restaurantDetails.toMap()
    private val preferenceData = preferences.toList()
    private val restrictionData = restrictions.toList()

    override suspend fun friends(): List<Friend> = friendsData

    override suspend fun locations(): List<String> =
        restaurantsData.map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> = restaurantsData

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? = detailsData[id]

    override suspend fun userPreferences(): List<String> = preferenceData

    override suspend fun userRestrictions(): List<String> = restrictionData
}
