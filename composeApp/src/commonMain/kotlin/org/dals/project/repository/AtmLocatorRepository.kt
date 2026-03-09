package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class AtmLocationDto(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val status: String,
    val distance: Double? = null,
    val features: List<String> = emptyList()
)

class AtmLocatorRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun findNearbyAtms(
        latitude: Double,
        longitude: Double,
        radiusMiles: Double = 10.0
    ): Result<List<AtmLocationDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/atm/nearby") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("radius", radiusMiles)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<List<AtmLocationDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to find nearby ATMs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAtms(query: String): Result<List<AtmLocationDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/atm/search") {
                parameter("query", query)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<List<AtmLocationDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to search ATMs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
