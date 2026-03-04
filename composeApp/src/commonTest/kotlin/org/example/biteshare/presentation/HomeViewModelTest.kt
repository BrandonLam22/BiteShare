package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.viewmodel.HomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeViewModelTest {

    @Test
    fun initLoadsHomepageContent() {
        val vm = HomeViewModel(FakeRepository())

        assertTrue(vm.uiState.categories.isNotEmpty())
        assertTrue(vm.uiState.popularDishes.isNotEmpty())
        assertTrue(vm.uiState.popularDrinks.isNotEmpty())
        assertEquals(false, vm.uiState.isLoading)
        assertEquals(null, vm.uiState.errorMessage)
    }

    @Test
    fun loadHomeUsesProvidedUserNameInGreeting() {
        val vm = HomeViewModel(FakeRepository())

        vm.loadHome("Kevin")

        assertEquals("Hi, Kevin", vm.uiState.greeting)
    }

    @Test
    fun homepageTypesAreDeliveryStyleCategories() {
        val vm = HomeViewModel(FakeRepository())
        val labels = vm.uiState.categories.map { it.label }

        assertTrue(labels.contains("Fast Food"))
        assertTrue(labels.contains("Pizza"))
        assertTrue(labels.any { it == "Coffee & Tea" || it == "Bubble Tea" })
    }

    @Test
    fun categoryClickCanMapToRestaurantListThroughRepository() {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo)
        val firstCategory = vm.uiState.categories.first().label

        val matched = repo.getRestaurantsByTag(firstCategory)

        assertTrue(matched.isNotEmpty())
    }
}
