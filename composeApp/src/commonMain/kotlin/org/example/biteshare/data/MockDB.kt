package org.example.biteshare.data

import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User

object MockDB {
    // --- REVIEW DATA ---
    // This provides a list of "Pre-made" reviews for testing
    val fakeReviews = listOf(
        Review(
            id = "1",
            restaurantName = "Pizza Palace",
            tags = listOf("Italian", "Casual"),
            content = "Great crust, but the service was slow.",
            rating = 8

        ),
        Review(
            id = "2",
            restaurantName = "Sushi Zen",
            tags = listOf("Japanese", "Fresh"),
            content = "Best salmon nigiri in the city!",
            rating = 7

        ),
        Review(
            id = "3",
            restaurantName = "Burger Joint",
            tags = listOf("Fast Food", "Greasy"),
            content = "Exactly what you expect for $5. Good value.",
            rating = 9

        )
    )


    // --- USER DATA ---
    val fakeUsers = listOf(
        User(
            id = "user_01",
            username = "Kevin",
            email = "k389zhan@uwaterloo.ca",
            password = "12345",
            savedRestaurantIds = listOf("p1", "b1", "bp1")
        ),
        User(
            id = "user_02",
            username = "Steven",
            email = "s38gao@uwaterloo.ca",
            password = "54321",
            savedRestaurantIds = listOf("s1", "c1")
        )
    )
}