package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.example.biteshare.data.BiteShareRepository
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.Review

class ReviewViewModel(
    model: Model,
    private val repo: BiteShareRepository = FakeRepository(model),
) {
    private val scope = MainScope()

    // 1. Data State (Information Hiding)
    private var restaurantNames: List<String> = emptyList()
    var restaurantQuery by mutableStateOf("")
        private set
    var selectedRestaurantName by mutableStateOf<String?>(null)
        private set
    var restaurantSuggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var restaurantSearchError by mutableStateOf<String?>(null)
        private set
    var postStatusMessage by mutableStateOf<String?>(null)
        private set
    var postErrorMessage by mutableStateOf<String?>(null)
        private set
    var isPosting by mutableStateOf(false)
        private set
    var reviewText by mutableStateOf("")
    var rating by mutableStateOf(5)  // 1-10, default 5
    // We use mutableStateListOf for the tags fo the UI observes additions/removals
    val selectedTags = mutableStateListOf<String>()
    val availableTags = listOf("I hate it!", "Wait long", "Economical",
        "Too Spicy", "Good Taste", "Expensive",
        "Come Back", "Too Salty", "Raw")

    val restaurantName: String
        get() = selectedRestaurantName.orEmpty()

    init {
        loadRestaurantNames()
    }

    /**
     * Loads restaurant names from the repository from autocomplete suggestions.
     * It removes duplicates, sorts the names, and refreshes suggestions if needed.
     */
    private fun loadRestaurantNames() {
        scope.launch {
            val names = (repo.restaurants() + repo.browseRestaurants())
                .map { it.name.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            restaurantNames = names
            if (selectedRestaurantName == null && restaurantQuery.isNotBlank()) {
                updateRestaurantQuery(restaurantQuery)
            }
        }
    }

    /**
     * Updates the search text and filters restaurant names by prefix.
     * Matching results are stored for the dropdown suggestion list.
     *
     * @param newQuery The text currently typed by the user
     */
    fun updateRestaurantQuery(newQuery: String) {
        restaurantQuery = newQuery
        restaurantSearchError = null
        postStatusMessage = null
        postErrorMessage = null

        val normalizedQuery = newQuery.trim()
        if (normalizedQuery.isBlank()) {
            restaurantSuggestions = emptyList()
            return
        }

        val query = normalizedQuery.lowercase()
        restaurantSuggestions = restaurantNames.filter { name ->
            val lowerName = name.lowercase()
            lowerName.startsWith(query) || lowerName.split(" ").any { word -> word.startsWith(query) }
        }.take(6)
    }

    /**
     * Saves the restaurant chosen from the suggestion list.
     * It also clears the dropdown and removes any error message.
     *
     * @param name The selected restaurant name.
     */
    fun selectRestaurant(name: String) {
        selectedRestaurantName = name
        restaurantQuery = name
        restaurantSuggestions = emptyList()
        restaurantSearchError = null
        postStatusMessage = null
        postErrorMessage = null
    }

    /**
     * Clears the selected restaurant and resets the search field state.
     */
    fun clearSelectedRestaurant() {
        selectedRestaurantName = null
        restaurantQuery = ""
        restaurantSuggestions = emptyList()
        restaurantSearchError = null
        postStatusMessage = null
        postErrorMessage = null
    }

    // Logic for the one-line review character limit
    fun updateReviewText(newText: String) {
        if (newText.length <= 50) {
            reviewText = newText
            postStatusMessage = null
            postErrorMessage = null
        }
    }

    fun toggleTag(tag: String) {
        if (selectedTags.contains(tag)) {
            selectedTags.remove(tag)
        } else {
            selectedTags.add(tag)
        }
        postStatusMessage = null
        postErrorMessage = null
    }

    /** Resets the form to default values (e.g. after a successful post). */
    fun resetForm() {
        clearSelectedRestaurant()
        reviewText = ""
        rating = 5
        selectedTags.clear()
    }

    fun onPostClicked() {
        if (isPosting) return

        val chosenRestaurant = selectedRestaurantName?.takeIf { it.isNotBlank() }
        if (chosenRestaurant == null) {
            restaurantSearchError = "Please choose a restaurant from the list."
            return
        }

        if (reviewText.isBlank()) {
            postErrorMessage = "Please write a short review before posting."
            postStatusMessage = null
            return
        }

        val newReview = Review(
            restaurantName = chosenRestaurant,
            tags = this.selectedTags.toList(),
            content = this.reviewText,
            rating = this.rating
        )

        isPosting = true
        postErrorMessage = null
        postStatusMessage = null

        scope.launch {
            runCatching {
                repo.submitReview(newReview)
            }.onSuccess {
                resetForm()
                postStatusMessage = "Review posted successfully."
            }.onFailure { error ->
                postErrorMessage = error.message ?: "Failed to post review."
            }
            isPosting = false
        }
    }

}
