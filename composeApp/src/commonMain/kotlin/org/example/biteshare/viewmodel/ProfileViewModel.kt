package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val friendCount: Int = 0,
    val notificationsEnabled: Boolean = false,
    val incomingRequestCount: Int = 0,
)

class ProfileViewModel(
    private val repo: BiteShareRepository,  // CHANGED: Remove ? = null, make it required
) {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    private val scope = MainScope()

    init {
        loadProfile()
    }

    fun loadProfile() {
        scope.launch {
            val profileData = repo.getProfile()  // CHANGED: Get from repository
            val incomingRequests = repo.incomingFriendRequests()

            uiState = ProfileUiState(
                name = profileData.name,
                email = profileData.email,
                friendCount = profileData.friendCount,
                notificationsEnabled = profileData.notificationsEnabled,
                incomingRequestCount = incomingRequests.size
            )
        }
    }

    fun toggleNotifications() {
        val newValue = !uiState.notificationsEnabled  // CHANGED: Store in variable
        uiState = uiState.copy(notificationsEnabled = newValue)
        scope.launch {
            repo.updateNotificationPreference(newValue)  // CHANGED: Persist to repository
        }
    }

    fun logout() {
        scope.launch {
            repo.logout()
        }
    }
}
