package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.PickRepository
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant

data class RecommendsUiState(
    val title: String = "Recommends",
    val items: List<Restaurant> = emptyList(),
)

class RecommendsViewModel(
    private val model: PickModel,
) {
    constructor(repo: PickRepository) : this(PickModel(repo))

    var uiState by mutableStateOf(RecommendsUiState())
        private set

    private val scope = MainScope()

    fun load(context: PickContext) {
        scope.launch {
            loadItems(model.recommend(context))
        }
    }

    fun loadItems(
        items: List<Restaurant>,
        title: String = "Recommends",
    ) {
        uiState = uiState.copy(
            title = title,
            items = items,
        )
    }

    fun toggleSaved(restaurantId: String) {
        uiState = uiState.copy(
            items = uiState.items.map {
                if (it.id == restaurantId) it.copy(isSaved = !it.isSaved) else it
            }
        )
    }
}
