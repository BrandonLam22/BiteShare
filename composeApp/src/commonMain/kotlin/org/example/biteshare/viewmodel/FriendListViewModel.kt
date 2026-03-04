package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Friend

data class FriendsListUiState(
    val friends: List<Friend> = emptyList()
)

class FriendsListViewModel(
    private val repo: FakeRepository,
) {
    var uiState by mutableStateOf(FriendsListUiState())
        private set

    init {
        loadFriends()
    }

    private fun loadFriends() {
        val friends = repo.getFriends()
        uiState = uiState.copy(friends = friends)
    }
}