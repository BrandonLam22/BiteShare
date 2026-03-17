package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.VoteSession

data class VoteHistoryDetailUiState(
    val session: VoteSession? = null,
)

class VoteHistoryDetailViewModel(
    private val model: PickModel,
    sessionId: String,
) {
    var uiState by mutableStateOf(VoteHistoryDetailUiState())
        private set
    private val scope = MainScope()

    init {
        scope.launch {
            uiState = VoteHistoryDetailUiState(session = model.voteSessionById(sessionId))
        }
    }

    fun voteCountForRestaurant(restaurantId: String): Int {
        val session = uiState.session ?: return 0
        return session.votesByUserId.values.count { restaurantId in it }
    }

    fun participantSelectedCount(participantId: String): Int {
        val session = uiState.session ?: return 0
        return session.votesByUserId[participantId]?.size ?: 0
    }

    fun participantVoteRestaurantNames(participantId: String): List<String> {
        val session = uiState.session ?: return emptyList()
        val byId = session.restaurants.associateBy { it.id }
        return session.votesByUserId[participantId]
            .orEmpty()
            .mapNotNull { byId[it]?.name }
    }

    fun rankedRestaurants(): List<Restaurant> {
        val session = uiState.session ?: return emptyList()
        return session.restaurants.sortedWith(
            compareByDescending<Restaurant> { voteCountForRestaurant(it.id) }
                .thenBy { it.name }
        )
    }
}
