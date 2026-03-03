package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository

data class EditProfileUiState(
    val username: String = "",
    val email: String = "",
    val bio: String = "",
    val preferences: Set<String> = emptySet(),
    val foodRestrictions: Set<String> = emptySet(),
    val preferenceInput: String = "",
    val restrictionInput: String = "",
    val showPreferenceError: Boolean = false,
    val showRestrictionError: Boolean = false,
    val hasChanges: Boolean = false
)

class EditProfileViewModel(
    private val repo: FakeRepository,
) {
    var uiState by mutableStateOf(EditProfileUiState())
        private set

    private var originalState: EditProfileUiState? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val profile = repo.getEditableProfile()
        val initialState = EditProfileUiState(
            username = profile.username,
            email = profile.email,
            bio = profile.bio,
            preferences = profile.preferences.toSet(),
            foodRestrictions = profile.foodRestrictions.toSet()
        )
        uiState = initialState
        originalState = initialState
    }

    fun updateUsername(value: String) {
        uiState = uiState.copy(username = value)
        checkForChanges()
    }

    fun updateEmail(value: String) {
        uiState = uiState.copy(email = value)
        checkForChanges()
    }

    fun updateBio(value: String) {
        uiState = uiState.copy(bio = value)
        checkForChanges()
    }

    // Preference methods
    fun updatePreferenceInput(value: String) {
        uiState = uiState.copy(
            preferenceInput = value,
            showPreferenceError = false
        )
    }

    fun addPreference() {
        val input = uiState.preferenceInput.trim()

        if (input.isEmpty()) {
            uiState = uiState.copy(showPreferenceError = true)
            return
        }

        if (uiState.preferences.any { it.equals(input, ignoreCase = true) }) {
            uiState = uiState.copy(showPreferenceError = true)
            return
        }

        val newPreferences = uiState.preferences + input.capitalize()
        uiState = uiState.copy(
            preferences = newPreferences,
            preferenceInput = "",
            showPreferenceError = false
        )
        checkForChanges()
    }

    fun removePreference(preference: String) {
        val newPreferences = uiState.preferences - preference
        uiState = uiState.copy(preferences = newPreferences)
        checkForChanges()
    }

    // Food restriction methods
    fun updateRestrictionInput(value: String) {
        uiState = uiState.copy(
            restrictionInput = value,
            showRestrictionError = false
        )
    }

    fun addRestriction() {
        val input = uiState.restrictionInput.trim()

        if (input.isEmpty()) {
            uiState = uiState.copy(showRestrictionError = true)
            return
        }

        if (uiState.foodRestrictions.any { it.equals(input, ignoreCase = true) }) {
            uiState = uiState.copy(showRestrictionError = true)
            return
        }

        val newRestrictions = uiState.foodRestrictions + input.capitalize()
        uiState = uiState.copy(
            foodRestrictions = newRestrictions,
            restrictionInput = "",
            showRestrictionError = false
        )
        checkForChanges()
    }

    fun removeRestriction(restriction: String) {
        val newRestrictions = uiState.foodRestrictions - restriction
        uiState = uiState.copy(foodRestrictions = newRestrictions)
        checkForChanges()
    }

    private fun checkForChanges() {
        uiState = uiState.copy(
            hasChanges = uiState.copy(
                preferenceInput = "",
                restrictionInput = "",
                showPreferenceError = false,
                showRestrictionError = false
            ) != originalState
        )
    }

    fun saveProfile() {
        repo.updateProfile(
            username = uiState.username,
            email = uiState.email,
            bio = uiState.bio,
            preferences = uiState.preferences.toList(),
            foodRestrictions = uiState.foodRestrictions.toList()
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}