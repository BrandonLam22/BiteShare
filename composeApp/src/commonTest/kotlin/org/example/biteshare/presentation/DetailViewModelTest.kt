package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.DetailViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @Test
    fun initLoadsRestaurantAndDetailData() = runMainTest {
        val vm = DetailViewModel(FakeRepository(), "uw1")
        advanceUntilIdle()
        val state = vm.uiState

        assertNotNull(state.restaurant)
        assertNotNull(state.restaurantDetail)
        val restaurant = state.restaurant
        val detail = state.restaurantDetail
        assertEquals("uw1", restaurant.id)
        assertEquals("uw1", detail.restaurantId)
    }

    @Test
    fun averageReviewAndCountAreComputedFromDetailReviews() = runMainTest {
        val repo = FakeRepository()
        val detail = repo.getRestaurantDetailById("p1")
        assertNotNull(detail)
        val expectedCount = detail.reviews.size
        val expectedAverage = detail.reviews.map { it.ratingForAverage() }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        val vm = DetailViewModel(repo, "p1")
        advanceUntilIdle()
        val state = vm.uiState

        assertEquals(expectedCount, state.reviewCount)
        assertEquals(expectedAverage, state.averageReviewScore)
    }

    @Test
    fun toggleSavedFlipsSavedState() = runMainTest {
        val vm = DetailViewModel(FakeRepository(), "b1")
        advanceUntilIdle()
        val before = vm.uiState.restaurant?.isSaved ?: false

        vm.toggleSaved()
        advanceUntilIdle()
        val after = vm.uiState.restaurant?.isSaved ?: false

        assertEquals(!before, after)
    }
}
