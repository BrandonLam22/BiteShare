package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.FakeRepository
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
    val visibleFriends: List<Friend> = emptyList(),
    val friendSearchQuery: String = "",
    val newFriendName: String = "",
    val friendActionMessage: String? = null,
    val selectedFriendIds: Set<String> = emptySet(),
    val filters: PickFilters = PickFilters(),
    val locations: List<String> = emptyList(),
    val resultPreviewCount: Int = 0,
)

class PickForMeViewModel(
    private val model: PickModel,
) {
    constructor(repo: FakeRepository) : this(PickModel(repo))

    var uiState by mutableStateOf(
        PickForMeUiState(
            mode = PickMode.ME_ONLY,
            friends = model.friends(),
            visibleFriends = model.friends(),
            locations = listOf("Any") + model.locations(),
        )
    )
        private set

    init {
        refreshPreview()
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

    fun updateFriendSearchQuery(query: String) {
        uiState = uiState.copy(friendSearchQuery = query)
        applyFriendSearch()
    }

    fun updateNewFriendName(name: String) {
        uiState = uiState.copy(newFriendName = name, friendActionMessage = null)
    }

    fun addFriend() {
        val name = uiState.newFriendName.trim()
        if (name.isBlank()) {
            uiState = uiState.copy(friendActionMessage = "Friend name cannot be empty.")
            return
        }

        val added = model.addFriend(name)
        if (!added) {
            uiState = uiState.copy(friendActionMessage = "Unable to add friend (duplicate or unavailable).")
            return
        }

        val refreshed = model.friends()
        uiState = uiState.copy(
            friends = refreshed,
            newFriendName = "",
            friendActionMessage = "Added friend: $name"
        )
        applyFriendSearch()
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
        uiState = uiState.copy(resultPreviewCount = model.previewCount(buildPickContext()))
    }

    private fun applyFriendSearch() {
        val filtered = model.searchFriends(uiState.friendSearchQuery)
        uiState = uiState.copy(visibleFriends = filtered)
    }
}


// Picking Logic

// 

