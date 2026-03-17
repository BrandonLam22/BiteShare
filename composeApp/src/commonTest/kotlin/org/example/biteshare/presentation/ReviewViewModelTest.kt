package org.example.biteshare.presentation

import org.example.biteshare.domain.Model
import org.example.biteshare.viewmodel.ReviewViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReviewViewModel (presentation layer).
 * Tests tag selection, review validation, and character limit.
 */
class ReviewViewModelTest {

    @Test
    fun toggleTagAddsTagWhenNotSelected() {
        // Arrange
        val vm = ReviewViewModel(Model())
        val tag = "Good Taste"

        // Act
        vm.toggleTag(tag)

        // Assert
        assertTrue(vm.selectedTags.contains(tag))
    }

    @Test
    fun toggleTagRemovesTagWhenAlreadySelected() {
        // Arrange
        val vm = ReviewViewModel(Model())
        val tag = "Economical"
        vm.toggleTag(tag)
        assertTrue(vm.selectedTags.contains(tag))

        // Act
        vm.toggleTag(tag)

        // Assert
        assertFalse(vm.selectedTags.contains(tag))
    }

    @Test
    fun updateReviewTextAcceptsUpTo50Characters() {
        // Arrange
        val vm = ReviewViewModel(Model())
        val text = "a".repeat(50)

        // Act
        vm.updateReviewText(text)

        // Assert
        assertEquals(50, vm.reviewText.length)
        assertEquals(text, vm.reviewText)
    }

    @Test
    fun updateReviewTextIgnoresTextOver50Characters() {
        // Arrange
        val vm = ReviewViewModel(Model())
        val validText = "a".repeat(50)
        vm.updateReviewText(validText)

        // Act
        vm.updateReviewText("a".repeat(60))

        // Assert
        assertEquals(50, vm.reviewText.length)
        assertEquals(validText, vm.reviewText)
    }

    @Test
    fun availableTagsContainsExpectedTags() {
        // Arrange & Act
        val vm = ReviewViewModel(Model())

        // Assert
        assertTrue(vm.availableTags.contains("Good Taste"))
        assertTrue(vm.availableTags.contains("Economical"))
        assertTrue(vm.availableTags.isNotEmpty())
    }

    @Test
    fun selectRestaurantStoresSelectionAndClearsSuggestions() {
        // Arrange
        val vm = ReviewViewModel(Model())

        // Act
        vm.selectRestaurant("Joe's Pizza")

        // Assert
        assertEquals("Joe's Pizza", vm.restaurantName)
        assertEquals("Joe's Pizza", vm.restaurantQuery)
        assertTrue(vm.restaurantSuggestions.isEmpty())
    }

    @Test
    fun clearSelectedRestaurantResetsSearchState() {
        // Arrange
        val vm = ReviewViewModel(Model())
        vm.selectRestaurant("Joe's Pizza")

        // Act
        vm.clearSelectedRestaurant()

        // Assert
        assertEquals("", vm.restaurantName)
        assertEquals("", vm.restaurantQuery)
        assertTrue(vm.restaurantSuggestions.isEmpty())
    }

    @Test
    fun onPostClickedRequiresRestaurantSelection() {
        // Arrange
        val vm = ReviewViewModel(Model())

        // Act
        vm.onPostClicked()

        // Assert
        assertEquals("Please choose a restaurant from the list.", vm.restaurantSearchError)
    }

    @Test
    fun onPostClickedRequiresReviewText() {
        // Arrange
        val vm = ReviewViewModel(Model())
        vm.selectRestaurant("Joe's Pizza")

        // Act
        vm.onPostClicked()

        // Assert
        assertEquals("Please write a short review before posting.", vm.postErrorMessage)
    }
}