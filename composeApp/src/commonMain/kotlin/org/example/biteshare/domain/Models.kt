package org.example.biteshare.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.data.MockDB
import org.example.biteshare.data.PickRepository

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

enum class CuisineFilter(val label: String) {
    ALL("All"),
    LOCAL("Local"),
    FAST_FOOD("Fast Food"),
    DRINK("Drink"),
    BREAKFAST("Breakfast"),
    PIZZA("Pizza"),
    SUSHI("Sushi"),
    BURGERS("Burgers"),
    COFFEE("Coffee"),
    ITALIAN("Italian"),
}

data class Friend(
    val id: String,
    val name: String,
)

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
)

data class PickFilters(
    val location: String = "Any",
    val budget: BudgetFilter = BudgetFilter.ANY,
    val cuisine: CuisineFilter = CuisineFilter.ALL,
    val openNowOnly: Boolean = false,
    val minRating: Double = 0.0,
)

data class PickContext(
    val mode: PickMode,
    val selectedFriendIds: Set<String> = emptySet(),
    val filters: PickFilters = PickFilters(),
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

    suspend fun locations(): List<String> = repo.locations()

    suspend fun recommend(context: PickContext): List<Restaurant> {
        val restaurants = repo.restaurants()
        val filtered = applyFilters(restaurants, context.filters)
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
    ): List<Restaurant> {
        return restaurants.filter { restaurant ->
            val locationMatches = filters.location == "Any" ||
                restaurant.location.equals(filters.location, ignoreCase = true)
            val budgetMatches = matchesBudget(restaurant, filters.budget)
            val cuisineMatches = matchesCuisine(restaurant, filters.cuisine)
            val openStatusMatches = !filters.openNowOnly || restaurant.isOpenNow
            // Restaurant.rating is stored on a 5-point scale; UI filter uses 10-point scale.
            val ratingMatches = (restaurant.rating * 2) >= filters.minRating

            locationMatches && budgetMatches && cuisineMatches && openStatusMatches && ratingMatches
        }
    }

    private fun matchesBudget(
        restaurant: Restaurant,
        budget: BudgetFilter,
    ): Boolean {
        val maxPrice = budget.maxPrice ?: return true
        return priceValue(restaurant.price) <= maxPrice
    }

    private fun matchesCuisine(
        restaurant: Restaurant,
        cuisine: CuisineFilter,
    ): Boolean {
        if (cuisine == CuisineFilter.ALL) return true

        val category = restaurant.category.lowercase()
        return when (cuisine) {
            CuisineFilter.ALL -> true
            CuisineFilter.LOCAL -> category == "local"
            CuisineFilter.FAST_FOOD -> category == "fast food" || category == "burgers"
            CuisineFilter.DRINK -> category == "drink" || category == "coffee"
            CuisineFilter.BREAKFAST -> category == "breakfast"
            CuisineFilter.PIZZA -> category == "pizza"
            CuisineFilter.SUSHI -> category == "sushi"
            CuisineFilter.BURGERS -> category == "burgers"
            CuisineFilter.COFFEE -> category == "coffee"
            CuisineFilter.ITALIAN -> category == "italian"
        }
    }

    private fun priceValue(rawPrice: String): Double =
        rawPrice.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: Double.MAX_VALUE

    private suspend fun rankForMode(
        restaurants: List<Restaurant>,
        mode: PickMode,
        selectedFriendIds: Set<String>,
        preferences: List<String>,
    ): List<Restaurant> {
        val preferenceKeywords = extractKeywords(preferences, forRestrictions = false)
        val preferenceScoreById = mutableMapOf<String, Int>()
        if (preferenceKeywords.isNotEmpty()) {
            for (restaurant in restaurants) {
                preferenceScoreById[restaurant.id] = preferenceScore(restaurant, preferenceKeywords)
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
        val blockedKeywords = extractKeywords(restrictions, forRestrictions = true)
        if (blockedKeywords.isEmpty()) return restaurants

        val result = mutableListOf<Restaurant>()
        for (restaurant in restaurants) {
            val signals = restaurantSignals(restaurant)
            if (blockedKeywords.none { keyword -> signals.contains(keyword) }) {
                result.add(restaurant)
            }
        }
        return result
    }

    private suspend fun preferenceScore(
        restaurant: Restaurant,
        preferenceKeywords: Set<String>,
    ): Int {
        if (preferenceKeywords.isEmpty()) return 0
        val signals = restaurantSignals(restaurant)
        return preferenceKeywords.count { keyword -> signals.contains(keyword) }
    }

    private suspend fun restaurantSignals(restaurant: Restaurant): String {
        val detail = repo.getRestaurantDetailById(restaurant.id)
        val detailText = detail?.featuredItems
            ?.joinToString(" ") { "${it.name} ${it.description}" }
            .orEmpty()

        return buildString {
            append(restaurant.name.lowercase())
            append(" ")
            append(restaurant.category.lowercase())
            append(" ")
            append(restaurant.location.lowercase())
            append(" ")
            append(detail?.description?.lowercase().orEmpty())
            append(" ")
            append(detail?.attributes?.joinToString(" ")?.lowercase().orEmpty())
            append(" ")
            append(detailText.lowercase())
        }
    }

    private fun extractKeywords(
        values: List<String>,
        forRestrictions: Boolean,
    ): Set<String> {
        val stopWords = setOf(
            "and", "the", "with", "for", "food", "only", "prefer", "preference",
            "allergy", "allergic", "avoid", "without", "from", "that", "this",
            "mine", "my", "eat", "eating", "to", "totally"
        )

        val keywords = mutableSetOf<String>()
        values.forEach { raw ->
            val normalized = raw.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
            if (normalized.isBlank()) return@forEach
            keywords += normalized

            normalized.split(" ")
                .filter { token -> token.length >= 3 && token !in stopWords }
                .forEach { token -> keywords += token }

            if ("bubble tea" in normalized) keywords += "bubble tea"
            if ("fast food" in normalized) keywords += "fast food"
            if ("middle eastern" in normalized) keywords += "middle eastern"
        }

        return keywords
            .flatMap { keyword -> expandKeyword(keyword, forRestrictions) }
            .toSet()
    }

    private fun expandKeyword(
        keyword: String,
        forRestrictions: Boolean,
    ): Set<String> {
        if (forRestrictions) {
            return when (keyword) {
                "sushi", "seafood", "fish", "shrimp" ->
                    setOf("sushi", "seafood", "fish", "shrimp", "japanese")
                "coffee", "cafe", "tea", "caffeine", "bubble tea", "boba" ->
                    setOf("coffee", "cafe", "tea", "bubble tea", "milk tea", "boba")
                "burger", "burgers", "fast food", "fries" ->
                    setOf("burger", "burgers", "fast food", "fries", "poutine")
                "halal", "shawarma", "middle eastern", "kabob" ->
                    setOf("halal", "shawarma", "middle eastern", "kabob", "kofta")
                "totally vegan" ->
                    setOf(
                        "vegan", "vegetarian",
                        "meat", "beef", "chicken", "pork", "lamb", "steak", "bacon", "ham", "sausage",
                        "milk", "cheese", "cream", "butter", "yogurt", "egg", "mayo", "honey"
                    )
                "vegetarian", "vegan" ->
                    setOf(
                        "vegetarian",
                        "meat", "beef", "chicken", "pork", "lamb", "steak", "bacon", "ham", "sausage"
                    )
                "meat" ->
                    setOf("meat", "beef", "chicken", "pork", "lamb", "steak", "bacon", "ham", "sausage")
                "beef" -> setOf("beef", "steak", "brisket", "roast", "rib")
                "chicken" -> setOf("chicken")
                "pork" -> setOf("pork", "bacon", "ham", "sausage", "prosciutto", "pepperoni")
                "lamb" -> setOf("lamb")
                "gluten", "wheat" ->
                    setOf("bread", "noodle", "pizza", "wrap", "pasta")
                "dairy", "lactose", "milk", "cheese" ->
                    setOf("milk", "cheese", "cream", "latte", "mocha", "butter", "yogurt")
                else -> setOf(keyword)
            }
        }

        return when (keyword) {
            "sushi", "seafood" -> setOf("sushi", "seafood", "japanese")
            "coffee", "cafe", "tea", "bubble tea", "boba" ->
                setOf("coffee", "cafe", "tea", "bubble tea", "milk tea", "boba")
            "burger", "burgers", "fast food" ->
                setOf("burger", "burgers", "fast food", "fries")
            else -> setOf(keyword)
        }
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
        currentUser = currentUser?.copy(password = newPassword)
    }

}
