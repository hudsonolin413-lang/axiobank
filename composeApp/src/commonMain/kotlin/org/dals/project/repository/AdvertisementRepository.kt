package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.dals.project.API_BASE_URL
import org.dals.project.model.Advertisement
import org.dals.project.model.AdvertisementsResponse

class AdvertisementRepository {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 120000
        }
    }

    private val baseUrl = API_BASE_URL

    private val _advertisements = MutableStateFlow<List<Advertisement>>(emptyList())
    val advertisements: StateFlow<List<Advertisement>> = _advertisements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun fetchAdvertisements() {
        _isLoading.value = true
        _error.value = null

        try {
            println("📢 Fetching advertisements from server: $baseUrl/admin/advertisements")

            val response = httpClient.get("$baseUrl/admin/advertisements") {
                contentType(ContentType.Application.Json)
                parameter("includeInactive", false)
            }

            if (response.status == HttpStatusCode.OK) {
                val adsResponse = response.body<AdvertisementsResponse>()

                if (adsResponse.success && !adsResponse.data.isNullOrEmpty()) {
                    val activeAds = adsResponse.data.filter { it.isActive }
                        .sortedBy { it.displayOrder }

                    _advertisements.value = activeAds
                    println("✅ Successfully loaded ${activeAds.size} advertisements")
                } else {
                    _advertisements.value = emptyList()
                    println("ℹ️ No advertisements available")
                }
            } else if (response.status == HttpStatusCode.NotFound) {
                _advertisements.value = emptyList()
                println("ℹ️ Advertisement endpoint not yet available (404). This is normal if the feature hasn't been deployed to production yet.")
            } else {
                _error.value = "Server error: ${response.status}"
                _advertisements.value = emptyList()
                println("❌ Error fetching advertisements: ${response.status}")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            _error.value = errorMsg
            _advertisements.value = emptyList()

            if (errorMsg.contains("404") || errorMsg.contains("Not Found")) {
                println("ℹ️ Advertisement endpoint not available. This is normal if the feature hasn't been deployed to production yet.")
            } else {
                println("❌ Error fetching advertisements: $errorMsg")
            }
        } finally {
            _isLoading.value = false
        }
    }
}
