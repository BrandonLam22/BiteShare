package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.SignupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unit tests for SignupViewModel (presentation layer).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    @Test
    fun onSignupClickedWithValidInputReturnsTrue() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = SignupViewModel(model, repo)
        vm.username = "NewUser"
        vm.password = "password"
        vm.email = "new@test.com"
        var success = false

        // Act
        vm.onSignupClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertTrue(success)
        assertTrue(model.currentUser != null)
        assertEquals("NewUser", model.currentUser?.username)
    }

    @Test
    fun onSignupClickedWithBlankUsernameReturnsFalse() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = SignupViewModel(model, repo)
        vm.username = ""
        vm.password = "pass"
        vm.email = "a@b.com"
        var success = false

        // Act
        vm.onSignupClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertFalse(success)
        assertTrue(vm.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun onSignupClickedWithInvalidEmailReturnsFalse() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = SignupViewModel(model, repo)
        vm.username = "User"
        vm.password = "pass"
        vm.email = "no-at-sign"
        var success = false

        // Act
        vm.onSignupClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertFalse(success)
        assertTrue(vm.errorMessage?.contains("valid", ignoreCase = true) == true)
    }
}
