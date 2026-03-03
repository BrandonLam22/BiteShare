package org.example.biteshare.presentation

import org.example.biteshare.viewmodel.ReviewViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReviewViewModel (presentation layer).
 * Tests tag selection and character limit; ViewModel does not yet take Model.
 */
class ReviewViewModelTest {

    @Test
    fun toggleTagAddsTagWhenNotSelected() {
        // Arrange
        val vm = ReviewViewModel()
        val tag = "Good Taste"

        // Act
        vm.toggleTag(tag)

        // Assert
        assertTrue(vm.selectedTags.contains(tag))
    }

    @Test
    fun toggleTagRemovesTagWhenAlreadySelected() {
        // Arrange
        val vm = ReviewViewModel()
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
        val vm = ReviewViewModel()
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
        val vm = ReviewViewModel()
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
        val vm = ReviewViewModel()

        // Assert
        assertTrue(vm.availableTags.contains("Good Taste"))
        assertTrue(vm.availableTags.contains("Economical"))
        assertTrue(vm.availableTags.isNotEmpty())
    }
}