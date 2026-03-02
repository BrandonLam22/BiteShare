package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.*
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickMode

data class PickForMeUiState(
    val mode: PickMode = PickMode.ME_ONLY,
    val friends: List<Friend> = emptyList(),
    val selectedFriendIds: Set<String> = emptySet(),
)

class PickForMeViewModel(
    private val repo: FakeRepository,
) {
    var uiState by mutableStateOf(
        PickForMeUiState(
            mode = PickMode.ME_ONLY,
            friends = repo.friends(),
        )
    )
        private set

    fun setMode(mode: PickMode) {
        uiState = uiState.copy(
            mode = mode,
            selectedFriendIds = emptySet()
        )
    }

    fun toggleFriend(friendId: String) {
        val cur = uiState.selectedFriendIds
        uiState = uiState.copy(
            selectedFriendIds = if (cur.contains(friendId)) cur - friendId else cur + friendId
        )
    }

    fun buildPickContext(): PickContext =
        PickContext(mode = uiState.mode, selectedFriendIds = uiState.selectedFriendIds)
}
