package org.example.biteshare.data

import org.example.biteshare.domain.CategoryItem
import org.example.biteshare.domain.FeaturedItem
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.HomeFeed
import org.example.biteshare.domain.PopularItem
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.RestaurantDetail
import org.example.biteshare.domain.RestaurantLocation
import org.example.biteshare.domain.Review
import org.example.biteshare.domain.User

object MockDB {

    val friends = listOf(
        Friend("alex", "Alex"),
        Friend("sally", "Sally"),
        Friend("mandy", "Mandy"),
        Friend("david", "David"),
        Friend("kevin", "Kevin"),
        Friend("steven", "Steven"),
    )
    val fakeFriends: List<Friend> = friends

    val fakeUsers = listOf(
        User(
            id = "test-user-1",
            username = "Kevin",
            email = "k389zhan@uwaterloo.ca",
            password = "12345",
            savedRestaurantIds = listOf("p1", "b1", "bp1"),
            friends = fakeFriends
        ),
        User(
            id = "user_02",
            username = "Steven",
            email = "s38gao@uwaterloo.ca",
            password = "54321",
            savedRestaurantIds = listOf("s1", "c1")
        ),
        User(
            id = "user_03",
            username = "Alice",
            email = "alice@uwaterloo.ca",
            password = "alice123"
        ),
        User(
            id = "user_04",
            username = "Brandon",
            email = "blam@uwaterloo.ca",
            password = "brandon123"
        )
    )

    val coreRestaurants = listOf(
        Restaurant("p1", "Joe's Pizza", "Pizza", "$12.99", "18-28 min", 4.6, isSaved = true, location = "Waterloo"),
        Restaurant("s1", "Ken Sushi House", "Sushi", "$19.50", "25-35 min", 4.4, location = "Waterloo"),
        Restaurant("b1", "Mel's Diner", "Burgers", "$13.75", "20-30 min", 4.2, isSaved = true, location = "Waterloo"),
        Restaurant("c1", "Williams Fresh Cafe", "Coffee", "$5.20", "8-15 min", 4.5, location = "Waterloo"),
        Restaurant("h1", "Hope Café", "Coffee", "$4.50", "10-15 min", 4.9, location = "Addis Ababa"),
        Restaurant("l1", "Abebe Hot", "Local", "$8.90", "20-30 min", 4.3, location = "Addis Ababa"),
        Restaurant("uw1", "Lazeez Shawarma", "Middle Eastern", "$11.25", "10-18 min", 4.3, isSaved = true, location = "Waterloo"),
        Restaurant("uw2", "iPotato", "Fast Food", "$9.99", "12-20 min", 4.1, location = "Waterloo"),
        Restaurant("uw3", "The Alley Waterloo", "Drink", "$6.40", "9-16 min", 4.4, location = "Waterloo"),
        Restaurant("uw4", "Gol's Lanzhou Noodle", "Noodles", "$14.30", "18-26 min", 4.5, location = "Waterloo"),
    )

    val browseRestaurants = listOf(
        Restaurant("bp1", "Pasta Palace", "Italian", "$13.50", "22-32 min", 4.2, isSaved = true, location = "Waterloo"),
        Restaurant("bp2", "Pizza Plaza", "Pizza", "$11.50", "19-29 min", 4.0, location = "Addis Ababa"),
        Restaurant("bp3", "Campus Pizza", "Pizza", "$10.20", "16-24 min", 4.1, location = "Waterloo"),
        Restaurant("bp4", "Burger Town", "Fast Food", "$7.99", "20-30 min", 3.9, location = "Kitchener"),
        Restaurant("uw5", "Shawerma Plus", "Middle Eastern", "$12.40", "14-22 min", 4.2, location = "Waterloo"),
        Restaurant("uw6", "Bao Sandwich Bar", "Asian Fusion", "$13.80", "17-27 min", 4.4, location = "Waterloo"),
        Restaurant("uw7", "The Cactus Board Game Cafe", "Cafe", "$8.60", "11-18 min", 4.3, location = "Waterloo"),
        Restaurant("uw8", "Aunty's Kitchen", "Indian", "$15.90", "23-33 min", 4.5, location = "Waterloo"),
        Restaurant("uw9", "Kabob Hut Waterloo", "Grill", "$14.60", "20-30 min", 4.2, location = "Waterloo"),
    )

    val fakeRestaurants: List<Restaurant> = coreRestaurants
    val fakeBrowseRestaurants: List<Restaurant> = coreRestaurants + browseRestaurants
    val defaultSavedRestaurantIds: Set<String> = fakeUsers.firstOrNull()?.savedRestaurantIds?.toSet().orEmpty()

    val allRestaurants: List<Restaurant> = (coreRestaurants + browseRestaurants).distinctBy { it.id }

    val fakeReviews = listOf(
        Review(
            id = "r1",
            restaurantName = "Joe's Pizza",
            tags = listOf("Pizza", "Late Night"),
            content = "Great crust and very generous toppings.",
            rating = 9
        ),
        Review(
            id = "r2",
            restaurantName = "Ken Sushi House",
            tags = listOf("Fresh", "Seafood"),
            content = "Good sashimi quality for student budget.",
            rating = 8
        ),
        Review(
            id = "r3",
            restaurantName = "Mel's Diner",
            tags = listOf("Burgers", "Comfort Food"),
            content = "Classic diner burgers and quick service.",
            rating = 8
        ),
        Review(
            id = "r4",
            restaurantName = "Williams Fresh Cafe",
            tags = listOf("Coffee", "Study Spot"),
            content = "Nice place to study with reliable coffee.",
            rating = 9
        ),
        Review(
            id = "r5",
            restaurantName = "Hope Café",
            tags = listOf("Coffee", "Affordable"),
            content = "Budget coffee with consistent quality.",
            rating = 9
        ),
        Review(
            id = "r6",
            restaurantName = "Lazeez Shawarma",
            tags = listOf("Spicy", "Fast"),
            content = "Fast and filling after evening classes.",
            rating = 8
        ),
        Review(
            id = "r7",
            restaurantName = "iPotato",
            tags = listOf("Fast Food", "Affordable"),
            content = "Good value and lots of topping choices.",
            rating = 7
        ),
        Review(
            id = "r8",
            restaurantName = "The Alley Waterloo",
            tags = listOf("Bubble Tea", "Sweet"),
            content = "The brown sugar milk tea is consistently good.",
            rating = 9
        ),
        Review(
            id = "r9",
            restaurantName = "Gol's Lanzhou Noodle",
            tags = listOf("Noodles", "Hand Pulled"),
            content = "Hand-pulled noodles are chewy and flavorful.",
            rating = 9
        ),
        Review(
            id = "r10",
            restaurantName = "Pasta Palace",
            tags = listOf("Italian", "Group Friendly"),
            content = "Great portion sizes for sharing.",
            rating = 8
        ),
        Review(
            id = "r11",
            restaurantName = "Pizza Plaza",
            tags = listOf("Pizza", "Takeout"),
            content = "Reliable takeout and good lunch specials.",
            rating = 8
        ),
        Review(
            id = "r12",
            restaurantName = "Pizza Palace",
            tags = listOf("Pizza", "Cheesy"),
            content = "Great cheesy slices near campus.",
            rating = 8
        ),
        Review(
            id = "r13",
            restaurantName = "Campus Pizza",
            tags = listOf("Budget Friendly", "Quick Bite"),
            content = "Solid quick bite near campus.",
            rating = 7
        ),
        Review(
            id = "r14",
            restaurantName = "Shawerma Plus",
            tags = listOf("Middle Eastern", "Late Night"),
            content = "Open late and portions are huge.",
            rating = 8
        ),
        Review(
            id = "r15",
            restaurantName = "Bao Sandwich Bar",
            tags = listOf("Fusion", "Creative"),
            content = "Interesting flavors and very fresh buns.",
            rating = 8
        ),
        Review(
            id = "r16",
            restaurantName = "The Cactus Board Game Cafe",
            tags = listOf("Cafe", "Group"),
            content = "Fun spot for board games and drinks.",
            rating = 9
        ),
        Review(
            id = "r17",
            restaurantName = "Aunty's Kitchen",
            tags = listOf("Indian", "Rich Flavor"),
            content = "Very flavorful curry and friendly staff.",
            rating = 9
        ),
        Review(
            id = "r18",
            restaurantName = "Kabob Hut Waterloo",
            tags = listOf("Grill", "Protein"),
            content = "Fresh grilled skewers and great garlic sauce.",
            rating = 8
        )
    )

    val homeCategories = listOf(
        CategoryItem("local", "Local"),
        CategoryItem("fastfood", "Fast Food"),
        CategoryItem("drink", "Drink"),
        CategoryItem("breakfast", "Breakfast"),
    )

    fun homeFeedFor(userName: String): HomeFeed {
        val topRated = allRestaurants.sortedByDescending { it.rating }.take(3)
        return HomeFeed(
            greeting = "Hi, $userName",
            categories = homeCategories,
            popularDishes = listOf(
                PopularItem("d1", "Cheese Burger", topRated.getOrNull(0)?.name ?: "Mel's Diner"),
                PopularItem("d2", "Pizza", topRated.getOrNull(1)?.name ?: "Joe's Pizza"),
                PopularItem("d3", "Beyayenet", topRated.getOrNull(2)?.name ?: "Aunty's Kitchen"),
            ),
            popularDrinks = listOf(
                PopularItem("dr1", "Coffee", "Williams Fresh Cafe"),
                PopularItem("dr2", "Milkshake", "Mel's Diner"),
                PopularItem("dr3", "Capo chino", "Williams Fresh Cafe"),
            ),
        )
    }

    val restaurantDetails: Map<String, RestaurantDetail> = allRestaurants.associate { restaurant ->
        restaurant.id to generatedDetail(restaurant)
    }.toMutableMap().apply {
        this["uw1"] = generatedDetail(
            restaurant = allRestaurants.first { it.id == "uw1" },
            description = "Popular student stop for shawarma and rice platters near UW.",
            attributes = listOf("Halal", "Late Night", "Student Favorite"),
            featured = listOf(
                FeaturedItem("Chicken on the Rocks", "$12.25", "Rice, chicken, garlic sauce, and hot sauce"),
                FeaturedItem("Wrap Combo", "$11.50", "Shawarma wrap with fries and drink")
            )
        )
        this["uw3"] = generatedDetail(
            restaurant = allRestaurants.first { it.id == "uw3" },
            description = "Bubble tea shop known for brown sugar series and chewy pearls.",
            attributes = listOf("Bubble Tea", "Dessert Drinks", "Takeout"),
            featured = listOf(
                FeaturedItem("Brown Sugar Deerioca", "$6.90", "Signature drink with warm pearls"),
                FeaturedItem("Royal No. 9 Milk Tea", "$6.20", "House black tea blend")
            )
        )
        this["uw8"] = generatedDetail(
            restaurant = allRestaurants.first { it.id == "uw8" },
            description = "Comfort Indian dishes with rich curries and fresh naan.",
            attributes = listOf("Indian", "Spicy", "Dine-In"),
            featured = listOf(
                FeaturedItem("Butter Chicken", "$16.90", "Creamy tomato curry with naan"),
                FeaturedItem("Paneer Tikka", "$15.40", "Char-grilled paneer with spices")
            )
        )
    }

    fun reviewsForRestaurant(name: String): List<Review> =
        fakeReviews.filter { it.restaurantName == name }

    private fun generatedDetail(
        restaurant: Restaurant,
        description: String = "${restaurant.name} is a reliable option near University of Waterloo for ${restaurant.category.lowercase()} lovers.",
        attributes: List<String> = attributesByCategory(restaurant.category),
        featured: List<FeaturedItem> = featuredItemsByCategory(restaurant.category, restaurant.price),
    ): RestaurantDetail {
        val query = restaurant.name.replace(" ", "+")
        return RestaurantDetail(
            restaurantId = restaurant.id,
            name = restaurant.name,
            images = listOf(imageByCategory(restaurant.category), "drink_home"),
            featuredItems = featured,
            location = RestaurantLocation(
                city = restaurant.location,
                address = "${restaurant.name}, University Ave W, ${restaurant.location}",
                distance = restaurant.eta.replace(" min", " away")
            ),
            reviews = reviewsForRestaurant(restaurant.name),
            description = description,
            rating = restaurant.rating,
            attributes = attributes,
            googleMapsLink = "https://www.google.com/maps/search/?api=1&query=$query+${restaurant.location.replace(" ", "+")}",
            restaurantWebsite = "https://example.com/${restaurant.id}",
            priceRange = restaurant.price,
        )
    }

    private fun imageByCategory(category: String): String {
        return when (category.lowercase()) {
            "pizza" -> "pizza_home"
            "sushi" -> "beyayenet_home"
            "burgers", "fast food" -> "chesse_burger_home"
            "coffee", "cafe", "drink" -> "coffee_home"
            "italian" -> "local_home"
            else -> "local_home"
        }
    }

    private fun featuredItemsByCategory(category: String, price: String): List<FeaturedItem> {
        return when (category.lowercase()) {
            "pizza" -> listOf(
                FeaturedItem("Pepperoni Pizza", price, "Crispy crust, rich tomato sauce, and mozzarella"),
                FeaturedItem("Deluxe Slice Combo", price, "Two slices with house dipping sauce")
            )
            "sushi" -> listOf(
                FeaturedItem("Salmon Nigiri Set", price, "Fresh salmon nigiri with miso soup"),
                FeaturedItem("Dragon Roll", price, "Avocado, tempura shrimp, and eel sauce")
            )
            "burgers" -> listOf(
                FeaturedItem("Classic Beef Burger", price, "House patty, cheddar, lettuce, and signature sauce"),
                FeaturedItem("Loaded Fries", price, "Fries topped with cheese, herbs, and garlic aioli")
            )
            "coffee", "cafe" -> listOf(
                FeaturedItem("Flat White", price, "Smooth espresso with silky steamed milk"),
                FeaturedItem("Iced Mocha", price, "Chocolate, espresso, and milk over ice")
            )
            "drink" -> listOf(
                FeaturedItem("Brown Sugar Milk Tea", price, "Warm pearls with creamy milk tea"),
                FeaturedItem("Mango Fruit Tea", price, "Refreshing mango tea with fruit jelly")
            )
            "middle eastern" -> listOf(
                FeaturedItem("Chicken Shawarma Plate", price, "Rice, garlic sauce, pickles, and chicken"),
                FeaturedItem("Beef Wrap", price, "Grilled beef wrap with fries and tahini")
            )
            "fast food" -> listOf(
                FeaturedItem("Loaded Poutine", price, "Crispy fries with gravy and cheese curds"),
                FeaturedItem("Chicken Tenders Basket", price, "Breaded tenders with dipping sauces")
            )
            "noodles" -> listOf(
                FeaturedItem("Hand-Pulled Beef Noodle", price, "Rich broth with fresh hand-pulled noodles"),
                FeaturedItem("Spicy Dan Dan Noodles", price, "Sesame chili sauce and minced pork")
            )
            "italian" -> listOf(
                FeaturedItem("Chicken Alfredo Pasta", price, "Creamy sauce with grilled chicken"),
                FeaturedItem("Baked Lasagna", price, "Layered pasta with beef ragu and mozzarella")
            )
            "asian fusion" -> listOf(
                FeaturedItem("Crispy Bao Combo", price, "Steamed bao with crispy protein filling"),
                FeaturedItem("Korean Chicken Rice Bowl", price, "Gochujang glazed chicken over rice")
            )
            "indian" -> listOf(
                FeaturedItem("Butter Chicken", price, "Creamy tomato curry served with naan"),
                FeaturedItem("Paneer Tikka Masala", price, "Paneer cubes in spiced masala gravy")
            )
            "grill" -> listOf(
                FeaturedItem("Mixed Grill Platter", price, "Chicken, beef, and lamb skewers with rice"),
                FeaturedItem("Garlic Kofta Plate", price, "Seasoned kofta with salad and pita")
            )
            else -> listOf(
                FeaturedItem("House Special", price, "Most ordered item from this restaurant"),
                FeaturedItem("Chef Recommendation", price, "Balanced flavor and generous portion")
            )
        }
    }

    private fun attributesByCategory(category: String): List<String> {
        return when (category.lowercase()) {
            "pizza" -> listOf("Pizza", "Late Night", "Takeout")
            "sushi" -> listOf("Seafood", "Fresh", "Date Night")
            "burgers" -> listOf("Comfort Food", "Fast Service", "Takeout")
            "coffee", "cafe" -> listOf("Coffee", "Study Friendly", "Wi-Fi")
            "drink" -> listOf("Bubble Tea", "Dessert Drinks", "Quick Pickup")
            "middle eastern" -> listOf("Halal", "Protein Rich", "Late Night")
            "fast food" -> listOf("Budget Friendly", "Quick Bite", "Takeout")
            "noodles" -> listOf("Noodles", "Hot Meals", "Popular")
            "italian" -> listOf("Italian", "Group Friendly", "Dine-In")
            "asian fusion" -> listOf("Fusion", "Trendy", "Takeout")
            "indian" -> listOf("Indian", "Spicy", "Rich Flavor")
            "grill" -> listOf("Grill", "High Protein", "Family Friendly")
            "local" -> listOf("Local", "Traditional", "Student Friendly")
            else -> listOf(category, "Takeout", "Student Friendly")
        }
    }
}
