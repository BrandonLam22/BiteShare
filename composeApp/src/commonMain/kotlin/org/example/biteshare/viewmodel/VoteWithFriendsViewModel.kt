package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession

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
    private val pollIntervalMs: Long = DEFAULT_VOTE_POLL_MS,
) {
    var uiState by mutableStateOf(VoteWithFriendsUiState())
        private set

    private val scope = MainScope()
    private var currentUserId: String? = null
    val sessionId: String = "vote_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    private var participants: List<Friend> = emptyList()
    private var recommended: List<Restaurant> = emptyList()
    private var recommendedIds: Set<String> = emptySet()
    private var pollJob: Job? = null

    init {
        scope.launch {
            val allFriends = model.friends()
            val selectedFriends = allFriends.filter { it.id in context.selectedFriendIds }
            participants = if (selectedFriends.isNotEmpty()) selectedFriends else allFriends
            recommended = if (candidateRestaurants.isNotEmpty()) {
                candidateRestaurants
            } else {
                model.recommend(context)
            }
            recommendedIds = recommended.map { it.id }.toSet()
            val myUserId = model.currentUserId()?.takeIf { it.isNotBlank() }
            currentUserId = myUserId
            val selectionUserIds = buildSet {
                addAll(participants.map { it.id })
                myUserId?.let { add(it) }
            }
            val selectionsByUser = if (selectionUserIds.isEmpty()) {
                emptyMap()
            } else {
                model.restaurantSelectionsByUserIds(selectionUserIds)
            }
            val myAllVotes = myUserId?.let { selectionsByUser[it].orEmpty() }.orEmpty()
            val myVotes = myAllVotes.filter { it in recommendedIds }.toSet()
            val friendVotes = participants
                .filter { it.id != myUserId }
                .associate { friend ->
                    friend.id to selectionsByUser[friend.id].orEmpty().filter { it in recommendedIds }.toSet()
                }
            uiState = VoteWithFriendsUiState(
                friends = participants,
                restaurants = recommended,
                myVotes = myVotes,
                friendVotes = friendVotes
            )

            val participantMap = linkedMapOf<String, VoteParticipant>()
            participants.forEach { friend ->
                if (friend.id.isNotBlank()) {
                    participantMap[friend.id] = VoteParticipant(id = friend.id, name = friend.name)
                }
            }
            myUserId?.let { id ->
                if (id.isNotBlank()) {
                    participantMap[id] = VoteParticipant(id = id, name = "You", isSelf = true)
                }
            }
            val voteParticipants = participantMap.values.toList()
            val title = if (participants.isEmpty()) {
                "Group vote"
            } else {
                "Vote with " + participants.joinToString(", ") { it.name }
            }
            val votesByUserId = buildMap {
                if (myUserId != null) put(myUserId, myVotes)
                friendVotes.forEach { (id, votes) -> put(id, votes) }
            }
            model.createVoteSession(
                session = VoteSession(
                    id = sessionId,
                    title = title,
                    createdAtEpoch = System.currentTimeMillis(),
                    participants = voteParticipants,
                    restaurants = recommended,
                    votesByUserId = votesByUserId
                )
            )

            refreshVotesFromSession(refreshClosed = true)
            startPolling()
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
        scope.launch {
            model.updateVoteSessionVotes(sessionId, myUserId, newVotes)
        }
    }

    fun finishVoting() {
        if (uiState.isVotingClosed) return
        uiState = uiState.copy(isVotingClosed = true)
        pollJob?.cancel()
        val closedAt = System.currentTimeMillis()
        scope.launch {
            model.closeVoteSession(sessionId, closedAt)
        }
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

    private fun startPolling() {
        if (pollIntervalMs <= 0) return
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(pollIntervalMs)
                refreshVotesFromSession()
            }
        }
    }

    private suspend fun refreshVotesFromSession(refreshClosed: Boolean = false) {
        val votesByUser = model.voteSessionVotes(sessionId)
        applyVotesFromSession(votesByUser)
        if (refreshClosed) {
            val session = model.voteSessionById(sessionId)
            uiState = uiState.copy(isVotingClosed = session?.isClosed == true)
        }
    }

    private fun applyVotesFromSession(votesByUser: Map<String, Set<String>>) {
        val filteredVotes = votesByUser.mapValues { (_, votes) ->
            votes.filter { it in recommendedIds }.toSet()
        }
        val myVotes = currentUserId?.let { filteredVotes[it].orEmpty() } ?: uiState.myVotes
        val friendVotes = participants
            .associate { friend -> friend.id to filteredVotes[friend.id].orEmpty() }
        uiState = uiState.copy(
            myVotes = myVotes,
            friendVotes = friendVotes
        )
    }

}

private const val DEFAULT_VOTE_POLL_MS = 3000L
