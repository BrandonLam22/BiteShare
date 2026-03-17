package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickFilters
import org.example.biteshare.domain.PickMode
import org.example.biteshare.domain.PickModel
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.VoteWithFriendsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoteWithFriendsViewModelTest {

    private val model = PickModel(FakeRepository())
    private val context = PickContext(
        mode = PickMode.WITH_FRIENDS,
        selectedFriendIds = setOf("alex", "sally"),
        filters = PickFilters()
    )

    @Test
    fun initUsesProvidedCandidateRestaurants() = runMainTest {
        val generated = model.recommend(context).take(2)
        val vm = VoteWithFriendsViewModel(
            model = model,
            context = context,
            candidateRestaurants = generated,
            pollIntervalMs = 0
        )
        advanceUntilIdle()

        assertEquals(2, vm.uiState.restaurants.size)
        assertEquals(generated.map { it.id }, vm.uiState.restaurants.map { it.id })
    }

    @Test
    fun initUsesSelectedFriendsAndLoadsRestaurants() = runMainTest {
        val vm = VoteWithFriendsViewModel(model, context, pollIntervalMs = 0)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.friends.size)
        assertTrue(vm.uiState.friends.any { it.id == "alex" })
        assertTrue(vm.uiState.friends.any { it.id == "sally" })
        assertTrue(vm.uiState.restaurants.isNotEmpty())
        assertEquals(2, vm.uiState.friendVotes.size)
    }

    @Test
    fun toggleMyVoteOnlyChangesMyVotes() = runMainTest {
        val vm = VoteWithFriendsViewModel(model, context, pollIntervalMs = 0)
        advanceUntilIdle()
        val firstRestaurant = vm.uiState.restaurants.first().id
        val friendVotesBefore = vm.uiState.friendVotes

        vm.toggleMyVote(firstRestaurant)

        assertTrue(vm.isMySelection(firstRestaurant))
        assertEquals(friendVotesBefore, vm.uiState.friendVotes)

        vm.toggleMyVote(firstRestaurant)
        assertFalse(vm.isMySelection(firstRestaurant))
    }

    @Test
    fun buildRankedRecommendationsIncludesMyVoteInCounts() = runMainTest {
        val vm = VoteWithFriendsViewModel(model, context, pollIntervalMs = 0)
        advanceUntilIdle()
        val firstRestaurant = vm.uiState.restaurants.first().id
        val beforeCount = vm.voteCountForRestaurant(firstRestaurant)

        vm.toggleMyVote(firstRestaurant)
        val afterCount = vm.voteCountForRestaurant(firstRestaurant)
        val ranked = vm.buildRankedRecommendations()

        assertEquals(beforeCount + 1, afterCount)
        assertTrue(ranked.isNotEmpty())
        assertNotEquals(0, vm.mySelectedCount())
    }

    @Test
    fun finishVotingLocksFurtherChanges() = runMainTest {
        val vm = VoteWithFriendsViewModel(model, context, pollIntervalMs = 0)
        advanceUntilIdle()
        val firstRestaurant = vm.uiState.restaurants.first().id

        vm.toggleMyVote(firstRestaurant)
        assertTrue(vm.isMySelection(firstRestaurant))

        vm.finishVoting()
        assertTrue(vm.uiState.isVotingClosed)

        vm.toggleMyVote(firstRestaurant)
        assertTrue(vm.isMySelection(firstRestaurant))
    }
}
