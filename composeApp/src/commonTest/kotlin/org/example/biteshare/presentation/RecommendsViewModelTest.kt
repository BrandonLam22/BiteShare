package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.PickMockDB
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickFilters
import org.example.biteshare.domain.PickMode
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.RecommendsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendsViewModelTest {

    @Test
    fun loadAppliesContextFilters() = runMainTest {
        val restaurants = listOf(
            Restaurant("p1", "Pizza Plaza", "Pizza", "$11.50", "19-29 min", 4.0, location = "Addis Ababa"),
            Restaurant("b1", "Burger Town", "Burgers", "$9.25", "20-30 min", 4.4, location = "Addis Ababa"),
            Restaurant("c1", "Cafe One", "Coffee", "$5.20", "10-15 min", 4.9, location = "Waterloo"),
        )
        val vm = RecommendsViewModel(PickMockDB(restaurants = restaurants))
        val context = PickContext(
            mode = PickMode.ME_ONLY,
            filters = PickFilters(
                location = "Addis Ababa",
                budget = BudgetFilter.MID,
                cuisine = CuisineFilter.PIZZA,
                minRating = 8.0
            )
        )

        vm.load(context)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.items.size)
        assertEquals("Pizza Plaza", vm.uiState.items.first().name)
    }

    @Test
    fun toggleSavedFlipsSavedStateForTargetItem() = runMainTest {
        val vm = RecommendsViewModel(PickMockDB())
        vm.load(PickContext(mode = PickMode.ME_ONLY))
        advanceUntilIdle()
        val target = vm.uiState.items.first()

        vm.toggleSaved(target.id)

        val updated = vm.uiState.items.first { it.id == target.id }
        assertEquals(!target.isSaved, updated.isSaved)
        assertTrue(vm.uiState.items.isNotEmpty())
        assertFalse(vm.uiState.items.none { it.id == target.id })
    }
}
