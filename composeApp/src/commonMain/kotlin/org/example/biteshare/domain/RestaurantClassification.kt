package org.example.biteshare.domain

object RestaurantClassification {
    /**
     * Keys for bundled drawables used by [org.example.biteshare.view.DetailView] when
     * no remote image URLs are available.
     */
    /**
     * Maps a category label (e.g. from Google types: "Sushi restaurant") to a bundled drawable key
     * for Home/Browse when [Restaurant.imageUrl] is null.
     */
    fun categoryLabelToImageKey(label: String): String {
        val l = label.lowercase()
        return when {
            "pizza" in l -> "pizza_home"
            "sushi" in l || "japanese" in l -> "beyayenet_home"
            "burger" in l || "fast food" in l -> "chesse_burger_home"
            "coffee" in l || "cafe" in l || "café" in l || "tea" in l -> "coffee_home"
            "italian" in l -> "local_home"
            "indian" in l || "chinese" in l || "thai" in l || "vietnamese" in l || "korean" in l -> "local_home"
            "mexican" in l -> "fast_food_home"
            "bubble" in l || "sandwich" in l || "shop" in l -> "drink_home"
            "breakfast" in l || "brunch" in l -> "breakfast_home"
            "dessert" in l || "milkshake" in l || "ice cream" in l -> "milkshake_home"
            "seafood" in l -> "beyayenet_home"
            "middle eastern" in l || "shawarma" in l || "halal" in l -> "fast_food_home"
            else -> "local_home"
        }
    }

    fun defaultDetailImageKeys(category: String): List<String> {
        val key = when (category.lowercase()) {
            "pizza" -> "pizza_home"
            "sushi" -> "beyayenet_home"
            "burgers", "fast food" -> "chesse_burger_home"
            "coffee", "cafe", "drink" -> "coffee_home"
            "italian" -> "local_home"
            else -> "coffee_home"
        }
        return listOf(key, "drink_home")
    }

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
