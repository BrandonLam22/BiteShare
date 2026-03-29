package org.example.biteshare.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import org.example.biteshare.data.MockDB
import org.example.biteshare.data.PickRepository
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PickMode { ME_ONLY, WITH_FRIENDS }

enum class BudgetFilter(
    val label: String,
    val maxPrice: Double?,
) {
    ANY("Any", null),
    BUDGET("$", 10.0),
    MID("$$", 20.0),
    PREMIUM("$$$", 60.0),
}

enum class DistanceFilter(
    val label: String,
    val radiusKm: Double?,
) {
    ANY("Any", null),
    ONE_KM("1 km", 1.0),
    FIVE_KM("5 km", 5.0),
    TEN_KM("10 km", 10.0),
}

enum class DietaryLevel {
    NONE,
    PARTIAL,
    FULL,
    UNKNOWN,
}

enum class DietaryRestriction {
    VEGAN,
    VEGETARIAN,
    HALAL,
    GLUTEN_FREE,
    DAIRY_FREE,
}

data class DietaryProfile(
    val vegan: DietaryLevel = DietaryLevel.UNKNOWN,
    val vegetarian: DietaryLevel = DietaryLevel.UNKNOWN,
    val halal: DietaryLevel = DietaryLevel.UNKNOWN,
    val glutenFree: DietaryLevel = DietaryLevel.UNKNOWN,
    val dairyFree: DietaryLevel = DietaryLevel.UNKNOWN,
) {
    fun levelFor(restriction: DietaryRestriction): DietaryLevel =
        when (restriction) {
            DietaryRestriction.VEGAN -> vegan
            DietaryRestriction.VEGETARIAN -> vegetarian
            DietaryRestriction.HALAL -> halal
            DietaryRestriction.GLUTEN_FREE -> glutenFree
            DietaryRestriction.DAIRY_FREE -> dairyFree
        }
}

enum class CuisineFilter(val label: String) {
    ANY("Any"),
    CHINESE("Chinese"),
    FRENCH("French"),
    HAMBURGER("Hamburger"),
    INDIAN("Indian"),
    ITALIAN("Italian"),
    JAPANESE("Japanese"),
    MEXICAN("Mexican"),
    MIDDLE_EASTERN("Middle Eastern"),
    PIZZA("Pizza"),
    SEAFOOD("Seafood"),
    SUSHI("Sushi"),
    THAI("Thai"),
    VIETNAMESE("Vietnamese"),
}
@Serializable
data class Friend(
    val id: String,
    val name: String,
)

enum class FriendAddResult {
    SUCCESS,
    EMPTY_INPUT,
    SELF_NOT_ALLOWED,
    ALREADY_FRIENDS,
    NOT_FOUND,
    ERROR,
}

data class Restaurant(
    val id: String,
    val name: String,
    val category: String,
    val price: String,
    val eta: String,
    val rating: Double,
    val isSaved: Boolean = false,
    val location: String = "Addis Ababa",
    val isOpenNow: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tags: Set<String> = emptySet(),
    val dietaryProfile: DietaryProfile = DietaryProfile(),
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class PickFilters(
    val location: String = "Any",
    val distance: DistanceFilter = DistanceFilter.ANY,
    val budget: BudgetFilter = BudgetFilter.ANY,
    val cuisine: CuisineFilter = CuisineFilter.ANY,
    val openNowOnly: Boolean = false,
    val minRating: Double = 0.0,
)

data class PickContext(
    val mode: PickMode,
    val selectedFriendIds: Set<String> = emptySet(),
    val filters: PickFilters = PickFilters(),
    val currentLocation: GeoPoint? = null,
)

data class CategoryItem(
    val id: String,
    val label: String,
)

data class PopularItem(
    val id: String,
    val title: String,
    val subtitle: String,
)

class PickModel(
    private val repo: PickRepository,
) {
    suspend fun friends(): List<Friend> = repo.friends()

    suspend fun searchUsers(query: String): List<Friend> = repo.searchUsers(query)

    suspend fun addFriend(friendId: String): FriendAddResult = repo.addFriend(friendId)

    suspend fun locations(): List<String> = repo.locations()

    suspend fun recommend(context: PickContext): List<Restaurant> {
        val restaurants = repo.restaurants()
        val filtered = applyFilters(restaurants, context.filters, context.currentLocation)
        val restrictions = mergedRestrictions(context)
        val withoutRestrictedItems = applyUserRestrictions(filtered, restrictions)
        val preferences = mergedPreferences(context)
        return rankForMode(
            restaurants = withoutRestrictedItems,
            mode = context.mode,
            selectedFriendIds = context.selectedFriendIds,
            preferences = preferences,
        )
    }

    suspend fun previewCount(context: PickContext): Int = recommend(context).size

    suspend fun currentUserId(): String? = repo.currentUserId()

    suspend fun currentUserLocation(): GeoPoint? = repo.currentUserLocation()

    suspend fun restaurantSelectionsByUserIds(userIds: Set<String>): Map<String, Set<String>> =
        repo.restaurantSelectionsByUserIds(userIds)

    suspend fun setRestaurantSelection(userId: String, restaurantId: String, selected: Boolean) {
        repo.setRestaurantSelection(userId, restaurantId, selected)
    }

    suspend fun allRestaurants(): List<Restaurant> = repo.restaurants()

    suspend fun createVoteSession(session: VoteSession) {
        repo.createVoteSession(session)
    }

    suspend fun updateVoteSessionVotes(sessionId: String, userId: String, votes: Set<String>) {
        repo.updateVoteSessionVotes(sessionId, userId, votes)
    }

    suspend fun voteSessionVotes(sessionId: String): Map<String, Set<String>> =
        repo.voteSessionVotes(sessionId)

    suspend fun closeVoteSession(sessionId: String, closedAtEpoch: Long) {
        repo.closeVoteSession(sessionId, closedAtEpoch)
    }

    suspend fun voteSessionsForCurrentUser(): List<VoteSession> {
        val userId = currentUserId()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return repo.voteSessionsForUser(userId).map { session ->
            val participants = session.participants.map { participant ->
                if (participant.id == userId) {
                    participant.copy(name = "You", isSelf = true)
                } else {
                    participant.copy(isSelf = false)
                }
            }
            session.copy(participants = participants)
        }
    }

    suspend fun voteSessionById(sessionId: String): VoteSession? {
        val userId = currentUserId()?.takeIf { it.isNotBlank() }
        val session = repo.voteSessionById(sessionId) ?: return null
        if (userId == null) return session
        val participants = session.participants.map { participant ->
            if (participant.id == userId) {
                participant.copy(name = "You", isSelf = true)
            } else {
                participant.copy(isSelf = false)
            }
        }
        return session.copy(participants = participants)
    }

    private suspend fun mergedPreferences(context: PickContext): List<String> {
        val base = repo.userPreferences()
        if (context.mode != PickMode.WITH_FRIENDS) return base
        val userIds = buildSet {
            repo.currentUserId()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(context.selectedFriendIds.filter { it.isNotBlank() })
        }
        if (userIds.isEmpty()) return base
        val byUser = repo.userPreferencesByUserIds(userIds)
        if (byUser.isEmpty()) return base
        return (byUser.values.flatten() + base).distinct()
    }

    private suspend fun mergedRestrictions(context: PickContext): List<String> {
        val base = repo.userRestrictions()
        if (context.mode != PickMode.WITH_FRIENDS) return base
        val userIds = buildSet {
            repo.currentUserId()?.takeIf { it.isNotBlank() }?.let { add(it) }
            addAll(context.selectedFriendIds.filter { it.isNotBlank() })
        }
        if (userIds.isEmpty()) return base
        val byUser = repo.userRestrictionsByUserIds(userIds)
        if (byUser.isEmpty()) return base
        return (byUser.values.flatten() + base).distinct()
    }

    private fun applyFilters(
        restaurants: List<Restaurant>,
        filters: PickFilters,
        currentLocation: GeoPoint?,
    ): List<Restaurant> {
        return restaurants.filter { restaurant ->
            val locationMatches = filters.location == "Any" ||
                restaurant.location.equals(filters.location, ignoreCase = true)
            val distanceMatches = matchesDistance(restaurant, filters.distance, currentLocation)
            val budgetMatches = matchesBudget(restaurant, filters.budget)
            val cuisineMatches = matchesCuisine(restaurant, filters.cuisine)
            val openStatusMatches = !filters.openNowOnly || restaurant.isOpenNow
            // Restaurant.rating is stored on a 5-point scale; UI filter uses 10-point scale.
            val ratingMatches = (restaurant.rating * 2) >= filters.minRating

            locationMatches &&
                distanceMatches &&
                budgetMatches &&
                cuisineMatches &&
                openStatusMatches &&
                ratingMatches
        }
    }

    private fun matchesBudget(
        restaurant: Restaurant,
        budget: BudgetFilter,
    ): Boolean {
        val maxPrice = budget.maxPrice ?: return true
        return priceValue(restaurant.price) <= maxPrice
    }

    private fun matchesDistance(
        restaurant: Restaurant,
        distance: DistanceFilter,
        currentLocation: GeoPoint?,
    ): Boolean {
        val radiusKm = distance.radiusKm ?: return true
        val location = currentLocation ?: return true
        if (!hasValidCoordinates(restaurant.latitude, restaurant.longitude)) return true
        val km = haversineKm(
            location.latitude,
            location.longitude,
            restaurant.latitude,
            restaurant.longitude
        )
        return km <= radiusKm
    }

    private fun hasValidCoordinates(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun matchesCuisine(
        restaurant: Restaurant,
        cuisine: CuisineFilter,
    ): Boolean {
        if (cuisine == CuisineFilter.ANY) return true
        val tags = if (restaurant.tags.isNotEmpty()) {
            restaurant.tags
        } else {
            setOf(RestaurantClassification.normalizeTag(restaurant.category))
        }
        val requiredTags = cuisineTags(cuisine)
        return requiredTags.any { tag -> tag in tags }
    }

    private fun cuisineTags(cuisine: CuisineFilter): Set<String> =
        when (cuisine) {
            CuisineFilter.ANY -> emptySet()
            CuisineFilter.CHINESE -> setOf("chinese")
            CuisineFilter.FRENCH -> setOf("french")
            CuisineFilter.HAMBURGER -> setOf("hamburger", "burger", "burgers")
            CuisineFilter.INDIAN -> setOf("indian")
            CuisineFilter.ITALIAN -> setOf("italian", "pasta")
            CuisineFilter.JAPANESE -> setOf("japanese")
            CuisineFilter.MEXICAN -> setOf("mexican")
            CuisineFilter.MIDDLE_EASTERN -> setOf("middle eastern", "shawarma")
            CuisineFilter.PIZZA -> setOf("pizza")
            CuisineFilter.SEAFOOD -> setOf("seafood", "fish")
            CuisineFilter.SUSHI -> setOf("sushi")
            CuisineFilter.THAI -> setOf("thai")
            CuisineFilter.VIETNAMESE -> setOf("vietnamese")
        }

    private fun priceValue(rawPrice: String): Double =
        rawPrice.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: Double.MAX_VALUE

    private suspend fun rankForMode(
        restaurants: List<Restaurant>,
        mode: PickMode,
        selectedFriendIds: Set<String>,
        preferences: List<String>,
    ): List<Restaurant> {
        val preferenceTags = resolvePreferenceTags(preferences)
        val preferenceScoreById = mutableMapOf<String, Int>()
        if (preferenceTags.isNotEmpty()) {
            for (restaurant in restaurants) {
                preferenceScoreById[restaurant.id] = preferenceScore(restaurant, preferenceTags)
            }
        }
        fun score(restaurant: Restaurant): Int = preferenceScoreById[restaurant.id] ?: 0

        return when (mode) {
            PickMode.ME_ONLY -> {
                restaurants.sortedWith(
                    compareByDescending<Restaurant> { score(it) }
                        .thenByDescending { it.rating }
                        .thenBy { priceValue(it.price) }
                )
            }

            PickMode.WITH_FRIENDS -> {
                if (selectedFriendIds.size < 2) {
                    restaurants.sortedWith(
                        compareByDescending<Restaurant> { score(it) }
                            .thenByDescending { it.rating }
                            .thenBy { priceValue(it.price) }
                    )
                } else {
                    restaurants.sortedWith(
                        compareByDescending<Restaurant> { score(it) }
                            .thenBy { priceValue(it.price) }
                            .thenByDescending { it.rating }
                    )
                }
            }
        }
    }

    private suspend fun applyUserRestrictions(
        restaurants: List<Restaurant>,
        restrictions: List<String>,
    ): List<Restaurant> {
        val policy = buildRestrictionPolicy(restrictions)
        if (policy.isEmpty()) return restaurants
        return restaurants.filter { restaurant ->
            if (policy.excludedTags.any { tag -> tag in restaurant.tags }) return@filter false
            policy.requiredDietary.all { restriction ->
                restaurant.dietaryProfile.levelFor(restriction) != DietaryLevel.NONE
            }
        }
    }

    private fun preferenceScore(
        restaurant: Restaurant,
        preferenceTags: Set<String>,
    ): Int {
        if (preferenceTags.isEmpty()) return 0
        return preferenceTags.count { tag -> tag in restaurant.tags }
    }

    private fun resolvePreferenceTags(preferences: List<String>): Set<String> {
        if (preferences.isEmpty()) return emptySet()
        val tags = mutableSetOf<String>()
        preferences.forEach { raw ->
            val normalized = normalizeTag(raw)
            if (normalized.isBlank()) return@forEach
            tags += RestaurantClassification.deriveTags(normalized, emptySet())
        }
        return tags
    }

    private fun buildRestrictionPolicy(restrictions: List<String>): RestrictionPolicy {
        if (restrictions.isEmpty()) return RestrictionPolicy()
        val excludedTags = mutableSetOf<String>()
        val requiredDietary = mutableSetOf<DietaryRestriction>()

        restrictions.forEach { raw ->
            val normalized = normalizeTag(raw)
            if (normalized.isBlank()) return@forEach
            val dietary = resolveDietaryRestriction(normalized)
            if (dietary != null) {
                requiredDietary += dietary
            } else {
                val cleaned = stripRestrictionPrefix(normalized)
                if (cleaned.isNotBlank()) {
                    excludedTags += RestaurantClassification.deriveTags(cleaned, emptySet())
                }
            }
        }

        return RestrictionPolicy(
            excludedTags = excludedTags,
            requiredDietary = requiredDietary,
        )
    }

    private fun resolveDietaryRestriction(value: String): DietaryRestriction? {
        return when {
            "vegan" in value -> DietaryRestriction.VEGAN
            "vegetarian" in value -> DietaryRestriction.VEGETARIAN
            "halal" in value -> DietaryRestriction.HALAL
            "gluten" in value -> DietaryRestriction.GLUTEN_FREE
            "dairy" in value || "lactose" in value -> DietaryRestriction.DAIRY_FREE
            else -> null
        }
    }

    private fun stripRestrictionPrefix(value: String): String {
        val stopWords = setOf(
            "no", "avoid", "without", "allergy", "allergic", "none",
            "dont", "don t", "do not", "not"
        )
        val tokens = value.split(" ").filter { it.isNotBlank() }
        val filtered = tokens.filterNot { it in stopWords }
        return filtered.joinToString(" ").trim()
    }

    private fun normalizeTag(value: String): String =
        RestaurantClassification.normalizeTag(value)

    private data class RestrictionPolicy(
        val excludedTags: Set<String> = emptySet(),
        val requiredDietary: Set<DietaryRestriction> = emptySet(),
    ) {
        fun isEmpty(): Boolean = excludedTags.isEmpty() && requiredDietary.isEmpty()
    }
}

data class HomeFeed(
    val greeting: String,
    val categories: List<CategoryItem>,
    val popularDishes: List<PopularItem>,
    val popularDrinks: List<PopularItem>,
)

data class ProfileData(
    val name: String,
    val email: String,
    val friendCount: Int,
    val notificationsEnabled: Boolean,
)

data class EditableProfile(
    val username: String,
    val email: String,
    val bio: String,
    val preferences: List<String>,
    val foodRestrictions: List<String>
)

class Model {
    // --- AUTH STATE ---
    // 1. Current Session State (The "Gatekeeper")
    var currentUser by mutableStateOf<User?>(null)
        private set

    private val _users = mutableStateListOf<User>().apply {
        addAll(MockDB.fakeUsers)
    }

    // --- AUTH FUNCTIONS ---
    /**
     * Attempts to log in a user by checking credentials against the MockDB.
     * Returns 'true' if successful, allowing the ViewModel to trigger navigation.
     */
    fun login(username: String, password: String): Boolean {
        // We search our "Fake Database" for a match
        val user = _users.find { it.username == username && it.password == password }

        return if (user != null) {
            currentUser = user // Update the "Source of Truth"
            true
        } else {
            false
        }
    }

    /**
     * Creates a new user session.
     */
    fun signup(username: String, password: String, email: String): Boolean {
        val newUser = User(username = username, password = password, email = email) // Note: 'id' will be generated automatically in data class
        // In a real app, save this to a database here
        _users.add(newUser)
        currentUser = newUser
        return true
    }


    // --- REVIEW SCREEN STATE ---
    // A private mutable list so only the Model can modify it
    private val _reviews = mutableStateListOf<Review>().apply {
        addAll(MockDB.fakeReviews)
    }

    // A public read-only list for the ViewModels to observe
    val reviews: List<Review> get() = _reviews

    // --- REVIEW SCREEN FUNCTIONS ---
    /**
     * Adds a new review to the source of truth.
     * This will automatically notify any UI observing 'reviews'
     */
    fun addReview(newReview: Review) {
        _reviews.add(newReview)
    }

    /**
     * Removes a specific review from the application state using its unique ID.
     */
    fun removeReview(reviewID: String) {
        // We use removeAll with a predicate to find the matching ID in the observable list
        _reviews.removeAll { it.id == reviewID }
    }

    /**
     * Filters the global list of reviews to return only those matching a specific restaurant.
     * * @param name The name of the restaurant to filter by.
     * @return A list of Review objects that match the provided name.
     */
    fun getReviewsForRestaurant(name: String): List<Review> {
        // Returns a new list containing only the reviews that meet the criteria
        return _reviews.filter { it.restaurantName == name }
    }

    /**
     * Toggles a restaurant's saved state for the current user
     */
    fun toggleSavedRestaurant(restaurantId: String) {
        val user = currentUser ?: return
        val currentSaved = user.savedRestaurantIds.toMutableList()

        if (currentSaved.contains(restaurantId)) {
            currentSaved.remove(restaurantId)
        } else {
            currentSaved.add(restaurantId)
        }

        // Update current user with new saved list
        val updatedUser = user.copy(savedRestaurantIds = currentSaved)
        currentUser = updatedUser

        // Also update in the users list
        val index = _users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _users[index] = updatedUser
        }
    }

     /**
     * Gets the list of saved restaurant IDs for the current user
     */
    fun getSavedRestaurantIds(): Set<String> {
        return currentUser?.savedRestaurantIds?.toSet() ?: emptySet()
    }

    fun updateSavedRestaurants(savedRestaurantIds: List<String>) {
        val user = currentUser ?: return
        val updatedUser = user.copy(savedRestaurantIds = savedRestaurantIds.distinct())
        currentUser = updatedUser

        val index = _users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _users[index] = updatedUser
        }
    }

    fun logout() {
        currentUser = null
        println("User logged out")
    }

    fun updateUserProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>
    ) {
        val user = currentUser ?: return

        val updatedUser = user.copy(
            username = username,
            email = email,
            bio = bio,
            preferences = preferences,
            foodRestrictions = foodRestrictions
        )

        currentUser = updatedUser

        // Update in users list
        val index = _users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _users[index] = updatedUser
        }
    }

    fun updateUserPassword(newPassword: String) {
        val user = currentUser ?: return
        val updatedUser = user.copy(password = newPassword)
        currentUser = updatedUser

        val index = _users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _users[index] = updatedUser
        }
    }

    fun applyAuthenticatedUser(user: User?) {
        currentUser = user
    }


}
