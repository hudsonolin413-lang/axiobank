package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.API_BASE_URL
import org.dals.project.repository.ApiResponse

@Serializable
data class ReferralData(
    val id: String,
    val referrerId: String,
    val referralCode: String,
    val referredEmail: String? = null,
    val status: String,
    val rewardAmount: String,
    val createdAt: String,
    val referredName: String? = null
)

@Serializable
data class InviteFriendRequest(
    val referrerId: String,
    val friendEmail: String,
    val referralCode: String
)

class ReferralRepository(
    private val authRepository: AuthRepository
) {
    private val baseUrl = API_BASE_URL

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
    }

    private val _referrals = MutableStateFlow<List<ReferralData>>(emptyList())
    val referrals: StateFlow<List<ReferralData>> = _referrals.asStateFlow()

    suspend fun fetchReferrals(customerId: String): Result<List<ReferralData>> {
        return try {
            val response = httpClient.get("$baseUrl/referrals/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val data = response.body<List<ReferralData>>()
                _referrals.value = data
                Result.success(data)
            } else {
                Result.failure(Exception("Failed to fetch referrals: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inviteFriend(friendEmail: String, referralCode: String): Result<String> {
        val currentUser = authRepository.currentUser.value ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val request = InviteFriendRequest(
                referrerId = currentUser.id,
                friendEmail = friendEmail,
                referralCode = referralCode
            )

            val response = httpClient.post("$baseUrl/referrals/invite") {
                contentType(ContentType.Application.Json)
                setBody(request)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<ApiResponse<String>>()
                if (apiResponse.success) {
                    // Refresh referrals list
                    fetchReferrals(currentUser.id)
                    Result.success<String>(apiResponse.message ?: "Invitation sent successfully")
                } else {
                    Result.failure<String>(Exception(apiResponse.message ?: "Failed to send invitation"))
                }
            } else {
                val apiResponse = try { response.body<ApiResponse<String>>() } catch (e: Exception) { null }
                Result.failure<String>(Exception(apiResponse?.message ?: "Failed to send invitation: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure<String>(e)
        }
    }
}
