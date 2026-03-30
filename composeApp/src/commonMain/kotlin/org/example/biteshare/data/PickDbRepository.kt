package org.example.biteshare.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.example.biteshare.domain.FeaturedItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.FriendAddResult
import org.example.biteshare.domain.GeoPoint
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.ReviewTagCatalog
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantClassification
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession
import org.example.biteshare.domain.VoteNotification
import org.example.biteshare.domain.FriendRequest
import org.example.biteshare.domain.FriendRequestResult
import org.example.biteshare.domain.FriendRequestStatus
import kotlin.random.Random

class PickDbRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val userIdProvider: () -> String? = { null },
) : PickRepository {
    // Prefer the candidate app read model for display, but keep legacy-only rows available
    // so existing saved/vote/detail flows do not disappear during the staged migration.
    private val legacyRestaurantsTable = "restaurants2"
    private val appReadModelTable = "google_maps_places_app"
    private val legacyRestaurantDetailsTable = "restaurant_details"
    private val friendsTable = "friends"
    private val featuredItemsTable = "featured_items"
    private val userSavedRestaurantsTable = "user_saved_restaurants"
    private val reviewsTable = "reviews"
    private val usersTable = "users"
    private val voteSessionsTable = "vote_sessions"
    private val voteParticipantsTable = "vote_session_participants"
    private val voteRestaurantsTable = "vote_session_restaurants"
    private val voteSessionVotesTable = "vote_session_votes"
    private val voteNotificationsTable = "vote_notifications"
    private val defaultUserId = "demo_user"
    private var searchSignalCache: Map<String, String>? = null
    private val friendRequestsTable = "friend_requests"

    override suspend fun friends(): List<Friend> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[friendsTable]
                .select(Columns.raw("friend_id, user_id, name")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("friend_id")
                if (id.isBlank()) return@mapNotNull null
                val name = row.getString("name").ifBlank { id }
                Friend(id = id, name = name)
            }
        }.getOrElse { emptyList() }

    override suspend fun searchUsers(query: String): List<Friend> =
        runCatching {
            val needle = query.trim().lowercase()
            if (needle.isBlank()) return@runCatching emptyList()

            val userId = effectiveUserId()
            val rawUsers = client.postgrest[usersTable]
                .select(Columns.raw("id, username, email"))
                .decodeList<JsonObject>()

            rawUsers.mapNotNull { row ->
                val id = row.getString("id")
                val username = row.getString("username")
                val email = row.getString("email")
                if (id.isBlank() || username.isBlank()) return@mapNotNull null
                if (id == userId) return@mapNotNull null
                val matches = id.lowercase().contains(needle) ||
                    username.lowercase().contains(needle) ||
                    email.lowercase().contains(needle)
                if (!matches) return@mapNotNull null
                Friend(id = id, name = username)
            }.sortedWith(
                compareBy<Friend> { friend -> searchRank(friend, needle) }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.id.lowercase() }
            )
        }.getOrElse { emptyList() }

    override suspend fun addFriend(friendId: String): FriendAddResult =
        runCatching {
            val normalizedId = friendId.trim()
            if (normalizedId.isBlank()) return@runCatching FriendAddResult.EMPTY_INPUT

            val userId = effectiveUserId()
            if (normalizedId == userId) return@runCatching FriendAddResult.SELF_NOT_ALLOWED

            val existingFriendIds = friends().map { it.id }.toSet()
            if (normalizedId in existingFriendIds) return@runCatching FriendAddResult.ALREADY_FRIENDS

            val userRow = client.postgrest[usersTable]
                .select(Columns.raw("id, username")) {
                    filter { eq("id", normalizedId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@runCatching FriendAddResult.NOT_FOUND

            client.postgrest[friendsTable].insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("friend_id", normalizedId)
                    put("name", userRow.getString("username").ifBlank { normalizedId })
                }
            )
            FriendAddResult.SUCCESS
        }.getOrElse { throwable ->
            println("addFriend error: ${throwable.message}")
            FriendAddResult.ERROR
        }

    override suspend fun removeFriend(friendId: String): Boolean =
        runCatching {
            val normalizedId = friendId.trim()
            if (normalizedId.isBlank()) return@runCatching false

            val userId = effectiveUserId()

            client.postgrest[friendsTable].delete {
                filter {
                    eq("user_id", userId)
                    eq("friend_id", normalizedId)
                }
            }

            client.postgrest[friendsTable].delete {
                filter {
                    eq("user_id", normalizedId)
                    eq("friend_id", userId)
                }
            }

            true
        }.getOrElse { throwable ->
            println("removeFriend error: ${throwable.message}")
            false
        }

    // FRIEND REQUEST
    // HELPER FUNCTION
    private fun JsonObject.toFriendRequest(
        senderName: String,
        receiverName: String = "",
    ): FriendRequest {
        return FriendRequest(
            id = getString("id"),
            senderId = getString("sender_id"),
            senderName = senderName,
            receiverId = getString("receiver_id"),
            receiverName = receiverName,
            status = when (getString("status").lowercase()) {
                "accepted" -> FriendRequestStatus.ACCEPTED
                "rejected" -> FriendRequestStatus.REJECTED
                else -> FriendRequestStatus.PENDING
            }
        )
    }
    // HELPER FUNCTION
    private suspend fun getUsernameById(userId: String): String {
        if (userId.isBlank()) return ""
        return client.postgrest[usersTable]
            .select(Columns.raw("username")) {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?.getString("username")
            .orEmpty()
    }

    override suspend fun sendFriendRequest(targetUserId: String): FriendRequestResult =
        runCatching {
            // reject blank input
            val normalizedId = targetUserId.trim()
            if (normalizedId.isBlank()) return@runCatching FriendRequestResult.EMPTY_INPUT

            // reject self-request
            val userId = effectiveUserId()
            if (normalizedId == userId) return@runCatching FriendRequestResult.SELF_NOT_ALLOWED

            // reject if already friends
            val existingFriendIds = friends().map { it.id }.toSet()
            if (normalizedId in existingFriendIds) return@runCatching FriendRequestResult.ALREADY_FRIENDS

            val userRow = client.postgrest[usersTable]
                .select(Columns.raw("id, username")) {
                    filter { eq("id", normalizedId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@runCatching FriendRequestResult.NOT_FOUND

            val pendingRequests = client.postgrest[friendRequestsTable]
                .select(Columns.raw("id")) {
                    filter {
                        eq("status", "pending")
                        or {
                            and {
                                eq("sender_id", userId)
                                eq("receiver_id", normalizedId)
                            }
                            and {
                                eq("sender_id", normalizedId)
                                eq("receiver_id", userId)
                            }
                        }
                    }
                }
                .decodeList<JsonObject>()

            if (pendingRequests.isNotEmpty()) {
                return@runCatching FriendRequestResult.ALREADY_PENDING
            }

            client.postgrest[friendRequestsTable].insert(
                buildJsonObject {
                    put("sender_id", userId)
                    put("receiver_id", normalizedId)
                    put("status", "pending")
                }
            )
            FriendRequestResult.REQUEST_SENT
        }.getOrElse { throwable ->
            println("sendFriendRequest error: ${throwable.message}")
            FriendRequestResult.ERROR
        }

    // return requests sent to the current user and still pending.
    override suspend fun incomingFriendRequests(): List<FriendRequest> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[friendRequestsTable]
                .select(Columns.raw("id, sender_id, receiver_id, status")) {
                    filter {
                        eq("receiver_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                val senderId = row.getString("sender_id")
                val receiverId = row.getString("receiver_id")
                val statusRaw = row.getString("status")
                if (id.isBlank() || senderId.isBlank() || receiverId.isBlank()) return@mapNotNull null
                FriendRequest(
                    id = id,
                    senderId = senderId,
                    senderName = getUsernameById(senderId),
                    receiverId = receiverId,
                    receiverName = "",
                    status = when (statusRaw.lowercase()) {
                        "accepted" -> FriendRequestStatus.ACCEPTED
                        "rejected" -> FriendRequestStatus.REJECTED
                        else -> FriendRequestStatus.PENDING
                    }
                )
            }
        }.getOrElse { throwable ->
            println("incomingFriendRequests error: ${throwable.message}")
            emptyList()
        }

    override suspend fun outgoingFriendRequests(): List<FriendRequest> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[friendRequestsTable]
                .select(Columns.raw("id, sender_id, receiver_id, status")) {
                    filter {
                        eq("sender_id", userId)
                        eq("status", "pending")
                    }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                val senderId = row.getString("sender_id")
                val receiverId = row.getString("receiver_id")
                val statusRaw = row.getString("status")
                if (id.isBlank() || senderId.isBlank() || receiverId.isBlank()) return@mapNotNull null
                FriendRequest(
                    id = id,
                    senderId = senderId,
                    senderName = getUsernameById(senderId),
                    receiverId = receiverId,
                    receiverName = getUsernameById(receiverId),
                    status = when (statusRaw.lowercase()) {
                        "accepted" -> FriendRequestStatus.ACCEPTED
                        "rejected" -> FriendRequestStatus.REJECTED
                        else -> FriendRequestStatus.PENDING
                    }
                )
            }
        }.getOrElse { throwable ->
            println("outgoingFriendRequests error: ${throwable.message}")
            emptyList()
        }

    override suspend fun acceptFriendRequest(requestId: String): Boolean =
        runCatching {
            val normalizedId = requestId.trim()
            if (normalizedId.isBlank()) return@runCatching false

            val currentUserId = effectiveUserId()

            val requestRow = client.postgrest[friendRequestsTable]
                .select(Columns.raw("id, sender_id, receiver_id, status")) {
                    filter { eq("id", normalizedId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@runCatching false

            val senderId = requestRow.getString("sender_id")
            val receiverId = requestRow.getString("receiver_id")
            val status = requestRow.getString("status")

            if (senderId.isBlank() || receiverId.isBlank()) return@runCatching false
            if (receiverId != currentUserId) return@runCatching false
            if (status.lowercase() != "pending") return@runCatching false

            val senderName = getUsernameById(senderId).ifBlank { senderId }
            val receiverName = getUsernameById(receiverId).ifBlank { receiverId }

            client.postgrest[friendsTable].upsert(
                body = JsonArray(
                    listOf(
                        buildJsonObject {
                            put("user_id", senderId)
                            put("friend_id", receiverId)
                            put("name", receiverName)
                        },
                        buildJsonObject {
                            put("user_id", receiverId)
                            put("friend_id", senderId)
                            put("name", senderName)
                        }
                    )
                )
            ) {
                onConflict = "user_id,friend_id"
            }

            client.postgrest[friendRequestsTable].update(
                body = buildJsonObject {
                    put("status", "accepted")
                }
            ) {
                filter { eq("id", normalizedId) }
            }

            true
        }.getOrElse { throwable ->
            println("acceptFriendRequest error: ${throwable.message}")
            false
        }

    override suspend fun rejectFriendRequest(requestId: String): Boolean =
        runCatching {
            val normalizedId = requestId.trim()
            if (normalizedId.isBlank()) return@runCatching false

            val userId = effectiveUserId()
            val requestRow = client.postgrest[friendRequestsTable]
                .select(Columns.raw("id, receiver_id, status")) {
                    filter { eq("id", normalizedId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@runCatching false
            val receiverId = requestRow.getString("receiver_id")
            val status = requestRow.getString("status")
            if (receiverId != userId) return@runCatching false
            if (status.lowercase() != "pending") return@runCatching false
            client.postgrest[friendRequestsTable].update(
                body = buildJsonObject {
                    put("status", "rejected")
                }
            ) {
                filter { eq("id", normalizedId) }
            }
            true
        }.getOrElse { throwable ->
            println("rejectFriendRequest error: ${throwable.message}")
            false
        }


    override suspend fun locations(): List<String> =
        restaurants().map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> =
        runCatching {
            val savedIds = fetchSelectionsForUser(effectiveUserId())
            val raw = loadRestaurantRows()
            raw.mapNotNull { it.toRestaurant(savedIds) }
        }.getOrElse { emptyList() }

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? =
        runCatching {
            val featuredItems = client.postgrest[featuredItemsTable]
                .select(Columns.raw("restaurant_id, name, price, description")) {
                    filter { eq("restaurant_id", id) }
                }
                .decodeList<JsonObject>()
                .mapNotNull { it.toFeaturedItem() }
            val summary = restaurants().firstOrNull { it.id == id }
            val detailRow = loadAppReadModelRow(id) ?: loadLegacyDetailRow(id) ?: return@runCatching null
            detailRow.toRestaurantDetail(summary, featuredItems)
        }.getOrElse { null }

    override suspend fun restaurantSearchSignals(restaurant: Restaurant): String {
        val baseSignals = buildBaseSignals(restaurant)
        val detailSignals = searchSignalsById()[restaurant.id].orEmpty()
        return if (detailSignals.isBlank()) baseSignals else "$baseSignals $detailSignals"
    }

    override suspend fun userPreferences(): List<String> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[usersTable]
                .select(Columns.raw("id, preferences")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
            val row = raw.firstOrNull() ?: return@runCatching emptyList()
            row.getStringList("preferences")
        }.getOrElse { emptyList() }

    override suspend fun userRestrictions(): List<String> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[usersTable]
                .select(Columns.raw("id, food_restrictions")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
            val row = raw.firstOrNull() ?: return@runCatching emptyList()
            row.getStringList("food_restrictions")
        }.getOrElse { emptyList() }

    override suspend fun userPreferencesByUserIds(userIds: Set<String>): Map<String, List<String>> =
        runCatching {
            val filteredIds = userIds.filter { it.isNotBlank() }
            if (filteredIds.isEmpty()) return@runCatching emptyMap()
            val raw = client.postgrest[usersTable]
                .select(Columns.raw("id, preferences")) {
                    filter { isIn("id", filteredIds) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                if (id.isBlank()) return@mapNotNull null
                id to row.getStringList("preferences")
            }.toMap()
        }.getOrElse { emptyMap() }

    override suspend fun userRestrictionsByUserIds(userIds: Set<String>): Map<String, List<String>> =
        runCatching {
            val filteredIds = userIds.filter { it.isNotBlank() }
            if (filteredIds.isEmpty()) return@runCatching emptyMap()
            val raw = client.postgrest[usersTable]
                .select(Columns.raw("id, food_restrictions")) {
                    filter { isIn("id", filteredIds) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                if (id.isBlank()) return@mapNotNull null
                id to row.getStringList("food_restrictions")
            }.toMap()
        }.getOrElse { emptyMap() }

    override suspend fun reviewsByUserIds(userIds: Set<String>): List<Review> =
        runCatching {
            val filteredIds = userIds.filter { it.isNotBlank() }
            if (filteredIds.isEmpty()) return@runCatching emptyList()
            val raw = client.postgrest[reviewsTable]
                .select(Columns.raw("*")) {
                    filter { isIn("user_id", filteredIds) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row -> row.toReview() }
        }.getOrElse { emptyList() }

    override suspend fun createVoteSession(session: VoteSession) {
        if (session.id.isBlank()) return
        runCatching {
            client.postgrest[voteSessionsTable].upsert(
                body = JsonArray(
                    listOf(
                        buildJsonObject {
                            put("id", session.id)
                            put("title", session.title)
                            put("created_at_epoch", session.createdAtEpoch)
                            put("is_closed", session.isClosed)
                            session.closedAtEpoch?.let { put("closed_at", it) }
                        }
                    )
                )
            ) {
                onConflict = "id"
            }

            val participantRows = session.participants
                .filter { it.id.isNotBlank() }
                .distinctBy { it.id }
                .map { participant ->
                    buildJsonObject {
                        put("session_id", session.id)
                        put("user_id", participant.id)
                        put("display_name", participant.name)
                    }
                }
            if (participantRows.isNotEmpty()) {
                client.postgrest[voteParticipantsTable].upsert(
                    body = JsonArray(participantRows)
                ) {
                    onConflict = "session_id,user_id"
                }
            }

            val restaurantRows = session.restaurants
                .map { it.id }
                .filter { it.isNotBlank() }
                .distinct()
                .map { restaurantId ->
                    buildJsonObject {
                        put("session_id", session.id)
                        put("restaurant_id", restaurantId)
                    }
                }
            if (restaurantRows.isNotEmpty()) {
                client.postgrest[voteRestaurantsTable].upsert(
                    body = JsonArray(restaurantRows)
                ) {
                    onConflict = "session_id,restaurant_id"
                }
            }

            val voteRows = session.votesByUserId.flatMap { (userId, votes) ->
                votes.filter { it.isNotBlank() }.map { restaurantId ->
                    buildJsonObject {
                        put("session_id", session.id)
                        put("user_id", userId)
                        put("restaurant_id", restaurantId)
                    }
                }
            }
            if (voteRows.isNotEmpty()) {
                client.postgrest[voteSessionVotesTable].upsert(
                    body = JsonArray(voteRows)
                ) {
                    onConflict = "session_id,user_id,restaurant_id"
                }
            }

            val creatorId = effectiveUserId().takeIf { it.isNotBlank() }
            val notificationRows = session.participants
                .map { it.id }
                .filter { it.isNotBlank() }
                .distinct()
                .filter { it != creatorId }
                .map { userId ->
                    buildJsonObject {
                        put("session_id", session.id)
                        put("user_id", userId)
                        put("created_at_epoch", session.createdAtEpoch)
                    }
                }
            if (notificationRows.isNotEmpty()) {
                client.postgrest[voteNotificationsTable].upsert(
                    body = JsonArray(notificationRows)
                ) {
                    onConflict = "session_id,user_id"
                }
            }
        }
    }

    override suspend fun updateVoteSessionVotes(sessionId: String, userId: String, votes: Set<String>) {
        if (sessionId.isBlank() || userId.isBlank()) return
        runCatching {
            client.postgrest[voteSessionVotesTable].delete {
                filter {
                    eq("session_id", sessionId)
                    eq("user_id", userId)
                }
            }
            if (votes.isNotEmpty()) {
                val rows = votes.filter { it.isNotBlank() }.map { restaurantId ->
                    buildJsonObject {
                        put("session_id", sessionId)
                        put("user_id", userId)
                        put("restaurant_id", restaurantId)
                    }
                }
                client.postgrest[voteSessionVotesTable].upsert(
                    body = JsonArray(rows)
                ) {
                    onConflict = "session_id,user_id,restaurant_id"
                }
            }
        }
    }

    override suspend fun voteSessionVotes(sessionId: String): Map<String, Set<String>> =
        runCatching {
            if (sessionId.isBlank()) return@runCatching emptyMap()
            val raw = client.postgrest[voteSessionVotesTable]
                .select(Columns.raw("user_id, restaurant_id")) {
                    filter { eq("session_id", sessionId) }
                }
                .decodeList<JsonObject>()
            val votesByUser = mutableMapOf<String, MutableSet<String>>()
            raw.forEach { row ->
                val userId = row.getString("user_id")
                val restaurantId = row.getString("restaurant_id")
                if (userId.isBlank() || restaurantId.isBlank()) return@forEach
                votesByUser.getOrPut(userId) { mutableSetOf() }.add(restaurantId)
            }
            votesByUser.mapValues { it.value.toSet() }
        }.getOrElse { emptyMap() }

    override suspend fun closeVoteSession(sessionId: String, closedAtEpoch: Long) {
        if (sessionId.isBlank()) return
        runCatching {
            client.postgrest[voteSessionsTable].update(
                body = buildJsonObject {
                    put("is_closed", true)
                    put("closed_at", closedAtEpoch)
                }
            ) {
                filter { eq("id", sessionId) }
            }
        }
    }

    override suspend fun voteSessionsForUser(userId: String): List<VoteSession> =
        runCatching {
            if (userId.isBlank()) return@runCatching emptyList()
            val sessionIds = client.postgrest[voteParticipantsTable]
                .select(Columns.raw("session_id")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
                .mapNotNull { it.getString("session_id").takeIf { id -> id.isNotBlank() } }
                .distinct()
            if (sessionIds.isEmpty()) return@runCatching emptyList()

            buildVoteSessions(sessionIds)
        }.getOrElse { emptyList() }

    override suspend fun voteSessionById(sessionId: String): VoteSession? =
        runCatching {
            if (sessionId.isBlank()) return@runCatching null
            buildVoteSessions(listOf(sessionId)).firstOrNull()
        }.getOrElse { null }

    override suspend fun voteNotificationsForUser(userId: String): List<VoteNotification> =
        runCatching {
            if (userId.isBlank()) return@runCatching emptyList()
            val raw = client.postgrest[voteNotificationsTable]
                .select(Columns.raw("id, session_id, user_id, created_at_epoch, read_at_epoch")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                val sessionId = row.getString("session_id")
                val ownerId = row.getString("user_id")
                if (id.isBlank() || sessionId.isBlank() || ownerId.isBlank()) return@mapNotNull null
                VoteNotification(
                    id = id,
                    sessionId = sessionId,
                    userId = ownerId,
                    createdAtEpoch = row.getLong("created_at_epoch"),
                    readAtEpoch = row.getLong("read_at_epoch").takeIf { it > 0 }
                )
            }
        }.getOrElse { emptyList() }

    override suspend fun markVoteNotificationsRead(sessionId: String, userId: String) {
        if (sessionId.isBlank() || userId.isBlank()) return
        val now = System.currentTimeMillis()
        runCatching {
            client.postgrest[voteNotificationsTable].update(
                body = buildJsonObject { put("read_at_epoch", now) }
            ) {
                filter {
                    eq("session_id", sessionId)
                    eq("user_id", userId)
                }
            }
        }
    }

    override suspend fun userDisplayName(userId: String): String? =
        runCatching {
            getUsernameById(userId).takeIf { it.isNotBlank() }
        }.getOrElse { null }

    override suspend fun restaurantSelectionsByUserIds(userIds: Set<String>): Map<String, Set<String>> =
        runCatching {
            val filteredUsers = userIds.filter { it.isNotBlank() }
            if (filteredUsers.isEmpty()) return@runCatching emptyMap()
            val raw = client.postgrest[userSavedRestaurantsTable]
                .select(Columns.raw("user_id, restaurant_id")) {
                    filter { isIn("user_id", filteredUsers) }
                }
                .decodeList<JsonObject>()
            val selections = mutableMapOf<String, MutableSet<String>>()
            raw.forEach { row ->
                val userId = row.getString("user_id")
                val restaurantId = row.getString("restaurant_id")
                if (userId.isBlank() || restaurantId.isBlank()) return@forEach
                selections.getOrPut(userId) { mutableSetOf() }.add(restaurantId)
            }
            selections.mapValues { it.value.toSet() }
        }.getOrElse { emptyMap() }

    override suspend fun setRestaurantSelection(userId: String, restaurantId: String, selected: Boolean) {
        if (userId.isBlank() || restaurantId.isBlank()) return
        runCatching {
            if (selected) {
                client.postgrest[userSavedRestaurantsTable].upsert(
                    body = JsonArray(listOf(buildSelectionPayload(userId, restaurantId)))
                ) {
                    onConflict = "user_id,restaurant_id"
                }
            } else {
                client.postgrest[userSavedRestaurantsTable].delete {
                    filter {
                        eq("user_id", userId)
                        eq("restaurant_id", restaurantId)
                    }
                }
            }
        }
    }

    override suspend fun currentUserId(): String? = effectiveUserId()

    override suspend fun currentUserLocation(): GeoPoint? = null

    private fun effectiveUserId(): String = userIdProvider() ?: defaultUserId

    private suspend fun fetchSelectionsForUser(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        val raw = client.postgrest[userSavedRestaurantsTable]
            .select(Columns.raw("user_id, restaurant_id")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<JsonObject>()
        return raw.mapNotNull { row ->
            row.getString("restaurant_id").takeIf { it.isNotBlank() }
        }.toSet()
    }

    private suspend fun loadRestaurantRows(): List<JsonObject> {
        val appRows = runCatching {
            client.postgrest[appReadModelTable]
                .select(Columns.raw("*"))
                .decodeList<JsonObject>()
        }.getOrDefault(emptyList())
        val legacyRows = runCatching {
            client.postgrest[legacyRestaurantsTable]
                .select(Columns.raw("*"))
                .decodeList<JsonObject>()
        }.getOrDefault(emptyList())
        if (appRows.isEmpty()) return legacyRows
        if (legacyRows.isEmpty()) return appRows

        val appIds = appRows.mapNotNull { row -> row.getString("id").takeIf { it.isNotBlank() } }.toSet()
        val legacyOnlyRows = legacyRows.filter { row ->
            val id = row.getString("id")
            id.isBlank() || id !in appIds
        }
        return appRows + legacyOnlyRows
    }

    private suspend fun loadLegacyDetailRow(id: String): JsonObject? =
        runCatching {
            client.postgrest[legacyRestaurantDetailsTable]
                .select(Columns.raw("*")) {
                    filter { eq("restaurant_id", id) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
        }.getOrNull()

    private suspend fun loadAppReadModelRow(id: String): JsonObject? =
        runCatching {
            client.postgrest[appReadModelTable]
                .select(Columns.raw("*")) {
                    filter { eq("id", id) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
        }.getOrNull()

    private fun buildSelectionPayload(userId: String, restaurantId: String): JsonObject =
        buildJsonObject {
            put("user_id", userId)
            put("restaurant_id", restaurantId)
        }

    private fun JsonObject.toRestaurant(savedIds: Set<String>): Restaurant? {
        val id = getString("id")
        if (id.isBlank()) return null
        val category = getString("category_name").ifBlank { getString("category") }
        val tags = RestaurantClassification.deriveTags(category, getStringList("tags").toSet())
        val reviewTagProfile = getDoubleMap("review_tag_profile")
        val dietaryFromDb = RestaurantClassification.profileFromLevels(
            vegan = getString("vegan_level"),
            vegetarian = getString("vegetarian_level"),
            halal = getString("halal_level"),
            glutenFree = getString("gluten_free_level"),
            dairyFree = getString("dairy_free_level"),
        )
        val dietaryProfile = if (RestaurantClassification.isAllUnknown(dietaryFromDb)) {
            RestaurantClassification.deriveDietaryProfile(category, tags)
        } else {
            dietaryFromDb
        }
        return Restaurant(
            id = id,
            name = getString("name").ifBlank { getString("title") },
            category = category,
            price = getString("price").ifBlank { getString("price_range") },
            eta = getString("eta").ifBlank { "ETA unavailable" },
            rating = getDouble("rating"),
            isSaved = id in savedIds,
            location = getString("city").ifBlank { getString("location", "Addis Ababa") },
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
            imageUrl = getString("image_url").takeIf { it.isNotBlank() },
            tags = tags,
            dietaryProfile = dietaryProfile,
            reviewTagProfile = reviewTagProfile,
        )
    }

    private fun JsonObject.toFeaturedItem(): FeaturedItem? {
        val name = getString("name")
        if (name.isBlank()) return null
        return FeaturedItem(
            name = name,
            price = getString("price"),
            description = getString("description"),
        )
    }

    private fun JsonObject.toRestaurantDetail(
        summary: Restaurant?,
        featuredItems: List<FeaturedItem>,
    ): RestaurantDetail {
        val id = getString("id").ifBlank { getString("restaurant_id") }
        val name = getString("name").ifBlank { getString("title") }.ifBlank { summary?.name.orEmpty() }
        val city = getString("city").ifBlank { getString("location") }.ifBlank { summary?.location ?: "Unknown" }
        val address = getString("address")
            .ifBlank { summary?.name?.let { "$it, $city" }.orEmpty() }
        val distance = getString("distance").ifBlank { "Distance unavailable" }
        val imageUrl = getString("image_url")
        val fromList = getStringList("images")
        val images = buildList {
            addAll(fromList)
            imageUrl.takeIf { it.isNotBlank() }?.let { u ->
                if (none { it == u }) add(u)
            }
        }.distinct()
        val resolvedImages = if (images.isEmpty()) {
            RestaurantClassification.defaultDetailImageKeys(
                getString("category_name").ifBlank { getString("category") }
                    .ifBlank { summary?.category.orEmpty() },
            )
        } else {
            images
        }
        val attributes = getStringList("attributes").ifEmpty {
            listOfNotNull(
                summary?.category?.takeIf { it.isNotBlank() },
                getString("category_name").takeIf { it.isNotBlank() },
                getString("category").takeIf { it.isNotBlank() }
            ).distinct()
        }
        return RestaurantDetail(
            restaurantId = id,
            name = name,
            images = resolvedImages,
            featuredItems = featuredItems,
            location = RestaurantLocation(
                city = city,
                address = address,
                distance = distance,
            ),
            reviews = emptyList(),
            description = getString("description").ifBlank {
                summary?.let { "${it.name} is known for ${it.category.lowercase()}." }.orEmpty()
            },
            rating = getDouble("rating", summary?.rating ?: 0.0),
            attributes = attributes,
            googleMapsLink = getFirstString("google_maps_url", "google_maps_link", "url"),
            restaurantWebsite = getFirstString("website", "restaurant_website"),
            priceRange = getString("price_range").ifBlank { getString("price", summary?.price ?: "$$") },
        )
    }

    private fun JsonObject.getString(key: String, fallback: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: fallback

    private fun JsonObject.getDouble(key: String, fallback: Double = 0.0): Double =
        this[key]?.jsonPrimitive?.doubleOrNull ?: fallback

    private fun JsonObject.getInt(key: String, fallback: Int = 0): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: fallback

    private fun JsonObject.getBoolean(key: String, fallback: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: fallback

    private fun JsonObject.getFirstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun JsonObject.getStringList(key: String): List<String> {
        val element = this[key] ?: return emptyList()
        return when (element) {
            is JsonArray ->
                element.mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() }
            is JsonPrimitive ->
                element.contentOrNull?.takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
            else -> emptyList()
        }
    }

    private fun JsonObject.getDoubleMap(key: String): Map<String, Double> {
        val element = this[key] ?: return emptyMap()
        if (element !is JsonObject) return emptyMap()
        return element.mapNotNull { (k, v) ->
            v.jsonPrimitive.doubleOrNull?.let { k to it }
        }.toMap()
    }

    private fun JsonObject.toReview(): Review? {
        val restaurantName = getString("restaurant_name")
        if (restaurantName.isBlank()) return null
        val source = getFirstString("source", "review_source")?.lowercase()?.trim()
        if (!source.isNullOrBlank() && (source == "google" || source.contains("google"))) return null
        if (getBoolean("is_google") || getBoolean("is_google_review")) return null

        val uid = getString("user_id").takeIf { it.isNotBlank() }
        val usernameCol = getString("username").takeIf { it.isNotBlank() }
        val displayName = usernameCol
            ?: getFirstString("reviewer_name", "author_name", "author", "reviewer_display_name", "review_author")
                ?.takeIf { it.isNotBlank() }

        return Review(
            id = getString("id", Random.nextInt(0, 1_000_000).toString()),
            restaurantName = restaurantName,
            tags = ReviewTagCatalog.normalizeTags(getStringList("tags")),
            content = getString("content"),
            rating = getInt("rating", 5),
            userId = uid,
            username = displayName,
            createdAt = getString("created_at").takeIf { it.isNotBlank() },
        )
    }

    private suspend fun searchSignalsById(): Map<String, String> {
        searchSignalCache?.let { return it }
        val cache = runCatching {
            val appRows = runCatching {
                client.postgrest[appReadModelTable]
                    .select(Columns.raw("id, description, attributes"))
                    .decodeList<JsonObject>()
            }.getOrDefault(emptyList())
            val legacyDetailRows = runCatching {
                client.postgrest[legacyRestaurantDetailsTable]
                    .select(Columns.raw("restaurant_id, description, attributes"))
                    .decodeList<JsonObject>()
            }.getOrDefault(emptyList())
            val detailRows = appRows + legacyDetailRows.filter { row ->
                val id = row.getString("restaurant_id")
                id.isNotBlank() && appRows.none { app -> app.getString("id") == id }
            }
            val featuredRows = client.postgrest[featuredItemsTable]
                .select(Columns.raw("restaurant_id, name, description"))
                .decodeList<JsonObject>()

            val featuredById = featuredRows
                .mapNotNull { row ->
                    val restaurantId = row.getString("restaurant_id")
                    if (restaurantId.isBlank()) return@mapNotNull null
                    val text = listOf(row.getString("name"), row.getString("description"))
                        .joinToString(" ")
                        .trim()
                    restaurantId to text
                }
                .groupBy({ it.first }, { it.second })

            detailRows.mapNotNull { row ->
                val restaurantId = row.getString("restaurant_id").ifBlank { row.getString("id") }
                if (restaurantId.isBlank()) return@mapNotNull null
                val description = normalizeSignal(row.getString("description"))
                val attributes = normalizeSignal(row.getStringList("attributes").joinToString(" "))
                val featured = normalizeSignal(featuredById[restaurantId]?.joinToString(" ").orEmpty())
                val combined = listOf(description, attributes, featured)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (combined.isBlank()) null else restaurantId to combined
            }.toMap()
        }.getOrElse { emptyMap() }
        searchSignalCache = cache
        return cache
    }

    private fun buildBaseSignals(restaurant: Restaurant): String =
        listOf(restaurant.name, restaurant.category, restaurant.location)
            .map { normalizeSignal(it) }
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun normalizeSignal(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private suspend fun buildVoteSessions(sessionIds: List<String>): List<VoteSession> {
        val normalizedIds = sessionIds.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) return emptyList()

        val sessions = client.postgrest[voteSessionsTable]
            .select(Columns.raw("id, title, created_at_epoch, is_closed, closed_at")) {
                filter { isIn("id", normalizedIds) }
            }
            .decodeList<JsonObject>()
            .associateBy { it.getString("id") }

        val participants = client.postgrest[voteParticipantsTable]
            .select(Columns.raw("session_id, user_id, display_name")) {
                filter { isIn("session_id", normalizedIds) }
            }
            .decodeList<JsonObject>()
            .groupBy { it.getString("session_id") }

        val restaurantsBySession = client.postgrest[voteRestaurantsTable]
            .select(Columns.raw("session_id, restaurant_id")) {
                filter { isIn("session_id", normalizedIds) }
            }
            .decodeList<JsonObject>()
            .groupBy { it.getString("session_id") }

        val votesBySession = client.postgrest[voteSessionVotesTable]
            .select(Columns.raw("session_id, user_id, restaurant_id")) {
                filter { isIn("session_id", normalizedIds) }
            }
            .decodeList<JsonObject>()
            .groupBy { it.getString("session_id") }

        val restaurantIndex = restaurants().associateBy { it.id }

        return normalizedIds.mapNotNull { sessionId ->
            val sessionRow = sessions[sessionId] ?: return@mapNotNull null
            val sessionParticipants = participants[sessionId].orEmpty().map { row ->
                val id = row.getString("user_id")
                val name = row.getString("display_name").ifBlank { id }
                VoteParticipant(id = id, name = name)
            }
            val restaurantIds = restaurantsBySession[sessionId].orEmpty()
                .map { it.getString("restaurant_id") }
                .filter { it.isNotBlank() }
            val restaurantList = restaurantIds.mapNotNull { restaurantIndex[it] }

            val votesByUser = mutableMapOf<String, MutableSet<String>>()
            votesBySession[sessionId].orEmpty().forEach { row ->
                val user = row.getString("user_id")
                val restaurantId = row.getString("restaurant_id")
                if (user.isBlank() || restaurantId.isBlank()) return@forEach
                votesByUser.getOrPut(user) { mutableSetOf() }.add(restaurantId)
            }

            VoteSession(
                id = sessionId,
                title = sessionRow.getString("title"),
                createdAtEpoch = sessionRow.getLong("created_at_epoch"),
                participants = sessionParticipants,
                restaurants = restaurantList,
                votesByUserId = votesByUser.mapValues { it.value.toSet() },
                isClosed = sessionRow.getBoolean("is_closed", false),
                closedAtEpoch = sessionRow.getLong("closed_at").takeIf { it > 0 }
            )
        }
    }

    private fun JsonObject.getLong(key: String, fallback: Long = 0L): Long {
        return this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: fallback
    }

    private fun searchRank(friend: Friend, needle: String): Int {
        val id = friend.id.lowercase()
        val name = friend.name.lowercase()
        return when {
            name == needle || id == needle -> 0
            name.startsWith(needle) || id.startsWith(needle) -> 1
            name.contains(needle) || id.contains(needle) -> 2
            else -> 3
        }
    }

}
