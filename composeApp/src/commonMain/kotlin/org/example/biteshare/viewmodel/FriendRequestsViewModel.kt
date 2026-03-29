package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.FriendRequest

data class FriendRequestsUiState(
    val requests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
)

class FriendRequestsViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(FriendRequestsUiState())
        private set
    private val scope = MainScope()
    init {
        loadRequests()
    }
    fun loadRequests() {
        scope.launch {
            uiState = uiState.copy(isLoading = true, message = null)
            val requests = repo.incomingFriendRequests()
            uiState = uiState.copy(
                requests = requests,
                isLoading = false
            )
        }
    }

    fun acceptRequest(request: FriendRequest) {
        scope.launch {
            val success = repo.acceptFriendRequest(request.id)
            val updatedRequests = repo.incomingFriendRequests()
            uiState = uiState.copy(
                requests = updatedRequests,
                message = if (success) {
                    "${request.senderName} is now your friend."
                } else {
                    "Unable to accept ${request.senderName}'s request right now."
                }
            )
        }
    }

    fun rejectRequest(request: FriendRequest) {
        scope.launch {
            val success = repo.rejectFriendRequest(request.id)
            val updatedRequests = repo.incomingFriendRequests()
            uiState = uiState.copy(
                requests = updatedRequests,
                message = if (success) {
                    "Rejected ${request.senderName}'s request."
                } else {
                    "Unable to reject ${request.senderName}'s request right now."
                }
            )
        }
    }
}