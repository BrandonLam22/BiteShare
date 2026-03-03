package org.example.biteshare.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * Unit tests for Model (domain layer).
 * Each test verifies a single behaviour in isolation using Arrange-Act-Assert.
 */
class ModelTest {

    @Test
    fun loginWithValidCredentialsReturnsTrueAndSetsCurrentUser() {
        // Arrange
        val model = Model()

        // Act
        val result = model.login("Kevin", "12345")

        // Assert
        assertTrue(result)
        assertTrue(model.currentUser != null)
        assertEquals("Kevin", model.currentUser?.username)
    }

    @Test
    fun loginWithInvalidCredentialsReturnsFalse() {
        // Arrange
        val model = Model()

        // Act
        val result = model.login("WrongUser", "WrongPass")

        // Assert
        assertFalse(result)
        assertNull(model.currentUser)
    }

    @Test
    fun signupCreatesUserAndSetsCurrentUser() {
        // Arrange
        val model = Model()
        val username = "NewUser"
        val password = "pass"
        val email = "new@test.com"

        // Act
        val result = model.signup(username, password, email)

        // Assert
        assertTrue(result)
        assertTrue(model.currentUser != null)
        assertEquals(username, model.currentUser?.username)
        assertEquals(email, model.currentUser?.email)
    }

    @Test
    fun addReviewAddsToReviewsList() {
        // Arrange
        val model = Model()
        val initialCount = model.reviews.size
        val newReview = Review(
            id = "test-1",
            restaurantName = "Test Restaurant",
            tags = listOf("Good"),
            content = "Nice food"
        )

        // Act
        model.addReview(newReview)

        // Assert
        assertEquals(initialCount + 1, model.reviews.size)
        assertTrue(model.reviews.any { it.id == "test-1" && it.restaurantName == "Test Restaurant" })
    }

    @Test
    fun removeReviewRemovesById() {
        // Arrange
        val model = Model()
        val reviewToRemove = model.reviews.first()
        val idToRemove = reviewToRemove.id
        val countBefore = model.reviews.size

        // Act
        model.removeReview(idToRemove)

        // Assert
        assertEquals(countBefore - 1, model.reviews.size)
        assertFalse(model.reviews.any { it.id == idToRemove })
    }

    @Test
    fun getReviewsForRestaurantReturnsOnlyMatchingReviews() {
        // Arrange
        val model = Model()
        val targetName = "Pizza Palace"

        // Act
        val filtered = model.getReviewsForRestaurant(targetName)

        // Assert
        assertTrue(filtered.all { it.restaurantName == targetName })
        assertTrue(filtered.isNotEmpty())
    }
}