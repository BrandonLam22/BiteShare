package org.example.biteshare.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.example.biteshare.data.FakeRepository
import org.example.biteshare.runMainTest
import org.example.biteshare.viewmodel.BrowseViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    @Test
    fun initLoadsBrowseRestaurants() = runMainTest {
        val repo = FakeRepository()
        val vm = BrowseViewModel(repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.restaurants.isNotEmpty())
        assertEquals(vm.uiState.restaurants.size, vm.uiState.allRestaurants.size)
        assertEquals(vm.uiState.restaurants.size, vm.uiState.resultCount)
        assertEquals("Top Food Places", vm.uiState.headerTitle)
    }

    @Test
    fun applyTagFilterUsesRepository() = runMainTest {
        val repo = FakeRepository()
        val vm = BrowseViewModel(repo)
        advanceUntilIdle()

        vm.applyTagFilter("Pizza")
        advanceUntilIdle()

        val expected = repo.getRestaurantsByTag("Pizza")
        assertEquals(expected.map { it.id }.toSet(), vm.uiState.restaurants.map { it.id }.toSet())
        assertEquals("Top Pizza Places", vm.uiState.headerTitle)
        assertEquals("Pizza", vm.uiState.activeTag)
    }

    @Test
    fun searchQueryFiltersResults() = runMainTest {
        val repo = FakeRepository()
        val vm = BrowseViewModel(repo)
        advanceUntilIdle()

        vm.updateSearchQuery("Pizza Plaza")
        advanceUntilIdle()

        assertTrue(vm.uiState.searchQuery.contains("Pizza"))
        assertTrue(vm.uiState.restaurants.any { it.name == "Pizza Plaza" })
    }
}
