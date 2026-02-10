package org.example.biteshare.model

class FakeRepository {

    fun friends(): List<Friend> = listOf(
        Friend("alex", "Alex"),
        Friend("sally", "Sally"),
        Friend("mandy", "Mandy"),
        Friend("david", "David"),
    )

    fun restaurants(): List<Restaurant> = listOf(
        Restaurant("p1", "Joe's Pizza", "Pizza", "$2.99", "30-40 min", 4.5, isSaved = true),
        Restaurant("s1", "Sushi Star", "Sushi", "$49.99", "50-60 min", 4.0),
        Restaurant("b1", "Light Restaurant", "Burgers", "$12.50", "25-35 min", 4.2, isSaved = true),
        Restaurant("c1", "Hope Café", "Coffee", "$4.50", "10-15 min", 4.6),
    )

    fun recommend(context: PickContext): List<Restaurant> {
        val base = restaurants().sortedByDescending { it.rating }
        return when (context.mode) {
            PickMode.ME_ONLY -> base
            PickMode.WITH_FRIENDS -> {
                if (context.selectedFriendIds.size >= 2) base.reversed() else base
            }
        }
    }
}
