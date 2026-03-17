package org.example.biteshare.data

import org.example.biteshare.domain.Model
import org.example.biteshare.domain.FriendAddResult
import org.example.biteshare.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeRepositoryTest {

    private val repo = FakeRepository()

    private fun loggedInRepo(): FakeRepository {
        val model = Model()
        model.login("Kevin", "12345")
        return FakeRepository(model)
    }


    @Test
    fun allRestaurantsShouldHaveDetailData() = runMainTest {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        assertTrue(all.isNotEmpty(), "Restaurant list should not be empty.")

        all.forEach { restaurant ->
            val detail = repo.getRestaurantDetailById(restaurant.id)
            assertNotNull(detail, "Detail should exist for ${restaurant.id}")
            assertEquals(restaurant.id, detail.restaurantId)
        }
    }

    @Test
    fun homeFeedShouldContainBaselineSections() = runMainTest {
        val feed = repo.getHomeFeed("Tester")
        assertTrue(feed.categories.isNotEmpty(), "Home categories should not be empty.")
        assertTrue(feed.popularDishes.isNotEmpty(), "Popular dishes should not be empty.")
        assertTrue(feed.popularDrinks.isNotEmpty(), "Popular drinks should not be empty.")
        assertTrue(feed.greeting.contains("Tester"), "Greeting should include user name.")
    }

    @Test
    fun waterlooRestaurantsShouldBeRichEnoughForDemo() = runMainTest {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        assertTrue(all.size >= 12, "Expected at least 12 restaurants for demo richness.")

        val names = all.map { it.name }
        assertTrue(names.any { it.contains("Lazeez", ignoreCase = true) })
        assertTrue(names.any { it.contains("Ken Sushi", ignoreCase = true) })
        assertTrue(names.any { it.contains("Williams", ignoreCase = true) })
    }

    @Test
    fun featuredItemsShouldBeCategorySpecificNotGeneric() = runMainTest {
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
    fun attributesShouldBeCategorySpecific() = runMainTest {
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
    fun homeFeedCategoriesShouldUseDeliveryStyleTypes() = runMainTest {
        val feed = repo.getHomeFeed("TagUser")
        assertTrue(feed.categories.size >= 6, "Homepage should expose multiple tag-based category types.")
        val labels = feed.categories.map { it.label.lowercase() }
        assertTrue(labels.any { it.contains("pizza") })
        assertTrue(labels.any { it.contains("fast food") })
        assertTrue(labels.any { it.contains("coffee") || it.contains("bubble tea") })
    }

    @Test
    fun getRestaurantsByTagShouldReturnRelevantList() = runMainTest {
        val halal = repo.getRestaurantsByTag("Halal")
        val sushi = repo.getRestaurantsByTag("Sushi")

        assertTrue(halal.isNotEmpty(), "Halal tag should map to at least one restaurant.")
        assertTrue(sushi.isNotEmpty(), "Sushi tag should map to at least one restaurant.")
        assertTrue(halal.any { it.name.contains("Lazeez", ignoreCase = true) || it.name.contains("Shaw", ignoreCase = true) })
        assertTrue(sushi.any { it.name.contains("Sushi", ignoreCase = true) })
    }

    @Test
    fun tagSynonymsShouldMapDrinkAndBubbleTea() = runMainTest {
        val byDrink = repo.getRestaurantsByTag("Drink")
        val byBubbleTea = repo.getRestaurantsByTag("Bubble Tea")

        assertTrue(byDrink.isNotEmpty())
        assertTrue(byBubbleTea.isNotEmpty())
        assertTrue(byDrink.any { it.name.contains("Alley", ignoreCase = true) })
        assertTrue(byBubbleTea.any { it.name.contains("Alley", ignoreCase = true) })
    }

    @Test
    fun friendsReturnsMockDbData() = runMainTest {
        val repo = FakeRepository()

        val friends = repo.friends()

        assertEquals(MockDB.fakeFriends, friends)
        assertTrue(friends.isNotEmpty())
    }

    @Test
    fun getRestaurantByIdCanReadBrowseData() = runMainTest {
        val repo = FakeRepository()

        val restaurant = repo.getRestaurantById("bp2")

        assertNotNull(restaurant)
        assertEquals("Pizza Plaza", restaurant.name)
    }

    @Test
    fun locationsReturnsDistinctSortedValues() = runMainTest {
        val repo = FakeRepository()

        val locations = repo.locations()

        assertTrue(locations.isNotEmpty())
        assertEquals(locations.sorted(), locations)
        assertEquals(locations.toSet().size, locations.size)
    }

    @Test
    fun searchRestaurantsShouldFindSpecificRestaurantByName() = runMainTest {
        val result = repo.searchRestaurants("Ken Sushi")

        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.name == "Ken Sushi House" })
    }

    @Test
    fun searchRestaurantsShouldMatchAttributesAndDescriptions() = runMainTest {
        val result = repo.searchRestaurants("halal")

        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.name.contains("Lazeez", ignoreCase = true) })
    }

    @Test
    fun searchUsersShouldReturnOnlyEligibleUsers() = runMainTest {
        val repo = loggedInRepo()

        val results = repo.searchUsers("ali")

        assertEquals(1, results.size)
        assertEquals("user_03", results.first().id)
        assertEquals("Alice", results.first().name)
    }

    @Test
    fun addFriendShouldAppendNewFriendToCurrentUser() = runMainTest {
        val repo = loggedInRepo()

        val result = repo.addFriend("user_03")

        assertEquals(FriendAddResult.SUCCESS, result)
        assertTrue(repo.friends().any { it.id == "user_03" && it.name == "Alice" })
    }

    @Test
    fun addFriendShouldRejectDuplicateFriend() = runMainTest {
        val repo = loggedInRepo()

        assertEquals(FriendAddResult.SUCCESS, repo.addFriend("user_03"))
        assertEquals(FriendAddResult.ALREADY_FRIENDS, repo.addFriend("user_03"))
    }

    // ── Profile & User Management ──────────────────────────────────────────────

    @Test
    fun getProfileShouldReturnUserData() = runMainTest {
        val profile = repo.getProfile()
        assertNotNull(profile)
        assertTrue(profile.name.isNotBlank(), "Profile name should not be blank.")
        assertTrue(profile.friendCount >= 0, "Friend count should be non-negative.")
    }

    @Test
    fun getEditableProfileShouldMatchGetProfile() = runMainTest {
        val profile = repo.getProfile()
        val editable = repo.getEditableProfile()
        assertEquals(profile.name, editable.username, "Username should match between profile and editable profile.")
        assertEquals(profile.email, editable.email, "Email should match between profile and editable profile.")
    }

    @Test
    fun updateProfileShouldPersistChanges() = runMainTest {
        val repo = loggedInRepo()

        repo.updateProfile(
            username = "UpdatedUser",
            email = "updated@example.com",
            bio = "New bio",
            preferences = listOf("Pizza", "Sushi"),
            foodRestrictions = listOf("Gluten-Free")
        )

        val after = repo.getEditableProfile()
        assertEquals("UpdatedUser", after.username)
        assertEquals("updated@example.com", after.email)
        assertEquals("New bio", after.bio)
        assertEquals(listOf("Pizza", "Sushi"), after.preferences)
        assertEquals(listOf("Gluten-Free"), after.foodRestrictions)
    }

    @Test
    fun updateNotificationPreferenceShouldToggleCorrectly() = runMainTest {
        repo.updateNotificationPreference(false)
        assertFalse(repo.getProfile().notificationsEnabled, "Notifications should be disabled.")

        repo.updateNotificationPreference(true)
        assertTrue(repo.getProfile().notificationsEnabled, "Notifications should be re-enabled.")
    }

// ── Saved Restaurants ──────────────────────────────────────────────────────

    @Test
    fun toggleSavedShouldAddRestaurantToSavedList() = runMainTest {
        val repo = loggedInRepo()
        // Kevin already has ["p1", "b1", "bp1"] saved — pick one that isn't saved
        val unsavedId = "s1"
        assertFalse(repo.getSavedRestaurants().any { it.id == unsavedId }, "s1 should not be saved initially.")

        repo.toggleSaved(unsavedId)

        assertTrue(repo.getSavedRestaurants().any { it.id == unsavedId }, "s1 should appear in saved list after toggle.")
    }

    @Test
    fun toggleSavedTwiceShouldRemoveRestaurantFromSavedList() = runMainTest {
        val repo = loggedInRepo()
        val unsavedId = "s1"

        repo.toggleSaved(unsavedId)
        repo.toggleSaved(unsavedId)

        assertFalse(repo.getSavedRestaurants().any { it.id == unsavedId }, "s1 should be removed after toggling twice.")
    }

    @Test
    fun getSavedRestaurantsShouldReturnNonEmptyList() = runMainTest {
        val saved = repo.getSavedRestaurants()
        assertTrue(saved.isNotEmpty(), "Saved restaurants should not be empty (defaults from MockDB expected).")
    }

    @Test
    fun getSavedRestaurantsShouldOnlyContainSavedEntries() = runMainTest {
        val saved = repo.getSavedRestaurants()
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        saved.forEach { savedRestaurant ->
            val match = all.find { it.id == savedRestaurant.id }
            assertNotNull(match, "Saved restaurant ${savedRestaurant.id} should exist in full list.")
            assertTrue(
                repo.getRestaurantById(savedRestaurant.id) != null,
                "Each saved restaurant should be retrievable by ID."
            )
        }
    }

// ── getRestaurantsByTag edge cases ─────────────────────────────────────────

    @Test
    fun getRestaurantsByTagWithBlankTagShouldReturnAllRestaurants() = runMainTest {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        val result = repo.getRestaurantsByTag("")
        assertEquals(all.size, result.size, "Blank tag should return all restaurants.")
    }

    @Test
    fun getRestaurantsByTagWithUnknownTagShouldReturnAllRestaurants() = runMainTest {
        val all = (repo.restaurants() + repo.browseRestaurants()).distinctBy { it.id }
        val result = repo.getRestaurantsByTag("xyzunknowntag")
        assertEquals(all.size, result.size, "Unknown tag should return all restaurants.")
    }

    @Test
    fun getRestaurantsByTagShouldBeCaseInsensitive() = runMainTest {
        val lower = repo.getRestaurantsByTag("pizza")
        val upper = repo.getRestaurantsByTag("PIZZA")
        val mixed = repo.getRestaurantsByTag("PiZzA")
        assertEquals(lower.map { it.id }.toSet(), upper.map { it.id }.toSet(), "Tag matching should be case-insensitive.")
        assertEquals(lower.map { it.id }.toSet(), mixed.map { it.id }.toSet())
    }

// ── getHomeFeed edge cases ─────────────────────────────────────────────────

    @Test
    fun getHomeFeedUsesProvidedName() = runMainTest {
        val feed = repo.getHomeFeed("John")
        assertTrue(feed.greeting.contains("John"), "Greeting should include provided name.")
    }



// ── getRestaurantDetailById edge cases ────────────────────────────────────

    @Test
    fun getRestaurantDetailByIdShouldReturnNullForNonExistentId() = runMainTest {
        val detail = repo.getRestaurantDetailById("nonexistent_id_xyz")
        assertEquals(null, detail, "Non-existent ID should return null.")
    }







// ── locations ─────────────────────────────────────────────────────────────

    @Test
    fun locationsShouldAllCorrespondToRealRestaurants() = runMainTest {
        val locations = repo.locations()
        val allRestaurantLocations = (repo.restaurants() + repo.browseRestaurants())
            .map { it.location }
            .toSet()
        locations.forEach { location ->
            assertTrue(
                location in allRestaurantLocations,
                "Location '$location' from locations() should exist in at least one restaurant."
            )
        }
    }
}
