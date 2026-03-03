package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Restaurant

enum class BrowseFilterTab { Location, Price, Type }

data class BrowseUiState(
    val allRestaurants: List<Restaurant> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val selectedFilter: BrowseFilterTab = BrowseFilterTab.Location,
    val resultCount: Int = 0,
    val locationLabel: String = "Addis Ababa",
    val priceLabel: String = "$$$",
    val openStatus: String = "Open Now",
    val activeTag: String? = null,
    val headerTitle: String = "Top Food Places",
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
            allRestaurants = list,
            restaurants = list,
            resultCount = list.size,
            headerTitle = "Top Food Places",
        )
    }

    fun selectFilter(tab: BrowseFilterTab) {
        uiState = uiState.copy(selectedFilter = tab)
    }

    fun applyTagFilter(tag: String) {
        val filtered = repo.getRestaurantsByTag(tag)
        uiState = uiState.copy(
            restaurants = filtered,
            resultCount = filtered.size,
            activeTag = tag,
            headerTitle = "Top $tag Places",
        )
    }

    fun clearTagFilter() {
        val all = uiState.allRestaurants
        uiState = uiState.copy(
            restaurants = all,
            resultCount = all.size,
            activeTag = null,
            headerTitle = "Top Food Places",
        )
    }
}
