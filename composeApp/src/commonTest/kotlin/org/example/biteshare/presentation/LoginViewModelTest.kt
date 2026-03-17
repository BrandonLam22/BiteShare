package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.LoginViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for LoginViewModel (presentation layer).
 * Uses a real Model (backed by MockDB) so we test behaviour without a mock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Test
    fun onLoginClickedWithValidCredentialsReturnsTrue() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = LoginViewModel(model, repo)
        vm.username = "Kevin"
        vm.password = "12345"
        var success = false

        // Act
        vm.onLoginClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertTrue(success)
        assertTrue(model.currentUser != null)
    }

    @Test
    fun onLoginClickedWithInvalidCredentialsReturnsFalse() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = LoginViewModel(model, repo)
        vm.username = "WrongUser"
        vm.password = "WrongPass"
        var success = false

        // Act
        vm.onLoginClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertFalse(success)
        assertFalse(model.currentUser != null)
    }

    @Test
    fun onLoginClickedWithBlankUsernameReturnsFalse() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = LoginViewModel(model, repo)
        vm.username = ""
        vm.password = "12345"
        var success = false

        // Act
        vm.onLoginClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertFalse(success)
        assertTrue(vm.errorMessage?.contains("username", ignoreCase = true) == true)
    }

    @Test
    fun onLoginClickedWithBlankPasswordReturnsFalse() = runMainTest {
        // Arrange
        val model = Model()
        val repo = FakeRepository(model)
        val vm = LoginViewModel(model, repo)
        vm.username = "Kevin"
        vm.password = ""
        var success = false

        // Act
        vm.onLoginClicked { success = true }
        advanceUntilIdle()

        // Assert
        assertFalse(success)
        assertTrue(vm.errorMessage?.contains("password", ignoreCase = true) == true)
    }
}
