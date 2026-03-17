package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.biteshare.data.PickRepository
import org.example.biteshare.domain.BudgetFilter
import org.example.biteshare.domain.CuisineFilter
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickFilters
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.PickMode

data class PickForMeUiState(
    val mode: PickMode = PickMode.ME_ONLY,
    val friends: List<Friend> = emptyList(),
    val selectedFriendIds: Set<String> = emptySet(),
    val filters: PickFilters = PickFilters(),
    val locations: List<String> = emptyList(),
    val resultPreviewCount: Int = 0,
)

class PickForMeViewModel(
    private val model: PickModel,
) {
    constructor(repo: PickRepository) : this(PickModel(repo))

    var uiState by mutableStateOf(PickForMeUiState(mode = PickMode.ME_ONLY))
        private set

    private val scope = MainScope()
    private var previewJob: Job? = null

    init {
        loadLookups()
    }

    fun setMode(mode: PickMode) {
        uiState = uiState.copy(
            mode = mode,
            selectedFriendIds = emptySet()
        )
        refreshPreview()
    }

    fun toggleFriend(friendId: String) {
        val cur = uiState.selectedFriendIds
        uiState = uiState.copy(
            selectedFriendIds = if (cur.contains(friendId)) cur - friendId else cur + friendId
        )
        refreshPreview()
    }

    fun setLocation(location: String) {
        updateFilters { it.copy(location = location) }
    }

    fun setBudget(budget: BudgetFilter) {
        updateFilters { it.copy(budget = budget) }
    }

    fun setCuisine(cuisine: CuisineFilter) {
        updateFilters { it.copy(cuisine = cuisine) }
    }

    fun setOpenNowOnly(openNowOnly: Boolean) {
        updateFilters { it.copy(openNowOnly = openNowOnly) }
    }

    fun setMinRating(minRating: Double) {
        val normalized = (minRating * 2).toInt() / 2.0
        updateFilters { it.copy(minRating = normalized) }
    }

    fun buildPickContext(): PickContext =
        PickContext(
            mode = uiState.mode,
            selectedFriendIds = uiState.selectedFriendIds,
            filters = uiState.filters
        )

    private fun updateFilters(update: (PickFilters) -> PickFilters) {
        uiState = uiState.copy(filters = update(uiState.filters))
        refreshPreview()
    }

    private fun refreshPreview() {
        val context = buildPickContext()
        previewJob?.cancel()
        previewJob = scope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            val count = model.previewCount(context)
            uiState = uiState.copy(resultPreviewCount = count)
        }
    }

    private fun loadLookups() {
        scope.launch {
            val friends = model.friends()
            val locations = listOf("Any") + model.locations()
            uiState = uiState.copy(friends = friends, locations = locations)
            refreshPreview()
        }
    }
}

private const val PREVIEW_DEBOUNCE_MS = 250L


// Picking Logic

// 
