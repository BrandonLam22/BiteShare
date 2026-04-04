package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.PickMockDB
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.VoteHistoryViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class VoteHistoryViewModelTest {

    @Test
    fun refreshSortsSessionsByCreatedAt() = runMainTest {
        val userId = "me"
        val sessions = listOf(
            VoteSession(
                id = "s1",
                title = "Old",
                createdAtEpoch = 1000L,
                participants = listOf(VoteParticipant(id = userId, name = "Me")),
                restaurants = emptyList(),
                votesByUserId = emptyMap(),
            ),
            VoteSession(
                id = "s2",
                title = "New",
                createdAtEpoch = 2000L,
                participants = listOf(VoteParticipant(id = userId, name = "Me")),
                restaurants = emptyList(),
                votesByUserId = emptyMap(),
            ),
        )
        val repo = PickMockDB(currentUserId = userId, initialVoteSessions = sessions)
        val model = PickModel(repo)
        val vm = VoteHistoryViewModel(model)
        advanceUntilIdle()

        assertEquals(listOf("s2", "s1"), vm.uiState.sessions.map { it.id })
    }
}
