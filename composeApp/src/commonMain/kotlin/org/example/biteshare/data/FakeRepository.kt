package org.example.biteshare.data

import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.FeaturedItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickModel
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.domain.ProfileData
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User

class FakeRepository(
    private val model: Model = Model(),
) : BiteShareRepository {
    private data class HomeType(
        val id: String,
        val label: String,
        val isDrinkType: Boolean = false,
    )

    private val homeTypeCatalog = listOf(
        HomeType("fast_food", "Fast Food"),
        HomeType("pizza", "Pizza"),
        HomeType("chinese", "Chinese"),
        HomeType("japanese", "Japanese"),
        HomeType("middle_eastern", "Middle Eastern"),
        HomeType("indian", "Indian"),
        HomeType("coffee_tea", "Coffee & Tea", isDrinkType = true),
        HomeType("bubble_tea", "Bubble Tea", isDrinkType = true),
        HomeType("desserts", "Desserts", isDrinkType = true),
    )

    private var notificationsEnabled = true

    override suspend fun friends(): List<Friend> = model.currentUser?.friends ?: MockDB.fakeFriends

    override suspend fun restaurants(): List<Restaurant> {
        val savedIds = effectiveSavedIds()
        return MockDB.coreRestaurants.map { restaurant ->
            restaurant.copy(isSaved = restaurant.id in savedIds)
        }
    }

    override suspend fun browseRestaurants(): List<Restaurant> {
        val savedIds = effectiveSavedIds()
        return MockDB.browseRestaurants.map { restaurant ->
            restaurant.copy(isSaved = restaurant.id in savedIds)
        }
    }

    override suspend fun getRestaurantById(id: String): Restaurant? {
        val userSavedIds = model.getSavedRestaurantIds()

        return (restaurants() + browseRestaurants())
            .distinctBy { it.id }
            .find { it.id == id }
            ?.copy(isSaved = userSavedIds.contains(id))
    }

    override suspend fun getHomeFeed(userName: String): HomeFeed {
        val all = (restaurants() + browseRestaurants()).distinctBy { it.id }
        val typeCounts = linkedMapOf<String, Int>()
        all.forEach { restaurant ->
            classifyRestaurantTypes(restaurant).forEach { label ->
                typeCounts[label] = (typeCounts[label] ?: 0) + 1
            }
        }

        val activeTypes = homeTypeCatalog.filter { (typeCounts[it.label] ?: 0) > 0 }
        val categories = activeTypes.map { CategoryItem(id = it.id, label = it.label) }
        val popularDishes = activeTypes
            .filterNot { it.isDrinkType }
            .sortedByDescending { typeCounts[it.label] ?: 0 }
            .take(4)
            .mapIndexed { idx, type ->
                PopularItem("dish_type_${idx + 1}", type.label, "${typeCounts[type.label] ?: 0} places")
            }
        val popularDrinks = activeTypes
            .filter { it.isDrinkType }
            .sortedByDescending { typeCounts[it.label] ?: 0 }
            .take(4)
            .mapIndexed { idx, type ->
                PopularItem("drink_type_${idx + 1}", type.label, "${typeCounts[type.label] ?: 0} places")
            }

        return HomeFeed(
            greeting = "Hi, $userName",
            categories = if (categories.isEmpty()) MockDB.homeCategories else categories,
            popularDishes = if (popularDishes.isEmpty()) {
                listOf(
                    PopularItem("dish_fallback_1", "Fast Food", "3 places"),
                    PopularItem("dish_fallback_2", "Pizza", "3 places"),
                    PopularItem("dish_fallback_3", "Japanese", "1 place"),
                )
            } else popularDishes,
            popularDrinks = if (popularDrinks.isEmpty()) {
                listOf(
                    PopularItem("drink_fallback_1", "Coffee & Tea", "2 places"),
                    PopularItem("drink_fallback_2", "Bubble Tea", "1 place"),
                )
            } else popularDrinks,
        )
    }

    override suspend fun getRestaurantDetailById(id: String): RestaurantDetail? {
        val summary = getRestaurantById(id) ?: return null
        val baseDetail = MockDB.restaurantDetails[id] ?: buildFallbackDetail(summary)
        val reviews = getReviewsForRestaurant(summary.name)
        val averageRating = reviews.map { it.rating }.average().takeIf { !it.isNaN() } ?: baseDetail.rating
        return baseDetail.copy(
            reviews = reviews,
            rating = averageRating,
        )
    }

    override suspend fun getReviewsForRestaurant(restaurantName: String): List<Review> =
        (model.getReviewsForRestaurant(restaurantName) + MockDB.reviewsForRestaurant(restaurantName))
            .distinctBy { it.id }

    override suspend fun submitReview(review: Review) {
        model.addReview(review)
    }

    override suspend fun getRestaurantsByTag(tag: String): List<Restaurant> {
        val targetTypes = resolveHomeTypes(tag)
        val all = (restaurants() + browseRestaurants()).distinctBy { it.id }
        if (targetTypes.isEmpty()) return all
        return all.filter { restaurant -> classifyRestaurantTypes(restaurant).any { it in targetTypes } }
    }

    override suspend fun locations(): List<String> =
        (restaurants() + browseRestaurants())
            .map { it.location }
            .distinct()
            .sorted()

    suspend fun recommend(context: PickContext): List<Restaurant> = PickModel(this).recommend(context)

    override suspend fun userPreferences(): List<String> =
        model.currentUser?.preferences ?: emptyList()

    override suspend fun userRestrictions(): List<String> =
        model.currentUser?.foodRestrictions ?: emptyList()

    override suspend fun getProfile(): ProfileData {
        val user = model.currentUser ?: MockDB.fakeUsers.firstOrNull()
        return ProfileData(
            name = user?.username ?: "Guest",
            email = user?.email ?: "",
            friendCount = user?.friends?.size ?: MockDB.friends.size,
            notificationsEnabled = notificationsEnabled
        )
    }

    override suspend fun updateNotificationPreference(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    override suspend fun getSavedRestaurants(): List<Restaurant> {
        val savedIds = effectiveSavedIds()
        return (restaurants() + browseRestaurants())
            .distinctBy { it.id }
            .filter { it.id in savedIds }
    }

    override suspend fun toggleSaved(restaurantId: String) {
        model.toggleSavedRestaurant(restaurantId)
    }

    override suspend fun logout() {
        model.logout()
    }

    override suspend fun getEditableProfile(): EditableProfile {
        val user = model.currentUser ?: MockDB.fakeUsers.firstOrNull()
        return EditableProfile(
            username = user?.username ?: "",
            email = user?.email ?: "",
            bio = user?.bio ?: "",
            preferences = user?.preferences ?: emptyList(),
            foodRestrictions = user?.foodRestrictions ?: emptyList()
        )
    }

    override suspend fun updateProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>
    ) {
        model.updateUserProfile(username, email, bio, preferences, foodRestrictions)
    }

    private fun effectiveSavedIds(): Set<String> {
        val userSaved = model.getSavedRestaurantIds()
        return if (userSaved.isNotEmpty()) userSaved else MockDB.defaultSavedRestaurantIds
    }

    private fun buildFallbackDetail(summary: Restaurant): RestaurantDetail {
        val query = summary.name.replace(" ", "+")
        val imageKey = when (summary.category.lowercase()) {
            "pizza" -> "pizza_home"
            "sushi" -> "beyayenet_home"
            "burgers", "fast food" -> "chesse_burger_home"
            "coffee", "cafe", "drink" -> "coffee_home"
            else -> "coffee_home"
        }
        return RestaurantDetail(
            restaurantId = summary.id,
            name = summary.name,
            images = listOf(imageKey, "drink_home"),
            featuredItems = listOf(FeaturedItem("Chef Recommendation", summary.price, "Popular choice from this restaurant")),
            location = RestaurantLocation(
                city = summary.location,
                address = "${summary.name}, ${summary.location}",
                distance = "2.5 miles away",
            ),
            reviews = MockDB.reviewsForRestaurant(summary.name),
            description = "${summary.name} is known for ${summary.category.lowercase()} and consistent service.",
            rating = summary.rating,
            attributes = listOf(summary.category, "Popular"),
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=$query+${summary.location.replace(" ", "+")}",
            restaurantWebsite = "https://example.com/${summary.id}",
            priceRange = summary.price,
        )
    }

    private fun classifyRestaurantTypes(restaurant: Restaurant): Set<String> {
        val detail = MockDB.restaurantDetails[restaurant.id]
        val haystack = buildString {
            append(restaurant.name.lowercase())
            append(" ")
            append(restaurant.category.lowercase())
            append(" ")
            append(detail?.attributes?.joinToString(" ")?.lowercase().orEmpty())
        }

        val labels = mutableSetOf<String>()
        fun containsAny(vararg keywords: String) = keywords.any { haystack.contains(it) }

        if (containsAny("pizza", "slice")) labels += "Pizza"
        if (containsAny("sushi", "japanese", "nigiri", "roll")) labels += "Japanese"
        if (containsAny("noodle", "chinese", "lanzhou", "dan dan")) labels += "Chinese"
        if (containsAny("shawarma", "middle eastern", "halal", "kabob", "kofta")) labels += "Middle Eastern"
        if (containsAny("indian", "paneer", "butter chicken", "curry")) labels += "Indian"
        if (containsAny("burger", "fast food", "poutine", "fries")) labels += "Fast Food"
        if (containsAny("coffee", "cafe", "latte", "capo", "tea")) labels += "Coffee & Tea"
        if (containsAny("bubble tea", "deerioca", "alley", "milk tea")) labels += "Bubble Tea"
        if (containsAny("dessert", "sweet", "milkshake")) labels += "Desserts"
        if (labels.isEmpty()) labels += "Fast Food"
        return labels
    }

    private fun resolveHomeTypes(rawTag: String): Set<String> {
        val tag = normalizeTag(rawTag)
        if (tag.isBlank()) return emptySet()

        val aliases = mapOf(
            "fast food" to setOf("Fast Food"),
            "pizza" to setOf("Pizza"),
            "chinese" to setOf("Chinese"),
            "japanese" to setOf("Japanese"),
            "sushi" to setOf("Japanese"),
            "middle eastern" to setOf("Middle Eastern"),
            "shawarma" to setOf("Middle Eastern"),
            "halal" to setOf("Middle Eastern"),
            "indian" to setOf("Indian"),
            "coffee" to setOf("Coffee & Tea"),
            "coffee tea" to setOf("Coffee & Tea"),
            "cafe" to setOf("Coffee & Tea"),
            "drink" to setOf("Coffee & Tea", "Bubble Tea"),
            "bubble tea" to setOf("Bubble Tea", "Coffee & Tea"),
            "dessert drinks" to setOf("Bubble Tea"),
            "desserts" to setOf("Desserts"),
            "sweet" to setOf("Desserts"),
        )

        aliases[tag]?.let { return it }
        return homeTypeCatalog
            .filter { normalizeTag(it.label).contains(tag) || tag.contains(normalizeTag(it.label)) }
            .map { it.label }
            .toSet()
    }

    private fun normalizeTag(tag: String): String =
        tag.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    override suspend fun getPassword(): String {
        return model.currentUser?.password ?: "password123"
    }

    override suspend fun updatePassword(newPassword: String) {
        model.updateUserPassword(newPassword)
    }

    // Login/Signup
    override suspend fun login(username: String, password: String): User? {
        return if (model.login(username, password)) model.currentUser else null
    }

    override suspend fun signup(username: String, password: String, email: String): User? {
        return if (model.signup(username, password, email)) model.currentUser else null
    }
}
