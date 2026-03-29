package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Friend

// --- UI MODELS ---

data class FriendDetails(
    val name: String,
    val preferences: List<String>,
    val restrictions: List<String>,
    val reviews: List<FriendReview>,
    val savedRestaurants: List<String>
)

data class FriendReview(
    val restaurantName: String,
    val rating: Double,
    val comment: String
)

data class FriendsListUiState(
    val friends: List<Friend> = emptyList(),
    val selectedFriendDetails: FriendDetails? = null,
    val isLoadingDetails: Boolean = false
)

// --- VIEWMODEL ---

class FriendsListViewModel(
    private val repo: BiteShareRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    var uiState by mutableStateOf(FriendsListUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            try {
                val friends = repo.friends()
                uiState = uiState.copy(friends = friends)
            } catch (e: Exception) {
                println("Error loading friends: ${e.message}")
            }
        }
    }

    fun loadFriendDetails(friendId: String) {
        scope.launch {
            uiState = uiState.copy(isLoadingDetails = true)

            try {
                val details = repo.getFriendDetails(friendId)
                uiState = uiState.copy(
                    selectedFriendDetails = details,
                    isLoadingDetails = false
                )
            } catch (e: Exception) {
                println("Error loading friend details: ${e.message}")
                uiState = uiState.copy(isLoadingDetails = false)
            }
        }
    }

    fun clearSelectedFriend() {
        uiState = uiState.copy(selectedFriendDetails = null)
    }
}