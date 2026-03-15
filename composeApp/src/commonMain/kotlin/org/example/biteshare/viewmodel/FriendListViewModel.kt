package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Friend

data class FriendsListUiState(
    val friends: List<Friend> = emptyList()
)

class FriendsListViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(FriendsListUiState())
        private set

    private val scope = MainScope()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        scope.launch {
            val friends = repo.friends()
            uiState = uiState.copy(friends = friends)
        }
    }
}
