package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.domain.PickModel

data class VoteInviteItem(
    val notificationId: String,
    val sessionId: String,
    val title: String,
    val participantCount: Int,
    val restaurantCount: Int,
    val createdAtEpoch: Long,
    val isRead: Boolean,
    val isClosed: Boolean,
)

data class VoteInvitesUiState(
    val invites: List<VoteInviteItem> = emptyList(),
    val isLoading: Boolean = false,
)

class VoteInvitesViewModel(
    private val model: PickModel,
) {
    var uiState by mutableStateOf(VoteInvitesUiState())
        private set

    private val scope = MainScope()

    fun refresh() {
        scope.launch {
            uiState = uiState.copy(isLoading = true)
            val notifications = model.voteNotificationsForCurrentUser()
            val sessions = model.voteSessionsForCurrentUser()
            val sessionsById = sessions.associateBy { it.id }.toMutableMap()
            val invitesBySessionId = linkedMapOf<String, VoteInviteItem>()
            notifications.forEach { notification ->
                val session = sessionsById[notification.sessionId]
                    ?: model.voteSessionById(notification.sessionId)
                        ?.also { sessionsById[notification.sessionId] = it }
                    ?: return@forEach
                invitesBySessionId[session.id] = VoteInviteItem(
                    notificationId = notification.id,
                    sessionId = session.id,
                    title = session.title,
                    participantCount = session.participants.size,
                    restaurantCount = session.restaurants.size,
                    createdAtEpoch = notification.createdAtEpoch,
                    isRead = notification.isRead,
                    isClosed = session.isClosed
                )
            }

            val invites = invitesBySessionId.values.sortedByDescending { it.createdAtEpoch }
            uiState = VoteInvitesUiState(invites = invites, isLoading = false)
        }
    }
}
