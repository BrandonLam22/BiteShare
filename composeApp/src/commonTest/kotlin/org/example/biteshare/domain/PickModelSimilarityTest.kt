package org.example.biteshare.domain

import org.example.biteshare.data.PickMockDB
import org.example.biteshare.runMainTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PickModelSimilarityTest {

    @Test
    fun hardConstraintsApplyBeforeSimilarity() = runMainTest {
        val restaurants = listOf(
            restaurant("near", tags = setOf("pizza"), lat = 43.4643, lon = -80.5204),
            restaurant("far", tags = setOf("pizza"), lat = 43.6532, lon = -79.3832),
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            savedSelectionsByUserId = mapOf("me" to setOf("far")),
        )
        val model = PickModel(repo)

        val results = model.recommend(
            PickContext(
                mode = PickMode.ME_ONLY,
                currentLocation = GeoPoint(43.4643, -80.5204),
                filters = PickFilters(distance = DistanceFilter.ONE_KM),
            )
        )

        assertTrue(results.any { it.id == "near" })
        assertTrue(results.none { it.id == "far" })
    }

    @Test
    fun savedSimilarityBoostsRanking() = runMainTest {
        val restaurants = listOf(
            restaurant("saved", tags = setOf("pizza")),
            restaurant("similar", tags = setOf("pizza", "italian")),
            restaurant("other", tags = setOf("sushi")),
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            savedSelectionsByUserId = mapOf("me" to setOf("saved")),
        )
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))
        val similarIndex = results.indexOfFirst { it.id == "similar" }
        val otherIndex = results.indexOfFirst { it.id == "other" }

        assertTrue(similarIndex in results.indices && otherIndex in results.indices)
        assertTrue(similarIndex < otherIndex)
    }

    @Test
    fun highRatedSimilarityBoostsRanking() = runMainTest {
        val restaurants = listOf(
            restaurant("high", tags = setOf("sushi")),
            restaurant("similar", tags = setOf("sushi", "japanese")),
            restaurant("other", tags = setOf("pizza")),
        )
        val reviews = listOf(
            Review(
                id = "r1",
                restaurantName = "high",
                tags = listOf("taste_good"),
                content = "Great taste",
                rating = 9,
                userId = "me",
            )
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            reviewsByUserId = mapOf("me" to reviews),
        )
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))
        val similarIndex = results.indexOfFirst { it.id == "similar" }
        val otherIndex = results.indexOfFirst { it.id == "other" }

        assertTrue(similarIndex in results.indices && otherIndex in results.indices)
        assertTrue(similarIndex < otherIndex)
    }

    @Test
    fun lowRatedSimilarityPenalizesRanking() = runMainTest {
        val restaurants = listOf(
            restaurant("bad", tags = setOf("burgers")),
            restaurant("similar", tags = setOf("burgers", "fast food")),
            restaurant("safe", tags = setOf("sushi")),
        )
        val reviews = listOf(
            Review(
                id = "r2",
                restaurantName = "bad",
                tags = listOf("taste_greasy"),
                content = "Too greasy",
                rating = 2,
                userId = "me",
            )
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            reviewsByUserId = mapOf("me" to reviews),
        )
        val model = PickModel(repo)

        val results = model.recommend(PickContext(mode = PickMode.ME_ONLY))
        val safeIndex = results.indexOfFirst { it.id == "safe" }
        val similarIndex = results.indexOfFirst { it.id == "similar" }

        assertTrue(safeIndex in results.indices && similarIndex in results.indices)
        assertTrue(safeIndex < similarIndex)
    }

    @Test
    fun friendReviewSignalsAreIncludedInGroupMode() = runMainTest {
        val restaurants = listOf(
            restaurant("friendFav", tags = setOf("ramen")),
            restaurant("similar", tags = setOf("ramen", "japanese")),
            restaurant("other", tags = setOf("pizza")),
        )
        val reviews = listOf(
            Review(
                id = "r3",
                restaurantName = "friendFav",
                tags = listOf("taste_good"),
                content = "Friend favorite",
                rating = 9,
                userId = "friend1",
            )
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            reviewsByUserId = mapOf("friend1" to reviews),
        )
        val model = PickModel(repo)

        val results = model.recommend(
            PickContext(
                mode = PickMode.WITH_FRIENDS,
                selectedFriendIds = setOf("friend1"),
            )
        )
        val similarIndex = results.indexOfFirst { it.id == "similar" }
        val otherIndex = results.indexOfFirst { it.id == "other" }

        assertTrue(similarIndex in results.indices && otherIndex in results.indices)
        assertTrue(similarIndex < otherIndex)
    }

    private fun restaurant(
        id: String,
        category: String = "Test",
        tags: Set<String> = emptySet(),
        reviewProfile: Map<String, Double> = emptyMap(),
        rating: Double = 4.0,
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
            latitude = lat,
            longitude = lon,
            tags = tags,
            reviewTagProfile = reviewProfile,
        )
}
