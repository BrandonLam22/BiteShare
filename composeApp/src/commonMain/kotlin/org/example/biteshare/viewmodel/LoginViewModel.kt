package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.domain.Model

class LoginViewModel(private val model: Model) {
    // 1. DATA: Move the 'state' here
    // We use 'var ... by' so the View can still observe changes automatically
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    // 2. LOGIC: Move the 'behavior' here
    // The View only knows the user clicked a button; the ViewModel decides what happens
    fun onLoginClicked(): Boolean {
        if (username.isBlank() || password.isBlank()) return false
            return model.login(username, password)
    }
}
