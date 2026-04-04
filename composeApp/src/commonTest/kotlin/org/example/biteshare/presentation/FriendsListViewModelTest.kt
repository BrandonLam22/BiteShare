package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.FriendsListViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsListViewModelTest {

    @Test
    fun initLoadsFriends() = runMainTest {
        val repo = FakeRepository()
        val vm = FriendsListViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.friends.isNotEmpty())
    }

    @Test
    fun loadFriendDetailsPopulatesSelection() = runMainTest {
        val repo = FakeRepository()
        val vm = FriendsListViewModel(repo)
        advanceUntilIdle()

        vm.loadFriendDetails("user_03")
        advanceUntilIdle()

        assertNotNull(vm.uiState.selectedFriendDetails)
        assertEquals("Alice", vm.uiState.selectedFriendDetails?.name)
        assertFalse(vm.uiState.isLoadingDetails)
    }

    @Test
    fun clearSelectedFriendResetsState() = runMainTest {
        val repo = FakeRepository()
        val vm = FriendsListViewModel(repo)
        advanceUntilIdle()

        vm.loadFriendDetails("user_03")
        advanceUntilIdle()
        vm.clearSelectedFriend()

        assertEquals(null, vm.uiState.selectedFriendDetails)
    }
}
