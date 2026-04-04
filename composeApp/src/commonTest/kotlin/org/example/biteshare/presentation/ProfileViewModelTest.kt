package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.ProfileViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun initLoadsProfile() = runMainTest {
        val repo = FakeRepository()
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.name.isNotBlank())
        assertTrue(vm.uiState.friendCount >= 0)
        assertEquals(0, vm.uiState.incomingRequestCount)
    }

    @Test
    fun toggleNotificationsPersistsToRepository() = runMainTest {
        val repo = FakeRepository()
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        val initial = vm.uiState.notificationsEnabled
        vm.toggleNotifications()
        advanceUntilIdle()

        assertTrue(vm.uiState.notificationsEnabled != initial)
        val profile = repo.getProfile()
        assertEquals(profile.notificationsEnabled, vm.uiState.notificationsEnabled)
    }
}
