package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Model

class SignupViewModel(
    private val model: Model,
    private val repo: BiteShareRepository,) {
    // Localized data for the signup view
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var email by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val scope = MainScope()

    // Returns true if signup succeeded (user created and logged in)
    fun onSignupClicked(onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank() || !email.contains("@")) {
            errorMessage = "Please enter a valid username, email, and password."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            runCatching { repo.signup(username, password, email) }
                .onSuccess { user ->
                    if (user != null) {
                        model.applyAuthenticatedUser(user)
                        onSuccess()
                    } else {
                        errorMessage = "Username or email already exists."
                    }
                }
                .onFailure { t ->
                    println("SIGNUP ERROR: ${t.message}")
                    errorMessage = t.message ?: "Unable to create your account right now. Please try again."
                }
            isLoading = false
        }
    }
}

