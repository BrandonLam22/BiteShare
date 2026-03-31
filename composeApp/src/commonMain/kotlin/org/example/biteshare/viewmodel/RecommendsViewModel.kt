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
    val canShuffle: Boolean = false,
)

class RecommendsViewModel(
    private val model: PickModel,
) {
    constructor(repo: PickRepository) : this(PickModel(repo))

    var uiState by mutableStateOf(RecommendsUiState())
        private set

    private val scope = MainScope()
    private var allItems: List<Restaurant> = emptyList()
    private var shuffleLimit: Int = DEFAULT_GROUP_RECOMMEND_LIMIT

    fun load(context: PickContext) {
        scope.launch {
            loadItems(model.recommend(context))
        }
    }

    fun loadItems(
        items: List<Restaurant>,
        title: String = "Recommends",
    ) {
        allItems = items
        shuffleLimit = DEFAULT_GROUP_RECOMMEND_LIMIT
        uiState = uiState.copy(
            title = title,
            items = items,
            canShuffle = false,
        )
    }

    fun loadGroupItems(
        items: List<Restaurant>,
        title: String = "Recommended for Your Group",
        limit: Int = DEFAULT_GROUP_RECOMMEND_LIMIT,
    ) {
        allItems = items
        shuffleLimit = limit
        val limited = items.take(limit)
        uiState = uiState.copy(
            title = title,
            items = limited,
            canShuffle = items.size > limited.size,
        )
    }

    fun shuffleGroupItems() {
        if (allItems.isEmpty()) return
        val limited = if (shuffleLimit <= 0 || allItems.size <= shuffleLimit) {
            allItems
        } else {
            allItems.shuffled().take(shuffleLimit)
        }
        uiState = uiState.copy(
            items = limited,
            canShuffle = allItems.size > limited.size,
        )
    }

    fun toggleSaved(restaurantId: String) {
        allItems = allItems.map {
            if (it.id == restaurantId) it.copy(isSaved = !it.isSaved) else it
        }
        uiState = uiState.copy(
            items = uiState.items.map {
                if (it.id == restaurantId) it.copy(isSaved = !it.isSaved) else it
            }
        )
    }
}

private const val DEFAULT_GROUP_RECOMMEND_LIMIT = 20
