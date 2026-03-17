package org.example.biteshare.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.EditableProfile
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

class SupabaseRepository(
    private val model: Model,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val fallback: FakeRepository = FakeRepository(model),
) : BiteShareRepository {
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

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? =
        fallback.getRestaurantDetailById(id)

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

    private fun JsonObject.getString(key: String, fallback: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: fallback

    private fun JsonObject.getDouble(key: String, fallback: Double = 0.0): Double =
        this[key]?.jsonPrimitive?.doubleOrNull ?: fallback

    private fun JsonObject.getBoolean(key: String, fallback: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: fallback

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
