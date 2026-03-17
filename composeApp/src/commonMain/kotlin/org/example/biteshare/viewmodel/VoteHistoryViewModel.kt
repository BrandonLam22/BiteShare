package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.VoteSession

data class VoteHistoryUiState(
    val sessions: List<VoteSession> = emptyList(),
)

class VoteHistoryViewModel(
    private val model: PickModel,
) {
    var uiState by mutableStateOf(VoteHistoryUiState())
        private set

    private val scope = MainScope()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            val sessions = model.voteSessionsForCurrentUser()
                .sortedByDescending { it.createdAtEpoch }
            uiState = VoteHistoryUiState(sessions = sessions)
        }
    }
}
