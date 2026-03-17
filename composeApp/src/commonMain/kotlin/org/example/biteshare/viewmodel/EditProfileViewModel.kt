package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository

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
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class EditProfileViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(EditProfileUiState())
        private set

    private var originalState: EditProfileUiState? = null
    private val scope = MainScope()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        scope.launch {
            runCatching { repo.getEditableProfile() }
                .onSuccess { profile ->
                    val initialState = EditProfileUiState(
                        username = profile.username,
                        email = profile.email,
                        bio = profile.bio,
                        preferences = profile.preferences.toSet(),
                        foodRestrictions = profile.foodRestrictions.toSet()
                    )
                    uiState = initialState
                    originalState = comparableState(initialState)
                }
                .onFailure { throwable ->
                    uiState = uiState.copy(
                        errorMessage = throwable.message ?: "Unable to load your profile."
                    )
                }
        }
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
            hasChanges = comparableState(uiState) != originalState
        )
    }

    fun saveProfile(onSuccess: () -> Unit = {}) {
        if (uiState.isSaving) return

        val snapshot = uiState
        val savedState = comparableState(snapshot)
        uiState = uiState.copy(isSaving = true, errorMessage = null)
        scope.launch {
            runCatching {
                repo.updateProfile(
                    username = snapshot.username,
                    email = snapshot.email,
                    bio = snapshot.bio,
                    preferences = snapshot.preferences.toList(),
                    foodRestrictions = snapshot.foodRestrictions.toList()
                )
            }.onSuccess {
                originalState = savedState
                uiState = savedState
                onSuccess()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = throwable.message ?: "Unable to save your profile."
                )
            }
        }
    }

    private fun comparableState(state: EditProfileUiState): EditProfileUiState {
        return state.copy(
            preferenceInput = "",
            restrictionInput = "",
            showPreferenceError = false,
            showRestrictionError = false,
            hasChanges = false,
            isSaving = false,
            errorMessage = null
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
