package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.viewmodel.DetailViewModel
import org.example.biteshare.viewmodel.MealTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DetailViewModelTest {

    @Test
    fun initLoadsRestaurantAndDetailData() {
        val vm = DetailViewModel(FakeRepository(), "uw1")
        val state = vm.uiState

        assertNotNull(state.restaurant)
        assertNotNull(state.restaurantDetail)
        assertEquals("uw1", state.restaurant?.id)
        assertEquals("uw1", state.restaurantDetail?.restaurantId)
        assertTrue(state.timeSlots.isNotEmpty())
        assertNotNull(state.selectedTimeSlot)
    }

    @Test
    fun averageReviewAndCountAreComputedFromDetailReviews() {
        val repo = FakeRepository()
        val detail = repo.getRestaurantDetailById("p1")
        val expectedCount = detail?.reviews?.size ?: 0
        val expectedAverage = detail?.reviews?.map { it.rating }?.average()?.takeIf { !it.isNaN() } ?: 0.0

        val vm = DetailViewModel(repo, "p1")
        val state = vm.uiState

        assertEquals(expectedCount, state.reviewCount)
        assertEquals(expectedAverage, state.averageReviewScore)
    }

    @Test
    fun selectMealUpdatesTimeSlotsAndResetsSelectedTime() {
        val vm = DetailViewModel(FakeRepository(), "uw3")

        vm.selectMeal(MealTab.Dinner)

        assertEquals(MealTab.Dinner, vm.uiState.selectedMeal)
        assertEquals(listOf("5:30 PM", "6:00 PM", "6:30 PM", "7:00 PM", "7:30 PM", "8:00 PM"), vm.uiState.timeSlots)
        assertEquals("5:30 PM", vm.uiState.selectedTimeSlot)
    }

    @Test
    fun selectTimeSlotUpdatesOnlySelectedSlot() {
        val vm = DetailViewModel(FakeRepository(), "uw8")
        val target = "11:00 AM"

        vm.selectMeal(MealTab.Brunch)
        vm.selectTimeSlot(target)

        assertEquals(target, vm.uiState.selectedTimeSlot)
        assertTrue(vm.uiState.timeSlots.contains(target))
    }

    @Test
    fun toggleSavedFlipsSavedState() {
        val vm = DetailViewModel(FakeRepository(), "b1")
        val before = vm.uiState.restaurant?.isSaved ?: false

        vm.toggleSaved()
        val after = vm.uiState.restaurant?.isSaved ?: false

        assertEquals(!before, after)
    }
}
