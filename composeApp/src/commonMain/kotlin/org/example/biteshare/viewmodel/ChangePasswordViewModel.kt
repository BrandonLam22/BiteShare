package org.example.biteshare.viewmodel


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSaved: Boolean = false
)

class ChangePasswordViewModel(private val repo: FakeRepository) {
    var uiState by mutableStateOf(ChangePasswordUiState())
        private set

    fun updateCurrentPassword(value: String) {
        uiState = uiState.copy(currentPassword = value, currentPasswordError = null)
    }

    fun updateNewPassword(value: String) {
        uiState = uiState.copy(newPassword = value, newPasswordError = null)
    }

    fun updateConfirmPassword(value: String) {
        uiState = uiState.copy(confirmPassword = value, confirmPasswordError = null)
    }

    fun save(): Boolean {
        var valid = true

        if (uiState.currentPassword != repo.getPassword()) {
            uiState = uiState.copy(currentPasswordError = "Incorrect current password")
            valid = false
        }
        if (uiState.newPassword.length < 8) {
            uiState = uiState.copy(newPasswordError = "Must be at least 8 characters")
            valid = false
        }
        if (uiState.newPassword != uiState.confirmPassword) {
            uiState = uiState.copy(confirmPasswordError = "Passwords do not match")
            valid = false
        }

        if (valid) {
            repo.updatePassword(uiState.newPassword)
            uiState = uiState.copy(isSaved = true)
        }
        return valid
    }
}