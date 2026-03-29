package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.Restaurant

data class BrowseUiState(
    val allRestaurants: List<Restaurant> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val searchQuery: String = "",
    val resultCount: Int = 0,
    val locationLabel: String = "Addis Ababa",
    val priceLabel: String = "$$$",
    val activeTag: String? = null,
    val headerTitle: String = "Top Food Places",
)

class BrowseViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(BrowseUiState())
        private set

    private val scope = MainScope()

    init {
        loadRestaurants()
    }

    private fun loadRestaurants() {
        scope.launch {
            val list = repo.browseRestaurants()
            uiState = uiState.copy(
                allRestaurants = list,
                restaurants = list,
                resultCount = list.size,
                headerTitle = "Top Food Places",
            )
        }
    }

    fun applyTagFilter(tag: String) {
        uiState = uiState.copy(
            activeTag = tag,
            headerTitle = "Top $tag Places",
        )
        refreshResults()
    }

    fun clearTagFilter() {
        uiState = uiState.copy(
            activeTag = null,
            headerTitle = "Top Food Places",
        )
        refreshResults()
    }

    fun updateSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
        refreshResults()
    }

    fun clearSearch() {
        uiState = uiState.copy(searchQuery = "")
        refreshResults()
    }

    private fun refreshResults() {
        scope.launch {
            val base = if (uiState.activeTag != null) {
                repo.getRestaurantsByTag(uiState.activeTag!!)
            } else {
                uiState.allRestaurants
            }
            val filtered = repo.searchRestaurants(uiState.searchQuery, base)
            uiState = uiState.copy(
                restaurants = filtered,
                resultCount = filtered.size,
            )
        }
    }
}
