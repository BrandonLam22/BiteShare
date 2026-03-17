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

class PickDbRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val userIdProvider: () -> String? = { null },
    private val userEmailProvider: () -> String? = { null },
    private val userNameProvider: () -> String? = { null },
) : PickRepository {
    private val restaurantsTable = "restaurants2"
    private val friendsTable = "friends"
    private val restaurantDetailsTable = "restaurant_details"
    private val featuredItemsTable = "featured_items"
    private val userSavedRestaurantsTable = "user_saved_restaurants"
    private val usersTable = "users"
    private val defaultUserId = "demo_user"
    private var cachedIdentityKey: String? = null
    private var cachedUserId: String? = null

    override suspend fun friends(): List<Friend> =
        runCatching {
            val userId = resolveUserId()
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
            val savedIds = fetchSelectionsForUser(resolveUserId())
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
            val userId = resolveUserId()
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
            val userId = resolveUserId()
            val raw = client.postgrest[usersTable]
                .select(Columns.raw("id, food_restrictions")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<JsonObject>()
            val row = raw.firstOrNull() ?: return@runCatching emptyList()
            row.getStringList("food_restrictions")
        }.getOrElse { emptyList() }

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

    override suspend fun currentUserId(): String? =
        runCatching { resolveUserId() }.getOrNull()

    private suspend fun resolveUserId(): String {
        val providedId = userIdProvider()?.trim().orEmpty()
        val providedEmail = userEmailProvider()?.trim().orEmpty()
        val providedName = userNameProvider()?.trim().orEmpty()
        val identityKey = listOf(providedId, providedEmail, providedName).joinToString("|")
        if (cachedIdentityKey == identityKey && cachedUserId != null) {
            return cachedUserId ?: defaultUserId
        }

        cachedIdentityKey = identityKey
        cachedUserId = null

        val fallback = defaultUserId
        val resolved = runCatching {
            val byEmail = if (providedEmail.isNotBlank()) findUserIdBy("email", providedEmail) else null
            if (!byEmail.isNullOrBlank()) return@runCatching byEmail
            val byName = if (providedName.isNotBlank()) findUserIdBy("username", providedName) else null
            if (!byName.isNullOrBlank()) return@runCatching byName
            if (providedId.isNotBlank()) return@runCatching providedId
            fallback
        }.getOrElse { fallback }

        cachedUserId = resolved
        return resolved
    }

    private suspend fun findUserIdBy(column: String, value: String): String? {
        val raw = client.postgrest[usersTable]
            .select(Columns.raw("id")) {
                filter { eq(column, value) }
                limit(1)
            }
            .decodeList<JsonObject>()
        return raw.firstOrNull()?.getString("id")?.takeIf { it.isNotBlank() }
    }

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

}
