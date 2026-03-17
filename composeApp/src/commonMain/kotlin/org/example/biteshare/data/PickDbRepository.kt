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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.example.biteshare.domain.FeaturedItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.VoteParticipant
import org.example.biteshare.domain.VoteSession

class PickDbRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val userIdProvider: () -> String? = { null },
) : PickRepository {
    private val restaurantsTable = "restaurants2"
    private val friendsTable = "friends"
    private val restaurantDetailsTable = "restaurant_details"
    private val featuredItemsTable = "featured_items"
    private val userSavedRestaurantsTable = "user_saved_restaurants"
    private val usersTable = "users"
    private val voteSessionsTable = "vote_sessions"
    private val voteParticipantsTable = "vote_session_participants"
    private val voteRestaurantsTable = "vote_session_restaurants"
    private val voteSessionVotesTable = "vote_session_votes"
    private val defaultUserId = "demo_user"

    override suspend fun friends(): List<Friend> =
        runCatching {
            val userId = effectiveUserId()
            val raw = client.postgrest[friendsTable]
                .select(Columns.raw("id, user_id, name")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                val id = row.getString("id")
                if (id.isBlank()) return@mapNotNull null
                val name = row.getString("name").ifBlank { id }
                Friend(id = id, name = name)
            }
        }.getOrElse { emptyList() }

    override suspend fun locations(): List<String> =
        restaurants().map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> =
        runCatching {
            val savedIds = fetchSelectionsForUser(effectiveUserId())
            val raw = client.postgrest[restaurantsTable]
                .select(Columns.raw("*"))
                .decodeList<JsonObject>()
            raw.mapNotNull { it.toRestaurant(savedIds) }
        }.getOrElse { emptyList() }

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? =
        runCatching {
            val detailRow = client.postgrest[restaurantDetailsTable]
                .select(Columns.raw("*")) {
                    filter { eq("restaurant_id", id) }
                    limit(1)
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@runCatching null
            val featuredItems = client.postgrest[featuredItemsTable]
                .select(Columns.raw("restaurant_id, name, price, description")) {
                    filter { eq("restaurant_id", id) }
                }
                .decodeList<JsonObject>()
                .mapNotNull { it.toFeaturedItem() }
            val summary = restaurants().firstOrNull { it.id == id }
            detailRow.toRestaurantDetail(summary, featuredItems)
        }.getOrElse { null }

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

    private fun buildSelectionPayload(userId: String, restaurantId: String): JsonObject =
        buildJsonObject {
            put("user_id", userId)
            put("restaurant_id", restaurantId)
        }

    private fun JsonObject.toRestaurant(savedIds: Set<String>): Restaurant? {
        val id = getString("id")
        if (id.isBlank()) return null
        return Restaurant(
            id = id,
            name = getString("name"),
            category = getString("category"),
            price = getString("price"),
            eta = getString("eta"),
            rating = getDouble("rating"),
            isSaved = id in savedIds,
            location = getString("location", "Addis Ababa"),
            isOpenNow = getBoolean("is_open_now", true),
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
        val id = getString("restaurant_id")
        val name = getString("name").ifBlank { summary?.name.orEmpty() }
        val city = getString("city").ifBlank { summary?.location ?: "Unknown" }
        val address = getString("address")
            .ifBlank { summary?.name?.let { "$it, $city" }.orEmpty() }
        val distance = getString("distance")
        return RestaurantDetail(
            restaurantId = id,
            name = name,
            images = getStringList("images"),
            featuredItems = featuredItems,
            location = RestaurantLocation(
                city = city,
                address = address,
                distance = distance,
            ),
            reviews = emptyList(),
            description = getString("description"),
            rating = getDouble("rating", summary?.rating ?: 0.0),
            attributes = getStringList("attributes"),
            googleMapsLink = getFirstString("google_maps_link"),
            restaurantWebsite = getFirstString("restaurant_website"),
            priceRange = getString("price_range", summary?.price ?: "$$"),
        )
    }

    private fun JsonObject.getString(key: String, fallback: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: fallback

    private fun JsonObject.getDouble(key: String, fallback: Double = 0.0): Double =
        this[key]?.jsonPrimitive?.doubleOrNull ?: fallback

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

}
