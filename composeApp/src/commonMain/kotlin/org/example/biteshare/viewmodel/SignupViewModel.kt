package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SignupViewModel {
    // Localized data for the signup view
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var email by mutableStateOf("")

    // Process user intention
    fun onSignupClicked() {
        if (username.isNotBlank() && email.contains("@")) {
            println("ViewModel Logic: Creating account for $username with $email")
            // Future step: send data down to the Domain layer
        }
    }
}
