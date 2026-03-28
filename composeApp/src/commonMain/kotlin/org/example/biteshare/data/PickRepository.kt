package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.FriendAddResult
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.VoteSession
import org.example.biteshare.domain.FriendRequest
import org.example.biteshare.domain.FriendRequestResult

interface PickRepository {
    suspend fun friends(): List<Friend>
    suspend fun searchUsers(query: String): List<Friend> = emptyList()
    suspend fun addFriend(friendId: String): FriendAddResult = FriendAddResult.ERROR
    suspend fun removeFriend(friendId: String): Boolean = false

    // FRIEND REQUEST
    suspend fun sendFriendRequest(targetUserId: String): FriendRequestResult = FriendRequestResult.ERROR
    suspend fun incomingFriendRequests(): List<FriendRequest> = emptyList()
    suspend fun outgoingFriendRequests(): List<FriendRequest> = emptyList()
    suspend fun acceptFriendRequest(requestId: String): Boolean = false
    suspend fun rejectFriendRequest(requestId: String): Boolean = false

    suspend fun locations(): List<String>
    suspend fun restaurants(): List<Restaurant>
    suspend fun getRestaurantDetailById(id: String): RestaurantDetail?
    suspend fun userPreferences(): List<String>
    suspend fun userRestrictions(): List<String>
    suspend fun userPreferencesByUserIds(userIds: Set<String>): Map<String, List<String>> = emptyMap()
    suspend fun userRestrictionsByUserIds(userIds: Set<String>): Map<String, List<String>> = emptyMap()
    suspend fun createVoteSession(session: VoteSession) {}
    suspend fun updateVoteSessionVotes(sessionId: String, userId: String, votes: Set<String>) {}
    suspend fun voteSessionVotes(sessionId: String): Map<String, Set<String>> = emptyMap()
    suspend fun closeVoteSession(sessionId: String, closedAtEpoch: Long) {}
    suspend fun voteSessionsForUser(userId: String): List<VoteSession> = emptyList()
    suspend fun voteSessionById(sessionId: String): VoteSession? = null
    suspend fun currentUserId(): String? = null
    suspend fun restaurantSelectionsByUserIds(userIds: Set<String>): Map<String, Set<String>> = emptyMap()
    suspend fun setRestaurantSelection(userId: String, restaurantId: String, selected: Boolean) {}
}
