package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Restaurant

enum class MealTab { Lunch, Dinner, Brunch }

data class DetailUiState(
    val restaurant: Restaurant? = null,
    val selectedMeal: MealTab = MealTab.Lunch,
    val reviewCount: Int = 120,
    val location: String = "Addis Ababa",
    val priceRange: String = "$$$",
    val distance: String = "2.5 miles away",
    val timeSlots: List<String> = emptyList(),
)

class DetailViewModel(
    private val repo: FakeRepository,
    restaurantId: String,
) {
    var uiState by mutableStateOf(DetailUiState())
        private set

    init {
        loadDetail(restaurantId)
    }

    private fun loadDetail(restaurantId: String) {
        val restaurant = repo.getRestaurantById(restaurantId)
        uiState = uiState.copy(
            restaurant = restaurant,
            timeSlots = listOf(
                "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM",
                "2:00 PM", "2:30 PM", "3:00 PM", "6:00 PM", "6:30 PM", "7:00 PM"
            ),
        )
    }

    fun selectMeal(tab: MealTab) {
        uiState = uiState.copy(selectedMeal = tab)
    }

    fun toggleSaved() {
        uiState.restaurant?.let { r ->
            uiState = uiState.copy(
                restaurant = r.copy(isSaved = !r.isSaved)
            )
        }
    }
}
