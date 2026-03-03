package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant

class FakeRepository {

    fun friends(): List<Friend> = MockDB.fakeFriends

    fun restaurants(): List<Restaurant> = MockDB.fakeRestaurants

    fun getRestaurantById(id: String): Restaurant? =
        (restaurants() + browseRestaurants()).distinctBy { it.id }.find { it.id == id }

    fun browseRestaurants(): List<Restaurant> = MockDB.fakeBrowseRestaurants

    fun locations(): List<String> =
        (restaurants() + browseRestaurants())
            .map { it.location }
            .distinct()
            .sorted()

    fun recommend(context: PickContext): List<Restaurant> = PickModel(this).recommend(context)
}
