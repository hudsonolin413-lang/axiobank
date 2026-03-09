package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class TaxReportRecord(
    val id: String,
    val customerId: String,
    val year: Int,
    val reportType: String,
    val totalIncome: String,
    val totalExpenses: String,
    val interestEarned: String,
    val interestPaid: String,
    val capitalGains: String,
    val dividends: String,
    val taxWithheld: String,
    val documentUrl: String?,
    val status: String,
    val generatedAt: String,
    val createdAt: String
)

@Serializable
data class GenerateTaxReportRequest(
    val customerId: String,
    val year: Int,
    val reportType: String
)

@Serializable
data class TaxSummary(
    val year: Int,
    val totalIncome: String,
    val totalExpenses: String,
    val netIncome: String,
    val interestEarned: String,
    val interestPaid: String,
    val capitalGains: String,
    val dividends: String,
    val taxWithheld: String,
    val estimatedTax: String,
    val reportsGenerated: Int
)

@Serializable
data class AvailableYearsResponse(
    val years: List<Int>
)

class TaxReportRepository(private val httpClient: HttpClient, private val apiBaseUrl: String) {
    private val baseUrl = "$apiBaseUrl/tax-reports"

    suspend fun getAllReports(customerId: String): Result<List<TaxReportRecord>> = try {
        val url = "$baseUrl/$customerId"
        println("🌐 Fetching tax reports from: $url")
        val response = httpClient.get(url)
        println("📡 Response status: ${response.status}")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<TaxReportRecord>>())
        } else {
            val errorMsg = "Failed to fetch tax reports from $url - Status: ${response.status}"
            println("❌ $errorMsg")
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        println("❌ Exception fetching tax reports: ${e.message}")
        e.printStackTrace()
        Result.failure(Exception("Error fetching tax reports: ${e.message}", e))
    }

    suspend fun getReportsByYear(customerId: String, year: Int): Result<List<TaxReportRecord>> = try {
        val response = httpClient.get("$baseUrl/$customerId/year/$year")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<List<TaxReportRecord>>())
        } else {
            Result.failure(Exception("Failed to fetch reports: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTaxSummary(customerId: String, year: Int): Result<TaxSummary> = try {
        val response = httpClient.get("$baseUrl/$customerId/summary/$year")
        if (response.status == HttpStatusCode.OK) {
            Result.success(response.body<TaxSummary>())
        } else {
            Result.failure(Exception("Failed to fetch summary: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAvailableYears(customerId: String): Result<List<Int>> = try {
        val url = "$baseUrl/$customerId/available-years"
        println("🌐 Fetching available years from: $url")
        val response = httpClient.get(url)
        println("📡 Response status: ${response.status}")
        if (response.status == HttpStatusCode.OK) {
            val yearsResponse = response.body<AvailableYearsResponse>()
            Result.success(yearsResponse.years)
        } else {
            val errorMsg = "Failed to fetch available years from $url - Status: ${response.status}"
            println("❌ $errorMsg")
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        println("❌ Exception fetching available years: ${e.message}")
        e.printStackTrace()
        Result.failure(Exception("Error fetching available years: ${e.message}", e))
    }

    suspend fun generateReport(request: GenerateTaxReportRequest): Result<TaxReportRecord> = try {
        val response = httpClient.post("$baseUrl/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Created) {
            Result.success(response.body<TaxReportRecord>())
        } else {
            Result.failure(Exception("Failed to generate report: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteReport(reportId: String): Result<Boolean> = try {
        val response = httpClient.delete("$baseUrl/$reportId")
        if (response.status == HttpStatusCode.OK) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to delete report: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
