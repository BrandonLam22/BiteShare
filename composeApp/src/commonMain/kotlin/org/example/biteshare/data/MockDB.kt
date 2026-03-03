package org.example.biteshare.data

import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.Restaurant
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
            password = "12345"
        ),
        User(
            id = "user_02",
            username = "Steven",
            email = "s38gao@uwaterloo.ca",
            password = "54321"
        )
    )

    val fakeFriends = listOf(
        Friend("alex", "Alex"),
        Friend("sally", "Sally"),
        Friend("mandy", "Mandy"),
        Friend("david", "David"),
    )

    val fakeRestaurants = listOf(
        Restaurant(
            id = "p1",
            name = "Joe's Pizza",
            category = "Pizza",
            price = "$12.99",
            eta = "30-40 min",
            rating = 4.5,
            isSaved = true,
            location = "Addis Ababa",
            isOpenNow = true
        ),
        Restaurant(
            id = "s1",
            name = "Sushi Star",
            category = "Sushi",
            price = "$49.99",
            eta = "50-60 min",
            rating = 4.0,
            location = "Addis Ababa",
            isOpenNow = false
        ),
        Restaurant(
            id = "b1",
            name = "Light Restaurant",
            category = "Burgers",
            price = "$12.50",
            eta = "25-35 min",
            rating = 4.2,
            isSaved = true,
            location = "Waterloo",
            isOpenNow = true
        ),
        Restaurant(
            id = "c1",
            name = "Hope Café",
            category = "Coffee",
            price = "$4.50",
            eta = "10-15 min",
            rating = 4.6,
            location = "Addis Ababa",
            isOpenNow = true
        ),
        Restaurant(
            id = "l1",
            name = "Abebe Hot",
            category = "Local",
            price = "$8.90",
            eta = "20-30 min",
            rating = 4.3,
            location = "Addis Ababa",
            isOpenNow = true
        ),
        Restaurant(
            id = "i1",
            name = "Pasta Palace",
            category = "Italian",
            price = "$18.99",
            eta = "30-40 min",
            rating = 4.4,
            location = "Kitchener",
            isOpenNow = false
        ),
        Restaurant(
            id = "d1",
            name = "Juice House",
            category = "Drink",
            price = "$6.00",
            eta = "15-20 min",
            rating = 4.1,
            location = "Waterloo",
            isOpenNow = true
        ),
        Restaurant(
            id = "bk1",
            name = "Sunrise Breakfast",
            category = "Breakfast",
            price = "$9.99",
            eta = "20-30 min",
            rating = 4.2,
            location = "Addis Ababa",
            isOpenNow = true
        ),
    )

    val fakeBrowseRestaurants = fakeRestaurants + listOf(
        Restaurant(
            id = "bp2",
            name = "Pizza Plaza",
            category = "Pizza",
            price = "$14.99",
            eta = "25-35 min",
            rating = 4.0,
            location = "Addis Ababa",
            isOpenNow = true
        ),
        Restaurant(
            id = "bp3",
            name = "Sushi Zen",
            category = "Sushi",
            price = "$32.99",
            eta = "30-45 min",
            rating = 4.1,
            location = "Waterloo",
            isOpenNow = true
        ),
        Restaurant(
            id = "bp4",
            name = "Burger Town",
            category = "Fast Food",
            price = "$7.99",
            eta = "20-30 min",
            rating = 3.9,
            location = "Kitchener",
            isOpenNow = true
        )
    )
}
