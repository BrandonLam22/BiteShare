package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Restaurant  // CHANGED: Import Restaurant

// DELETED: Remove SavedRestaurant data class entirely - use Restaurant instead

data class SavedUiState(
    val sortBy: String = "time",
    val savedRestaurants: List<Restaurant> = emptyList(),  // CHANGED: Use Restaurant
)

class SavedViewModel(
    private val repo: BiteShareRepository,  // CHANGED: Remove ? = null, make it required
) {
    var uiState by mutableStateOf(SavedUiState())
        private set

    private val scope = MainScope()

    init {
        loadSavedRestaurants()
    }
    fun refresh() {
        loadSavedRestaurants()
    }


    private fun loadSavedRestaurants() {
        scope.launch {
            val restaurants = repo.getSavedRestaurants()
            uiState = uiState.copy(savedRestaurants = restaurants)
        }
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
        scope.launch {
            repo.toggleSaved(restaurantId)
            loadSavedRestaurants()
        }
    }
}
