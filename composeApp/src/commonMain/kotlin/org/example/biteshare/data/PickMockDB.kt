package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.GeoPoint
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.VoteSession

class PickMockDB(
    friends: List<Friend> = MockDB.fakeFriends,
    restaurants: List<Restaurant> = MockDB.allRestaurants,
    restaurantDetails: Map<String, RestaurantDetail> = MockDB.restaurantDetails,
    preferences: List<String> = emptyList(),
    restrictions: List<String> = emptyList(),
    currentUserId: String? = null,
    currentUserLocation: GeoPoint? = null,
    preferencesByUserId: Map<String, List<String>> = emptyMap(),
    restrictionsByUserId: Map<String, List<String>> = emptyMap(),
    savedSelectionsByUserId: Map<String, Set<String>> = emptyMap(),
    initialVoteSessions: List<VoteSession> = emptyList(),
) : PickRepository {
    private val friendsData = friends.toList()
    private val restaurantsData = restaurants.toList()
    private val detailsData = restaurantDetails.toMap()
    private val preferenceData = preferences.toList()
    private val restrictionData = restrictions.toList()
    private val currentUserIdData = currentUserId
    private val currentUserLocationData = currentUserLocation
    private val preferencesByUserIdData = preferencesByUserId.mapValues { it.value.toList() }
    private val restrictionsByUserIdData = restrictionsByUserId.mapValues { it.value.toList() }
    private val savedSelectionsData = savedSelectionsByUserId
        .mapValues { it.value.toMutableSet() }
        .toMutableMap()
    private val voteSessions = initialVoteSessions
        .associateBy { it.id }
        .toMutableMap()

    override suspend fun friends(): List<Friend> = friendsData

    override suspend fun locations(): List<String> =
        restaurantsData.map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> = restaurantsData

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? = detailsData[id]

    override suspend fun restaurantSearchSignals(restaurant: Restaurant): String {
        val detail = detailsData[restaurant.id]
        val featuredText = detail?.featuredItems
            ?.joinToString(" ") { "${it.name} ${it.description}" }
            .orEmpty()
        return listOf(
            restaurant.name,
            restaurant.category,
            restaurant.location,
            detail?.description.orEmpty(),
            detail?.attributes?.joinToString(" ").orEmpty(),
            featuredText,
        )
            .map { normalizeSignal(it) }
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    override suspend fun userPreferences(): List<String> {
        val userId = currentUserIdData
        return if (userId != null) {
            preferencesByUserIdData[userId].orEmpty().ifEmpty { preferenceData }
        } else {
            preferenceData
        }
    }

    override suspend fun userRestrictions(): List<String> {
        val userId = currentUserIdData
        return if (userId != null) {
            restrictionsByUserIdData[userId].orEmpty().ifEmpty { restrictionData }
        } else {
            restrictionData
        }
    }

    override suspend fun userPreferencesByUserIds(userIds: Set<String>): Map<String, List<String>> {
        if (userIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, List<String>>()
        userIds.forEach { id ->
            val fromMap = preferencesByUserIdData[id]
            if (fromMap != null) {
                result[id] = fromMap
            } else if (id == currentUserIdData && preferenceData.isNotEmpty()) {
                result[id] = preferenceData
            }
        }
        return result
    }

    override suspend fun userRestrictionsByUserIds(userIds: Set<String>): Map<String, List<String>> {
        if (userIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, List<String>>()
        userIds.forEach { id ->
            val fromMap = restrictionsByUserIdData[id]
            if (fromMap != null) {
                result[id] = fromMap
            } else if (id == currentUserIdData && restrictionData.isNotEmpty()) {
                result[id] = restrictionData
            }
        }
        return result
    }

    override suspend fun currentUserId(): String? = currentUserIdData

    override suspend fun currentUserLocation(): GeoPoint? = currentUserLocationData

    override suspend fun restaurantSelectionsByUserIds(userIds: Set<String>): Map<String, Set<String>> {
        if (userIds.isEmpty()) return emptyMap()
        return userIds.associateWith { id -> savedSelectionsData[id].orEmpty().toSet() }
    }

    override suspend fun setRestaurantSelection(userId: String, restaurantId: String, selected: Boolean) {
        if (userId.isBlank() || restaurantId.isBlank()) return
        val selections = savedSelectionsData.getOrPut(userId) { mutableSetOf() }
        if (selected) {
            selections.add(restaurantId)
        } else {
            selections.remove(restaurantId)
        }
    }

    override suspend fun createVoteSession(session: VoteSession) {
        if (session.id.isBlank()) return
        voteSessions[session.id] = session
    }

    override suspend fun updateVoteSessionVotes(sessionId: String, userId: String, votes: Set<String>) {
        val session = voteSessions[sessionId] ?: return
        voteSessions[sessionId] = session.copy(votesByUserId = session.votesByUserId + (userId to votes))
    }

    override suspend fun voteSessionVotes(sessionId: String): Map<String, Set<String>> =
        voteSessions[sessionId]?.votesByUserId ?: emptyMap()

    override suspend fun closeVoteSession(sessionId: String, closedAtEpoch: Long) {
        val session = voteSessions[sessionId] ?: return
        voteSessions[sessionId] = session.copy(isClosed = true, closedAtEpoch = closedAtEpoch)
    }

    override suspend fun voteSessionsForUser(userId: String): List<VoteSession> =
        voteSessions.values.filter { session -> session.participants.any { it.id == userId } }

    override suspend fun voteSessionById(sessionId: String): VoteSession? = voteSessions[sessionId]

    private fun normalizeSignal(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
