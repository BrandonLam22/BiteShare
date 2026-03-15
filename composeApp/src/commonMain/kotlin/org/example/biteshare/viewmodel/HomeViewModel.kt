package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.PopularItem

data class HomeUiState(
    val greeting: String = "Hi, Kevin",
    val categories: List<CategoryItem> = emptyList(),
    val popularDishes: List<PopularItem> = emptyList(),
    val popularDrinks: List<PopularItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class HomeViewModel(
    private val repo: BiteShareRepository,
) {
    var uiState by mutableStateOf(HomeUiState())
        private set

    private val scope = MainScope()

    init {
        loadHome()
    }

    fun loadHome(userName: String = "John") {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        scope.launch {
            runCatching {
                repo.getHomeFeed(userName)
            }.onSuccess { feed ->
                uiState = uiState.copy(
                    greeting = feed.greeting,
                    categories = feed.categories,
                    popularDishes = feed.popularDishes,
                    popularDrinks = feed.popularDrinks,
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { t ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load homepage data.",
                )
            }
        }
    }

    fun retryLoad() {
        loadHome()
    }
}
