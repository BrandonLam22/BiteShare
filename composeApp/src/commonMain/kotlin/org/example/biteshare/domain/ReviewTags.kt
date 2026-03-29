package org.example.biteshare.domain

data class ReviewTag(
    val id: String,
    val label: String,
)

data class ReviewTagCategory(
    val id: String,
    val title: String,
    val tags: List<ReviewTag>,
)

object ReviewTagCatalog {
    private val tasteTextureTags = listOf(
        ReviewTag("taste_good", "Good Taste"),
        ReviewTag("taste_bland", "Bland"),
        ReviewTag("taste_salty", "Too Salty"),
        ReviewTag("taste_sweet", "Too Sweet"),
        ReviewTag("taste_spicy", "Too Spicy"),
        ReviewTag("taste_greasy", "Greasy"),
        ReviewTag("texture_crispy", "Crispy"),
        ReviewTag("texture_dry", "Dry"),
    )

    private val qualityValueTags = listOf(
        ReviewTag("quality_fresh", "Fresh"),
        ReviewTag("quality_stale", "Stale"),
        ReviewTag("quality_undercooked", "Undercooked"),
        ReviewTag("quality_overcooked", "Overcooked"),
        ReviewTag("portion_large", "Large Portion"),
        ReviewTag("portion_small", "Small Portion"),
        ReviewTag("value_good", "Good Value"),
        ReviewTag("value_bad", "Poor Value"),
    )

    private val serviceWaitTags = listOf(
        ReviewTag("service_fast", "Fast Service"),
        ReviewTag("service_slow", "Slow Service"),
        ReviewTag("service_friendly", "Friendly Staff"),
        ReviewTag("service_rude", "Rude Staff"),
        ReviewTag("wait_long", "Long Wait"),
        ReviewTag("wait_short", "Short Wait"),
    )

    private val ambienceTags = listOf(
        ReviewTag("ambience_quiet", "Quiet"),
        ReviewTag("ambience_noisy", "Noisy"),
        ReviewTag("ambience_cozy", "Cozy"),
        ReviewTag("ambience_family_friendly", "Family Friendly"),
    )

    private val allTags = tasteTextureTags + qualityValueTags + serviceWaitTags + ambienceTags

    val categories: List<ReviewTagCategory> = listOf(
        ReviewTagCategory(
            id = "taste_texture",
            title = "Taste & Texture",
            tags = tasteTextureTags,
        ),
        ReviewTagCategory(
            id = "quality_value",
            title = "Quality & Value",
            tags = qualityValueTags,
        ),
        ReviewTagCategory(
            id = "service_wait",
            title = "Service & Wait",
            tags = serviceWaitTags,
        ),
        ReviewTagCategory(
            id = "ambience",
            title = "Ambience",
            tags = ambienceTags,
        ),
    )

    val availableTags: List<ReviewTag> = allTags

    private val byId = allTags.associateBy { it.id }
    private val normalizedLabelToId = allTags.associate { normalizeKey(it.label) to it.id }
    private val legacyMapping = mapOf(
        "i hate it" to "value_bad",
        "wait long" to "wait_long",
        "economical" to "value_good",
        "too spicy" to "taste_spicy",
        "good taste" to "taste_good",
        "expensive" to "value_bad",
        "come back" to "taste_good",
        "too salty" to "taste_salty",
        "raw" to "quality_undercooked",
    )

    fun labelFor(tagId: String): String = byId[tagId]?.label ?: tagId

    fun normalizeTags(tags: List<String>): List<String> =
        tags.mapNotNull { normalizeTag(it) }.distinct()

    fun normalizeTag(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (byId.containsKey(trimmed)) return trimmed
        val normalized = normalizeKey(trimmed)
        normalizedLabelToId[normalized]?.let { return it }
        legacyMapping[normalized]?.let { return it }
        return null
    }

    private fun normalizeKey(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
