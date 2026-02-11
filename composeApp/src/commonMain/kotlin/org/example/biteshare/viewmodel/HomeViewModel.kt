package org.example.biteshare.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.biteshare.model.CategoryItem
import org.example.biteshare.model.PopularItem

data class HomeUiState(
    val greeting: String = "Hi, John",
    val categories: List<CategoryItem> = emptyList(),
    val popularDishes: List<PopularItem> = emptyList(),
    val popularDrinks: List<PopularItem> = emptyList(),
)

class HomeViewModel {
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadHome()
    }

    private fun loadHome() {
        uiState = HomeUiState(
            greeting = "Hi, John",
            categories = listOf(
                CategoryItem("local", "Local"),
                CategoryItem("fastfood", "Fast Food"),
                CategoryItem("drink", "Drink"),
                CategoryItem("breakfast", "Breakfast"),
            ),
            popularDishes = listOf(
                PopularItem("d1", "Chess Burger", "Light Restaurant"),
                PopularItem("d2", "Pizza", "Joe's Pizza"),
                PopularItem("d3", "Beyayenet", "Abebe Hot"),
            ),
            popularDrinks = listOf(
                PopularItem("dr1", "Coffee", "Hope Café"),
                PopularItem("dr2", "Milkshake", "Love Café"),
                PopularItem("dr3", "Capo chin", "Hope Café"),
            ),
        )
    }
}
