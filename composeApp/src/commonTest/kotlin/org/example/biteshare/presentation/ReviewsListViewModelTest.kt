package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.Review
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.ReviewsListViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsListViewModelTest {

    private fun loggedInRepoWithReview(): FakeRepository {
        val model = Model()
        model.login("Kevin", "12345")
        val userId = model.currentUser?.id ?: "user"
        model.addReview(
            Review(
                id = "review_1",
                restaurantName = "Joe's Pizza",
                tags = listOf("taste_good"),
                content = "Nice",
                rating = 8,
                userId = userId,
            )
        )
        return FakeRepository(model)
    }

    @Test
    fun initLoadsUserReviews() = runMainTest {
        val repo = loggedInRepoWithReview()
        val vm = ReviewsListViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.reviews.any { it.id == "review_1" })
    }

    @Test
    fun editAndDeleteReviewUpdateList() = runMainTest {
        val repo = loggedInRepoWithReview()
        val vm = ReviewsListViewModel(repo)
        advanceUntilIdle()

        vm.editReview("review_1", 6, "Updated")
        advanceUntilIdle()
        val updated = vm.uiState.reviews.first { it.id == "review_1" }
        assertEquals(6, updated.rating)
        assertEquals("Updated", updated.content)

        vm.deleteReview("review_1")
        advanceUntilIdle()
        assertTrue(vm.uiState.reviews.none { it.id == "review_1" })
    }
}
