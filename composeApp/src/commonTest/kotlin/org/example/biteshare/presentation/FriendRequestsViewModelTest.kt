package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.FriendRequest
import org.example.biteshare.domain.FriendRequestStatus
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.FriendRequestsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestsViewModelTest {

    private class FriendRequestRepo(
        private val base: BiteShareRepository,
        private val requests: MutableList<FriendRequest>,
    ) : BiteShareRepository by base {
        override suspend fun incomingFriendRequests(): List<FriendRequest> = requests.toList()

        override suspend fun acceptFriendRequest(requestId: String): Boolean {
            val before = requests.size
            requests.removeAll { it.id == requestId }
            return requests.size < before
        }

        override suspend fun rejectFriendRequest(requestId: String): Boolean {
            val before = requests.size
            requests.removeAll { it.id == requestId }
            return requests.size < before
        }
    }

    @Test
    fun initLoadsRequests() = runMainTest {
        val request = FriendRequest(
            id = "req_1",
            senderId = "alex",
            senderName = "Alex",
            receiverId = "me",
            status = FriendRequestStatus.PENDING
        )
        val repo = FriendRequestRepo(FakeRepository(), mutableListOf(request))
        val vm = FriendRequestsViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.requests.size)
        assertEquals(false, vm.uiState.isLoading)
    }

    @Test
    fun acceptRequestUpdatesMessageAndList() = runMainTest {
        val request = FriendRequest(
            id = "req_2",
            senderId = "alex",
            senderName = "Alex",
            receiverId = "me",
            status = FriendRequestStatus.PENDING
        )
        val repo = FriendRequestRepo(FakeRepository(), mutableListOf(request))
        val vm = FriendRequestsViewModel(repo)
        advanceUntilIdle()

        vm.acceptRequest(request)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.requests.size)
        assertEquals("Alex is now your friend.", vm.uiState.message)
    }

    @Test
    fun rejectRequestUpdatesMessageAndList() = runMainTest {
        val request = FriendRequest(
            id = "req_3",
            senderId = "alex",
            senderName = "Alex",
            receiverId = "me",
            status = FriendRequestStatus.PENDING
        )
        val repo = FriendRequestRepo(FakeRepository(), mutableListOf(request))
        val vm = FriendRequestsViewModel(repo)
        advanceUntilIdle()

        vm.rejectRequest(request)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.requests.size)
        assertEquals("Rejected Alex's request.", vm.uiState.message)
    }
}
