package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Model

class LoginViewModel(
    private val model: Model,
    private val repo: BiteShareRepository) {
    // 1. DATA: Move the 'state' here
    // We use 'var ... by' so the View can still observe changes automatically
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val scope = MainScope()

    // 2. LOGIC: Move the 'behavior' here
    // The View only knows the user clicked a button; the ViewModel decides what happens
    fun onLoginClicked(onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both username and password"
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            runCatching { repo.login(username, password) }
                .onSuccess { user ->
                    if (user != null) {
                        model.applyAuthenticatedUser(user)
                        onSuccess()
                    } else {
                        errorMessage = "Invalid username or password"
                    }
                }
                .onFailure { t ->
                    println("LOGIN ERROR: ${t.message}")
                    errorMessage = t.message ?: "Unable to reach Supabase right now. Please try again."
                }
            isLoading = false
        }
    }
}

