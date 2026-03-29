package org.example.biteshare.domain

object RestaurantClassification {
    fun normalizeTag(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    fun deriveTags(category: String, existing: Set<String> = emptySet()): Set<String> {
        val normalizedCategory = normalizeTag(category)
        val tags = existing
            .map { normalizeTag(it) }
            .filter { it.isNotBlank() }
            .toMutableSet()

        if (normalizedCategory.isNotBlank()) {
            tags += normalizedCategory
            tags += aliasTags(normalizedCategory)
        }

        return tags
    }

    fun deriveDietaryProfile(category: String, tags: Set<String>): DietaryProfile {
        val normalizedCategory = normalizeTag(category)
        val normalizedTags = tags.map { normalizeTag(it) }.toSet()

        if ("vegan" in normalizedTags) {
            return DietaryProfile(
                vegan = DietaryLevel.FULL,
                vegetarian = DietaryLevel.FULL,
            )
        }
        if ("vegetarian" in normalizedTags) {
            return DietaryProfile(
                vegan = DietaryLevel.PARTIAL,
                vegetarian = DietaryLevel.FULL,
            )
        }

        return when (normalizedCategory) {
            "coffee", "cafe", "drink" ->
                DietaryProfile(
                    vegan = DietaryLevel.PARTIAL,
                    vegetarian = DietaryLevel.FULL,
                    glutenFree = DietaryLevel.PARTIAL,
                    dairyFree = DietaryLevel.PARTIAL,
                )
            "pizza", "italian", "indian", "noodles", "asian fusion", "local" ->
                DietaryProfile(
                    vegan = DietaryLevel.PARTIAL,
                    vegetarian = DietaryLevel.PARTIAL,
                )
            "sushi", "seafood" ->
                DietaryProfile(
                    vegan = DietaryLevel.NONE,
                    vegetarian = DietaryLevel.PARTIAL,
                )
            "burgers", "burger", "fast food", "grill" ->
                DietaryProfile(
                    vegan = DietaryLevel.NONE,
                    vegetarian = DietaryLevel.PARTIAL,
                )
            "middle eastern", "shawarma" ->
                DietaryProfile(
                    vegan = DietaryLevel.PARTIAL,
                    vegetarian = DietaryLevel.PARTIAL,
                    halal = DietaryLevel.PARTIAL,
                )
            else -> DietaryProfile()
        }
    }

    fun profileFromLevels(
        vegan: String?,
        vegetarian: String?,
        halal: String?,
        glutenFree: String?,
        dairyFree: String?,
    ): DietaryProfile =
        DietaryProfile(
            vegan = parseDietaryLevel(vegan),
            vegetarian = parseDietaryLevel(vegetarian),
            halal = parseDietaryLevel(halal),
            glutenFree = parseDietaryLevel(glutenFree),
            dairyFree = parseDietaryLevel(dairyFree),
        )

    fun isAllUnknown(profile: DietaryProfile): Boolean =
        profile.vegan == DietaryLevel.UNKNOWN &&
            profile.vegetarian == DietaryLevel.UNKNOWN &&
            profile.halal == DietaryLevel.UNKNOWN &&
            profile.glutenFree == DietaryLevel.UNKNOWN &&
            profile.dairyFree == DietaryLevel.UNKNOWN

    fun parseDietaryLevel(value: String?): DietaryLevel =
        when (value?.trim()?.uppercase()) {
            "NONE" -> DietaryLevel.NONE
            "PARTIAL" -> DietaryLevel.PARTIAL
            "FULL" -> DietaryLevel.FULL
            "UNKNOWN" -> DietaryLevel.UNKNOWN
            else -> DietaryLevel.UNKNOWN
        }

    private fun aliasTags(category: String): Set<String> =
        when (category) {
            "pizza" -> setOf("pizza", "italian")
            "italian" -> setOf("italian", "pizza", "pasta")
            "sushi" -> setOf("sushi", "japanese", "seafood")
            "seafood" -> setOf("seafood", "fish")
            "burgers", "burger" -> setOf("burgers", "burger", "fast food")
            "fast food" -> setOf("fast food", "burger")
            "coffee" -> setOf("coffee", "cafe", "drink")
            "cafe" -> setOf("cafe", "coffee", "drink")
            "drink" -> setOf("drink", "bubble tea", "tea")
            "bubble tea" -> setOf("bubble tea", "tea", "drink")
            "tea" -> setOf("tea", "drink")
            "middle eastern" -> setOf("middle eastern", "shawarma", "halal")
            "shawarma" -> setOf("middle eastern", "shawarma", "halal")
            "indian" -> setOf("indian", "curry")
            "noodles" -> setOf("noodles", "chinese")
            "asian fusion" -> setOf("asian fusion", "asian")
            "grill" -> setOf("grill", "meat")
            "breakfast" -> setOf("breakfast", "brunch")
            "dessert", "desserts" -> setOf("dessert", "sweet")
            else -> emptySet()
        }
}
