package org.example.biteshare.domain

import org.example.biteshare.data.PickMockDB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PickModelTest {

    @Test
    fun recommendRespectsLocationFilter() {
        val repo = PickMockDB()
        val model = PickModel(repo)

        val context = PickContext(
            mode = PickMode.ME_ONLY,
            filters = PickFilters(location = "Waterloo")
        )

        val results = model.recommend(context)
        assertTrue(results.isNotEmpty(), "Expected recommendations for Waterloo.")
        assertTrue(results.all { it.location == "Waterloo" })
    }

    @Test
    fun recommendFiltersOutRestrictedItems() {
        val repo = PickMockDB(restrictions = listOf("coffee"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val results = model.recommend(context)

        assertTrue(results.isNotEmpty())
        assertTrue(results.none { it.category.equals("coffee", ignoreCase = true) })
    }

    @Test
    fun preferencesInfluenceRanking() {
        val repo = PickMockDB(preferences = listOf("pizza"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val results = model.recommend(context)

        assertTrue(results.isNotEmpty())
        assertTrue(results.first().category.equals("pizza", ignoreCase = true))
    }

    @Test
    fun previewCountMatchesRecommendationSize() {
        val repo = PickMockDB(preferences = listOf("sushi"))
        val model = PickModel(repo)

        val context = PickContext(mode = PickMode.ME_ONLY)
        val count = model.previewCount(context)
        val results = model.recommend(context)

        assertEquals(results.size, count)
    }
}
