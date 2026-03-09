package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class CryptoPriceDto(
    val symbol: String,
    val name: String,
    val price: Double,
    val change24h: Double,
    val volume24h: Double,
    val marketCap: Double
)

@Serializable
data class CryptoWalletDto(
    val id: String,
    val customerId: String,
    val symbol: String,
    val address: String,
    val balance: Double,
    val fiatEquivalent: Double,
    val updatedAt: String
)

@Serializable
data class CryptoTransactionDto(
    val id: String,
    val walletId: String,
    val type: String,
    val amount: Double,
    val fiatAmount: Double,
    val fee: Double,
    val status: String,
    val txHash: String?,
    val createdAt: String
)

@Serializable
data class BuyCryptoRequest(
    val customerId: String,
    val accountId: String,
    val symbol: String,
    val fiatAmount: Double
)

@Serializable
data class SellCryptoRequest(
    val customerId: String,
    val walletId: String,
    val accountId: String,
    val cryptoAmount: Double
)

class CryptoWalletRepository(private val client: HttpClient, private val baseUrl: String) {

    suspend fun getPrices(): Result<List<CryptoPriceDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/crypto/prices")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<CryptoPriceDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get prices"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWallets(customerId: String): Result<List<CryptoWalletDto>> {
        return try {
            val response = client.get("$baseUrl/api/v1/crypto/wallets/$customerId")
            if (response.status.isSuccess()) {
                Result.success(response.body<List<CryptoWalletDto>>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to get wallets"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buyCrypto(request: BuyCryptoRequest): Result<CryptoTransactionDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/crypto/buy") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<CryptoTransactionDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to buy crypto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sellCrypto(request: SellCryptoRequest): Result<CryptoTransactionDto> {
        return try {
            val response = client.post("$baseUrl/api/v1/crypto/sell") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body<CryptoTransactionDto>())
            } else {
                val errorBody = response.body<Map<String, String>>()
                Result.failure(Exception(errorBody["error"] ?: "Failed to sell crypto"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
