package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.model.FakeRepository

data class SavedRestaurant(
    val id: String,
    val category: String,
    val name: String,
    val price: String,
    val eta: String,
    val rating: Double,
)

data class SavedUiState(
    val sortBy: String = "time", // "time" or "name"
    val savedRestaurants: List<SavedRestaurant> = emptyList(),
)

class SavedViewModel(
    private val repo: FakeRepository? = null,
) {
    var uiState by mutableStateOf(SavedUiState())
        private set

    init {
        loadSavedRestaurants()
    }

    private fun loadSavedRestaurants() {
        // TODO: Load from repository/data source
        val restaurants = listOf(
            SavedRestaurant(
                id = "1",
                category = "Pizza",
                name = "Joe's Pizza",
                price = "$2.99",
                eta = "30-40 min",
                rating = 4.5
            ),
            SavedRestaurant(
                id = "2",
                category = "Sushi",
                name = "Sushi Palace",
                price = "$4.99",
                eta = "50-60 min",
                rating = 4.0
            ),
            SavedRestaurant(
                id = "3",
                category = "Burgers",
                name = "Burger House",
                price = "$3.49",
                eta = "25-35 min",
                rating = 4.2
            ),
        )

        uiState = uiState.copy(savedRestaurants = restaurants)
    }

    fun setSortBy(sortBy: String) {
        val sortedRestaurants = when (sortBy) {
            "name" -> uiState.savedRestaurants.sortedBy { it.name }
            "time" -> uiState.savedRestaurants.sortedBy { it.eta }
            else -> uiState.savedRestaurants
        }

        uiState = uiState.copy(
            sortBy = sortBy,
            savedRestaurants = sortedRestaurants
        )
    }

    fun toggleSaved(restaurantId: String) {
        // Remove restaurant from saved list
        uiState = uiState.copy(
            savedRestaurants = uiState.savedRestaurants.filter { it.id != restaurantId }
        )
        // TODO: Persist to repository/data source
    }
}