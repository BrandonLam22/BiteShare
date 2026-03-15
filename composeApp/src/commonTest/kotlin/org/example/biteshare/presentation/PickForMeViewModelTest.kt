package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.Model
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

    @Test
    fun friendSearchFiltersVisibleFriends() {
        val vm = PickForMeViewModel(FakeRepository())
        vm.setMode(PickMode.WITH_FRIENDS)

        vm.updateFriendSearchQuery("alex")

        assertTrue(vm.uiState.visibleFriends.isNotEmpty())
        assertTrue(vm.uiState.visibleFriends.all { it.name.contains("alex", ignoreCase = true) })
    }

    @Test
    fun addFriendWithBlankNameShowsMessage() {
        val vm = PickForMeViewModel(FakeRepository())
        vm.setMode(PickMode.WITH_FRIENDS)
        vm.updateNewFriendName("   ")

        vm.addFriend()

        assertTrue(vm.uiState.friendActionMessage != null)
        assertTrue(vm.uiState.friendActionMessage!!.contains("cannot be empty", ignoreCase = true))
    }

    @Test
    fun addFriendWithLoggedInUserUpdatesFriendList() {
        val model = Model()
        model.login("Kevin", "12345")
        val vm = PickForMeViewModel(FakeRepository(model))
        vm.setMode(PickMode.WITH_FRIENDS)
        val beforeCount = vm.uiState.friends.size

        vm.updateNewFriendName("Sam")
        vm.addFriend()

        assertEquals(beforeCount + 1, vm.uiState.friends.size)
        assertTrue(vm.uiState.friends.any { it.name == "Sam" })
    }
}
