package org.example.biteshare.domain

import org.example.biteshare.data.FakeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PickModelTest {

    private val repo = FakeRepository()
    private val model = PickModel(repo)

    @Test
    fun recommendInMeOnlyModeIsSortedByRating() {
        val context = PickContext(mode = PickMode.ME_ONLY)

        val items = model.recommend(context)

        assertTrue(items.isNotEmpty())
        assertEquals("Hope Café", items.first().name)
        assertTrue(items.zipWithNext().all { (a, b) -> a.rating >= b.rating })
    }

    @Test
    fun recommendAppliesLocationBudgetCuisineAndRatingFilters() {
        val filters = PickFilters(
            location = "Addis Ababa",
            budget = BudgetFilter.BUDGET,
            cuisine = CuisineFilter.DRINK,
            openNowOnly = true,
            minRating = 9.0
        )
        val context = PickContext(mode = PickMode.ME_ONLY, filters = filters)

        val items = model.recommend(context)

        assertEquals(1, items.size)
        assertEquals("Hope Café", items.first().name)
    }

    @Test
    fun withFriendsAndTwoSelectionsPrioritizesLowerPrice() {
        val context = PickContext(
            mode = PickMode.WITH_FRIENDS,
            selectedFriendIds = setOf("alex", "sally")
        )

        val items = model.recommend(context)
        val priceValues = items.map { it.price.filter { ch -> ch.isDigit() || ch == '.' }.toDouble() }

        assertTrue(items.isNotEmpty())
        assertEquals("Hope Café", items.first().name)
        assertTrue(priceValues.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun previewCountMatchesRecommendSize() {
        val context = PickContext(mode = PickMode.ME_ONLY)

        val previewCount = model.previewCount(context)
        val actualCount = model.recommend(context).size

        assertEquals(actualCount, previewCount)
    }
}
