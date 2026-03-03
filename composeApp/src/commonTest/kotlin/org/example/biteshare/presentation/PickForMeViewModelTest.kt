package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.PickMode
import org.example.biteshare.viewmodel.PickForMeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PickForMeViewModelTest {

    @Test
    fun initLoadsFriendsLocationsAndPreview() {
        val vm = PickForMeViewModel(FakeRepository())

        assertTrue(vm.uiState.friends.isNotEmpty())
        assertTrue(vm.uiState.locations.contains("Any"))
        assertTrue(vm.uiState.resultPreviewCount > 0)
    }

    @Test
    fun setModeResetsSelectedFriends() {
        val vm = PickForMeViewModel(FakeRepository())
        vm.setMode(PickMode.WITH_FRIENDS)
        vm.toggleFriend("alex")
        assertTrue(vm.uiState.selectedFriendIds.contains("alex"))

        vm.setMode(PickMode.ME_ONLY)

        assertTrue(vm.uiState.selectedFriendIds.isEmpty())
    }

    @Test
    fun filterChangesAreIncludedInPickContext() {
        val vm = PickForMeViewModel(FakeRepository())

        vm.setLocation("Addis Ababa")
        vm.setBudget(BudgetFilter.BUDGET)
        vm.setCuisine(CuisineFilter.DRINK)
        vm.setOpenNowOnly(true)
        vm.setMinRating(8.7)
        val context = vm.buildPickContext()

        assertEquals("Addis Ababa", context.filters.location)
        assertEquals(BudgetFilter.BUDGET, context.filters.budget)
        assertEquals(CuisineFilter.DRINK, context.filters.cuisine)
        assertTrue(context.filters.openNowOnly)
        assertEquals(8.5, context.filters.minRating)
    }

    @Test
    fun toggleFriendAddsAndRemovesFriendId() {
        val vm = PickForMeViewModel(FakeRepository())

        vm.toggleFriend("sally")
        assertTrue(vm.uiState.selectedFriendIds.contains("sally"))

        vm.toggleFriend("sally")
        assertFalse(vm.uiState.selectedFriendIds.contains("sally"))
    }
}
