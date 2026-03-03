package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.biteshare.domain.Review

class ReviewViewModel {
    // 1. Data State (Information Hiding)
    var restaurantName by mutableStateOf("")
    var reviewText by mutableStateOf("")
    var rating by mutableStateOf(5)  // 1-10, default 5
    // We use mutableStateListOf for the tags fo the UI observes additions/removals
    val selectedTags = mutableStateListOf<String>()
    val availableTags = listOf("I hate it!", "Wait long", "Economical",
        "Too Spicy", "Good Taste", "Expensive",
        "Come Back", "Too Salty", "Raw")

    // Logic for the one-line review character limit
    fun updateReviewText(newText: String) {
        if (newText.length <= 50) {
            reviewText = newText
        }
    }

    fun toggleTag(tag: String) {
        if (selectedTags.contains(tag)) {
            selectedTags.remove(tag)
        } else {
            selectedTags.add(tag)
        }
    }

     /** Resets the form to default values (e.g. after a successful post). */
    fun resetForm() {
        restaurantName = ""
        reviewText = ""
        rating = 5
        selectedTags.clear()
    }

    fun onPostClicked() {
        // 1. Create the Domain Object
        val newReview = Review(
            restaurantName = this.restaurantName,
            tags = this.selectedTags.toList(),
            content = this.reviewText,
            rating = this.rating
        )

        // 2. Send it to the Model (We will write this next)
        // model.addReview(newReview)

        println("Review created: $newReview")
        resetForm()  // reset so the next review starts with defaults
    }

}
