package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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

    private val scope = MainScope()
    private var currentUserId: String? = null

    init {
        scope.launch {
            val allFriends = model.friends()
            val selectedFriends = allFriends.filter { it.id in context.selectedFriendIds }
            val participants = if (selectedFriends.isNotEmpty()) selectedFriends else allFriends
            val recommended = if (candidateRestaurants.isNotEmpty()) {
                candidateRestaurants
            } else {
                model.recommend(context)
            }
            val recommendedIds = recommended.map { it.id }.toSet()
            val myUserId = model.currentUserId()?.takeIf { it.isNotBlank() }
            currentUserId = myUserId
            val selectionUserIds = buildSet {
                addAll(participants.map { it.id })
                myUserId?.let { add(it) }
            }
            val selectionsByUser = if (selectionUserIds.isEmpty() || recommendedIds.isEmpty()) {
                emptyMap()
            } else {
                model.restaurantSelectionsByUserIds(selectionUserIds)
                    .mapValues { (_, ids) -> ids.filter { it in recommendedIds }.toSet() }
            }
            val myVotes = myUserId?.let { selectionsByUser[it].orEmpty() }.orEmpty()
            val friendVotes = participants
                .filter { it.id != myUserId }
                .associate { friend -> friend.id to selectionsByUser[friend.id].orEmpty() }

            uiState = VoteWithFriendsUiState(
                friends = participants,
                restaurants = recommended,
                myVotes = myVotes,
                friendVotes = friendVotes
            )
        }
    }

    fun toggleMyVote(restaurantId: String) {
        if (uiState.isVotingClosed) return
        val oldVotes = uiState.myVotes
        val newVotes = if (restaurantId in oldVotes) oldVotes - restaurantId else oldVotes + restaurantId
        uiState = uiState.copy(myVotes = newVotes)
        val myUserId = currentUserId ?: return
        val selected = restaurantId in newVotes
        scope.launch {
            model.setRestaurantSelection(myUserId, restaurantId, selected)
        }
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

}
