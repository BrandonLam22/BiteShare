package org.example.biteshare.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.FeaturedItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import org.example.biteshare.domain.UserRow
import org.jetbrains.compose.resources.getString
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User
import kotlin.random.Random

class SupabaseRepository(
    private val model: Model,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val fallback: FakeRepository = FakeRepository(model),
) : BiteShareRepository {

    private val usersTable = "users"
    private val reviewsTable = "reviews"

    override suspend fun login(username: String, password: String): User? {
        val normalizedUsername = username.trim()

        return client.from(usersTable)
            .select(Columns.raw("*")) {
                filter {
                    eq("username", normalizedUsername)
                    eq("password", password)
                }
                limit(1)
            }
            .decodeSingleOrNull<JsonObject>()
            ?.toUser()
    }

    override suspend fun signup(username: String, password: String, email: String): User? {
        val normalizedUsername = username.trim()
        val normalizedEmail = email.trim().lowercase()
        val alreadyExists = client.from(usersTable)
            .select(columns = Columns.list("id")) {
                filter {
                    or {
                        eq("username", normalizedUsername)
                        eq("email", normalizedEmail)
                    }
                }
                limit(1)
            }
            .decodeSingleOrNull<JsonObject>() != null
        if (alreadyExists) return null

        val newId = Random.nextInt(0, 1_000_000).toString()
        val newUserJson = buildJsonObject {
            put("id", newId)
            put("username", normalizedUsername)
            put("email", normalizedEmail)
            put("password", password)
            put("bio", "")
            put("preferences", buildJsonArray { })
            put("food_restrictions", buildJsonArray { })
            put("latitude", 0.0)
            put("longitude", 0.0)
            put("notifications_enabled", true)
        }
        client.from(usersTable).insert(newUserJson)
        return newUserJson.toUser()
    }

    private val restaurantsTable = "restaurants2"

    override suspend fun getHomeFeed(userName: String): HomeFeed {
        val restaurants = restaurants()
        if (restaurants.isEmpty()) {
            println("SupabaseRepository.getHomeFeed: restaurants empty, using fallback")
            return fallback.getHomeFeed(userName)
        }

        val grouped = restaurants
            .groupBy { it.category.ifBlank { "Other" } }
            .toList()
            .sortedByDescending { (_, items) -> items.size }

        val categories = grouped.map { (category, _) ->
            CategoryItem(
                id = normalizeCategoryId(category),
                label = category,
            )
        }

        val (drinkGroups, dishGroups) = grouped.partition { (category, _) -> isDrinkCategory(category) }
        val popularDishes = dishGroups.take(4).mapIndexed { idx, (category, items) ->
            PopularItem(
                id = "dish_${idx + 1}",
                title = category,
                subtitle = "${items.size} places",
            )
        }
        val popularDrinks = drinkGroups.take(4).mapIndexed { idx, (category, items) ->
            PopularItem(
                id = "drink_${idx + 1}",
                title = category,
                subtitle = "${items.size} places",
            )
        }

        return HomeFeed(
            greeting = "Hi, $userName",
            categories = categories,
            popularDishes = popularDishes,
            popularDrinks = popularDrinks,
        )
    }

    private var cachedUserRow: UserRow? = null

    private suspend fun fetchUserRow(): UserRow? {
        cachedUserRow?.let { return it }
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return try {
            val row = client.from("users")
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserRow>()
            cachedUserRow = row
            row
        } catch (t: Throwable) {
            println("fetchUserRow error: ${t.message}")
            null
        }
    }

    override suspend fun browseRestaurants(): List<Restaurant> = restaurants()

    override suspend fun getRestaurantsByTag(tag: String): List<Restaurant> {
        val normalized = tag.lowercase().trim()
        if (normalized.isBlank()) return restaurants()
        return restaurants().filter { restaurant ->
            restaurant.category.lowercase().contains(normalized) ||
                restaurant.name.lowercase().contains(normalized)
        }
    }

    override suspend fun getRestaurantById(id: String): Restaurant? =
        restaurants().firstOrNull { it.id == id }

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? {
        val summary = getRestaurantById(id) ?: return null
        val fallbackDetail = fallback.getRestaurantDetailById(id)
        val reviews = getReviewsForRestaurant(summary.name)
        val baseDetail = fallbackDetail ?: buildRestaurantDetail(summary)
        val mergedReviews = (reviews + baseDetail.reviews)
            .distinctBy { it.id }
        val averageRating = mergedReviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: baseDetail.rating

        return baseDetail.copy(
            reviews = mergedReviews,
            rating = averageRating,
        )
    }

    override suspend fun getReviewsForRestaurant(restaurantName: String): List<Review> {
        val normalizedName = restaurantName.trim()
        if (normalizedName.isBlank()) return emptyList()

        return try {
            val reviewRows = client.from(reviewsTable)
                .select(Columns.raw("*")) {
                    filter {
                        eq("restaurant_name", normalizedName)
                    }
                }
                .decodeList<JsonObject>()

            val usernameByUserId = mutableMapOf<String, String?>()
            reviewRows.mapNotNull { row ->
                val userId = row.getString("user_id").takeIf { it.isNotBlank() }
                if (userId != null && userId !in usernameByUserId) {
                    usernameByUserId[userId] = getUsernameByUserId(userId)
                }
                row.toReview(usernameByUserId[userId])
            }.sortedByDescending { review -> review.createdAt.orEmpty() }
        } catch (t: Throwable) {
            fallback.getReviewsForRestaurant(normalizedName)
        }
    }

    override suspend fun submitReview(review: Review) {
        val matchedRestaurant = restaurants().firstOrNull { restaurant ->
            restaurant.name.equals(review.restaurantName, ignoreCase = true)
        }
        val currentUserId = model.currentUser?.id?.takeIf { it.isNotBlank() }

        val newReviewJson = buildJsonObject {
            put("id", review.id)
            matchedRestaurant?.id?.let { restaurantId ->
                put("restaurant_id", restaurantId)
            }
            put("restaurant_name", review.restaurantName)
            currentUserId?.let { userId ->
                put("user_id", userId)
            }
            put("rating", review.rating)
            put("content", review.content)
            put("tags", buildJsonArray {
                review.tags.forEach { tag -> add(JsonPrimitive(tag)) }
            })
        }

        client.from(reviewsTable).insert(newReviewJson)
        model.addReview(review)
    }

    override suspend fun getSavedRestaurants(): List<Restaurant> {
        val savedIds = model.getSavedRestaurantIds()
        if (savedIds.isEmpty()) return emptyList()
        return restaurants().filter { it.id in savedIds }
    }

    override suspend fun toggleSaved(restaurantId: String) {
        model.toggleSavedRestaurant(restaurantId)
    }

    override suspend fun getProfile(): ProfileData {
        val row = fetchUserRow() ?: return fallback.getProfile()
        return ProfileData(
            name = row.username,
            email = row.email,
            friendCount = row.friendCount,
            notificationsEnabled = row.notificationsEnabled
        )
    }

    override suspend fun updateNotificationPreference(enabled: Boolean) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) {
            fallback.updateNotificationPreference(enabled)
            return
        }
        try {
            client.from("users").update({
                set("notifications_enabled", enabled)
            }) { filter { eq("id", userId) } }
            cachedUserRow = cachedUserRow?.copy(notificationsEnabled = enabled) // update cache in place instead of clearing it
        } catch (t: Throwable) {
            println("updateNotificationPreference error: ${t.message}")
            fallback.updateNotificationPreference(enabled)
        }
    }

    override suspend fun logout() {
        cachedUserRow = null
        try {
            client.auth.signOut()
        } catch (t: Throwable) {
            println("logout error: ${t.message}")
        }
        fallback.logout()
    }

    override suspend fun getEditableProfile(): EditableProfile {
        val row = fetchUserRow() ?: return fallback.getEditableProfile()
        return EditableProfile(
            username = row.username,
            email = row.email,
            bio = row.bio,
            preferences = row.preferences,
            foodRestrictions = row.foodRestrictions
        )
    }

    override suspend fun verifyCurrentPassword(password: String): Boolean {
        return try {
            val email = client.auth.currentUserOrNull()?.email ?: return false
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (t: Throwable) {
            false
        }
    }

    override suspend fun updateProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>
    ) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) {
            fallback.updateProfile(username, email, bio, preferences, foodRestrictions)
            return
        }
        try {
            client.from("users").update({
                set("username", username)
                set("email", email)
                set("bio", bio)
                set("preferences", preferences)
                set("food_restrictions", foodRestrictions)
            }) { filter { eq("id", userId) } }
            cachedUserRow = null  // invalidate so next fetch gets fresh data
        } catch (t: Throwable) {
            println("updateProfile error: ${t.message}")
            fallback.updateProfile(username, email, bio, preferences, foodRestrictions)
        }
    }

    override suspend fun friends(): List<Friend> = fallback.friends()

    override suspend fun locations(): List<String> =
        restaurants().map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> {
        val savedIds = model.getSavedRestaurantIds()
        return try {
            val raw = client.postgrest[restaurantsTable]
                .select(Columns.raw("*"))
                .decodeList<JsonObject>()
            val mapped = raw.mapNotNull { it.toDomain(savedIds) }
            println("SupabaseRepository.restaurants: fetched=${raw.size} mapped=${mapped.size}")
            if (raw.isNotEmpty()) {
                println("SupabaseRepository.restaurants: sample keys=${raw.first().keys}")
            }
            mapped
        } catch (t: Throwable) {
            println("SupabaseRepository.restaurants: error=${t.message}")
            t.printStackTrace()
            fallback.restaurants()
        }
    }

    override suspend fun userPreferences(): List<String> {
        return fetchUserRow()?.preferences ?: fallback.userPreferences()
    }

    override suspend fun userRestrictions(): List<String> {
        return fetchUserRow()?.foodRestrictions ?: fallback.userRestrictions()
    }


    override suspend fun getPassword(): String = ""

    override suspend fun updatePassword(newPassword: String) {
        try {
            client.auth.updateUser { password = newPassword }
        } catch (t: Throwable) {
            println("updatePassword error: ${t.message}")
            fallback.updatePassword(newPassword)
        }
    }

    private fun JsonObject.toUser(): User? {
        val id = getString("id")
        if (id.isBlank()) return null

        return User(
            id = id,
            username = getString("username"),
            email = getString("email"),
            password = getString("password"),
            bio = getString("bio"),
            preferences = getStringList("preferences"),
            foodRestrictions = getStringList("food_restrictions"),
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
        )
    }

    private fun JsonObject.toDomain(savedIds: Set<String>): Restaurant? {
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

    private suspend fun getUsernameByUserId(userId: String): String? {
        return client.from(usersTable)
            .select(Columns.list("username")) {
                filter {
                    eq("id", userId)
                }
                limit(1)
            }
            .decodeSingleOrNull<JsonObject>()
            ?.getString("username")
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.toReview(username: String?): Review? {
        val restaurantName = getString("restaurant_name")
        if (restaurantName.isBlank()) return null

        return Review(
            id = getString("id", Random.nextInt(0, 1_000_000).toString()),
            restaurantName = restaurantName,
            tags = getStringList("tags"),
            content = getString("content"),
            rating = getInt("rating", 5),
            userId = getString("user_id").takeIf { it.isNotBlank() },
            username = username,
            createdAt = getString("created_at").takeIf { it.isNotBlank() },
        )
    }

    private fun buildRestaurantDetail(summary: Restaurant): RestaurantDetail {
        val query = summary.name.replace(" ", "+")
        val imageKey = when (summary.category.lowercase()) {
            "pizza" -> "pizza_home"
            "sushi" -> "beyayenet_home"
            "burgers", "fast food" -> "chesse_burger_home"
            "coffee", "cafe", "drink" -> "coffee_home"
            "italian" -> "local_home"
            else -> "coffee_home"
        }

        return RestaurantDetail(
            restaurantId = summary.id,
            name = summary.name,
            images = listOf(imageKey, "drink_home"),
            featuredItems = listOf(
                FeaturedItem(
                    name = "Chef Recommendation",
                    price = summary.price,
                    description = "Popular choice from this restaurant"
                )
            ),
            location = RestaurantLocation(
                city = summary.location,
                address = "${summary.name}, ${summary.location}",
                distance = summary.eta
            ),
            reviews = emptyList(),
            description = "${summary.name} is known for ${summary.category.lowercase()} and consistent service.",
            rating = summary.rating,
            attributes = listOf(summary.category, "Popular"),
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=$query+${summary.location.replace(" ", "+")}",
            restaurantWebsite = "https://example.com/${summary.id}",
            priceRange = summary.price,
        )
    }

    private fun JsonObject.getString(key: String, fallback: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: fallback

    private fun JsonObject.getDouble(key: String, fallback: Double = 0.0): Double =
        this[key]?.jsonPrimitive?.doubleOrNull ?: fallback

    private fun JsonObject.getInt(key: String, fallback: Int = 0): Int =
        this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: fallback

    private fun JsonObject.getBoolean(key: String, fallback: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: fallback

    private fun JsonObject.getStringList(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull }
            ?: emptyList()

    private fun isDrinkCategory(category: String): Boolean {
        val key = category.lowercase()
        return key.contains("coffee") ||
            key.contains("tea") ||
            key.contains("drink") ||
            key.contains("bubble")
    }

    private fun normalizeCategoryId(category: String): String =
        category.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}
