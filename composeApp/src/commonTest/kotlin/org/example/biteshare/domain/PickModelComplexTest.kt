package org.example.biteshare.domain

import org.example.biteshare.data.PickMockDB
import org.example.biteshare.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PickModelComplexTest {

    @Test
    fun distanceFilterKeepsUnknownCoordinates() = runMainTest {
        val restaurants = listOf(
            restaurant("near", "Pizza", lat = 43.4643, lon = -80.5204),
            restaurant("far", "Pizza", lat = 43.6532, lon = -79.3832),
            restaurant("unknown", "Pizza")
        )
        val model = PickModel(PickMockDB(restaurants = restaurants))
        val context = PickContext(
            mode = PickMode.ME_ONLY,
            currentLocation = GeoPoint(43.4643, -80.5204),
            filters = PickFilters(distance = DistanceFilter.ONE_KM)
        )

        val results = model.recommend(context)

        assertTrue(results.any { it.id == "near" })
        assertTrue(results.any { it.id == "unknown" })
        assertFalse(results.any { it.id == "far" })
    }

    @Test
    fun veganRestrictionOnlyFiltersNoneLevel() = runMainTest {
        val restaurants = listOf(
            restaurant("none", "Burgers", dietary = DietaryProfile(vegan = DietaryLevel.NONE)),
            restaurant("partial", "Pizza", dietary = DietaryProfile(vegan = DietaryLevel.PARTIAL)),
            restaurant("full", "Salad", dietary = DietaryProfile(vegan = DietaryLevel.FULL)),
        )
        val repo = PickMockDB(restaurants = restaurants, restrictions = listOf("vegan"))
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))

        assertFalse(results.any { it.id == "none" })
        assertTrue(results.any { it.id == "partial" })
        assertTrue(results.any { it.id == "full" })
    }

    @Test
    fun nonDietaryRestrictionUsesTags() = runMainTest {
        val restaurants = listOf(
            restaurant("seafood", "Sushi", tags = setOf("seafood")),
            restaurant("fish", "Sushi", tags = setOf("fish")),
            restaurant("safe", "Pizza", tags = setOf("pizza")),
        )
        val repo = PickMockDB(restaurants = restaurants, restrictions = listOf("seafood"))
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))

        assertTrue(results.any { it.id == "safe" })
        assertFalse(results.any { it.id == "seafood" })
        assertFalse(results.any { it.id == "fish" })
    }

    @Test
    fun preferenceTagsOverrideRatingOrdering() = runMainTest {
        val restaurants = listOf(
            restaurant("sushi", "Sushi", tags = setOf("sushi"), rating = 4.0),
            restaurant("pizza", "Pizza", tags = setOf("pizza"), rating = 5.0),
        )
        val repo = PickMockDB(restaurants = restaurants, preferences = listOf("sushi"))
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))

        assertEquals("sushi", results.first().id)
    }

    @Test
    fun mergedPreferencesFavorCombinedTags() = runMainTest {
        val restaurants = listOf(
            restaurant("combo", "Fusion", tags = setOf("sushi", "pizza"), rating = 3.5),
            restaurant("sushi", "Sushi", tags = setOf("sushi"), rating = 5.0),
            restaurant("pizza", "Pizza", tags = setOf("pizza"), rating = 5.0),
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            preferencesByUserId = mapOf(
                "me" to listOf("pizza"),
                "friend1" to listOf("sushi")
            )
        )
        val model = PickModel(repo)
        val context = PickContext(
            mode = PickMode.WITH_FRIENDS,
            selectedFriendIds = setOf("friend1"),
        )

        val results = model.recommend(context)

        assertEquals("combo", results.first().id)
    }

    @Test
    fun combinedFiltersNarrowResults() = runMainTest {
        val restaurants = listOf(
            restaurant(
                "near_sushi",
                "Sushi",
                tags = setOf("sushi"),
                rating = 4.5,
                isOpenNow = true,
                lat = 43.4643,
                lon = -80.5204,
            ),
            restaurant(
                "far_sushi",
                "Sushi",
                tags = setOf("sushi"),
                rating = 4.5,
                isOpenNow = true,
                lat = 43.6532,
                lon = -79.3832,
            ),
            restaurant(
                "near_sushi_closed",
                "Sushi",
                tags = setOf("sushi"),
                rating = 4.5,
                isOpenNow = false,
                lat = 43.4643,
                lon = -80.5204,
            ),
            restaurant(
                "near_pizza",
                "Pizza",
                tags = setOf("pizza"),
                rating = 4.5,
                isOpenNow = true,
                lat = 43.4643,
                lon = -80.5204,
            ),
            restaurant(
                "near_sushi_low",
                "Sushi",
                tags = setOf("sushi"),
                rating = 3.5,
                isOpenNow = true,
                lat = 43.4643,
                lon = -80.5204,
            ),
        )
        val repo = PickMockDB(restaurants = restaurants)
        val model = PickModel(repo)
        val context = PickContext(
            mode = PickMode.ME_ONLY,
            currentLocation = GeoPoint(43.4643, -80.5204),
            filters = PickFilters(
                distance = DistanceFilter.ONE_KM,
                cuisine = CuisineFilter.SUSHI,
                openNowOnly = true,
                minRating = 8.0,
            )
        )

        val results = model.recommend(context)

        assertEquals(1, results.size)
        assertEquals("near_sushi", results.first().id)
    }

    private fun restaurant(
        id: String,
        category: String,
        tags: Set<String> = emptySet(),
        dietary: DietaryProfile = DietaryProfile(),
        rating: Double = 4.0,
        isOpenNow: Boolean = true,
        lat: Double = 0.0,
        lon: Double = 0.0,
        location: String = "Waterloo",
        price: String = "$$",
    ): Restaurant =
        Restaurant(
            id = id,
            name = id,
            category = category,
            price = price,
            eta = "10-20 min",
            rating = rating,
            isSaved = false,
            location = location,
            isOpenNow = isOpenNow,
            latitude = lat,
            longitude = lon,
            tags = tags,
            dietaryProfile = dietary,
        )
}
