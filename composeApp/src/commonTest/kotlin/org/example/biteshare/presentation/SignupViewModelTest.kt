package org.example.biteshare.presentation

import org.example.biteshare.domain.Model
import org.example.biteshare.viewmodel.SignupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for SignupViewModel (presentation layer).
 */
class SignupViewModelTest {

    @Test
    fun onSignupClickedWithValidInputReturnsTrue() {
        // Arrange
        val model = Model()
        val vm = SignupViewModel(model)
        vm.username = "NewUser"
        vm.password = "password"
        vm.email = "new@test.com"

        // Act
        val result = vm.onSignupClicked()

        // Assert
        assertTrue(result)
        assertTrue(model.currentUser != null)
        assertEquals("NewUser", model.currentUser?.username)
    }

    @Test
    fun onSignupClickedWithBlankUsernameReturnsFalse() {
        // Arrange
        val model = Model()
        val vm = SignupViewModel(model)
        vm.username = ""
        vm.password = "pass"
        vm.email = "a@b.com"

        // Act
        val result = vm.onSignupClicked()

        // Assert
        assertFalse(result)
    }

    @Test
    fun onSignupClickedWithInvalidEmailReturnsFalse() {
        // Arrange
        val model = Model()
        val vm = SignupViewModel(model)
        vm.username = "User"
        vm.password = "pass"
        vm.email = "no-at-sign"

        // Act
        val result = vm.onSignupClicked()

        // Assert
        assertFalse(result)
    }
}