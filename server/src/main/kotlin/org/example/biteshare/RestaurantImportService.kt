package org.example.biteshare

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object RestaurantImportService {
    private const val defaultSupabaseUrl = "https://gxpxxzfhfvvtvgvszrhc.supabase.co"
    private const val defaultSupabaseKey = "sb_publishable_k7Us_sO5ew4o9pOe_t46jg_QuXXlcU8"
    private const val restaurantsTable = "restaurants2"
    private const val restaurantDetailsTable = "restaurant_details"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val supabaseClient = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL").orEmpty().ifBlank { defaultSupabaseUrl },
        supabaseKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
            .orEmpty()
            .ifBlank { System.getenv("SUPABASE_KEY").orEmpty().ifBlank { defaultSupabaseKey } },
    ) {
        install(Postgrest)
    }

    suspend fun importRestaurant(request: RestaurantImportRequest): RestaurantImportResponse {
        val name = request.restaurantName.trim()
        val address = request.address.trim()
        val postCode = request.postCode.trim()

        if (name.isBlank()) {
            return RestaurantImportResponse(
                success = false,
                created = false,
                message = "Restaurant name is required."
            )
        }
        if (address.isBlank() && postCode.isBlank()) {
            return RestaurantImportResponse(
                success = false,
                created = false,
                message = "Address or post code is required to verify the restaurant."
            )
        }

        val googleApiKey = System.getenv("GOOGLE_MAPS_API_KEY").orEmpty()
        if (googleApiKey.isBlank()) {
            return RestaurantImportResponse(
                success = false,
                created = false,
                message = "GOOGLE_MAPS_API_KEY is not configured on the server."
            )
        }

        val query = listOf(name, address, postCode)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        val searchResponse = runCatching {
            httpClient.get("https://maps.googleapis.com/maps/api/place/textsearch/json") {
                parameter("query", query)
                parameter("key", googleApiKey)
            }.body<GoogleTextSearchResponse>()
        }.getOrElse { throwable ->
            return RestaurantImportResponse(
                success = false,
                created = false,
                message = throwable.message ?: "Unable to search Google Maps right now."
            )
        }

        val bestMatch = searchResponse.results
            .sortedWith(
                compareByDescending<GoogleTextSearchResult> { scorePlaceMatch(it, name, address, postCode) }
                    .thenBy { it.name.lowercase() }
            )
            .firstOrNull()
            ?: return RestaurantImportResponse(
                success = false,
                created = false,
                message = "We could not verify that restaurant. Please refine the address or post code."
            )

        val details = fetchPlaceDetails(bestMatch.placeId, googleApiKey)
        val restaurantId = "gm_${bestMatch.placeId}"

        val exists = runCatching {
            supabaseClient.from(restaurantsTable)
                .select(Columns.list("id")) {
                    filter { eq("id", restaurantId) }
                    limit(1)
                }
                .decodeSingleOrNull<JsonObject>() != null
        }.getOrDefault(false)

        if (exists) {
            return RestaurantImportResponse(
                success = true,
                created = false,
                message = "${details?.name ?: bestMatch.name} already exists in BiteShare.",
                restaurantId = restaurantId,
                restaurantName = details?.name ?: bestMatch.name
            )
        }

        val normalizedCategory = mapCategory(details?.types ?: bestMatch.types)
        val priceRange = mapPrice(details?.priceLevel)
        val city = details?.city
            ?: extractCity(bestMatch.formattedAddress)
            ?: "Unknown"
        val canonicalName = details?.name ?: bestMatch.name
        val canonicalAddress = details?.formattedAddress ?: bestMatch.formattedAddress
        val rating = details?.rating ?: bestMatch.rating ?: 0.0
        val isOpenNow = details?.openNow ?: true
        val attributes = buildAttributes(details?.types ?: bestMatch.types, normalizedCategory)
        val mapsUrl = details?.googleMapsUrl
            ?: "https://www.google.com/maps/search/?api=1&query=${canonicalName.replace(" ", "+")}"

        runCatching {
            supabaseClient.postgrest[restaurantsTable].upsert(
                body = JsonArray(
                    listOf(
                        buildJsonObject {
                            put("id", restaurantId)
                            put("name", canonicalName)
                            put("category", normalizedCategory)
                            put("price", priceRange)
                            put("eta", "20-30 min")
                            put("rating", rating)
                            put("location", city)
                            put("is_open_now", isOpenNow)
                        }
                    )
                )
            ) {
                onConflict = "id"
            }

            supabaseClient.postgrest[restaurantDetailsTable].upsert(
                body = JsonArray(
                    listOf(
                        buildJsonObject {
                            put("restaurant_id", restaurantId)
                            put("name", canonicalName)
                            put(
                                "description",
                                "$canonicalName was imported from Google Maps and verified using the provided address information."
                            )
                            put("rating", rating)
                            put("price_range", priceRange)
                            put("images", buildJsonArray { })
                            put("attributes", buildJsonArray {
                                attributes.forEach { add(JsonPrimitive(it)) }
                            })
                            put("google_maps_link", mapsUrl)
                            details?.website?.takeIf { it.isNotBlank() }?.let { put("restaurant_website", it) }
                            put("city", city)
                            put("address", canonicalAddress)
                            put("distance", "Unknown")
                        }
                    )
                )
            ) {
                onConflict = "restaurant_id"
            }
        }.getOrElse { throwable ->
            return RestaurantImportResponse(
                success = false,
                created = false,
                message = throwable.message ?: "Unable to save the restaurant to Supabase."
            )
        }

        return RestaurantImportResponse(
            success = true,
            created = true,
            message = "$canonicalName was added successfully.",
            restaurantId = restaurantId,
            restaurantName = canonicalName
        )
    }

    private suspend fun fetchPlaceDetails(placeId: String, googleApiKey: String): GooglePlaceDetails? {
        if (placeId.isBlank()) return null
        val response = runCatching {
            httpClient.get("https://maps.googleapis.com/maps/api/place/details/json") {
                parameter("place_id", placeId)
                parameter(
                    "fields",
                    "name,formatted_address,address_component,website,url,rating,price_level,opening_hours,types"
                )
                parameter("key", googleApiKey)
            }.body<GooglePlaceDetailsResponse>()
        }.getOrNull() ?: return null

        val result = response.result ?: return null
        return GooglePlaceDetails(
            name = result.name,
            formattedAddress = result.formattedAddress,
            website = result.website.orEmpty(),
            googleMapsUrl = result.url.orEmpty(),
            rating = result.rating,
            priceLevel = result.priceLevel,
            openNow = result.openingHours?.openNow,
            city = result.addressComponents
                .firstOrNull { component ->
                    "locality" in component.types || "postal_town" in component.types
                }
                ?.longName,
            types = result.types
        )
    }

    private fun scorePlaceMatch(
        place: GoogleTextSearchResult,
        restaurantName: String,
        address: String,
        postCode: String,
    ): Int {
        val normalizedName = normalize(restaurantName)
        val normalizedAddress = normalize(address)
        val normalizedPostCode = normalize(postCode)
        val placeName = normalize(place.name)
        val placeAddress = normalize(place.formattedAddress)

        var score = 0
        when {
            placeName == normalizedName -> score += 100
            placeName.startsWith(normalizedName) -> score += 70
            placeName.contains(normalizedName) -> score += 40
        }
        if (normalizedAddress.isNotBlank()) {
            when {
                placeAddress.contains(normalizedAddress) -> score += 40
                normalizedAddress.contains(placeAddress) -> score += 20
            }
        }
        if (normalizedPostCode.isNotBlank() && placeAddress.contains(normalizedPostCode)) {
            score += 30
        }
        if (place.businessStatus == "OPERATIONAL") {
            score += 10
        }
        return score
    }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun mapCategory(types: List<String>): String {
        val normalized = types.map { it.lowercase() }
        return when {
            "pizza_restaurant" in normalized -> "Pizza"
            "cafe" in normalized || "coffee_shop" in normalized -> "Coffee"
            "bar" in normalized || "bubble_tea_store" in normalized -> "Drink"
            "meal_delivery" in normalized || "fast_food_restaurant" in normalized -> "Fast Food"
            "sushi_restaurant" in normalized -> "Sushi"
            "italian_restaurant" in normalized -> "Italian"
            "indian_restaurant" in normalized -> "Indian"
            "middle_eastern_restaurant" in normalized -> "Middle Eastern"
            "restaurant" in normalized -> "Restaurant"
            else -> "Other"
        }
    }

    private fun mapPrice(priceLevel: Int?): String =
        when (priceLevel ?: 2) {
            0, 1 -> "$"
            2 -> "$$"
            3 -> "$$$"
            else -> "$$$$"
        }

    private fun buildAttributes(types: List<String>, category: String): List<String> {
        val cleaned = types
            .map { type -> type.split("_").joinToString(" ") { token -> token.replaceFirstChar(Char::uppercase) } }
            .filterNot { it.equals("Point Of Interest", ignoreCase = true) || it.equals("Establishment", ignoreCase = true) }
            .distinct()
            .take(4)
            .toMutableList()
        if (category !in cleaned) cleaned.add(0, category)
        if ("Google Verified" !in cleaned) cleaned.add("Google Verified")
        return cleaned.distinct()
    }

    private fun extractCity(formattedAddress: String): String? {
        if (formattedAddress.isBlank()) return null
        return formattedAddress
            .split(",")
            .map { it.trim() }
            .firstOrNull { part -> part.isNotBlank() && !part.any(Char::isDigit) }
    }
}

@Serializable
private data class GoogleTextSearchResponse(
    val results: List<GoogleTextSearchResult> = emptyList(),
)

@Serializable
private data class GoogleTextSearchResult(
    val name: String,
    @SerialName("formatted_address") val formattedAddress: String = "",
    @SerialName("place_id") val placeId: String,
    val rating: Double? = null,
    val types: List<String> = emptyList(),
    @SerialName("business_status") val businessStatus: String? = null,
)

@Serializable
private data class GooglePlaceDetailsResponse(
    val result: GooglePlaceDetailsResult? = null,
)

@Serializable
private data class GooglePlaceDetailsResult(
    val name: String,
    @SerialName("formatted_address") val formattedAddress: String = "",
    val website: String? = null,
    val url: String? = null,
    val rating: Double? = null,
    @SerialName("price_level") val priceLevel: Int? = null,
    @SerialName("opening_hours") val openingHours: GoogleOpeningHours? = null,
    @SerialName("address_components") val addressComponents: List<GoogleAddressComponent> = emptyList(),
    val types: List<String> = emptyList(),
)

@Serializable
private data class GoogleOpeningHours(
    @SerialName("open_now") val openNow: Boolean? = null,
)

@Serializable
private data class GoogleAddressComponent(
    @SerialName("long_name") val longName: String = "",
    val types: List<String> = emptyList(),
)

private data class GooglePlaceDetails(
    val name: String,
    val formattedAddress: String,
    val website: String,
    val googleMapsUrl: String,
    val rating: Double?,
    val priceLevel: Int?,
    val openNow: Boolean?,
    val city: String?,
    val types: List<String>,
)
