package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickFilters
import org.example.biteshare.domain.PickMode
import org.example.biteshare.viewmodel.RecommendsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecommendsViewModelTest {

    @Test
    fun loadAppliesContextFilters() {
        val vm = RecommendsViewModel(FakeRepository())
        val context = PickContext(
            mode = PickMode.ME_ONLY,
            filters = PickFilters(
                location = "Addis Ababa",
                budget = BudgetFilter.BUDGET,
                cuisine = CuisineFilter.DRINK,
                openNowOnly = true,
                minRating = 9.0
            )
        )

        vm.load(context)

        assertEquals(1, vm.uiState.items.size)
        assertEquals("Hope Café", vm.uiState.items.first().name)
    }

    @Test
    fun toggleSavedFlipsSavedStateForTargetItem() {
        val vm = RecommendsViewModel(FakeRepository())
        vm.load(PickContext(mode = PickMode.ME_ONLY))
        val target = vm.uiState.items.first()

        vm.toggleSaved(target.id)

        val updated = vm.uiState.items.first { it.id == target.id }
        assertEquals(!target.isSaved, updated.isSaved)
        assertTrue(vm.uiState.items.isNotEmpty())
        assertFalse(vm.uiState.items.none { it.id == target.id })
    }
}
