package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.domain.Model

class SignupViewModel(private val model: Model) {
    // Localized data for the signup view
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var email by mutableStateOf("")

    // Returns true if signup succeeded (user created and logged in)
    fun onSignupClicked(): Boolean {
        if (username.isBlank() || password.isBlank() || !email.contains("@")) return false
        return model.signup(username, password, email)
    }
}

