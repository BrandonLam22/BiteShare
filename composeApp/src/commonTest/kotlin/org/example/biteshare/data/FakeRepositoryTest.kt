package org.example.biteshare.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FakeRepositoryTest {

    private val repo = FakeRepository()

    @Test
    fun allRestaurantsShouldHaveDetailData() {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        assertTrue(all.isNotEmpty(), "Restaurant list should not be empty.")

        all.forEach { restaurant ->
            val detail = repo.getRestaurantDetailById(restaurant.id)
            assertNotNull(detail, "Detail should exist for ${restaurant.id}")
            assertEquals(restaurant.id, detail.restaurantId)
        }
    }

    @Test
    fun homeFeedShouldContainBaselineSections() {
        val feed = repo.getHomeFeed("Tester")
        assertTrue(feed.categories.isNotEmpty(), "Home categories should not be empty.")
        assertTrue(feed.popularDishes.isNotEmpty(), "Popular dishes should not be empty.")
        assertTrue(feed.popularDrinks.isNotEmpty(), "Popular drinks should not be empty.")
        assertTrue(feed.greeting.contains("Tester"), "Greeting should include user name.")
    }

    @Test
    fun waterlooRestaurantsShouldBeRichEnoughForDemo() {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        assertTrue(all.size >= 12, "Expected at least 12 restaurants for demo richness.")

        val names = all.map { it.name }
        assertTrue(names.any { it.contains("Lazeez", ignoreCase = true) })
        assertTrue(names.any { it.contains("Ken Sushi", ignoreCase = true) })
        assertTrue(names.any { it.contains("Williams", ignoreCase = true) })
    }

    @Test
    fun featuredItemsShouldBeCategorySpecificNotGeneric() {
        val pizzaDetail = repo.getRestaurantDetailById("p1")
        val sushiDetail = repo.getRestaurantDetailById("s1")

        assertNotNull(pizzaDetail)
        assertNotNull(sushiDetail)
        assertTrue(pizzaDetail.featuredItems.isNotEmpty())
        assertTrue(sushiDetail.featuredItems.isNotEmpty())

        val pizzaNames = pizzaDetail.featuredItems.map { it.name.lowercase() }
        val sushiNames = sushiDetail.featuredItems.map { it.name.lowercase() }

        assertTrue(pizzaNames.any { it.contains("pizza") || it.contains("slice") })
        assertTrue(sushiNames.any { it.contains("nigiri") || it.contains("roll") })
        assertFalse(pizzaNames.contains("house special"))
        assertFalse(sushiNames.contains("house special"))
    }

    @Test
    fun attributesShouldBeCategorySpecific() {
        val coffeeDetail = repo.getRestaurantDetailById("c1")
        val grillDetail = repo.getRestaurantDetailById("uw9")

        assertNotNull(coffeeDetail)
        assertNotNull(grillDetail)

        val coffeeAttrs = coffeeDetail.attributes.map { it.lowercase() }
        val grillAttrs = grillDetail.attributes.map { it.lowercase() }

        assertTrue(coffeeAttrs.any { it.contains("coffee") || it.contains("study") || it.contains("wi-fi") })
        assertTrue(grillAttrs.any { it.contains("grill") || it.contains("protein") })
        assertFalse(coffeeAttrs.contains("student friendly"), "Coffee attributes should be category-specific, not generic fallback.")
    }

    @Test
    fun homeFeedCategoriesShouldUseDeliveryStyleTypes() {
        val feed = repo.getHomeFeed("TagUser")
        assertTrue(feed.categories.size >= 6, "Homepage should expose multiple tag-based category types.")
        val labels = feed.categories.map { it.label.lowercase() }
        assertTrue(labels.any { it.contains("pizza") })
        assertTrue(labels.any { it.contains("fast food") })
        assertTrue(labels.any { it.contains("coffee") || it.contains("bubble tea") })
    }

    @Test
    fun getRestaurantsByTagShouldReturnRelevantList() {
        val halal = repo.getRestaurantsByTag("Halal")
        val sushi = repo.getRestaurantsByTag("Sushi")

        assertTrue(halal.isNotEmpty(), "Halal tag should map to at least one restaurant.")
        assertTrue(sushi.isNotEmpty(), "Sushi tag should map to at least one restaurant.")
        assertTrue(halal.any { it.name.contains("Lazeez", ignoreCase = true) || it.name.contains("Shaw", ignoreCase = true) })
        assertTrue(sushi.any { it.name.contains("Sushi", ignoreCase = true) })
    }

    @Test
    fun tagSynonymsShouldMapDrinkAndBubbleTea() {
        val byDrink = repo.getRestaurantsByTag("Drink")
        val byBubbleTea = repo.getRestaurantsByTag("Bubble Tea")

        assertTrue(byDrink.isNotEmpty())
        assertTrue(byBubbleTea.isNotEmpty())
        assertTrue(byDrink.any { it.name.contains("Alley", ignoreCase = true) })
        assertTrue(byBubbleTea.any { it.name.contains("Alley", ignoreCase = true) })
    }
}