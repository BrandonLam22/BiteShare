package org.example.biteshare.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.biteshare.RestaurantImportRequest
import org.example.biteshare.RestaurantImportResponse
import org.example.biteshare.SERVER_PORT

object RestaurantImportApi {
    // For Android emulator this may need changing to 10.0.2.2.
    private const val SERVER_BASE_URL = "http://localhost:$SERVER_PORT"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
    }

    suspend fun importRestaurant(request: RestaurantImportRequest): RestaurantImportResponse {
        return client.post("$SERVER_BASE_URL/api/restaurants/import") {
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
            setBody(request)
        }.body()
    }
}
