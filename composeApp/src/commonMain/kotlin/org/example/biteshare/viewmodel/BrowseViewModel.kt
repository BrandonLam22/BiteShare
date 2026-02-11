package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.model.FakeRepository
import org.example.biteshare.model.Restaurant

enum class BrowseFilterTab { Location, Price, Type }

data class BrowseUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val selectedFilter: BrowseFilterTab = BrowseFilterTab.Location,
    val resultCount: Int = 0,
    val locationLabel: String = "Addis Ababa",
    val priceLabel: String = "$$$",
    val openStatus: String = "Open Now",
)

class BrowseViewModel(
    private val repo: FakeRepository,
) {
    var uiState by mutableStateOf(BrowseUiState())
        private set

    init {
        loadRestaurants()
    }

    private fun loadRestaurants() {
        val list = repo.browseRestaurants()
        uiState = uiState.copy(
            restaurants = list,
            resultCount = 120,
        )
    }

    fun selectFilter(tab: BrowseFilterTab) {
        uiState = uiState.copy(selectedFilter = tab)
    }
}
