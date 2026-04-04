package org.example.biteshare.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainEntityTest {

    @Test
    fun restaurantClassificationNormalizesTags() {
        val normalized = RestaurantClassification.normalizeTag("  Sushi---Roll  ")
        assertEquals("sushi roll", normalized)
    }

    @Test
    fun restaurantClassificationDerivesAliases() {
        val tags = RestaurantClassification.deriveTags("Pizza", setOf("Italian"))
        assertTrue("pizza" in tags)
        assertTrue("italian" in tags)
    }

    @Test
    fun restaurantClassificationDerivesDietaryProfile() {
        val profile = RestaurantClassification.deriveDietaryProfile("Sushi", emptySet())
        assertEquals(DietaryLevel.NONE, profile.vegan)
    }

    @Test
    fun reviewTagCatalogNormalizesLabelsAndLegacyTags() {
        assertEquals("taste_good", ReviewTagCatalog.normalizeTag("Good Taste"))
        assertEquals("taste_spicy", ReviewTagCatalog.normalizeTag("too spicy"))
    }

    @Test
    fun reviewRatingHelpersClampAndLabel() {
        val review = Review(
            id = "r1",
            restaurantName = "Test",
            tags = emptyList(),
            content = "Ok",
            rating = 12,
        )
        assertEquals(10.0, review.ratingForAverage())
        assertEquals("12/10", review.ratingLabel())
    }

    @Test
    fun voteNotificationReadStatusReflectsEpoch() {
        val unread = VoteNotification(
            id = "n1",
            sessionId = "s1",
            userId = "u1",
            createdAtEpoch = 100L,
            readAtEpoch = null,
        )
        val read = unread.copy(readAtEpoch = 5L)
        assertFalse(unread.isRead)
        assertTrue(read.isRead)
    }
}
