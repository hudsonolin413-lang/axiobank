package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dals.project.model.*

class SplitBillRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    private val _splitBills = MutableStateFlow<List<SplitBill>>(emptyList())
    val splitBills: StateFlow<List<SplitBill>> = _splitBills.asStateFlow()

    private val _summary = MutableStateFlow<SplitBillSummary?>(null)
    val summary: StateFlow<SplitBillSummary?> = _summary.asStateFlow()

    /**
     * Get all split bills for a customer
     */
    suspend fun getSplitBills(customerId: String): Result<List<SplitBill>> {
        return try {
            val url = "$baseUrl/split-bills/customer/$customerId"
            println("🌐 Fetching split bills from: $url")
            val response = httpClient.get(url)
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val responseData = response.body<SplitBillListResponse>()
                if (responseData.success) {
                    _splitBills.value = responseData.splitBills
                    Result.success(responseData.splitBills)
                } else {
                    Result.failure(Exception(responseData.message))
                }
            } else {
                val errorMsg = "Failed to fetch split bills from $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception fetching split bills: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error fetching split bills: ${e.message}", e))
        }
    }

    /**
     * Get split bill summary
     */
    suspend fun getSummary(customerId: String): Result<SplitBillSummary> {
        return try {
            val url = "$baseUrl/split-bills/customer/$customerId/summary"
            println("🌐 Fetching split bill summary from: $url")
            val response = httpClient.get(url)
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val summary = response.body<SplitBillSummary>()
                _summary.value = summary
                Result.success(summary)
            } else {
                val errorMsg = "Failed to fetch summary from $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception fetching summary: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error fetching summary: ${e.message}", e))
        }
    }

    /**
     * Get split bill by ID
     */
    suspend fun getSplitBillById(splitBillId: String): Result<SplitBill> {
        return try {
            val url = "$baseUrl/split-bills/$splitBillId"
            println("🌐 Fetching split bill from: $url")
            val response = httpClient.get(url)
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val responseData = response.body<SplitBillResponse>()
                if (responseData.success && responseData.splitBill != null) {
                    Result.success(responseData.splitBill)
                } else {
                    Result.failure(Exception(responseData.message))
                }
            } else {
                val errorMsg = "Failed to fetch split bill from $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception fetching split bill: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error fetching split bill: ${e.message}", e))
        }
    }

    /**
     * Create a new split bill
     */
    suspend fun createSplitBill(request: CreateSplitBillRequest): Result<SplitBill> {
        return try {
            val url = "$baseUrl/split-bills"
            println("🌐 Creating split bill at: $url")
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.Created) {
                val responseData = response.body<SplitBillResponse>()
                if (responseData.success && responseData.splitBill != null) {
                    // Refresh the list
                    getSplitBills(request.creatorId)
                    Result.success(responseData.splitBill)
                } else {
                    Result.failure(Exception(responseData.message))
                }
            } else {
                val errorMsg = "Failed to create split bill at $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception creating split bill: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error creating split bill: ${e.message}", e))
        }
    }

    /**
     * Pay a split bill
     */
    suspend fun paySplitBill(request: PaySplitBillRequest): Result<SplitBill> {
        return try {
            val url = "$baseUrl/split-bills/pay"
            println("🌐 Paying split bill at: $url")
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val responseData = response.body<SplitBillResponse>()
                if (responseData.success && responseData.splitBill != null) {
                    // Update the split bill in the list
                    _splitBills.value = _splitBills.value.map {
                        if (it.id == responseData.splitBill.id) responseData.splitBill else it
                    }
                    Result.success(responseData.splitBill)
                } else {
                    Result.failure(Exception(responseData.message))
                }
            } else {
                val errorMsg = "Failed to pay split bill at $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception paying split bill: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error paying split bill: ${e.message}", e))
        }
    }

    /**
     * Cancel a split bill
     */
    suspend fun cancelSplitBill(splitBillId: String): Result<SplitBill> {
        return try {
            val url = "$baseUrl/split-bills/$splitBillId/cancel"
            println("🌐 Cancelling split bill at: $url")
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
            }
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val responseData = response.body<SplitBillResponse>()
                if (responseData.success && responseData.splitBill != null) {
                    // Update the split bill in the list
                    _splitBills.value = _splitBills.value.map {
                        if (it.id == responseData.splitBill.id) responseData.splitBill else it
                    }
                    Result.success(responseData.splitBill)
                } else {
                    Result.failure(Exception(responseData.message))
                }
            } else {
                val errorMsg = "Failed to cancel split bill at $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception cancelling split bill: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error cancelling split bill: ${e.message}", e))
        }
    }

    /**
     * Send reminder to participant
     */
    suspend fun sendReminder(participantId: String): Result<Boolean> {
        return try {
            val url = "$baseUrl/split-bills/participant/$participantId/remind"
            println("🌐 Sending reminder at: $url")
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
            }
            println("📡 Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                Result.success(true)
            } else {
                val errorMsg = "Failed to send reminder at $url - Status: ${response.status}"
                println("❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            println("❌ Exception sending reminder: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Error sending reminder: ${e.message}", e))
        }
    }
}
