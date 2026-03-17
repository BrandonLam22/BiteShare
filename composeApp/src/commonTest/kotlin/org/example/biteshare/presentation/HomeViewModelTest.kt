package org.example.biteshare.presentation

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.HomeViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Test
    fun initLoadsHomepageContent() = runMainTest {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo, repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.categories.isNotEmpty())
        assertTrue(vm.uiState.popularDishes.isNotEmpty())
        assertTrue(vm.uiState.popularDrinks.isNotEmpty())
        assertEquals(false, vm.uiState.isLoading)
        assertEquals(null, vm.uiState.errorMessage)
    }

    @Test
    fun loadHomeUsesProvidedUserNameInGreeting() = runMainTest {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo, repo)

        vm.loadHome("Kevin")
        advanceUntilIdle()

        assertEquals("Hi, Kevin", vm.uiState.greeting)
    }

    @Test
    fun homepageTypesAreDeliveryStyleCategories() = runMainTest {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo, repo)
        advanceUntilIdle()
        val labels = vm.uiState.categories.map { it.label }

        assertTrue(labels.contains("Fast Food"))
        assertTrue(labels.contains("Pizza"))
        assertTrue(labels.any { it == "Coffee & Tea" || it == "Bubble Tea" })
    }

    @Test
    fun categoryClickCanMapToRestaurantListThroughRepository() = runMainTest {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo, repo)
        advanceUntilIdle()
        val firstCategory = vm.uiState.categories.first().label

        val matched = repo.getRestaurantsByTag(firstCategory)

        assertTrue(matched.isNotEmpty())
    }
}
