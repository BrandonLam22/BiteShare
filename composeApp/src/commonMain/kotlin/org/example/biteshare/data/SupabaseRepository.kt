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
import org.example.biteshare.domain.GeoPoint
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantClassification
import org.example.biteshare.domain.RestaurantDetail
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import org.example.biteshare.domain.UserRow
import org.jetbrains.compose.resources.getString
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.ReviewTagCatalog
import org.example.biteshare.domain.User
import org.example.biteshare.viewmodel.FriendDetails
import org.example.biteshare.viewmodel.FriendReview
import kotlin.collections.map
import kotlin.random.Random

@Serializable
data class UserSavedRestaurant(
    @SerialName("user_id")
    val userId: String,
    @SerialName("restaurant_id")
    val restaurantId: String
)
class SupabaseRepository(
    private val model: Model,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val fallback: FakeRepository = FakeRepository(model),
) : BiteShareRepository {

    private val usersTable = "users"
    private val reviewsTable = "reviews"
    private val userSavedRestaurantsTable = "user_saved_restaurants"

    // FRIEND REQUEST
    private val pickDbRepo = PickDbRepository(
        client = client,
        userIdProvider = { model.currentUser?.id }
    )

    override suspend fun sendFriendRequest(targetUserId: String) =
        pickDbRepo.sendFriendRequest(targetUserId)

    override suspend fun incomingFriendRequests() =
        pickDbRepo.incomingFriendRequests()

    override suspend fun outgoingFriendRequests() =
        pickDbRepo.outgoingFriendRequests()

    override suspend fun acceptFriendRequest(requestId: String): Boolean {
        val accepted = pickDbRepo.acceptFriendRequest(requestId)
        if (accepted) {
            cachedUserRow = null
        }
        return accepted
    }

    override suspend fun rejectFriendRequest(requestId: String) =
        pickDbRepo.rejectFriendRequest(requestId)

    override suspend fun login(username: String, password: String): User? {
        val normalizedUsername = username.trim()

        val user = client.from(usersTable)
            .select(Columns.raw("*")) {
                filter {
                    eq("username", normalizedUsername)
                    eq("password", password)
                }
                limit(1)
            }
            .decodeSingleOrNull<JsonObject>()
            ?.toUser()


        if (user != null) {
            model.applyAuthenticatedUser(user)
        }

        return user
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
        //client.from(usersTable).insert(newUserJson)
        //return newUserJson.toUser()
        val newUser = newUserJson.toUser() ?: return null
        model.applyAuthenticatedUser(newUser)  // ADD THIS
        return newUser

    }

    // Prefer the candidate app read model for browse/detail display, while keeping legacy-only
    // rows in the merged result so current saved/vote/comment flows do not disappear mid-migration.
    private val restaurantsTable = "restaurants2"
    private val appReadModelTable = "google_maps_places_app"

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
        val userId = model.currentUser?.id ?: return null
        return try {
            val row = client.from(usersTable)
                .select { filter { eq("id", userId) } }
                .decodeSingle<UserRow>()
            val friendCount = client.from("friends")
                .select { filter { eq("user_id", userId) } }
                .decodeList<JsonObject>()
                .size
            println("fetchUserRow: userId=$userId")
            val rowWithCount = row.copy(friendCount = friendCount)
            cachedUserRow = rowWithCount
            rowWithCount
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

    override suspend fun searchRestaurants(query: String, source: List<Restaurant>): List<Restaurant> {
        val needle = query.lowercase().trim()
        val pool = if (source.isNotEmpty()) source else restaurants()
        if (needle.isBlank()) return pool
        val detailsByRestaurantId = loadSearchDetailsByRestaurantId()

        return pool.filter { restaurant ->
            val detail = detailsByRestaurantId[restaurant.id]
            val description = detail?.getString("description").orEmpty().lowercase()
            val attributes = detail?.getStringList("attributes")
                ?.joinToString(" ")
                .orEmpty()
                .lowercase()
            restaurant.name.lowercase().contains(needle) ||
                restaurant.category.lowercase().contains(needle) ||
                restaurant.location.lowercase().contains(needle) ||
                description.contains(needle) ||
                attributes.contains(needle)
        }
    }

    override suspend fun getRestaurantById(id: String): Restaurant? =
        restaurants().firstOrNull { it.id == id }

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? {
        val summary = getRestaurantById(id) ?: return null
        val dbDetail = pickDbRepo.getRestaurantDetailById(id)
        val fallbackDetail = fallback.getRestaurantDetailById(id)
        val reviews = getReviewsForRestaurant(summary.name, summary.id)
        val baseDetail = dbDetail ?: fallbackDetail ?: buildRestaurantDetail(summary)
        val mergedReviews = (reviews + baseDetail.reviews)
            .distinctBy { it.id }
        val averageRating = when {
            mergedReviews.isNotEmpty() ->
                mergedReviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0
            baseDetail.rating > 0.0 -> baseDetail.rating
            else -> summary.rating
        }

        return baseDetail.copy(
            reviews = mergedReviews,
            rating = averageRating,
        )
    }

    /**
     * In-app reviews: first matches `restaurant_id` or case-insensitive full `restaurant_name`,
     * then falls back to a two-word `ilike` pattern when names in `reviews` are shorter than
     * the current Google-facing title.
     */
    override suspend fun getReviewsForRestaurant(restaurantName: String, restaurantId: String?): List<Review> {
        val normalizedName = restaurantName.trim()
        if (normalizedName.isBlank() && restaurantId.isNullOrBlank()) return emptyList()

        return try {
            val reviewRows = fetchReviewRowsForRestaurant(normalizedName, restaurantId)
            val usernameByUserId = mutableMapOf<String, String?>()
            reviewRows.mapNotNull { row ->
                val userId = row.getString("user_id").takeIf { it.isNotBlank() }
                if (userId != null && userId !in usernameByUserId) {
                    usernameByUserId[userId] = getUsernameByUserId(userId)
                }
                row.toReview(usernameByUserId[userId])
            }
                .sortedByDescending { review -> review.createdAt.orEmpty() }
                .take(25)
        } catch (t: Throwable) {
            fallback.getReviewsForRestaurant(normalizedName, restaurantId)
        }
    }

    private suspend fun fetchReviewRowsForRestaurant(
        normalizedName: String,
        restaurantId: String?,
    ): List<JsonObject> {
        val primary = runCatching {
            client.from(reviewsTable)
                .select(Columns.raw("*")) {
                    filter {
                        when {
                            !restaurantId.isNullOrBlank() && normalizedName.isNotBlank() -> {
                                or {
                                    eq("restaurant_id", restaurantId)
                                    ilike("restaurant_name", normalizedName)
                                }
                            }
                            !restaurantId.isNullOrBlank() -> {
                                eq("restaurant_id", restaurantId)
                            }
                            else -> {
                                ilike("restaurant_name", normalizedName)
                            }
                        }
                    }
                }
                .decodeList<JsonObject>()
        }.getOrDefault(emptyList()).distinctBy { it.getString("id") }

        if (primary.isNotEmpty()) return primary
        if (normalizedName.isBlank()) return emptyList()

        val words = normalizedName.split(Regex("\\s+")).filter { it.length > 1 }
        val fuzzyPattern = when {
            words.size >= 2 -> "%${words[0]}%${words[1]}%"
            words.size == 1 && words[0].length >= 4 -> "%${words[0]}%"
            else -> return emptyList()
        }

        return runCatching {
            client.from(reviewsTable)
                .select(Columns.raw("*")) {
                    filter {
                        ilike("restaurant_name", fuzzyPattern)
                    }
                }
                .decodeList<JsonObject>()
        }.getOrDefault(emptyList())
            .distinctBy { it.getString("id") }
            .filter { row ->
                namesLikelySame(normalizedName, row.getString("restaurant_name"))
            }
    }

    private fun namesLikelySame(appName: String, storedReviewName: String): Boolean {
        val a = appName.trim().lowercase()
        val b = storedReviewName.trim().lowercase()
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        if (a.contains(b) || b.contains(a)) return true
        val aw = a.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val bw = b.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val inter = aw.intersect(bw)
        return inter.size >= 2 ||
            (inter.isNotEmpty() && (inter.maxOfOrNull { it.length } ?: 0) >= 5)
    }

    override suspend fun reviewsByUserIds(userIds: Set<String>): List<Review> =
        pickDbRepo.reviewsByUserIds(userIds)

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
        val savedIds = fetchSavedRestaurantIds()
        if (savedIds.isEmpty()) return emptyList()
        return restaurants().filter { it.id in savedIds }
    }

    override suspend fun toggleSaved(restaurantId: String) {
        val userId = resolveCurrentUserId()
        if (userId == null) {
            fallback.toggleSaved(restaurantId)
            return
        }

        val currentSavedIds = fetchSavedRestaurantIds(userId)
        val shouldSave = restaurantId !in currentSavedIds

        try {
            if (shouldSave) {
                client.postgrest[userSavedRestaurantsTable].upsert(
                    body = JsonArray(listOf(buildSavedRestaurantPayload(userId, restaurantId)))
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
            syncSavedRestaurantsWithModel(
                if (shouldSave) currentSavedIds + restaurantId else currentSavedIds - restaurantId
            )
        } catch (t: Throwable) {
            println("toggleSaved error: ${t.message}")
            fallback.toggleSaved(restaurantId)
        }
    }

    override suspend fun getProfile(): ProfileData {
        val row = fetchUserRow() ?: return fallback.getProfile()
        val userId = model.currentUser?.id ?: return fallback.getProfile()
        val freshFriendCount = try {
            client.from("friends")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
                .size
        } catch (t: Throwable) {
            row.friendCount
        }

        return ProfileData(
            name = row.username,
            email = row.email,
            friendCount = freshFriendCount,
            notificationsEnabled = row.notificationsEnabled
        )
    }

    override suspend fun updateNotificationPreference(enabled: Boolean) {
        val userId = resolveCurrentUserId()
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
        val expectedPassword = getPassword()
        return expectedPassword.isNotBlank() && password == expectedPassword
    }

    override suspend fun updateProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>
    ) {
        val userId = resolveCurrentUserId()
        if (userId == null) {
            fallback.updateProfile(username, email, bio, preferences, foodRestrictions)
            return
        }
        val normalizedUsername = username.trim()
        val normalizedEmail = email.trim().lowercase()
        val existingRow = cachedUserRow ?: fetchUserRow()
        try {
            client.from("users").update({
                set("username", normalizedUsername)
                set("email", normalizedEmail)
                set("bio", bio)
                set("preferences", preferences)
                set("food_restrictions", foodRestrictions)
            }) { filter { eq("id", userId) } }
            model.updateUserProfile(
                username = normalizedUsername,
                email = normalizedEmail,
                bio = bio,
                preferences = preferences,
                foodRestrictions = foodRestrictions
            )
            cachedUserRow = existingRow?.copy(
                username = normalizedUsername,
                email = normalizedEmail,
                bio = bio,
                preferences = preferences,
                foodRestrictions = foodRestrictions
            ) ?: UserRow(
                id = userId,
                username = normalizedUsername,
                email = normalizedEmail,
                bio = bio,
                preferences = preferences,
                foodRestrictions = foodRestrictions
            )
        } catch (t: Throwable) {
            println("updateProfile error: ${t.message}")
            fallback.updateProfile(username, email, bio, preferences, foodRestrictions)
        }
    }

 override suspend fun friends(): List<Friend> {
        val userId = model.currentUser?.id ?: return fallback.friends()
        return try {
            val friendRows = client.from("friends")
                .select(Columns.raw("*")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()

            friendRows.mapNotNull { row ->
                val friendId = row.getString("friend_id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val friendUser = client.from(usersTable)
                    .select(Columns.list("id", "username")) {
                        filter { eq("id", friendId) }
                        limit(1)
                    }
                    .decodeSingleOrNull<JsonObject>() ?: return@mapNotNull null
                Friend(
                    id = friendId,
                    name = friendUser.getString("username")
                )
            }
        } catch (t: Throwable) {
            println("friends error: ${t.message}")
            fallback.friends()
        }
    }

    override suspend fun locations(): List<String> =
        restaurants().map { it.location }.distinct().sorted()

    override suspend fun restaurants(): List<Restaurant> {
        val savedIds = fetchSavedRestaurantIds()
        return try {
            val raw = loadRestaurantRows()
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

    private suspend fun loadRestaurantRows(): List<JsonObject> {
        val appRows = runCatching {
            client.postgrest[appReadModelTable]
                .select(Columns.raw("*"))
                .decodeList<JsonObject>()
        }.getOrDefault(emptyList())
        val legacyRows = runCatching {
            client.postgrest[restaurantsTable]
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

    override suspend fun userPreferences(): List<String> {
        return fetchUserRow()?.preferences ?: fallback.userPreferences()
    }

    override suspend fun userRestrictions(): List<String> {
        return fetchUserRow()?.foodRestrictions ?: fallback.userRestrictions()
    }

    override suspend fun currentUserId(): String? = resolveCurrentUserId()

    override suspend fun currentUserLocation(): GeoPoint? {
        val user = model.currentUser
        if (user == null || (user.latitude == 0.0 && user.longitude == 0.0)) return null
        return GeoPoint(user.latitude, user.longitude)
    }


    override suspend fun getPassword(): String {
        model.currentUser?.password?.takeIf { it.isNotBlank() }?.let { return it }

        val userId = resolveCurrentUserId() ?: return ""
        return try {
            client.from(usersTable)
                .select(Columns.list("password")) {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<JsonObject>()
                ?.getString("password")
                .orEmpty()
        } catch (t: Throwable) {
            println("getPassword error: ${t.message}")
            ""
        }
    }

    override suspend fun updatePassword(newPassword: String) {
        val userId = resolveCurrentUserId()
        if (userId == null) {
            fallback.updatePassword(newPassword)
            return
        }
        try {
            client.from(usersTable).update({
                set("password", newPassword)
            }) { filter { eq("id", userId) } }
            model.updateUserPassword(newPassword)
            runCatching {
                client.auth.updateUser { password = newPassword }
            }.onFailure { throwable ->
                println("updatePassword auth sync warning: ${throwable.message}")
            }
        } catch (t: Throwable) {
            println("updatePassword error: ${t.message}")
            fallback.updatePassword(newPassword)
        }
    }

    private fun resolveCurrentUserId(): String? {
        val modelUserId = model.currentUser?.id?.trim().orEmpty()
        if (modelUserId.isNotBlank()) {
            return modelUserId
        }
        return client.auth.currentUserOrNull()?.id?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun fetchSavedRestaurantIds(): Set<String> {
        val userId = resolveCurrentUserId() ?: return model.getSavedRestaurantIds()
        return fetchSavedRestaurantIds(userId)
    }

    private suspend fun fetchSavedRestaurantIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            val raw = client.postgrest[userSavedRestaurantsTable]
                .select(Columns.raw("restaurant_id")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JsonObject>()
            raw.mapNotNull { row ->
                row.getString("restaurant_id").takeIf { it.isNotBlank() }
            }.toSet().also { savedIds ->
                syncSavedRestaurantsWithModel(savedIds)
            }
        } catch (t: Throwable) {
            println("fetchSavedRestaurantIds error: ${t.message}")
            model.getSavedRestaurantIds()
        }
    }

    private fun syncSavedRestaurantsWithModel(savedIds: Set<String>) {
        if (model.currentUser != null) {
            model.updateSavedRestaurants(savedIds.toList())
        }
    }

    private fun buildSavedRestaurantPayload(userId: String, restaurantId: String): JsonObject =
        buildJsonObject {
            put("user_id", userId)
            put("restaurant_id", restaurantId)
        }

    private suspend fun loadSearchDetailsByRestaurantId(): Map<String, JsonObject> {
        val appRows = runCatching {
            client.postgrest[appReadModelTable]
                .select(Columns.raw("id, description, attributes"))
                .decodeList<JsonObject>()
                .associateBy { row -> row.getString("id") }
        }.getOrDefault(emptyMap())
        val legacyRows = runCatching {
            client.postgrest["restaurant_details"]
                .select(Columns.raw("restaurant_id, description, attributes"))
                .decodeList<JsonObject>()
                .associateBy { row -> row.getString("restaurant_id") }
        }.getOrDefault(emptyMap())
        return appRows + legacyRows.filterKeys { it !in appRows }
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

    private fun JsonObject.getFirstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private fun JsonObject.toReview(usernameFromUsersTable: String?): Review? {
        val restaurantName = getString("restaurant_name")
        if (restaurantName.isBlank()) return null
        val source = getFirstString("source", "review_source")?.lowercase()?.trim()
        if (!source.isNullOrBlank() && (source == "google" || source.contains("google"))) return null
        if (getBoolean("is_google") || getBoolean("is_google_review")) return null

        val uid = getString("user_id").takeIf { it.isNotBlank() }
        val displayName = usernameFromUsersTable
            ?: getFirstString("username", "reviewer_name", "author_name", "author", "reviewer_display_name", "review_author")
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

    private fun buildRestaurantDetail(summary: Restaurant): RestaurantDetail {
        val query = summary.name.replace(" ", "+")

        return RestaurantDetail(
            restaurantId = summary.id,
            name = summary.name,
            images = RestaurantClassification.defaultDetailImageKeys(summary.category),
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

    private fun JsonObject.getDoubleMap(key: String): Map<String, Double> {
        val element = this[key] as? JsonObject ?: return emptyMap()
        return element.mapNotNull { (k, v) ->
            v.jsonPrimitive.doubleOrNull?.let { k to it }
        }.toMap()
    }

    private fun isDrinkCategory(category: String): Boolean {
        val key = category.lowercase()
        return key.contains("coffee") ||
            key.contains("tea") ||
            key.contains("drink") ||
            key.contains("bubble")
    }

    private fun normalizeCategoryId(category: String): String =
        category.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')



    override suspend fun getUserReviews(): List<Review> {
        return withContext(Dispatchers.Default) {
            try {
                val userId = model.currentUser?.id ?: return@withContext emptyList()

                client
                    .from(reviewsTable)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Review>()   // 👈 direct decode

            } catch (e: Exception) {
                e.printStackTrace()
                fallback.getUserReviews()
            }
        }
    }
    override suspend fun deleteReview(reviewId: String) {
        try {
            client
                .from(reviewsTable)
                .delete {
                    filter {
                        eq("id", reviewId)
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
            fallback.deleteReview(reviewId)
        }
    }

    override suspend fun editReview(
        reviewId: String,
        newRating: Int,
        newComment: String
    ) {
        try {
            client
                .from(reviewsTable)
                .update(
                    mapOf(
                        "rating" to newRating,
                        "content" to newComment
                    )
                ) {
                    filter {
                        eq("id", reviewId)
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
            fallback.editReview(reviewId, newRating, newComment)
        }
    }
    override suspend fun getFriendDetails(friendId: String): FriendDetails {
        return try {
            // ✅ safer: don't decode full User
            val userJson = client.from(usersTable)
                .select {
                    filter { eq("id", friendId) }
                    limit(1)
                }
                .decodeSingle<JsonObject>()

            val username = userJson["username"]?.jsonPrimitive?.content ?: "Unknown"

            val preferences = userJson["preferences"]
                ?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf("None")

            val restrictions = userJson["food_restrictions"]
                ?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf("None")
            val reviews = client.from(reviewsTable)
                .select {
                    filter { eq("user_id", friendId) }
                }
                .decodeList<Review>()
                .sortedByDescending { it.createdAt }  // ✅ sort newest first
                .take(5)
            println(reviews)

            val savedRestaurants = client.from(userSavedRestaurantsTable)
                .select {
                    filter { eq("user_id", friendId) }
                }
                .decodeList<UserSavedRestaurant>()

            val savedRestaurantNames = savedRestaurants.mapNotNull { saved ->
                getRestaurantById(saved.restaurantId)?.name
            }

            FriendDetails(
                name = username,
                preferences = preferences as List<String>,
                restrictions = restrictions as List<String>,
                reviews = reviews.map {
                    FriendReview(
                        restaurantName = it.restaurantName,
                        rating = it.rating.toDouble(),
                        comment = it.content
                    )
                },
                savedRestaurants = savedRestaurantNames
            )


        } catch (e: Exception) {
            println("Supabase getFriendDetails failed: ${e.message}")
            fallback.getFriendDetails(friendId)
        }

    }
}
