package org.example.biteshare.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LoginViewModel {
    // 1. DATA: Move the 'state' here
    // We use 'var ... by' so the View can still observe changes automatically
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    // 2. LOGIC: Move the 'behavior' here
    // The View only knows the user clicked a button; the ViewModel decides what happens
    fun onLoginClicked() {
        if (username.isNotEmpty() && password.isNotEmpty()) {
            println("Logic: Attempting login for $username")
            // Here is where you'd eventually call your Domain/Data layer
        }
    }
}