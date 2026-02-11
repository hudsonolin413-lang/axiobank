package org.dals.project.repository

import org.dals.project.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LoansResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerLoanData>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10
)

@Serializable
data class LoanPaymentResponse(
    val success: Boolean,
    val message: String,
    val data: LoanPaymentData? = null,
    val error: String? = null
)

@Serializable
data class LoanPaymentData(
    val id: String,
    val loanId: String,
    val paymentDate: String,
    val amount: String,
    val principalAmount: String,
    val interestAmount: String,
    val feeAmount: String,
    val balanceAfter: String,
    val paymentMethod: String,
    val reference: String? = null,
    val processedBy: String,
    val createdAt: String
)

@Serializable
data class ServerLoanData(
    val id: String,
    val customerId: String,
    val accountId: String? = null,
    val applicationId: String? = null,
    val loanType: String,
    val originalAmount: String,
    val currentBalance: String,
    val interestRate: String,
    val termMonths: Int,
    val monthlyPayment: String,
    val paymentFrequency: String? = null,
    val status: String,
    val originationDate: String? = null,
    val maturityDate: String? = null,
    val nextPaymentDate: String? = null,
    val totalInterestPaid: String? = null,
    val totalPrincipalPaid: String? = null,
    val latePaymentFees: String? = null,
    val loanOfficerId: String? = null,
    val branchId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class LoanApplicationsResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerLoanApplicationData>? = null,
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 10
)

@Serializable
data class ServerLoanApplicationData(
    val id: String,
    val customerId: String,
    val loanType: String,
    val requestedAmount: Double,
    val purpose: String,
    val status: String,
    val applicationDate: String,
    val reviewedDate: String? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null,
    val annualIncome: Double? = null,
    val employmentHistory: String? = null
)

@Serializable
data class CreateLoanApplicationRequest(
    val customerId: String,
    val loanType: String,
    val requestedAmount: String,
    val purpose: String,
    val collateralDescription: String? = null,
    val collateralValue: String? = null,
    val creditScore: Int? = null,
    val annualIncome: String? = null,
    val employmentHistory: String? = null
)

@Serializable
data class LoanApplicationResponse(
    val success: Boolean,
    val message: String,
    val data: ServerLoanApplicationData? = null,
    val error: String? = null
)

class LoanRepository(
    private val authRepository: AuthRepository
) {
    private val baseUrl = "http://localhost:8081/api/v1"

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    private val _loanApplications = MutableStateFlow<List<LoanApplication>>(emptyList())
    val loanApplications: StateFlow<List<LoanApplication>> = _loanApplications.asStateFlow()

    private val _activeLoans = MutableStateFlow<List<Loan>>(emptyList())
    val activeLoans: StateFlow<List<Loan>> = _activeLoans.asStateFlow()

    private val _loanPayments = MutableStateFlow<List<LoanPayment>>(emptyList())
    val loanPayments: StateFlow<List<LoanPayment>> = _loanPayments.asStateFlow()

    init {
        // Listen for user changes and fetch real data when user logs in
        CoroutineScope(Dispatchers.Default).launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
//                    println("💰 User logged in, fetching loan data for customer: ${user.id}")
                    fetchLoansFromServer(user.id)
                    fetchLoanApplicationsFromServer(user.id)
                } else {
//                    println("💰 User logged out, clearing loan data")
                    clearData()
                }
            }
        }
    }

    private suspend fun fetchLoansFromServer(customerId: String) {
        try {
//            println("🌐 Fetching loans for customer: $customerId")

            val response = httpClient.get("$baseUrl/loans/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val loansResponse = response.body<LoansResponse>()

                if (loansResponse.success && !loansResponse.data.isNullOrEmpty()) {
                    val serverLoans = loansResponse.data

                    // Convert server loans to app model
                    val appLoans = serverLoans.map { serverLoan ->
                        Loan(
                            id = serverLoan.id,
                            applicationId = serverLoan.applicationId ?: serverLoan.accountId ?: "",
                            borrowerId = serverLoan.customerId,
                            lenderId = serverLoan.loanOfficerId,
                            amount = serverLoan.originalAmount.toDoubleOrNull() ?: 0.0,
                            interestRate = serverLoan.interestRate.toDoubleOrNull() ?: 0.0,
                            termInMonths = serverLoan.termMonths,
                            monthlyPayment = serverLoan.monthlyPayment.toDoubleOrNull() ?: 0.0,
                            remainingBalance = serverLoan.currentBalance.toDoubleOrNull() ?: 0.0,
                            status = parseLoanStatus(serverLoan.status),
                            createdDate = serverLoan.originationDate ?: serverLoan.createdAt,
                            dueDate = serverLoan.maturityDate ?: "",
                            lastPaymentDate = serverLoan.nextPaymentDate,
                            collateral = Collateral(
                                type = CollateralType.NONE,
                                description = "",
                                value = 0.0
                            ),
                            smartContractAddress = null
                        )
                    }

                    _activeLoans.value = appLoans
//                    println("✅ Fetched ${appLoans.size} loans")
                } else {
//                    println("ℹ️ No loans found for customer")
                    _activeLoans.value = emptyList()
                }
            } else {
//                println("❌ Failed to fetch loans: ${response.status}")
                _activeLoans.value = emptyList()
            }
        } catch (e: Exception) {
//            println("❌ Error fetching loans: ${e.message}")
            e.printStackTrace()
            _activeLoans.value = emptyList()
        }
    }

    private suspend fun fetchLoanApplicationsFromServer(customerId: String) {
        try {
//            println("🌐 Fetching loan applications for customer: $customerId")

            val response = httpClient.get("$baseUrl/loans/applications/customer/$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val applicationsResponse = response.body<LoanApplicationsResponse>()

                if (applicationsResponse.success && !applicationsResponse.data.isNullOrEmpty()) {
                    val serverApplications = applicationsResponse.data

                    // Convert server applications to app model
                    val appApplications = serverApplications.map { serverApp ->
                        LoanApplication(
                            id = serverApp.id,
                            borrowerId = serverApp.customerId,
                            amount = serverApp.requestedAmount,
                            purpose = parseLoanPurpose(serverApp.loanType),
                            termInMonths = 12, // Default, can be enhanced
                            interestRate = 8.5, // Default, can be enhanced
                            collateralType = CollateralType.NONE, // Default
                            collateralValue = 0.0,
                            status = parseLoanStatus(serverApp.status),
                            applicationDate = serverApp.applicationDate,
                            description = serverApp.purpose,
                            employmentInfo = null,
                            monthlyIncome = serverApp.annualIncome?.div(12) ?: 0.0,
                            existingDebts = 0.0
                        )
                    }

                    _loanApplications.value = appApplications
//                    println("✅ Fetched ${appApplications.size} loan applications")
                } else {
//                    println("ℹ️ No loan applications found for customer")
                    _loanApplications.value = emptyList()
                }
            } else {
//                println("❌ Failed to fetch loan applications: ${response.status}")
                _loanApplications.value = emptyList()
            }
        } catch (e: Exception) {
//            println("❌ Error fetching loan applications: ${e.message}")
            e.printStackTrace()
            _loanApplications.value = emptyList()
        }
    }

    private fun clearData() {
        _activeLoans.value = emptyList()
        _loanApplications.value = emptyList()
        _loanPayments.value = emptyList()
    }

    private fun parseLoanStatus(status: String): LoanStatus {
        return when (status.uppercase()) {
            "APPROVED" -> LoanStatus.APPROVED
            "PENDING" -> LoanStatus.PENDING
            "REJECTED" -> LoanStatus.REJECTED
            "ACTIVE" -> LoanStatus.ACTIVE
            "PAID_OFF" -> LoanStatus.PAID_OFF
            "DEFAULTED" -> LoanStatus.DEFAULTED
            else -> LoanStatus.PENDING
        }
    }

    private fun parseLoanPurpose(type: String): LoanPurpose {
        return when (type.uppercase()) {
            "BUSINESS", "BUSINESS_LOAN" -> LoanPurpose.BUSINESS
            "EDUCATION", "STUDENT_LOAN" -> LoanPurpose.EDUCATION
            "HOME", "HOME_LOAN", "MORTGAGE" -> LoanPurpose.HOME
            "PERSONAL", "PERSONAL_LOAN" -> LoanPurpose.PERSONAL
            "AUTO", "AUTO_LOAN" -> LoanPurpose.AUTO
            else -> LoanPurpose.PERSONAL
        }
    }

    private fun getCurrentDateString(): String {
        return java.time.Instant.now().toString()
    }

    fun getLoanById(id: String): Loan? {
        return _activeLoans.value.find { it.id == id }
    }

    fun getLoanApplicationById(id: String): LoanApplication? {
        return _loanApplications.value.find { it.id == id }
    }

    suspend fun submitLoanApplication(application: LoanApplication): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            println("📤 Submitting loan application to server for customer: ${currentUser.id}")

            // Map loan purpose to server's LoanType enum
            val serverLoanType = when (application.purpose) {
                LoanPurpose.HOME, LoanPurpose.HOME_IMPROVEMENT -> "HOME_LOAN"
                LoanPurpose.AUTO, LoanPurpose.VEHICLE -> "AUTO_LOAN"
                LoanPurpose.BUSINESS -> "BUSINESS_LOAN"
                LoanPurpose.EDUCATION -> "STUDENT_LOAN"
                LoanPurpose.PERSONAL, LoanPurpose.MEDICAL, 
                LoanPurpose.DEBT_CONSOLIDATION -> "PERSONAL_LOAN"
            }

            val request = CreateLoanApplicationRequest(
                customerId = currentUser.id,
                loanType = serverLoanType,
                requestedAmount = application.amount.toString(),
                purpose = application.purpose.name,
                collateralDescription = application.collateralType.name,
                collateralValue = application.collateralValue.toString(),
                creditScore = currentUser.creditScore,
                annualIncome = application.monthlyIncome.toString(),
                employmentHistory = application.employmentInfo?.let { 
                    "${it.jobTitle} at ${it.employerName} (${it.yearsOfExperience} years)" 
                }
            )

            println("📋 Request payload: $request")

            val response = httpClient.post("$baseUrl/loan-applications") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("📥 Server response status: ${response.status}")

            if (response.status.isSuccess()) {
                val serverResponse = response.body<LoanApplicationResponse>()
                if (serverResponse.success && serverResponse.data != null) {
                    println("✅ Loan application submitted successfully: ${serverResponse.data.id}")
                    
                    // Update local state
                    val newApplication = application.copy(
                        id = serverResponse.data.id,
                        borrowerId = currentUser.id,
                        status = LoanStatus.SUBMITTED
                    )
                    val currentApplications = _loanApplications.value.toMutableList()
                    currentApplications.add(newApplication)
                    _loanApplications.value = currentApplications
                    
                    Result.success(serverResponse.data.id)
                } else {
                    println("❌ Server returned error: ${serverResponse.message}")
                    Result.failure(Exception(serverResponse.message))
                }
            } else {
                // Try to get error details from response body
                try {
                    val errorBody = response.body<String>()
                    println("❌ HTTP error ${response.status}: $errorBody")
                    Result.failure(Exception("Failed to submit loan application: ${response.status} - $errorBody"))
                } catch (e: Exception) {
                    println("❌ HTTP error: ${response.status}")
                    Result.failure(Exception("Failed to submit loan application: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception submitting loan application: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun makePayment(loanId: String, amount: Double): Result<String> {
        return try {
            println("💳 Making loan payment: Loan ID=$loanId, Amount=$amount")

            // Prepare payment request
            val requestBody = mapOf(
                "loanId" to loanId,
                "amount" to amount.toString(),
                "paymentMethod" to "ACCOUNT_DEBIT"
            )

            // Make API call to server
            val response = httpClient.post("$baseUrl/api/v1/loans/$loanId/payments") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            println("✅ Payment response status: ${response.status}")

            if (response.status.value in 200..299) {
                val paymentResponse = response.body<LoanPaymentResponse>()
                println("✅ Payment successful: ${paymentResponse.message}")
                println("   Payment ID: ${paymentResponse.data?.id}")
                println("   Balance after: ${paymentResponse.data?.balanceAfter}")

                // Refresh loan data after successful payment
                val customerId = _activeLoans.value.firstOrNull()?.borrowerId
                if (customerId != null) {
                    fetchLoansFromServer(customerId)
                }

                Result.success("Payment processed successfully. New balance: ${paymentResponse.data?.balanceAfter}")
            } else {
                val errorResponse = try {
                    response.body<LoanPaymentResponse>()
                } catch (e: Exception) {
                    null
                }
                val errorMsg = errorResponse?.error ?: errorResponse?.message ?: "Unknown error"
                println("❌ Payment failed: $errorMsg")
                Result.failure(Exception("Payment failed: $errorMsg"))
            }
        } catch (e: Exception) {
            println("❌ Exception making payment: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getActiveLoanById(id: String): Loan? {
        return _activeLoans.value.find { it.id == id }
    }

    fun getPaymentsByLoanId(loanId: String): List<LoanPayment> {
        return _loanPayments.value.filter { it.loanId == loanId }
    }

    fun cleanup() {
        httpClient.close()
    }
}