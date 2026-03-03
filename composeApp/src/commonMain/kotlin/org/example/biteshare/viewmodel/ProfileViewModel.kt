package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val friendCount: Int = 0,
    val notificationsEnabled: Boolean = false,
)

class ProfileViewModel(
    private val repo: FakeRepository,  // CHANGED: Remove ? = null, make it required
) {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val profileData = repo.getProfile()  // CHANGED: Get from repository
        uiState = ProfileUiState(
            name = profileData.name,
            email = profileData.email,
            friendCount = profileData.followingCount,
            notificationsEnabled = profileData.notificationsEnabled
        )
    }

    fun toggleNotifications() {
        val newValue = !uiState.notificationsEnabled  // CHANGED: Store in variable
        uiState = uiState.copy(notificationsEnabled = newValue)
        repo.updateNotificationPreference(newValue)  // CHANGED: Persist to repository
    }

    fun logout() {
        // TODO: Clear user session and navigate to login
        println("User logged out")
    }
}