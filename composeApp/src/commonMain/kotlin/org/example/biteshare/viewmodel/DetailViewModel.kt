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

enum class MealTab { Lunch, Dinner, Brunch }

data class DetailUiState(
    val restaurant: Restaurant? = null,
    val restaurantDetail: RestaurantDetail? = null,
    val selectedMeal: MealTab = MealTab.Lunch,
    val reviewCount: Int = 120,
    val location: String = "Addis Ababa",
    val priceRange: String = "$$$",
    val distance: String = "2.5 miles away",
    val timeSlots: List<String> = emptyList(),
    val selectedTimeSlot: String? = null,
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
            val initialMeal = MealTab.Lunch
            val initialSlots = slotsForMeal(initialMeal)
            val reviews = detail?.reviews.orEmpty()
            uiState = uiState.copy(
                restaurant = restaurant,
                restaurantDetail = detail,
                reviewCount = reviews.size,
                location = detail?.location?.city ?: "Addis Ababa",
                priceRange = detail?.priceRange ?: "$$$",
                distance = detail?.location?.distance ?: "2.5 miles away",
                selectedMeal = initialMeal,
                timeSlots = initialSlots,
                selectedTimeSlot = initialSlots.firstOrNull(),
                averageReviewScore = reviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0,
                // Supabase reviews are merged in ahead of fallback reviews, so keep this order.
                reviewHighlights = reviews.take(3),
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

    fun selectMeal(tab: MealTab) {
        val slots = slotsForMeal(tab)
        uiState = uiState.copy(
            selectedMeal = tab,
            timeSlots = slots,
            selectedTimeSlot = slots.firstOrNull(),
        )
    }

    fun selectTimeSlot(slot: String) {
        uiState = uiState.copy(selectedTimeSlot = slot)
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

    private fun slotsForMeal(meal: MealTab): List<String> {
        return when (meal) {
            MealTab.Lunch -> listOf("12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM", "2:00 PM", "2:30 PM")
            MealTab.Dinner -> listOf("5:30 PM", "6:00 PM", "6:30 PM", "7:00 PM", "7:30 PM", "8:00 PM")
            MealTab.Brunch -> listOf("9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM")
        }
    }
}
