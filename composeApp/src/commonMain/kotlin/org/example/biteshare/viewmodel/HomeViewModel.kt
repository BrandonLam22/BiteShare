package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.data.PickRepository
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.FriendAddResult
import org.example.biteshare.domain.FriendRequestResult
import org.example.biteshare.domain.PopularItem

enum class HomeSearchMode(val label: String) {
    RESTAURANT("Restaurant"),
    FRIEND("Friend"),
}

data class HomeUiState(
    val greeting: String = "Hi, Kevin",
    val categories: List<CategoryItem> = emptyList(),
    val popularDishes: List<PopularItem> = emptyList(),
    val popularDrinks: List<PopularItem> = emptyList(),
    val searchMode: HomeSearchMode = HomeSearchMode.RESTAURANT,
    val searchQuery: String = "",
    val friendSearchResults: List<Friend> = emptyList(),
    val currentFriendIds: Set<String> = emptySet(),
    val friendActionMessage: String? = null,
    val isFriendActionLoading: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val outgoingRequestUserIds: Set<String> = emptySet(),
)

class HomeViewModel(
    private val repo: BiteShareRepository,
    private val pickRepo: PickRepository,
) {
    var uiState by mutableStateOf(HomeUiState())
        private set

    private val scope = MainScope()

    init {
        loadHome()
    }

    fun loadHome(userName: String = "John") {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        scope.launch {
            runCatching {
                repo.getHomeFeed(userName)
            }.onSuccess { feed ->
                uiState = uiState.copy(
                    greeting = feed.greeting,
                    categories = feed.categories,
                    popularDishes = feed.popularDishes,
                    popularDrinks = feed.popularDrinks,
                    currentFriendIds = pickRepo.friends().map { it.id }.toSet(),
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { t ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load homepage data.",
                )
            }
        }
    }

    fun retryLoad() {
        loadHome()
    }

    fun setSearchMode(mode: HomeSearchMode) {
        uiState = uiState.copy(
            searchMode = mode,
            searchQuery = "",
            friendSearchResults = emptyList(),
            friendActionMessage = null,
        )
        if (mode == HomeSearchMode.FRIEND) {
            scope.launch {
                refreshFriendIds()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        uiState = uiState.copy(
            searchQuery = query,
            friendActionMessage = if (uiState.searchMode == HomeSearchMode.FRIEND) null else uiState.friendActionMessage
        )
        if (uiState.searchMode == HomeSearchMode.FRIEND && query.isBlank()) {
            uiState = uiState.copy(friendSearchResults = emptyList())
        }
    }

    fun searchFriends() {
        scope.launch {
            refreshFriendIds()
            refreshOutgoingRequestIds()

            val query = uiState.searchQuery.trim()
            if (query.isBlank()) {
                uiState = uiState.copy(
                    friendSearchResults = emptyList(),
                    friendActionMessage = "Please enter a name or user id."
                )
                return@launch
            }

            val results = pickRepo.searchUsers(query)
            uiState = uiState.copy(
                friendSearchResults = results,
                friendActionMessage = if (results.isEmpty()) "No users found." else null
            )
        }
    }

    fun toggleFriend(friend: Friend) {
        scope.launch {
            val friendId = friend.id.trim()
            if (friendId.isBlank()) return@launch

            uiState = uiState.copy(isFriendActionLoading = true, friendActionMessage = null)
            refreshFriendIds()

            val currentlyFriend = friendId in uiState.currentFriendIds
            if (currentlyFriend) {
                val removed = pickRepo.removeFriend(friendId)
                refreshFriendIds()
                refreshFriendSearchResults()
                uiState = uiState.copy(
                    isFriendActionLoading = false,
                    friendActionMessage = if (removed) {
                        "${friend.name} was removed from your friends."
                    } else {
                        "Unable to remove ${friend.name} right now."
                    }
                )
            } else {
                val result = pickRepo.sendFriendRequest(friendId)
                refreshFriendIds()
                refreshOutgoingRequestIds()
                refreshFriendSearchResults()
                uiState = uiState.copy(
                    isFriendActionLoading = false,
                    friendActionMessage = when (result) {
                        FriendRequestResult.REQUEST_SENT -> "Friend request sent to ${friend.name}."
                        FriendRequestResult.EMPTY_INPUT -> "Please enter a name or user id."
                        FriendRequestResult.SELF_NOT_ALLOWED -> "You cannot add yourself."
                        FriendRequestResult.ALREADY_FRIENDS -> "${friend.name} is already in your friends list."
                        FriendRequestResult.ALREADY_PENDING -> "A friend request is already pending with ${friend.name}."
                        FriendRequestResult.NOT_FOUND -> "That user could not be found."
                        FriendRequestResult.ERROR -> "Unable to send request to ${friend.name} right now."
                    }
                )
            }
        }
    }

    private suspend fun refreshFriendIds() {
        uiState = uiState.copy(currentFriendIds = pickRepo.friends().map { it.id }.toSet())
    }

    private suspend fun refreshFriendSearchResults() {
        val query = uiState.searchQuery.trim()
        if (uiState.searchMode != HomeSearchMode.FRIEND || query.isBlank()) {
            uiState = uiState.copy(friendSearchResults = emptyList())
            return
        }
        uiState = uiState.copy(friendSearchResults = pickRepo.searchUsers(query))
    }

    private suspend fun refreshOutgoingRequestIds() {
        uiState = uiState.copy(
            outgoingRequestUserIds = pickRepo.outgoingFriendRequests()
                .map { it.receiverId }
                .toSet()
        )
    }

}
