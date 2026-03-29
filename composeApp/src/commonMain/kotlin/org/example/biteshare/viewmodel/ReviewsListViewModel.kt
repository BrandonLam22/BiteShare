package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Review

data class ReviewsListUiState(
    val reviews: List<Review> = emptyList()
)

class ReviewsListViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(ReviewsListUiState())
        private set

    // 👇 simple coroutine scope (since you're not using Android ViewModel)
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            loadReviews()
        }
    }

    suspend fun loadReviews() {
        val reviews = repo.getUserReviews()
        uiState = uiState.copy(reviews = reviews)
    }

    fun deleteReview(reviewId: String) {
        scope.launch {
            repo.deleteReview(reviewId)
            loadReviews()
        }
    }

    fun editReview(reviewId: String, newRating: Int, newComment: String) {
        scope.launch {
            repo.editReview(reviewId, newRating, newComment)
            loadReviews()
        }
    }
}