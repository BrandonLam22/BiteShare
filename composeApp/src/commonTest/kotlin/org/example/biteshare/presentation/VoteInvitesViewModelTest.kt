package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.PickRepository
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.VoteNotification
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.VoteInvitesViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoteInvitesViewModelTest {

    private class VoteInviteRepo(
        private val userId: String,
        private val notifications: List<VoteNotification>,
        private val sessions: List<VoteSession>,
    ) : PickRepository {
        override suspend fun friends(): List<Friend> = emptyList()
        override suspend fun locations(): List<String> = emptyList()
        override suspend fun restaurants(): List<Restaurant> = emptyList()
        override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? = null
        override suspend fun userPreferences(): List<String> = emptyList()
        override suspend fun userRestrictions(): List<String> = emptyList()
        override suspend fun currentUserId(): String? = userId

        override suspend fun voteNotificationsForUser(userId: String): List<VoteNotification> =
            if (userId == this.userId) notifications else emptyList()

        override suspend fun voteSessionsForUser(userId: String): List<VoteSession> =
            if (userId == this.userId) sessions else emptyList()

        override suspend fun voteSessionById(sessionId: String): VoteSession? =
            sessions.firstOrNull { it.id == sessionId }
    }

    @Test
    fun refreshBuildsSortedInviteList() = runMainTest {
        val userId = "me"
        val restaurants = listOf(
            Restaurant("r1", "Alpha", "Pizza", "$10", "10 min", 4.0),
            Restaurant("r2", "Beta", "Sushi", "$12", "12 min", 4.5),
        )
        val session1 = VoteSession(
            id = "s1",
            title = "Lunch",
            createdAtEpoch = 1000L,
            participants = listOf(
                VoteParticipant(id = userId, name = "Me"),
                VoteParticipant(id = "friend", name = "Friend")
            ),
            restaurants = restaurants,
            votesByUserId = emptyMap(),
        )
        val session2 = VoteSession(
            id = "s2",
            title = "Dinner",
            createdAtEpoch = 2000L,
            participants = listOf(
                VoteParticipant(id = userId, name = "Me"),
                VoteParticipant(id = "friend", name = "Friend")
            ),
            restaurants = restaurants,
            votesByUserId = emptyMap(),
        )
        val notifications = listOf(
            VoteNotification(
                id = "n1",
                sessionId = "s1",
                userId = userId,
                createdAtEpoch = 1000L,
                readAtEpoch = null,
            ),
            VoteNotification(
                id = "n2",
                sessionId = "s2",
                userId = userId,
                createdAtEpoch = 2000L,
                readAtEpoch = 55L,
            ),
        )

        val repo = VoteInviteRepo(userId, notifications, listOf(session1, session2))
        val model = PickModel(repo)
        val vm = VoteInvitesViewModel(model)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf("s2", "s1"), vm.uiState.invites.map { it.sessionId })
        val topInvite = vm.uiState.invites.first()
        assertEquals("Dinner", topInvite.title)
        assertEquals(2, topInvite.participantCount)
        assertEquals(2, topInvite.restaurantCount)
        assertTrue(topInvite.isRead)
        assertEquals(false, vm.uiState.isLoading)
    }
}
