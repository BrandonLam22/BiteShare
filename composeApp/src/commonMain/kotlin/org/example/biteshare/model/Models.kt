package org.example.biteshare.model

import org.example.biteshare.domain.Review
import androidx.compose.runtime.mutableStateListOf
import org.example.biteshare.domain.MockDB

enum class PickMode { ME_ONLY, WITH_FRIENDS }

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
)

data class PickContext(
    val mode: PickMode,
    val selectedFriendIds: Set<String> = emptySet(),
)

/** 主屏分类（Local, Fast Food, Drink, Breakfast） */
data class CategoryItem(
    val id: String,
    val label: String,
)

/** 热门菜品/饮品卡片：标题为菜品名，副标题为餐厅/咖啡馆名 */
data class PopularItem(
    val id: String,
    val title: String,
    val subtitle: String,
)


class Model {

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
}
