package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.*
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.Restaurant

data class RecommendsUiState(
    val title: String = "Recommends",
    val items: List<Restaurant> = emptyList(),
)

class RecommendsViewModel(
    private val repo: FakeRepository,
) {
    var uiState by mutableStateOf(RecommendsUiState())
        private set

    fun load(context: PickContext) {
        uiState = uiState.copy(items = repo.recommend(context))
    }

    fun toggleSaved(restaurantId: String) {
        uiState = uiState.copy(
            items = uiState.items.map {
                if (it.id == restaurantId) it.copy(isSaved = !it.isSaved) else it
            }
        )
    }
}
