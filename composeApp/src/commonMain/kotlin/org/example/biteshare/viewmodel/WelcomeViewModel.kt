package org.example.biteshare.viewmodel

class WelcomeViewModel {
    // this is where logic for the Welcome Screen will live

    fun onLoginClicked() {
        // This is an "Intent". The user expressed an intention to Login
        println("User wants to login")
    }

    fun onSignupClicked() {
        println("User wants to sign up")
    }
}
