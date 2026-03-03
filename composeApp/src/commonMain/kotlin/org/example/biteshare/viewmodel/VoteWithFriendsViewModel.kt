package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.absoluteValue
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant

data class VoteWithFriendsUiState(
    val friends: List<Friend> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val myVotes: Set<String> = emptySet(),
    val friendVotes: Map<String, Set<String>> = emptyMap(),
    val isVotingClosed: Boolean = false,
)

class VoteWithFriendsViewModel(
    private val model: PickModel,
    private val context: PickContext,
    private val candidateRestaurants: List<Restaurant> = emptyList(),
) {
    var uiState by mutableStateOf(VoteWithFriendsUiState())
        private set

    init {
        val allFriends = model.friends()
        val selectedFriends = allFriends.filter { it.id in context.selectedFriendIds }
        val participants = if (selectedFriends.isNotEmpty()) selectedFriends else allFriends
        val recommended = if (candidateRestaurants.isNotEmpty()) {
            candidateRestaurants
        } else {
            model.recommend(context)
        }
        val recommendedIds = recommended.map { it.id }

        uiState = VoteWithFriendsUiState(
            friends = participants,
            restaurants = recommended,
            friendVotes = participants.associate { friend ->
                friend.id to buildMockVotes(friend.id, recommendedIds)
            }
        )
    }

    fun toggleMyVote(restaurantId: String) {
        if (uiState.isVotingClosed) return
        val oldVotes = uiState.myVotes
        val newVotes = if (restaurantId in oldVotes) oldVotes - restaurantId else oldVotes + restaurantId
        uiState = uiState.copy(myVotes = newVotes)
    }

    fun finishVoting() {
        if (uiState.isVotingClosed) return
        uiState = uiState.copy(isVotingClosed = true)
    }

    fun isMySelection(restaurantId: String): Boolean = restaurantId in uiState.myVotes

    fun mySelectedCount(): Int = uiState.myVotes.size

    fun friendSelectedCount(friendId: String): Int = uiState.friendVotes[friendId]?.size ?: 0

    fun friendVoteRestaurantNames(friendId: String): List<String> {
        val byId = uiState.restaurants.associateBy { it.id }
        return uiState.friendVotes[friendId]
            .orEmpty()
            .mapNotNull { byId[it]?.name }
    }

    fun voteCountForRestaurant(restaurantId: String): Int {
        val friendCount = uiState.friendVotes.values.count { restaurantId in it }
        val myCount = if (restaurantId in uiState.myVotes) 1 else 0
        return friendCount + myCount
    }

    fun buildRankedRecommendations(): List<Restaurant> {
        val orderById = uiState.restaurants.mapIndexed { index, r -> r.id to index }.toMap()
        return uiState.restaurants.sortedWith(
            compareByDescending<Restaurant> { voteCountForRestaurant(it.id) }
                .thenBy { orderById[it.id] ?: Int.MAX_VALUE }
        )
    }

    private fun buildMockVotes(
        friendId: String,
        restaurantIds: List<String>,
    ): Set<String> {
        if (restaurantIds.isEmpty()) return emptySet()

        val size = restaurantIds.size
        val seed = friendId.hashCode().absoluteValue
        val start = seed % size
        val pickCount = ((seed % 3) + 1).coerceAtMost(size)

        return buildSet {
            repeat(pickCount) { offset ->
                add(restaurantIds[(start + offset) % size])
            }
        }
    }
}
