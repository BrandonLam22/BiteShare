package org.example.biteshare.domain

object MockDB {
    // --- REVIEW DATA ---
    // This provides a list of "Pre-made" reviews for testing
    val fakeReviews = listOf(
        Review(
            id = "1",
            restaurantName = "Pizza Palace",
            tags = listOf("Italian", "Casual"),
            content = "Great crust, but the service was slow."
        ),
        Review(
            id = "2",
            restaurantName = "Sushi Zen",
            tags = listOf("Japanese", "Fresh"),
            content = "Best salmon nigiri in the city!"
        ),
        Review(
            id = "3",
            restaurantName = "Burger Joint",
            tags = listOf("Fast Food", "Greasy"),
            content = "Exactly what you expect for $5. Good value."
        )
    )
}