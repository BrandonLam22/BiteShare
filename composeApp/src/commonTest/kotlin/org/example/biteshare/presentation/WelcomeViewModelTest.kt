package org.example.biteshare.presentation

import org.example.biteshare.domain.Model
import org.example.biteshare.viewmodel.WelcomeViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for WelcomeViewModel (presentation layer).
 * Verifies isUserLoggedIn and that navigation callbacks are invoked.
 */
class WelcomeViewModelTest {

    @Test
    fun isUserLoggedInReturnsFalseWhenNoCurrentUser() {
        // Arrange
        val model = Model()
        val vm = WelcomeViewModel(model)

        // Act & Assert
        assertFalse(vm.isUserLoggedIn)
    }

    @Test
    fun isUserLoggedInReturnsTrueAfterLogin() {
        // Arrange
        val model = Model()
        model.login("Kevin", "12345")
        val vm = WelcomeViewModel(model)

        // Act & Assert
        assertTrue(vm.isUserLoggedIn)
    }

    @Test
    fun onLoginClickedInvokesNavigationCallback() {
        // Arrange
        val model = Model()
        val vm = WelcomeViewModel(model)
        var callbackInvoked = false

        // Act
        vm.onLoginClicked { callbackInvoked = true }

        // Assert
        assertTrue(callbackInvoked)
    }

    @Test
    fun onSignupClickedInvokesNavigationCallback() {
        // Arrange
        val model = Model()
        val vm = WelcomeViewModel(model)
        var callbackInvoked = false

        // Act
        vm.onSignupClicked { callbackInvoked = true }

        // Assert
        assertTrue(callbackInvoked)
    }
}