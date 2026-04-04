package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.ChangePasswordViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

    private fun loggedInRepo(): FakeRepository {
        val model = Model()
        model.login("Kevin", "12345")
        return FakeRepository(model)
    }

    @Test
    fun saveRejectsShortPassword() = runMainTest {
        val repo = loggedInRepo()
        val vm = ChangePasswordViewModel(repo)

        vm.updateCurrentPassword("12345")
        vm.updateNewPassword("short")
        vm.updateConfirmPassword("short")
        vm.save()
        advanceUntilIdle()

        assertEquals("Must be at least 8 characters", vm.uiState.newPasswordError)
        assertFalse(vm.uiState.isSaved)
    }

    @Test
    fun saveRejectsPasswordMismatch() = runMainTest {
        val repo = loggedInRepo()
        val vm = ChangePasswordViewModel(repo)

        vm.updateCurrentPassword("12345")
        vm.updateNewPassword("newpassword")
        vm.updateConfirmPassword("different")
        vm.save()
        advanceUntilIdle()

        assertEquals("Passwords do not match", vm.uiState.confirmPasswordError)
        assertFalse(vm.uiState.isSaved)
    }

    @Test
    fun saveRejectsInvalidCurrentPassword() = runMainTest {
        val repo = loggedInRepo()
        val vm = ChangePasswordViewModel(repo)

        vm.updateCurrentPassword("wrong")
        vm.updateNewPassword("newpassword")
        vm.updateConfirmPassword("newpassword")
        vm.save()
        advanceUntilIdle()

        assertNotNull(vm.uiState.currentPasswordError)
        assertFalse(vm.uiState.isSaved)
    }

    @Test
    fun saveUpdatesPasswordWhenValid() = runMainTest {
        val repo = loggedInRepo()
        val vm = ChangePasswordViewModel(repo)

        vm.updateCurrentPassword("12345")
        vm.updateNewPassword("newpassword")
        vm.updateConfirmPassword("newpassword")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.isSaved)
        assertEquals("newpassword", repo.getPassword())
    }
}
