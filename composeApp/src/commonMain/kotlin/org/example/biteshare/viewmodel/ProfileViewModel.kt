package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.model.FakeRepository

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val followingCount: Int = 0,
    val followersCount: Int = 0,
    val notificationsEnabled: Boolean = false,
)

class ProfileViewModel(
    private val repo: FakeRepository? = null,
) {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        loadProfile()
    }

    private fun loadProfile() {
        // TODO: Load from repository/data source
        uiState = ProfileUiState(
            name = "John Cena",
            email = "johncena@gmail.com",
            followingCount = 28,
            followersCount = 53,
            notificationsEnabled = true
        )
    }

    fun toggleNotifications() {
        uiState = uiState.copy(
            notificationsEnabled = !uiState.notificationsEnabled
        )
        // TODO: Persist notification preference
    }

    fun logout() {
        // TODO: Clear user session and navigate to login
    }
}
