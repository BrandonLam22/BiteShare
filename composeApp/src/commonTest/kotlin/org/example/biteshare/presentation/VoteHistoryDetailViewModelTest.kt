package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.PickMockDB
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.VoteHistoryDetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoteHistoryDetailViewModelTest {

    @Test
    fun voteCountsAndRankingAreDerivedFromSession() = runMainTest {
        val restaurants = listOf(
            Restaurant("r1", "Alpha", "Pizza", "$10", "10 min", 4.0),
            Restaurant("r2", "Beta", "Sushi", "$12", "12 min", 4.5),
        )
        val session = VoteSession(
            id = "session_1",
            title = "Test",
            createdAtEpoch = 1234L,
            participants = listOf(
                VoteParticipant(id = "me", name = "Me"),
                VoteParticipant(id = "friend", name = "Friend")
            ),
            restaurants = restaurants,
            votesByUserId = mapOf(
                "me" to setOf("r1", "r2"),
                "friend" to setOf("r2"),
            ),
        )
        val repo = PickMockDB(currentUserId = "me", initialVoteSessions = listOf(session))
        val model = PickModel(repo)
        val vm = VoteHistoryDetailViewModel(model, "session_1")
        advanceUntilIdle()

        assertEquals(1, vm.voteCountForRestaurant("r1"))
        assertEquals(2, vm.voteCountForRestaurant("r2"))
        assertEquals(2, vm.participantSelectedCount("me"))
        assertEquals(setOf("Alpha", "Beta"), vm.participantVoteRestaurantNames("me").toSet())

        val ranked = vm.rankedRestaurants()
        assertTrue(ranked.first().id == "r2")
    }
}
