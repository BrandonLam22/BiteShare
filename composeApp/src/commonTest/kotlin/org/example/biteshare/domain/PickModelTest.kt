package org.example.biteshare.domain

import org.example.biteshare.data.PickMockDB
import org.example.biteshare.runMainTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PickModelTest {

    @Test
    fun recommendRespectsLocationFilter() = runMainTest {
        val repo = PickMockDB(
            currentUserId = "me",
            currentUserLocation = GeoPoint(43.4643, -80.5204)
        )
        val model = PickModel(repo)

        val context = PickContext(
            mode = PickMode.ME_ONLY,
            filters = PickFilters(distance = DistanceFilter.ONE_KM)
        )

        val results = model.recommend(context)
        assertTrue(results.isNotEmpty(), "Expected recommendations for Waterloo.")
        assertTrue(results.all { it.location == "Waterloo" })
    }

    @Test
    fun recommendFiltersOutRestrictedItems() = runMainTest {
        val repo = PickMockDB(restrictions = listOf("coffee"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val results = model.recommend(context)

        assertTrue(results.isNotEmpty())
        assertTrue(results.none { it.category.equals("coffee", ignoreCase = true) })
    }

    @Test
    fun preferencesInfluenceRanking() = runMainTest {
        val repo = PickMockDB(preferences = listOf("pizza"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val results = model.recommend(context)

        assertTrue(results.isNotEmpty())
        assertTrue(results.first().category.equals("pizza", ignoreCase = true))
    }

    @Test
    fun previewCountMatchesRecommendationSize() = runMainTest {
        val repo = PickMockDB(preferences = listOf("sushi"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val count = model.previewCount(context)
        val results = model.recommend(context)

        assertEquals(results.size, count)
    }

    @Test
    fun groupPreferencesAreMergedForRecommendations() = runMainTest {
        val restaurants = listOf(
            Restaurant("p1", "Pizza Place", "Pizza", "$10.00", "10 min", 4.0, location = "Waterloo"),
            Restaurant("s1", "Sushi Spot", "Sushi", "$10.00", "10 min", 5.0, location = "Waterloo"),
        )
        val repo = PickMockDB(
            restaurants = restaurants,
            currentUserId = "me",
            preferencesByUserId = mapOf(
                "me" to listOf("pizza"),
                "friend1" to listOf("sushi"),
            ),
        )
        val model = PickModel(repo)

        val context = PickContext(
            mode = PickMode.WITH_FRIENDS,
            selectedFriendIds = setOf("friend1"),
            filters = PickFilters(location = "Waterloo"),
        )

        val results = model.recommend(context)
        assertTrue(results.isNotEmpty())
        assertEquals("Sushi Spot", results.first().name)
    }

    @Test
    fun voteSessionVotesAreUpdated() = runMainTest {
        val restaurants = listOf(
            Restaurant("r1", "Cafe One", "Coffee", "$5.00", "8 min", 4.2),
            Restaurant("r2", "Burger Two", "Burgers", "$9.00", "12 min", 4.1),
        )
        val repo = PickMockDB(restaurants = restaurants, currentUserId = "me")
        val model = PickModel(repo)
        val session = VoteSession(
            id = "session-1",
            title = "Test Session",
            createdAtEpoch = 1000L,
            participants = listOf(VoteParticipant(id = "me", name = "Me")),
            restaurants = restaurants,
            votesByUserId = mapOf("me" to setOf("r1")),
        )

        model.createVoteSession(session)

        val initialVotes = model.voteSessionVotes(session.id)
        assertEquals(setOf("r1"), initialVotes["me"])

        model.updateVoteSessionVotes(session.id, "me", setOf("r2"))

        val updatedVotes = model.voteSessionVotes(session.id)
        assertEquals(setOf("r2"), updatedVotes["me"])
    }

    @Test
    fun closeVoteSessionPersistsClosedState() = runMainTest {
        val restaurants = listOf(
            Restaurant("r1", "Cafe One", "Coffee", "$5.00", "8 min", 4.2),
        )
        val repo = PickMockDB(restaurants = restaurants)
        val model = PickModel(repo)
        val session = VoteSession(
            id = "session-2",
            title = "Close Session",
            createdAtEpoch = 2000L,
            participants = emptyList(),
            restaurants = restaurants,
            votesByUserId = emptyMap(),
        )

        model.createVoteSession(session)

        val closedAt = 987654L
        model.closeVoteSession(session.id, closedAt)

        val updated = model.voteSessionById(session.id)
        assertTrue(updated?.isClosed == true)
        assertEquals(closedAt, updated?.closedAtEpoch)
    }
}
