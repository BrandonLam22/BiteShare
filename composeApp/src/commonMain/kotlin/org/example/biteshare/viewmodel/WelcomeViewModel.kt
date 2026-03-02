package org.example.biteshare.viewmodel

import org.example.biteshare.domain.Model

class WelcomeViewModel(private val model: Model) {
    // this is where logic for the Welcome Screen will live

    /**
     * Checks the Model's "Source of Truth" to see if a session already exists.
     * If the user is already logged in, the View should skip this screen and go straight to the Home page
     */
    val isUserLoggedIn: Boolean
        get() = model.currentUser != null

    /**
     * User intended to Log in. We execute any Welcome Screen logic and then trigger the navigation callback.
     * @param navigateToLogin The lambda that the View provides to switch screens
     */
    fun onLoginClicked(navigateToLogin: () -> Unit) {
        // This is an "Intent". The user expressed an intention to Login
        println("User wants to login")
        // Trigger the View's navigation logic
        navigateToLogin()
    }

    /**
     * User intended to sign up
     * @param navigateToSignup The lambda to trigger navigation in the View.
     */
    fun onSignupClicked(navigateToSignup: () -> Unit) {
        println("User wants to sign up")
        // Trigger the View's navigation logic
        navigateToSignup()
    }
}
