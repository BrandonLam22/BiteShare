package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.ReviewTagCatalog

data class DetailUiState(
    val restaurant: Restaurant? = null,
    val restaurantDetail: RestaurantDetail? = null,
    val reviewCount: Int = 0,
    val location: String = "Addis Ababa",
    val priceRange: String = "$$$",
    val distance: String = "2.5 miles away",
    val averageReviewScore: Double = 0.0,
    val reviewHighlights: List<Review> = emptyList(),
    val popularReviewTags: List<String> = emptyList(),
)

class DetailViewModel(
    private val repo: BiteShareRepository,
    restaurantId: String,
) {
    var uiState by mutableStateOf(DetailUiState())
        private set

    private val scope = MainScope()

    init {
        loadDetail(restaurantId)
    }

    private fun loadDetail(restaurantId: String) {
        scope.launch {
            val restaurant = repo.getRestaurantById(restaurantId)
            val detail = repo.getRestaurantDetailById(restaurantId)
            val reviews = detail?.reviews.orEmpty()
            uiState = uiState.copy(
                restaurant = restaurant,
                restaurantDetail = detail,
                reviewCount = reviews.size,
                location = detail?.location?.city ?: "Addis Ababa",
                priceRange = detail?.priceRange ?: "$$$",
                distance = detail?.location?.distance ?: "2.5 miles away",
                averageReviewScore = when {
                    reviews.isNotEmpty() ->
                        reviews.map { it.ratingForAverage() }.average().takeIf { !it.isNaN() } ?: 0.0
                    (detail?.rating ?: 0.0) > 0.0 -> detail!!.rating
                    else -> restaurant?.rating ?: 0.0
                },
                reviewHighlights = reviews.take(10),
                popularReviewTags = reviews
                    .flatMap { ReviewTagCatalog.normalizeTags(it.tags) }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { (_, count) -> count }
                    .take(4)
                    .map { (tag, _) -> ReviewTagCatalog.labelFor(tag) },
            )
        }
    }

    fun toggleSaved() {
        val current = uiState.restaurant ?: return
        scope.launch {
            repo.toggleSaved(current.id)
            val refreshed = repo.getRestaurantById(current.id)
            val next = when {
                refreshed == null -> current.copy(isSaved = !current.isSaved)
                refreshed.isSaved == current.isSaved -> refreshed.copy(isSaved = !current.isSaved)
                else -> refreshed
            }
            uiState = uiState.copy(restaurant = next)
        }
    }
}
