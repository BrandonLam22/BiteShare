package org.example.biteshare.presentation

import org.example.biteshare.data.FakeRepository
import org.example.biteshare.viewmodel.BrowseViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowseViewModelTest {

    @Test
    fun updateSearchQueryFiltersRestaurantList() {
        val vm = BrowseViewModel(FakeRepository())

        vm.updateSearchQuery("Lazeez")

        assertTrue(vm.uiState.restaurants.isNotEmpty())
        assertTrue(vm.uiState.restaurants.all { it.name.contains("Lazeez", ignoreCase = true) })
    }

    @Test
    fun applyTagFilterThenSearchCombinesBothConditions() {
        val vm = BrowseViewModel(FakeRepository())

        vm.applyTagFilter("Middle Eastern")
        vm.updateSearchQuery("Lazeez")

        assertTrue(vm.uiState.restaurants.isNotEmpty())
        assertTrue(vm.uiState.restaurants.all { it.name.contains("Lazeez", ignoreCase = true) })
        assertEquals("Middle Eastern", vm.uiState.activeTag)
    }

    @Test
    fun clearSearchRestoresCurrentTagResultSet() {
        val vm = BrowseViewModel(FakeRepository())

        vm.applyTagFilter("Pizza")
        val tagCount = vm.uiState.resultCount
        vm.updateSearchQuery("Campus")
        vm.clearSearch()

        assertEquals("", vm.uiState.searchQuery)
        assertEquals(tagCount, vm.uiState.resultCount)
    }
}
