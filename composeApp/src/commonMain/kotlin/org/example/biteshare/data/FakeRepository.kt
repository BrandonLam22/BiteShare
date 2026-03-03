package org.example.biteshare.data

import org.example.biteshare.domain.EditableProfile
import org.example.biteshare.domain.Friend
import org.example.biteshare.domain.PickContext
import org.example.biteshare.domain.PickMode
import org.example.biteshare.domain.Restaurant
import org.example.biteshare.domain.Model
import org.example.biteshare.domain.ProfileData

class FakeRepository (private val model: Model) {

    private var notificationsEnabled = true
    fun friends(): List<Friend> = listOf(
        Friend("alex", "Alex"),
        Friend("sally", "Sally"),
        Friend("mandy", "Mandy"),
        Friend("david", "David"),
    )

    fun restaurants(): List<Restaurant> {
        val userSavedIds = model.getSavedRestaurantIds()
        
        return listOf(
            Restaurant("p1", "Joe's Pizza", "Pizza", "$2.99", "30-40 min", 4.5, 
                isSaved = userSavedIds.contains("p1")),
            Restaurant("s1", "Sushi Star", "Sushi", "$49.99", "50-60 min", 4.0, 
                isSaved = userSavedIds.contains("s1")),
            Restaurant("b1", "Light Restaurant", "Burgers", "$12.50", "25-35 min", 4.2, 
                isSaved = userSavedIds.contains("b1")),
            Restaurant("c1", "Hope Café", "Coffee", "$4.50", "10-15 min", 4.6, 
                isSaved = userSavedIds.contains("c1")),
        )
    }

    fun getRestaurantById(id: String): Restaurant? =
        (restaurants() + browseRestaurants()).distinctBy { it.id }.find { it.id == id }

    /** 浏览页用：更多餐厅（含 Pasta Palace、Pizza Plaza 等） */
    fun browseRestaurants(): List<Restaurant> {
        val userSavedIds = model.getSavedRestaurantIds()
        
        return listOf(
            Restaurant("bp1", "Pasta Palace", "Italian", "$3.99", "30-40 min", 4.5, 
                isSaved = userSavedIds.contains("bp1")),
            Restaurant("bp2", "Pizza Plaza", "Pizza", "$2.99", "25-35 min", 4.0, 
                isSaved = userSavedIds.contains("bp2")),
            Restaurant("bp3", "Pizza", "Pizza", "$2.99", "30-40 min", 4.5, 
                isSaved = userSavedIds.contains("bp3")),
            Restaurant("b1", "Light Restaurant", "Burgers", "$12.50", "25-35 min", 4.2, 
                isSaved = userSavedIds.contains("b1")),
            Restaurant("p1", "Joe's Pizza", "Pizza", "$2.99", "30-40 min", 4.5, 
                isSaved = userSavedIds.contains("p1")),
            Restaurant("s1", "Sushi Star", "Sushi", "$49.99", "50-60 min", 4.0, 
                isSaved = userSavedIds.contains("s1")),
        )
    }

    fun recommend(context: PickContext): List<Restaurant> {
        val base = restaurants().sortedByDescending { it.rating }
        return when (context.mode) {
            PickMode.ME_ONLY -> base
            PickMode.WITH_FRIENDS -> {
                if (context.selectedFriendIds.size >= 2) base.reversed() else base
            }
        }
    }

    fun getProfile(): ProfileData {
        val user = model.currentUser
        
        return ProfileData(
            name = user?.username ?: "Guest",
            email = user?.email ?: "",
            friendCount = user?.friends?.size ?: 0,
            notificationsEnabled = notificationsEnabled
        )
    }

    fun updateNotificationPreference(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    fun getSavedRestaurants(): List<Restaurant> {
        val userSavedIds = model.getSavedRestaurantIds()
        
        return (restaurants() + browseRestaurants())
            .distinctBy { it.id }
            .filter { userSavedIds.contains(it.id) }
    }

    fun toggleSaved(restaurantId: String) {
        model.toggleSavedRestaurant(restaurantId)
    }

    fun logout() {
        model.logout()
    }

    fun getEditableProfile(): EditableProfile {
        val user = model.currentUser
        return EditableProfile(
            username = user?.username ?: "",
            email = user?.email ?: "",
            bio = user?.bio ?: "",
            preferences = user?.preferences ?: emptyList(),
            foodRestrictions = user?.foodRestrictions ?: emptyList()
        )
    }

    fun verifyPassword(password: String): Boolean {
        val user = model.currentUser ?: return false
        return user.password == password
    }

    fun updatePassword(newPassword: String) {
        model.updatePassword(newPassword)
    }

    fun updateProfile(
        username: String,
        email: String,
        bio: String,
        preferences: List<String>,
        foodRestrictions: List<String>
    ) {
        model.updateUserProfile(username, email, bio, preferences, foodRestrictions)
    }


}
