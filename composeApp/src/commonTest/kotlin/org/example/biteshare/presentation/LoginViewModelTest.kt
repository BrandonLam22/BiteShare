package org.example.biteshare.presentation

import org.example.biteshare.domain.Model
import org.example.biteshare.viewmodel.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for LoginViewModel (presentation layer).
 * Uses a real Model (backed by MockDB) so we test behaviour without a mock.
 */
class LoginViewModelTest {

    @Test
    fun onLoginClickedWithValidCredentialsReturnsTrue() {
        // Arrange
        val model = Model()
        val vm = LoginViewModel(model)
        vm.username = "Kevin"
        vm.password = "12345"

        // Act
        val result = vm.onLoginClicked()

        // Assert
        assertTrue(result)
        assertTrue(model.currentUser != null)
    }

    @Test
    fun onLoginClickedWithInvalidCredentialsReturnsFalse() {
        // Arrange
        val model = Model()
        val vm = LoginViewModel(model)
        vm.username = "WrongUser"
        vm.password = "WrongPass"

        // Act
        val result = vm.onLoginClicked()

        // Assert
        assertFalse(result)
    }

    @Test
    fun onLoginClickedWithBlankUsernameReturnsFalse() {
        // Arrange
        val model = Model()
        val vm = LoginViewModel(model)
        vm.username = ""
        vm.password = "12345"

        // Act
        val result = vm.onLoginClicked()

        // Assert
        assertFalse(result)
    }

    @Test
    fun onLoginClickedWithBlankPasswordReturnsFalse() {
        // Arrange
        val model = Model()
        val vm = LoginViewModel(model)
        vm.username = "Kevin"
        vm.password = ""

        // Act
        val result = vm.onLoginClicked()

        // Assert
        assertFalse(result)
    }
}