package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Restaurant

data class BrowseUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val resultCount: Int = 0,
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
            resultCount = list.size,
        )
    }
}
