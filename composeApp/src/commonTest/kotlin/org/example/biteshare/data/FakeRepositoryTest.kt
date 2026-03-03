package org.example.biteshare.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeRepositoryTest {

    @Test
    fun friendsReturnsMockDbData() {
        val repo = FakeRepository()

        val friends = repo.friends()

        assertEquals(MockDB.fakeFriends, friends)
        assertTrue(friends.isNotEmpty())
    }

    @Test
    fun getRestaurantByIdCanReadBrowseData() {
        val repo = FakeRepository()

        val restaurant = repo.getRestaurantById("bp2")

        assertNotNull(restaurant)
        assertEquals("Pizza Plaza", restaurant.name)
    }

    @Test
    fun locationsReturnsDistinctSortedValues() {
        val repo = FakeRepository()

        val locations = repo.locations()

        assertTrue(locations.isNotEmpty())
        assertEquals(locations.sorted(), locations)
        assertEquals(locations.toSet().size, locations.size)
    }
}
