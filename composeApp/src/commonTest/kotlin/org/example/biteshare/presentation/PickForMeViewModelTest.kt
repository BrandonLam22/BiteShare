package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.DistanceFilter
import org.example.biteshare.domain.PickMode
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.PickForMeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PickForMeViewModelTest {

    @Test
    fun initLoadsFriendsLocationsAndPreview() = runMainTest {
        val vm = PickForMeViewModel(FakeRepository())
        advanceUntilIdle()
        advanceTimeBy(300)
        advanceUntilIdle()

        assertTrue(vm.uiState.friends.isNotEmpty())
        assertTrue(vm.uiState.resultPreviewCount > 0)
    }

    @Test
    fun setModeResetsSelectedFriends() = runMainTest {
        val vm = PickForMeViewModel(FakeRepository())
        vm.setMode(PickMode.WITH_FRIENDS)
        vm.toggleFriend("alex")
        assertTrue(vm.uiState.selectedFriendIds.contains("alex"))

        vm.setMode(PickMode.ME_ONLY)

        assertTrue(vm.uiState.selectedFriendIds.isEmpty())
    }

    @Test
    fun filterChangesAreIncludedInPickContext() = runMainTest {
        val vm = PickForMeViewModel(FakeRepository())

        vm.setLocation("Addis Ababa")
        vm.setDistance(DistanceFilter.FIVE_KM)
        vm.setBudget(BudgetFilter.BUDGET)
        vm.setCuisine(CuisineFilter.CHINESE)
        vm.setMinRating(8.7)
        val context = vm.buildPickContext()

        assertEquals("Addis Ababa", context.filters.location)
        assertEquals(DistanceFilter.FIVE_KM, context.filters.distance)
        assertEquals(BudgetFilter.BUDGET, context.filters.budget)
        assertEquals(CuisineFilter.CHINESE, context.filters.cuisine)
        assertEquals(8.5, context.filters.minRating)
    }

    @Test
    fun toggleFriendAddsAndRemovesFriendId() = runMainTest {
        val vm = PickForMeViewModel(FakeRepository())

        vm.toggleFriend("sally")
        assertTrue(vm.uiState.selectedFriendIds.contains("sally"))

        vm.toggleFriend("sally")
        assertFalse(vm.uiState.selectedFriendIds.contains("sally"))
    }
}
