package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.EditProfileViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {

    private fun loggedInRepo(): FakeRepository {
        val model = Model()
        model.login("Kevin", "12345")
        return FakeRepository(model)
    }

    @Test
    fun loadProfilePopulatesState() = runMainTest {
        val repo = loggedInRepo()
        val vm = EditProfileViewModel(repo)
        advanceUntilIdle()

        val profile = repo.getEditableProfile()
        assertEquals(profile.username, vm.uiState.username)
        assertEquals(profile.email, vm.uiState.email)
    }

    @Test
    fun addPreferenceAndRestrictionValidatesInput() = runMainTest {
        val repo = loggedInRepo()
        val vm = EditProfileViewModel(repo)
        advanceUntilIdle()

        vm.addPreference()
        assertTrue(vm.uiState.showPreferenceError)

        vm.updatePreferenceInput("pizza")
        vm.addPreference()
        assertTrue(vm.uiState.preferences.any { it.equals("Pizza", ignoreCase = true) })
        assertEquals("", vm.uiState.preferenceInput)
        assertTrue(vm.uiState.hasChanges)

        vm.addRestriction()
        assertTrue(vm.uiState.showRestrictionError)

        vm.updateRestrictionInput("gluten-free")
        vm.addRestriction()
        assertTrue(vm.uiState.foodRestrictions.any { it.equals("Gluten-free", ignoreCase = true) })
        assertEquals("", vm.uiState.restrictionInput)
        assertTrue(vm.uiState.hasChanges)
    }

    @Test
    fun saveProfileClearsChangesAndPersists() = runMainTest {
        val repo = loggedInRepo()
        val vm = EditProfileViewModel(repo)
        advanceUntilIdle()

        vm.updateUsername("UpdatedUser")
        vm.updateBio("New bio")
        assertTrue(vm.uiState.hasChanges)

        var called = false
        vm.saveProfile { called = true }
        advanceUntilIdle()

        assertTrue(called)
        assertFalse(vm.uiState.hasChanges)
        assertFalse(vm.uiState.isSaving)
        assertEquals("UpdatedUser", repo.getEditableProfile().username)
    }
}
