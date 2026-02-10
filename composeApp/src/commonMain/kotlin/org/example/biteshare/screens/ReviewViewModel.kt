package org.example.biteshare.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class ReviewViewModel {
    // 1. Data State (Information Hiding)
    var restaurantName by mutableStateOf("")
    var reviewText by mutableStateOf("")
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

}
