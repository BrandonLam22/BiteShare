package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.SavedViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private fun loggedInRepo(): FakeRepository {
        val model = Model()
        model.login("Kevin", "12345")
        return FakeRepository(model)
    }

    @Test
    fun initLoadsSavedRestaurants() = runMainTest {
        val repo = FakeRepository()
        val vm = SavedViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.savedRestaurants.isNotEmpty())
    }

    @Test
    fun setSortByNameSortsRestaurants() = runMainTest {
        val repo = FakeRepository()
        val vm = SavedViewModel(repo)
        advanceUntilIdle()

        vm.setSortBy("name")

        val expected = vm.uiState.savedRestaurants.sortedBy { it.name }
        assertEquals(expected, vm.uiState.savedRestaurants)
    }

    @Test
    fun toggleSavedUpdatesList() = runMainTest {
        val repo = loggedInRepo()
        val vm = SavedViewModel(repo)
        advanceUntilIdle()

        val targetId = "s1"
        assertFalse(vm.uiState.savedRestaurants.any { it.id == targetId })

        vm.toggleSaved(targetId)
        advanceUntilIdle()
        assertTrue(vm.uiState.savedRestaurants.any { it.id == targetId })

        vm.toggleSaved(targetId)
        advanceUntilIdle()
        assertFalse(vm.uiState.savedRestaurants.any { it.id == targetId })
    }
}
