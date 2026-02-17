package org.dals.project.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import org.dals.project.models.ApiResponse
import org.dals.project.models.CreateCustomerRequest
import org.dals.project.models.OpenAccountForNewCustomerRequest
import org.dals.project.models.KycProfileDto
import org.dals.project.models.KycDocumentDetailsDto
import org.dals.project.models.UpdateKycDocumentStatusRequest
import org.dals.project.models.UpdateKycProfileRequest
import org.dals.project.models.KycDashboardMetricsDto
import org.dals.project.models.PendingKycCustomerDto
import org.dals.project.models.PendingKycDocumentDto
import org.dals.project.models.CustomerCareListResponse
import org.dals.project.models.CreateEmployeeRequest
import org.dals.project.models.CreateLoanApplicationRequest
import org.dals.project.models.CreateTransactionRequest
import org.dals.project.models.CreateRoleRequest
import org.dals.project.models.ListResponse
import org.dals.project.models.TransactionDto
import org.dals.project.models.LoanDto
import org.dals.project.models.LoanApplicationDto
import org.dals.project.models.LoanStatusUpdateData
import org.dals.project.model.UpdateLoanApplicationStatusRequest
import org.dals.project.models.MpesaReversalRequest
import org.dals.project.models.MpesaReversalResponse
import org.dals.project.models.MpesaWithdrawalRequest
import org.dals.project.models.DrawerDto
import org.dals.project.models.AssignDrawerRequest
import org.dals.project.models.DrawerAssignmentDto
import org.dals.project.models.AdvertisementDto
import org.dals.project.models.CreateAdvertisementRequest
import org.dals.project.models.UpdateAdvertisementRequest
import org.dals.project.services.CreateWalletRequest
import org.dals.project.services.CreateWalletTransactionRequest
import org.dals.project.services.CreateWalletAllocationRequest
import org.dals.project.services.CreateWalletReconciliationRequest
import org.dals.project.models.CreateAccountRequest
import org.dals.project.services.*
import org.dals.project.models.CustomerCareResponse
import org.dals.project.models.MpesaWalletDepositRequest
import org.dals.project.routes.*
import org.dals.project.database.*
import org.dals.project.database.CustomerStatus
import org.dals.project.database.UserRole
import org.dals.project.database.AccountType
import org.dals.project.database.AccountStatus
import org.dals.project.utils.IdGenerator
import org.dals.project.database.TransactionType
import org.dals.project.database.TransactionStatus
import org.dals.project.database.CustomerType
import org.dals.project.database.KycDocumentType
import org.dals.project.database.KycDocumentStatus
import org.dals.project.database.DocumentPriority
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.dals.project.database.MasterWalletTransactionType
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import kotlinx.serialization.Serializable
import java.util.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

// ==== DATA MODELS FOR SERIALIZATION ====

@kotlinx.serialization.Serializable
data class AccountOpeningRequest(
    val customerId: String,
    val accountType: String,
    val initialDeposit: Double = 0.0,
    val branchId: String,
    val openedBy: String
)

@kotlinx.serialization.Serializable
data class AccountOpeningResult(
    val accountId: String,
    val accountNumber: String,
    val type: String,
    val customerId: String,
    val customerName: String,
    val balance: String,
    val status: String,
    val branchId: String,
    val branchName: String,
    val interestRate: String,
    val minimumBalance: String,
    val createdAt: String,
    val initialDeposit: Double
)

@kotlinx.serialization.Serializable
data class ComplaintResult(
    val id: String,
    val customerId: String,
    val complaintType: String,
    val description: String,
    val reportedBy: String,
    val status: String,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class KYCStatusUpdateResult(
    val documentId: String,
    val newStatus: String,
    val customerKycStatus: String,
    val accountCreated: Boolean
)

@kotlinx.serialization.Serializable
data class CustomerSearchResult(
    val id: String,
    val customerNumber: String,
    val type: String,
    val status: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val email: String,
    val phoneNumber: String,
    val branchId: String,
    val kycStatus: String,
    val riskLevel: String,
    val createdAt: String,
    val fullName: String
)

// Customer Care specific serializable response models
@kotlinx.serialization.Serializable
data class ServerAccountDataDto(
    val id: String,
    val accountNumber: String,
    val customerId: String,
    val type: String,
    val status: String,
    val balance: Double,
    val availableBalance: Double,
    val minimumBalance: Double,
    val interestRate: Double,
    val branchId: String,
    val openedDate: String,
    val closedDate: String? = null
)

// Standard responses moved to ServerModels.kt

@kotlinx.serialization.Serializable
data class AddressInfo(
    val street: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val country: String?
)

@kotlinx.serialization.Serializable
data class BusinessInfo(
    val businessName: String?,
    val businessType: String?,
    val taxId: String?,
    val businessLicenseNumber: String?
)

@kotlinx.serialization.Serializable
data class CustomerProfileData(
    val id: String,
    val customerNumber: String,
    val username: String,
    val type: String,
    val status: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val dateOfBirth: String?,
    val ssn: String?,
    val email: String,
    val phoneNumber: String,
    val alternatePhone: String?,
    val primaryAddress: AddressInfo?,
    val mailingAddress: AddressInfo?,
    val occupation: String?,
    val employer: String?,
    val annualIncome: String?,
    val creditScore: Int?,
    val branchId: String,
    val branchName: String,
    val accountManagerId: String?,
    val onboardedDate: String,
    val lastContactDate: String?,
    val lastLoginAt: String?,
    val riskLevel: String,
    val kycStatus: String,
    val businessInfo: BusinessInfo?,
    val createdAt: String,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
data class ServerCustomerProfileResponseDto(
    val customer: CustomerProfileData,
    val accounts: List<ServerAccountDataDto>,
    val serviceRequests: List<ServiceRequestDto> = emptyList(),
    val complaints: List<ComplaintResult> = emptyList(),
    val kycDocuments: List<KYCDocumentDto>,
    val complianceChecks: List<ComplianceCheckDto> = emptyList()
)

@kotlinx.serialization.Serializable
data class ServiceRequestDto(
    val id: String,
    val customerId: String,
    val requestType: String,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdBy: String,
    val assignedTo: String? = null,
    val completedBy: String? = null,
    val estimatedCompletionDate: String? = null,
    val actualCompletionDate: String? = null,
    val rejectionReason: String? = null,
    val approvalRequired: Boolean = false,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null
)

@kotlinx.serialization.Serializable
data class ComplianceCheckDto(
    val id: String,
    val customerId: String,
    val checkType: String,
    val status: String,
    val riskScore: Int,
    val riskLevel: String,
    val checkedBy: String,
    val findings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val nextReviewDate: String? = null,
    val notes: String? = null,
    val checkedAt: String
)

// Mpesa deposit request for classic (account-based)
@kotlinx.serialization.Serializable
data class MpesaDepositRequest(
    val phoneNumber: String,
    val accountNumber: String,
    val amount: Double,
    val description: String,
    val operatorId: String? = null
)

// Secure version for advanced mpesa deposit requests (with custom description)
@kotlinx.serialization.Serializable
data class SecureMpesaDepositRequest(
    val phoneNumber: String,
    val accountNumber: String,
    val amount: Double,
    val transactionDesc: String,
    val operatorId: String? = null
)

// For account linking
@kotlinx.serialization.Serializable
data class SecureAccountLinkingRequest(
    val phoneNumber: String,
    val accountNumber: String,
    val verificationType: String = "SMS"
)

@kotlinx.serialization.Serializable
data class AccountLinkingResponse(
    val success: Boolean,
    val message: String,
    val linkingId: String? = null,
    val verificationRequired: Boolean = false,
    val nextSteps: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class AccountVerificationDetail(
    val status: String,
    val kycStatus: String
)

@kotlinx.serialization.Serializable
data class KYCDetail(
    val status: String,
    val level: String
)

@kotlinx.serialization.Serializable
data class TransactionLimitsDetail(
    val dailyLimit: Double,
    val monthlyLimit: Double,
    val singleTransactionLimit: Double,
    val remainingDailyLimit: Double,
    val remainingMonthlyLimit: Double
)

@kotlinx.serialization.Serializable
data class ComplianceCheckResponse(
    val result: String
)

@kotlinx.serialization.Serializable
data class OwnershipVerificationResponse(
    val isOwner: Boolean
)

@kotlinx.serialization.Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class CustomerRegisterRequest(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String
)

@kotlinx.serialization.Serializable
data class CustomerLoginResponse(
    val success: Boolean,
    val message: String,
    val data: CustomerLoginData? = null,
    val token: String? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class CustomerLoginData(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val customerNumber: String,
    val status: String,
    val kycStatus: String,
    val creditScore: Int?,
    val createdAt: String,
    val lastLoginAt: String?,
    val totalBorrowed: String = "0.0",
    val totalRepaid: String = "0.0",
    val activeLoans: Int = 0
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserLoginData? = null,
    val sessionId: String? = null
)

@kotlinx.serialization.Serializable

data class UserLoginData(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val status: String,
    val branchId: String?,
    val employeeId: String?,
    val permissions: List<String>,
    val lastLoginAt: String?
)

@kotlinx.serialization.Serializable
data class BranchActivity(
    val id: String,
    val type: String,
    val description: String,
    val timestamp: String,
    val userId: String,
    val userName: String,
    val customerId: String,
    val amount: String
)

@kotlinx.serialization.Serializable
data class BranchActivityResponse(
    val success: Boolean,
    val message: String,
    val data: List<BranchActivity>
)

@kotlinx.serialization.Serializable
data class BranchMetrics(
    val totalCustomers: String,
    val totalAccounts: String,
    val totalDeposits: String,
    val totalLoans: String,
    val activeLoans: String,
    val pendingApplications: String,
    val completedApprovalsToday: String,
    val weeklyTransactions: String,
    val monthlyTransactions: String,
    val pendingApprovals: String,
    val activeAccounts: String,
    val todayTransactions: String
)

@kotlinx.serialization.Serializable
data class BranchMetricsResponse(
    val success: Boolean,
    val message: String,
    val data: BranchMetrics
)

@kotlinx.serialization.Serializable
data class KYCDocumentDto(
    val id: String,
    val customerId: String,
    val customerName: String? = null,
    val documentType: String,
    val documentNumber: String,
    val fileName: String,
    val filePath: String,
    val uploadedAt: String,
    val uploadedBy: String,
    val status: String,
    val verifiedAt: String? = null,
    val verifiedBy: String? = null,
    val expiryDate: String? = null,
    val rejectionReason: String? = null,
    val notes: String? = null
)

@kotlinx.serialization.Serializable
data class BranchDetails(
    val id: String,
    val branchCode: String,
    val name: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val phoneNumber: String,
    val managerUserId: String?,
    val operatingHours: String,
    val status: String,
    val totalCustomers: Int,
    val totalAccounts: Int,
    val totalDeposits: String,
    val totalLoans: String,
    val createdAt: String,
    val updatedAt: String
)

// Already defined in ServerModels.kt

// Response for security alerts
@kotlinx.serialization.Serializable
data class SecurityAlertsResponse(
    val success: Boolean,
    val message: String,
    val data: List<String> = emptyList(),
    val timestamp: String? = null
)

@kotlinx.serialization.Serializable
data class MpesaDepositResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val checkoutRequestID: String? = null,
    val merchantRequestID: String? = null,
    val customerMessage: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)

@kotlinx.serialization.Serializable
data class TransactionStatusResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String,
    val status: String? = null,
    val amount: Double? = null,
    val phoneNumber: String? = null,
    val walletId: String? = null,
    val createdAt: String? = null,
    val error: String? = null,
    val timestamp: String? = null
)

@kotlinx.serialization.Serializable
data class MpesaDepositStatus(
    val transactionId: String,
    val status: String
)

@kotlinx.serialization.Serializable
data class ManualTransactionRegistrationRequest(
    val mpesaReceiptNumber: String,
    val walletId: String,
    val phoneNumber: String,
    val amount: Double,
    val description: String? = null
)

@kotlinx.serialization.Serializable
data class OpeningAccountType(
    val type: String,
    val displayName: String,
    val minimumDeposit: String,
    val interestRate: String,
    val features: List<String>
)

@kotlinx.serialization.Serializable
data class AccountTypesResponse(
    val accountTypes: List<OpeningAccountType>,
    val availableTypes: Int
)


fun Application.configureRouting() {
//    println("=== CONFIGURING ROUTING ===")
//    println("=== CONFIGURING ROUTING ===")
    val customerService = CustomerService()
    val accountService = AccountService()
    val transactionService = TransactionService()
    val loanService = LoanService()
    val creditAssessmentService = CreditAssessmentService()
    val userService = UserService()
    val adminService = AdminService()
    val customerCareService = CustomerCareService()
    val masterWalletService = MasterWalletService()
    val employeeService = EmployeeService()
    val branchService = BranchService()
    val branchOperationsService = BranchOperationsService()
    val cardService = org.dals.project.services.CardService()
    val mastercardIssuanceService = org.dals.project.services.MastercardIssuanceService()

    // Initialize MpesaService with required dependencies
    val mpesaService = MpesaService(
        database = DatabaseFactory.database,
        masterWalletService = masterWalletService
    )

    routing {
        // Static website hosting under server/resources/static
        // GET / -> serve index.html, and /static -> serve static assets
        get("/") {
            val resource = this::class.java.classLoader.getResource("static/index.html")
            if (resource == null) {
                call.respond(HttpStatusCode.NotFound, "index.html not found")
            } else {
                val bytes = resource.readBytes()
                call.respondBytes(bytes, ContentType.Text.Html)
            }
        }

        // Clean URL routing - serve HTML pages without .html extension
        get("/{page}") {
            val page = call.parameters["page"]
            val validPages = listOf("about", "who-we-serve", "services", "privacy", "terms")

            if (page in validPages) {
                val resource = this::class.java.classLoader.getResource("static/$page.html")
                if (resource != null) {
                    val bytes = resource.readBytes()
                    call.respondBytes(bytes, ContentType.Text.Html)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Page not found")
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "Page not found")
            }
        }

        // Serve service pages under /services directory
        get("/services/{page}") {
            val pageParam = call.parameters["page"] ?: ""
            // Remove .html extension if present
            val page = pageParam.removeSuffix(".html")

            val validServicePages = listOf(
                "personal-banking", "business-banking", "loans", "investments",
                "international-transfers", "mobile-banking", "cards", "index"
            )

            if (page in validServicePages) {
                val resource = this::class.java.classLoader.getResource("static/services/$page.html")
                if (resource != null) {
                    val bytes = resource.readBytes()
                    call.respondBytes(bytes, ContentType.Text.Html)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Service page not found")
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "Service page not found")
            }
        }

        // Serve any additional static assets like CSS, images, JS under /static
        staticResources("/static", "static")

        // ==================== SERVE UPLOADED KYC DOCUMENTS ====================
        get("/uploads/kyc/{customerId}/{fileName}") {
            try {
                val customerId = call.parameters["customerId"]
                val fileName = call.parameters["fileName"]

                if (customerId == null || fileName == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing customerId or fileName"))
                    return@get
                }

                val uploadsDir = java.io.File("uploads/kyc/$customerId")
                val file = java.io.File(uploadsDir, fileName)

                if (!file.exists() || !file.isFile) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                    return@get
                }

                // Determine content type based on file extension
                val contentType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> ContentType.Image.JPEG
                    "png" -> ContentType.Image.PNG
                    "gif" -> ContentType.Image.GIF
                    "pdf" -> ContentType.Application.Pdf
                    else -> ContentType.Application.OctetStream
                }

                call.respondFile(file)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to serve file: ${e.message}"))
            }
        }

        // ==================== HEALTH CHECK ENDPOINT - FIXED SERIALIZATION ✅ ====================
        get("/health") {
            @kotlinx.serialization.Serializable
            data class HealthData(
                val status: String,
                val service: String,
                val version: String,
                val timestamp: String
            )

            @kotlinx.serialization.Serializable
            data class HealthResponse(
                val success: Boolean,
                val message: String,
                val data: HealthData,
                val timestamp: String
            )

            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    success = true,
                    message = "AxionBank API is healthy and running",
                    data = HealthData(
                        status = "UP",
                        service = "AxionBank Core API",
                        version = "1.0.0",
                        timestamp = java.time.LocalDateTime.now().toString()
                    ),
                    timestamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        }

        // ==================== API HEALTH CHECK ====================
        get("/api/health") {
            @kotlinx.serialization.Serializable
            data class ApiHealthResponse(val status: String, val message: String, val timestamp: String)

            call.respond(
                HttpStatusCode.OK,
                ApiHealthResponse(
                    status = "UP",
                    message = "Axion Bank API is running",
                    timestamp = java.time.LocalDateTime.now().toString()
                )
            )
        }

        get("/api/v1/health") {
            @kotlinx.serialization.Serializable
            data class ApiHealthResponse(val status: String, val message: String, val timestamp: String)

            call.respond(
                HttpStatusCode.OK,
                ApiHealthResponse(
                    status = "UP",
                    message = "Axion Bank API v1 is running",
                    timestamp = java.time.LocalDateTime.now().toString()
                )
            )
        }

        // ==================== API V1 ROUTES ====================
        route("/api/v1") {

            // ==================== M-PESA INTEGRATION ENDPOINTS ==================== 
            route("/mpesa") {
                // STK Push initiation endpoint - WORKING ✅
                post("/stk-push") {
                    try {
                        val request = call.receive<MpesaWalletDepositRequest>()

                        // Validate request
                        if (request.phoneNumber.isBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<Unit>(
                                    success = false,
                                    message = "Phone number is required",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                            return@post
                        }

                        if (request.amount <= 0) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<Unit>(
                                    success = false,
                                    message = "Amount must be greater than zero",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                            return@post
                        }

//                        println("🚀 Processing STK Push request: Phone=${request.phoneNumber}, Amount=${request.amount}")

                        // Use REAL Safaricom API to initiate STK Push
                        val result = mpesaService.initiateStkPush(
                            phoneNumber = request.phoneNumber,
                            amount = request.amount,
                            description = request.description?.ifBlank { "AxionBank Payment" } ?: "AxionBank Payment"
                        )

                        val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                        call.respond(
                            statusCode, ApiResponse(
                                success = result.success,
                                message = result.message,
                                data = mapOf(
                                    "merchantRequestID" to result.merchantRequestID,
                                    "checkoutRequestID" to result.checkoutRequestID,
                                    "responseCode" to (if (result.success) "0" else "1"),
                                    "responseDescription" to result.message,
                                    "customerMessage" to result.customerMessage,
                                    "transactionId" to result.transactionId
                                )
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ STK Push endpoint error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "STK Push failed: ${e.message}",
                                error = "STK_PUSH_ERROR"
                            )
                        )
                    }
                }

                // Transaction status query endpoint - WORKING ✅
                get("/transaction-status/{checkoutRequestID}") {
                    try {
                        val checkoutRequestID = call.parameters["checkoutRequestID"]
                            ?: throw IllegalArgumentException("Checkout Request ID is required")

//                            println("🔍 Checking status for checkoutRequestID: $checkoutRequestID")

                        // Use the real STK Push Query API
                        val response = mpesaService.queryStkPushStatus(checkoutRequestID)

                        call.respond(
                            HttpStatusCode.OK, ApiResponse(
                                success = response.success,
                                message = response.message,
                                data = mapOf(
                                    "merchantRequestID" to response.merchantRequestID,
                                    "checkoutRequestID" to response.checkoutRequestID,
                                    "responseCode" to response.responseCode,
                                    "responseDescription" to response.responseDescription,
                                    "resultCode" to response.responseCode,
                                    "resultDesc" to response.responseDescription
                                )
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Status check error: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "Failed to check transaction status: ${e.message}",
                                error = "STATUS_CHECK_ERROR"
                            )
                        )
                    }
                }

                // Query M-Pesa transactions by account
                get("/transactions/account/{accountNumber}") {
                    try {
                        val accountNumber = call.parameters["accountNumber"]
                            ?: throw IllegalArgumentException("Account number is required")

//                        println("🔍 Fetching M-Pesa transactions for account: $accountNumber")

                        val transactions = transaction {
                            MpesaTransactions.select {
                                MpesaTransactions.accountNumber eq accountNumber
                            }.orderBy(MpesaTransactions.createdAt, SortOrder.DESC)
                                .limit(50)
                                .map { row ->
                                    mapOf(
                                        "id" to row[MpesaTransactions.id].toString(),
                                        "checkoutRequestId" to row[MpesaTransactions.checkoutRequestId],
                                        "mpesaReceiptNumber" to (row[MpesaTransactions.mpesaReceiptNumber] ?: "N/A"),
                                        "phoneNumber" to row[MpesaTransactions.phoneNumber],
                                        "amount" to row[MpesaTransactions.amount].toDouble(),
                                        "status" to row[MpesaTransactions.status],
                                        "description" to row[MpesaTransactions.description],
                                        "accountNumber" to row[MpesaTransactions.accountNumber],
                                        "createdAt" to row[MpesaTransactions.createdAt].toString()
                                    )
                                }
                        }

                        call.respond(
                            HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "M-Pesa transactions fetched successfully",
                                data = transactions
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error fetching M-Pesa transactions: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "Failed to fetch transactions: ${e.message}",
                                error = "FETCH_ERROR"
                            )
                        )
                    }
                }

                // Search M-Pesa transaction by reference (mpesaReceiptNumber)
                get("/transactions/reference/{reference}") {
                    try {
                        val reference = call.parameters["reference"]
                            ?: throw IllegalArgumentException("Transaction reference is required")

//                        println("🔍 Searching for transaction with reference: $reference")

                        val transactionData = try {
                            transaction {
                            // First, check main Transactions table
                            val mainTransaction = Transactions.select {
                                Transactions.reference eq reference
                            }.orderBy(Transactions.timestamp, SortOrder.DESC)
                             .firstOrNull()?.let { row ->
                                mapOf(
                                    "id" to row[Transactions.id].toString(),
                                    "reference" to row[Transactions.reference],
                                    "accountId" to row[Transactions.accountId].toString(),
                                    "amount" to row[Transactions.amount].toDouble(),
                                    "status" to row[Transactions.status].name,
                                    "type" to row[Transactions.type].name,
                                    "description" to row[Transactions.description],
                                    "timestamp" to row[Transactions.timestamp].toString(),
                                    "balanceAfter" to row[Transactions.balanceAfter].toDouble(),
                                    "category" to (row[Transactions.category] ?: "")
                                )
                            }

                            // If not found in main table, check MpesaTransactions
                            mainTransaction ?: MpesaTransactions.select {
                                MpesaTransactions.mpesaReceiptNumber eq reference
                            }.orderBy(MpesaTransactions.createdAt, SortOrder.DESC)
                             .firstOrNull()?.let { row ->
                                mapOf(
                                    "id" to row[MpesaTransactions.id].toString(),
                                    "reference" to (row[MpesaTransactions.mpesaReceiptNumber] ?: "N/A"),
                                    "checkoutRequestId" to row[MpesaTransactions.checkoutRequestId],
                                    "phoneNumber" to row[MpesaTransactions.phoneNumber],
                                    "accountNumber" to row[MpesaTransactions.accountNumber],
                                    "amount" to row[MpesaTransactions.amount].toDouble(),
                                    "status" to row[MpesaTransactions.status],
                                    "type" to "M-PESA DEPOSIT",
                                    "description" to row[MpesaTransactions.description],
                                    "timestamp" to (row[MpesaTransactions.transactionDate]?.toString() ?: row[MpesaTransactions.createdAt].toString()),
                                    "createdAt" to row[MpesaTransactions.createdAt].toString()
                                )
                            }
                        }
                        } catch (e: Exception) {
                            println("❌ Database error: ${e.message}")
                            e.printStackTrace()
                            null
                        }

                        println("🔍 Transaction data result: ${if (transactionData != null) "FOUND" else "NOT FOUND"}")

                        if (transactionData != null) {
                            // Manually build JSON to avoid serialization issues
                            val dataJson = transactionData.entries.joinToString(",") { (key, value) ->
                                val jsonValue = when (value) {
                                    is String -> "\"${value.replace("\"", "\\\"")}\""
                                    is Number -> value.toString()
                                    is Boolean -> value.toString()
                                    null -> "null"
                                    else -> "\"$value\""
                                }
                                "\"$key\":$jsonValue"
                            }
                            val jsonResponse = """{"success":true,"message":"Transaction found","data":{$dataJson}}"""
                            println("📤 Sending transaction response: $jsonResponse")
                            call.respondText(jsonResponse, ContentType.Application.Json, HttpStatusCode.OK)
                        } else {
                            call.respondText(
                                """{"success":false,"message":"Transaction not found with reference: $reference","error":"NOT_FOUND","data":null}""",
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                    } catch (e: Exception) {
                        println("❌ Error searching transaction: ${e.message}")
                        e.printStackTrace()
                        call.respondText(
                            """{"success":false,"message":"Failed to search transaction: ${e.message}","error":"SEARCH_ERROR","data":null}""",
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }

                // Automatic reversal detection and processing
                post("/detect-reversals") {
                    try {
                        println("🔍 Manual trigger: Detecting and processing reversals...")

                        // Run the automatic reversal detection
                        mpesaService.detectAndProcessReversals()

                        call.respond(
                            HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "Reversal detection completed. Check server logs for details.",
                                data = mapOf("status" to "completed")
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Reversal detection error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "Failed to detect reversals: ${e.message}",
                                error = "DETECTION_ERROR"
                            )
                        )
                    }
                }

                // Process all pending reversals - one-time fix
                post("/fix-all-reversals") {
                    try {
                        println("🔧 Manual trigger: Processing all pending reversals...")

                        // Run the batch reversal processing
                        mpesaService.processAllPendingReversals()

                        call.respond(
                            HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "Batch reversal processing completed. Check server logs for details.",
                                data = mapOf("status" to "completed")
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Batch reversal processing error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "Failed to process reversals: ${e.message}",
                                error = "BATCH_PROCESSING_ERROR"
                            )
                        )
                    }
                }

                // Get M-Pesa transactions by phone number or account
                get("/transactions") {
                    try {
                        val phoneNumber = call.request.queryParameters["phoneNumber"]
                        val accountNumber = call.request.queryParameters["accountNumber"]
                        val status = call.request.queryParameters["status"]

                        if (phoneNumber == null && accountNumber == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, MpesaReversalResponse(
                                    success = false,
                                    message = "Either phoneNumber or accountNumber is required",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                            return@get
                        }

                        val transactions = mpesaService.getMpesaTransactions(phoneNumber, accountNumber, status)

                        call.respond(
                            HttpStatusCode.OK, mapOf(
                                "success" to true,
                                "message" to "Transactions retrieved successfully",
                                "data" to transactions
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error fetching M-Pesa transactions: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError, MpesaReversalResponse(
                                success = false,
                                message = "Failed to fetch transactions: ${e.message}",
                                error = "QUERY_ERROR"
                            )
                        )
                    }
                }

                // B2C Withdrawal - Withdraw from bank account to M-Pesa
                post("/withdraw") {
                    try {
                        val request = call.receive<MpesaWithdrawalRequest>()

//                        println("🔄 Processing M-Pesa withdrawal: ${request.amount} to ${request.phoneNumber}")

                        val result = mpesaService.initiateB2CPayment(
                            phoneNumber = request.phoneNumber,
                            amount = request.amount,
                            accountNumber = request.accountNumber,
                            customerId = request.customerId,
                            remarks = request.remarks ?: "Withdrawal to M-Pesa"
                        )

                        val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                        call.respond(statusCode, result)
                    } catch (e: Exception) {
                        println("❌ B2C withdrawal error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, mapOf(
                                "success" to false,
                                "message" to "Failed to process withdrawal: ${e.message}",
                                "error" to "B2C_ERROR"
                            )
                        )
                    }
                }

                // M-Pesa reversal endpoint - Process reversals manually or via callback
                post("/reversal") {
                    try {
                        val request = call.receive<MpesaReversalRequest>()

//                        println("🔄 Processing M-Pesa reversal: CheckoutRequestID=${request.checkoutRequestID}")

                        val result = mpesaService.processWalletReversal(
                            checkoutRequestID = request.checkoutRequestID,
                            mpesaReceiptNumber = request.mpesaReceiptNumber,
                            reason = request.reason ?: "Transaction reversed by M-Pesa"
                        )

                        val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                        call.respond(
                            statusCode, MpesaReversalResponse(
                                success = result.success,
                                message = if (result.success) "Reversal processed successfully" else "Reversal failed",
                                transactionId = result.transactionId,
                                status = result.status,
                                amount = result.amount,
                                walletId = result.walletId,
                                error = result.error
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Reversal processing error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, MpesaReversalResponse(
                                success = false,
                                message = "Failed to process reversal: ${e.message}",
                                error = "REVERSAL_ERROR"
                            )
                        )
                    }
                }

                // Business Buy Goods - Pay to till number for goods/services
                post("/buy-goods") {
                    try {
                        @Serializable
                        data class BusinessBuyGoodsRequest(
                            val tillNumber: String,
                            val amount: Double,
                            val accountReference: String,
                            val remarks: String,
                            val requesterPhone: String? = null
                        )

                        val request = call.receive<BusinessBuyGoodsRequest>()

                        println("🛒 Processing Business Buy Goods: Till=${request.tillNumber}, Amount=${request.amount}")

                        val result = mpesaService.initiateBusinessBuyGoods(
                            tillNumber = request.tillNumber,
                            amount = request.amount,
                            accountReference = request.accountReference,
                            remarks = request.remarks,
                            requesterPhone = request.requesterPhone
                        )

                        val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                        call.respond(statusCode, result)
                    } catch (e: Exception) {
                        println("❌ Business Buy Goods error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError, mapOf(
                                "success" to false,
                                "message" to "Failed to process payment: ${e.message}",
                                "error" to "BUY_GOODS_ERROR"
                            )
                        )
                    }
                }

                // Customer account M-Pesa deposit endpoint - NEW ✅
                post("/deposit/account/{accountNumber}") {
                    try {
                        val accountNumber = call.parameters["accountNumber"]
                            ?: throw IllegalArgumentException("Account number is required")
                        val request = call.receive<MpesaWalletDepositRequest>()

                        println("🏦 Processing M-Pesa deposit to customer account: Account=$accountNumber, Phone=${request.phoneNumber}, Amount=${request.amount}")

                        // Validate request
                        if (request.phoneNumber.isBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<Unit>(
                                    success = false,
                                    message = "Phone number is required",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                            return@post
                        }

                        if (request.amount <= 0) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<Unit>(
                                    success = false,
                                    message = "Amount must be greater than zero",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                            return@post
                        }

                        // Initiate M-Pesa deposit to customer account
                        val result = mpesaService.initiateCustomerAccountDeposit(
                            accountNumber = accountNumber,
                            phoneNumber = request.phoneNumber,
                            amount = request.amount,
                            description = request.description?.ifBlank { "Deposit to account $accountNumber" } ?: "Deposit to account $accountNumber"
                        )

                        val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                        call.respond(
                            statusCode, ApiResponse(
                                success = result.success,
                                message = result.message,
                                data = mapOf(
                                    "transactionId" to result.transactionId,
                                    "checkoutRequestID" to result.checkoutRequestID,
                                    "merchantRequestID" to result.merchantRequestID,
                                    "customerMessage" to result.customerMessage,
                                    "accountNumber" to accountNumber
                                ),
                                error = result.error
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Customer account deposit error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<Unit>(
                                success = false,
                                message = "Failed to initiate account deposit: ${e.message}",
                                error = "ACCOUNT_DEPOSIT_ERROR"
                            )
                        )
                    }
                }

                // Account verification endpoint - WORKING ✅
                get("/account/verify/{accountNumber}") {
                    try {
                        val accountNumber = call.parameters["accountNumber"]
                            ?: throw IllegalArgumentException("Account number is required")

                        val account = newSuspendedTransaction {
                            Accounts.select { Accounts.accountNumber eq accountNumber }
                                .singleOrNull()
                        }

                        if (account != null) {
                            call.respond(
                                HttpStatusCode.OK, ApiResponse(
                                    success = true,
                                    message = "Account verification successful",
                                    data = AccountVerificationDetail(
                                        status = account[Accounts.status].name,
                                        kycStatus = "VERIFIED"
                                    )
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound, ApiResponse<Unit>(
                                    success = false,
                                    message = "Account not found",
                                    error = "ACCOUNT_NOT_FOUND"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                                success = false,
                                message = "Account verification failed: ${e.message}",
                                error = "ACCOUNT_VERIFICATION_ERROR"
                            )
                        )
                    }
                }

                // M-Pesa callback endpoint (webhook from Safaricom) - WORKING ✅
                post("/callback/{transactionId}") {
                    try {
                        val transactionId = call.parameters["transactionId"]
                            ?: throw IllegalArgumentException("Transaction ID is required")
                        val callbackData = call.receive<Map<String, Any>>()

                        // Log callback received
                        println("📞 M-Pesa callback received for transaction: $transactionId")
                        println("📋 Callback data: $callbackData")

                        call.respond(
                            HttpStatusCode.OK, mapOf(
                                "ResultCode" to 0,
                                "ResultDesc" to "Callback processed successfully"
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Callback processing error: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest, mapOf(
                                "ResultCode" to 1,
                                "ResultDesc" to "Callback processing error: ${e.message}"
                            )
                        )
                    }
                }

                // M-Pesa B2C/Result callback endpoint - WORKING ✅
                post("/result") {
                    try {
                        val resultData = call.receive<Map<String, Any>>()
                        println("📞 M-Pesa Result callback received: $resultData")
                        
                        // Pass to MpesaService for processing
                        // We check if it's a Business Buy Goods or B2C callback
                        val result = resultData["Result"] as? Map<String, Any>
                        val conversationId = result?.get("ConversationID")?.toString() ?: ""
                        
                        if (conversationId.isNotEmpty()) {
                            // Try both B2C and Business Buy Goods
                            // In a real system, you might distinguish by endpoint or metadata
                            mpesaService.handleBusinessBuyGoodsCallback(resultData)
                        }

                        call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 0, "ResultDesc" to "Success"))
                    } catch (e: Exception) {
                        println("❌ Result callback error: ${e.message}")
                        call.respond(HttpStatusCode.OK, mapOf("ResultCode" to 1, "ResultDesc" to "Error"))
                    }
                }

                // Public webhook endpoint for M-Pesa callbacks - WORKING ✅
                post("/callback") {
                    try {
                        val callbackData = call.receive<Map<String, Any>>()

                        println("📞 M-Pesa public callback received")
                        println("📋 Callback data: $callbackData")

                        call.respond(
                            HttpStatusCode.OK, mapOf(
                                "ResultCode" to 0,
                                "ResultDesc" to "Callback processed successfully"
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Public callback error: ${e.message}")
                        call.respond(
                            HttpStatusCode.OK, mapOf(
                                "ResultCode" to 1,
                                "ResultDesc" to "Callback processing error"
                            )
                        )
                    }
                }
            }

            // ==================== TRANSACTIONS ROUTES ====================
            route("/transactions") {
                // Search transaction by reference
                get("/reference/{reference}") {
                    try {
                        val reference = call.parameters["reference"]
                            ?: throw IllegalArgumentException("Transaction reference is required")

                        println("🔍 Searching for transaction with reference: $reference")
                        val transactionService = TransactionService()
                        val transaction = transactionService.getTransactionByReference(reference)
                        println("📄 Transaction data result: ${if (transaction != null) "FOUND" else "NOT FOUND"}")

                        if (transaction != null) {
                            // Enhance transaction with customer and balance information
                            println("🔄 Enhancing transaction data...")
                            val enhancedData = transactionService.getEnhancedTransactionDetails(transaction)
                            println("✅ Enhanced data keys: ${enhancedData.keys}")

                            // Manually build JSON to avoid serialization issues
                            val dataJson = enhancedData.entries.joinToString(",") { (key, value) ->
                                val jsonValue = when (value) {
                                    is String -> "\"${value.replace("\"", "\\\"")}\""
                                    is Number -> value.toString()
                                    is Boolean -> value.toString()
                                    null -> "null"
                                    else -> "\"$value\""
                                }
                                "\"$key\":$jsonValue"
                            }
                            val jsonResponse = """{"success":true,"message":"Transaction found","data":{$dataJson}}"""
                            println("📤 Sending transaction response with ${enhancedData.keys.size} fields")
                            call.respondText(jsonResponse, ContentType.Application.Json, HttpStatusCode.OK)
                        } else {
                            call.respondText(
                                """{"success":false,"message":"Transaction not found with reference: $reference","error":"NOT_FOUND","data":null}""",
                                ContentType.Application.Json,
                                HttpStatusCode.OK
                            )
                        }
                    } catch (e: Exception) {
                        println("❌ Error searching transaction: ${e.message}")
                        e.printStackTrace()
                        call.respondText(
                            """{"success":false,"message":"Failed to search transaction: ${e.message}","error":"SEARCH_ERROR","data":null}""",
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }
                }
            }

            // ==================== AUTHENTICATION ROUTES ==================== 
            route("/auth") {
                // Login endpoint - WORKING ✅
                post("/login") {
                    try {
                        val request = call.receive<LoginRequest>()

                        val userData = transaction {
                            Users.select { Users.username eq request.username }
                                .singleOrNull()
                                ?.let { row ->
                                    val passwordHash = row[Users.passwordHash]
                                    if (BCrypt.checkpw(request.password, passwordHash)) {
                                        val role = row[Users.role]

                                        // Read permissions from database
                                        val permissionsJson = row[Users.permissions]
                                        val permissions = try {
                                            kotlinx.serialization.json.Json.decodeFromString<List<String>>(permissionsJson)
                                        } catch (e: Exception) {
                                            println("⚠️ Failed to parse permissions for user ${row[Users.username]}: ${e.message}")
                                            emptyList()
                                        }

                                        println("✅ User ${row[Users.username]} logged in with ${permissions.size} permissions: $permissions")

                                        UserLoginData(
                                            id = row[Users.id].toString(),
                                            username = row[Users.username],
                                            email = row[Users.email],
                                            firstName = row[Users.firstName],
                                            lastName = row[Users.lastName],
                                            role = role.name,
                                            status = row[Users.status].name,
                                            branchId = row[Users.branchId]?.toString(),
                                            employeeId = row[Users.employeeId],
                                            permissions = permissions,
                                            lastLoginAt = row[Users.lastLoginAt]?.toString()
                                        )
                                    } else null
                                }
                        }

                        if (userData != null) {
                            // Create session
                            val sessionId = UUID.randomUUID()

                            transaction {
                                // Update last login time
                                Users.update({ Users.username eq request.username }) {
                                    it[lastLoginAt] = Instant.now()
                                }

                                // Create session record
                                UserSessions.insert {
                                    it[UserSessions.id] = sessionId
                                    it[UserSessions.userId] = UUID.fromString(userData.id)
                                    it[UserSessions.role] = UserRole.valueOf(userData.role)
                                    it[UserSessions.branchId] = userData.branchId?.let { bid -> UUID.fromString(bid) }
                                    it[UserSessions.ipAddress] = call.request.local.remoteHost
                                    it[UserSessions.userAgent] = call.request.headers["User-Agent"] ?: "Unknown"
                                }
                            }

                            call.respond(
                                HttpStatusCode.OK,
                                LoginResponse(
                                    success = true,
                                    message = "Login successful",
                                    user = userData,
                                    sessionId = sessionId.toString()
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                LoginResponse(
                                    success = false,
                                    message = "Invalid username or password"
                                )
                            )
                        }

                    } catch (e: Exception) {
                        println("❌ Login error: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            LoginResponse(
                                success = false,
                                message = "Login failed: ${e.message}"
                            )
                        )
                    }
                }

                // Logout endpoint - WORKING ✅
                post("/logout") {
                    try {
                        val sessionId = call.request.headers["Authorization"]?.removePrefix("Bearer ")

                        if (sessionId != null) {
                            transaction {
                                UserSessions.update({
                                    UserSessions.id eq UUID.fromString(sessionId)
                                }) {
                                    it[isActive] = false
                                }
                            }
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            LoginResponse(
                                success = true,
                                message = "Logout successful"
                            )
                        )

                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            LoginResponse(
                                success = false,
                                message = "Logout failed: ${e.message}"
                            )
                        )
                    }
                }

                // Session validation endpoint - WORKING ✅
                get("/validate") {
                    try {
                        val sessionId = call.request.headers["Authorization"]?.removePrefix("Bearer ")

                        if (sessionId == null) {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                LoginResponse(
                                    success = false,
                                    message = "No session token provided"
                                )
                            )
                            return@get
                        }

                        val sessionData = transaction {
                            UserSessions
                                .join(Users, JoinType.INNER, UserSessions.userId, Users.id)
                                .select {
                                    (UserSessions.id eq UUID.fromString(sessionId)) and
                                            (UserSessions.isActive eq true)
                                }
                                .singleOrNull()
                                ?.let { row ->
                                    val role = row[Users.role]
                                    // Generate permissions based on role
                                    val permissions = when (role) {
                                        UserRole.SYSTEM_ADMIN -> listOf(
                                            "SYSTEM_ADMIN",
                                            "USER_MANAGEMENT",
                                            "SYSTEM_CONFIG",
                                            "AUDIT_ACCESS"
                                        )

                                        UserRole.BRANCH_MANAGER -> listOf(
                                            "BRANCH_OPERATIONS",
                                            "APPROVE_TRANSACTIONS",
                                            "STAFF_MANAGEMENT"
                                        )

                                        UserRole.TELLER -> listOf(
                                            "TRANSACTION_PROCESS",
                                            "CUSTOMER_LOOKUP",
                                            "CASH_MANAGEMENT"
                                        )

                                        UserRole.CUSTOMER_SERVICE_OFFICER -> listOf(
                                            "CUSTOMER_CREATE",
                                            "ACCOUNT_MAINTENANCE",
                                            "KYC_MANAGEMENT",
                                            "SERVICE_REQUESTS"
                                        )

                                        UserRole.LOAN_OFFICER -> listOf(
                                            "LOAN_APPLICATION_REVIEW",
                                            "CREDIT_ASSESSMENT",
                                            "LOAN_APPROVAL"
                                        )

                                        UserRole.ACCOUNT_MANAGER -> listOf("ACCOUNT_MANAGEMENT", "CUSTOMER_RELATIONS")
                                        else -> listOf("BASIC_ACCESS")
                                    }

                                    UserLoginData(
                                        id = row[Users.id].toString(),
                                        username = row[Users.username],
                                        email = row[Users.email],
                                        firstName = row[Users.firstName],
                                        lastName = row[Users.lastName],
                                        role = role.name,
                                        status = row[Users.status].name,
                                        branchId = row[Users.branchId]?.toString(),
                                        employeeId = row[Users.employeeId],
                                        permissions = permissions,
                                        lastLoginAt = row[Users.lastLoginAt]?.toString()
                                    )
                                }
                        }

                        if (sessionData != null) {
                            // Update last activity
                            transaction {
                                UserSessions.update({
                                    UserSessions.id eq UUID.fromString(sessionId)
                                }) {
                                    it[lastActivity] = Instant.now()
                                }
                            }

                            call.respond(
                                HttpStatusCode.OK,
                                LoginResponse(
                                    success = true,
                                    message = "Session valid",
                                    user = sessionData
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                LoginResponse(
                                    success = false,
                                    message = "Invalid or expired session"
                                )
                            )
                        }

                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            LoginResponse(
                                success = false,
                                message = "Session validation failed: ${e.message}"
                            )
                        )
                    }
                }
            }

            // ==================== BRANCH ROUTES ====================
            route("/branches") {
                // Get branch statistics
                get("/{branchId}/statistics") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = branchService.getBranchStatistics(branchId)
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve branch statistics: ${e.message}"
                            )
                        )
                    }
                }

                // Get staff performance for a branch
                get("/{branchId}/staff-performance") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = branchService.getStaffPerformance(branchId)
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve staff performance: ${e.message}"
                            )
                        )
                    }
                }

                // Get branch operations
                get("/{branchId}/operations") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        val status = call.parameters["status"]

                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = branchOperationsService.getBranchOperations(branchId, status)
                        call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve operations: ${e.message}"
                            )
                        )
                    }
                }

                // Create branch operation
                post("/{branchId}/operations") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@post
                        }

                        @kotlinx.serialization.Serializable
                        data class CreateOperationRequest(
                            val createdBy: String,
                            val type: String,
                            val title: String,
                            val description: String?,
                            val priority: String,
                            val assignedTo: String?,
                            val dueDate: String?
                        )

                        val req = call.receive<CreateOperationRequest>()
                        val createdById = UUID.fromString(req.createdBy)
                        val assignedToId = req.assignedTo?.let { UUID.fromString(it) }
                        val dueDate = req.dueDate?.let { java.time.LocalDate.parse(it) }

                        val response = branchOperationsService.createBranchOperation(
                            branchId, createdById, req.type, req.title, req.description,
                            req.priority, assignedToId, dueDate
                        )
                        call.respond(if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to create operation: ${e.message}"
                            )
                        )
                    }
                }

                // Get daily operations summary
                get("/{branchId}/daily-summary") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        val date = call.parameters["date"]?.let { java.time.LocalDate.parse(it) }

                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = if (date != null) {
                            branchOperationsService.getDailySummary(branchId, date)
                        } else {
                            branchOperationsService.getDailySummary(branchId)
                        }
                        call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve daily summary: ${e.message}"
                            )
                        )
                    }
                }

                // Get performance metrics
                get("/{branchId}/performance-metrics") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        val date = call.parameters["date"]?.let { java.time.LocalDate.parse(it) }

                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = if (date != null) {
                            branchOperationsService.getPerformanceMetrics(branchId, date)
                        } else {
                            branchOperationsService.getPerformanceMetrics(branchId)
                        }
                        call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve performance metrics: ${e.message}"
                            )
                        )
                    }
                }

                // Get staff productivity
                get("/{branchId}/staff-productivity") {
                    try {
                        val branchId = call.parameters["branchId"]?.let { UUID.fromString(it) }
                        val date = call.parameters["date"]?.let { java.time.LocalDate.parse(it) }

                        if (branchId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid branch ID"
                                )
                            )
                            return@get
                        }

                        val response = if (date != null) {
                            branchOperationsService.getStaffProductivity(branchId, date)
                        } else {
                            branchOperationsService.getStaffProductivity(branchId)
                        }
                        call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve staff productivity: ${e.message}"
                            )
                        )
                    }
                }
            }

            route("/auth") {
                // ==================== CUSTOMER APP ENDPOINTS ====================
                route("/customer") {

                    // Customer Registration endpoint
                    post("/register") {
                        try {
                            val req = call.receive<CustomerRegisterRequest>()

                            // Validation
                            if (req.username.isBlank() ||
                                req.password.isBlank() ||
                                req.email.isBlank() ||
                                req.phoneNumber.isBlank() ||
                                req.firstName.isBlank() ||
                                req.lastName.isBlank()
                            ) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Missing required fields",
                                        error = "MISSING_FIELDS"
                                    )
                                )
                                return@post
                            }
                            if (req.password != req.confirmPassword) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Passwords do not match",
                                        error = "PASSWORD_MISMATCH"
                                    )
                                )
                                return@post
                            }

                            // Check if username or email or phone exists
                            val usernameExists = transaction {
                                Customers.select { Customers.username eq req.username }
                                    .singleOrNull() != null
                            }
                            val emailExists = transaction {
                                Customers.select {
                                    (Customers.email.isNotNull()) and (Customers.email eq req.email.trim().lowercase())
                                }.count() > 0
                            }
                            val phoneExists = transaction {
                                Customers.select { Customers.phoneNumber eq req.phoneNumber }
                                    .singleOrNull() != null
                            }

                            if (usernameExists) {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Username already exists",
                                        error = "USERNAME_EXISTS"
                                    )
                                )
                                return@post
                            }
                            if (emailExists) {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Email already registered",
                                        error = "EMAIL_EXISTS"
                                    )
                                )
                                return@post
                            }
                            if (phoneExists) {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Phone number already registered",
                                        error = "PHONE_EXISTS"
                                    )
                                )
                                return@post
                            }

                            // Hash password
                            val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())

                            // Get or create main branch
                            val mainBranchId = transaction {
                                Branches.select { Branches.branchCode eq "MAIN001" }
                                    .singleOrNull()?.get(Branches.id)
                                    ?: Branches.insertAndGetId {
                                        it[branchCode] = "MAIN001"
                                        it[name] = "Main Branch"
                                        it[street] = "123 Banking Street"
                                        it[city] = "New York"
                                        it[state] = "NY"
                                        it[zipCode] = "10001"
                                        it[phoneNumber] = "+1-555-BANK"
                                    }
                            }

                            val newCustomerId = transaction {
                                Customers.insert { itx ->
                                    itx[username] = req.username
                                    itx[passwordHash] = hash
                                    itx[type] = CustomerType.INDIVIDUAL // Default to INDIVIDUAL for regular signups
                                    itx[firstName] = req.firstName
                                    itx[lastName] = req.lastName
                                    itx[email] = req.email.trim().lowercase()
                                    itx[phoneNumber] = req.phoneNumber
                                    itx[customerNumber] = "CUST${System.currentTimeMillis()}"
                                    itx[branchId] = mainBranchId.value
                                    itx[createdAt] = Instant.now()
                                    itx[status] = CustomerStatus.ACTIVE
                                    itx[kycStatus] = "PENDING"
                                }[Customers.id]
                            }

                            val data = transaction {
                                Customers.select { Customers.id eq newCustomerId }
                                    .singleOrNull()
                                    ?.let { row ->
                                        // Calculate loan statistics
                                        val customerId = row[Customers.id].value
                                        val activeLoansData = Loans.select {
                                            (Loans.customerId eq customerId) and (Loans.status eq LoanStatus.ACTIVE)
                                        }.toList()

                                        val totalBorrowed = activeLoansData.sumOf { it[Loans.originalAmount] }
                                        val totalRepaid = activeLoansData.sumOf {
                                            it[Loans.totalPrincipalPaid].add(it[Loans.totalInterestPaid])
                                        }
                                        val activeLoansCount = activeLoansData.count()

                                        CustomerLoginData(
                                            id = row[Customers.id].toString(),
                                            username = row[Customers.username] ?: "",
                                            email = row[Customers.email] ?: "",
                                            firstName = row[Customers.firstName] ?: "",
                                            lastName = row[Customers.lastName] ?: "",
                                            phoneNumber = row[Customers.phoneNumber] ?: "",
                                            customerNumber = row[Customers.customerNumber],
                                            status = row[Customers.status].name,
                                            kycStatus = row[Customers.kycStatus],
                                            creditScore = row[Customers.creditScore],
                                            createdAt = row[Customers.createdAt].toString(),
                                            lastLoginAt = row[Customers.lastLoginAt]?.toString(),
                                            totalBorrowed = totalBorrowed.toString(),
                                            totalRepaid = totalRepaid.toString(),
                                            activeLoans = activeLoansCount
                                        )
                                    }
                            }

                            // TODO: Create JWT or similar for customer
                            val token = UUID.randomUUID().toString() // Dummy token for now

                            call.respond(
                                HttpStatusCode.Created,
                                CustomerLoginResponse(
                                    success = true,
                                    message = "Account created successfully",
                                    data = data,
                                    token = token,
                                    error = null
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Customer registration error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                CustomerLoginResponse(
                                    success = false,
                                    message = "Failed to register customer: ${e.message}",
                                    error = "REGISTRATION_ERROR"
                                )
                            )
                        }
                    }

                    // Forgot Password - Send Code endpoint
                    route("/forgot-password") {
                        // Store verification codes temporarily (in production, use Redis or database)
                        val verificationCodes = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>() // email -> (code, timestamp)

                        post("/send-code") {
                            @Serializable
                            data class ForgotPasswordResponse(
                                val success: Boolean,
                                val message: String,
                                val error: String? = null
                            )

                            try {
                                val request = call.receive<Map<String, String>>()
                                val email = request["email"]?.trim()?.lowercase()

                                if (email.isNullOrBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ForgotPasswordResponse(
                                            success = false,
                                            message = "Email is required",
                                            error = "MISSING_EMAIL"
                                        )
                                    )
                                    return@post
                                }

                                // Check if user exists in Customers or Users table
                                val userExists = transaction {
                                    // Check Customers table - email might be null for some customers
                                    val customerExists = Customers.select {
                                        (Customers.email.isNotNull()) and (Customers.email.lowerCase() eq email.lowercase())
                                    }.count() > 0

                                    if (customerExists) {
                                        println("✓ Found customer with email: $email")
                                        true
                                    } else {
                                        // Check Users table
                                        val userExistsInUsers = Users.select { Users.email.lowerCase() eq email.lowercase() }
                                            .count() > 0
                                        if (userExistsInUsers) {
                                            println("✓ Found user with email: $email")
                                        }
                                        userExistsInUsers
                                    }
                                }

                                if (!userExists) {
                                    println("⚠️ Password reset attempted for non-existent email: $email")
                                    call.respond(
                                        HttpStatusCode.NotFound,
                                        ForgotPasswordResponse(
                                            success = false,
                                            message = "No account found with this email address. Please check your email or register a new account.",
                                            error = "EMAIL_NOT_FOUND"
                                        )
                                    )
                                    return@post
                                }

                                // Generate 6-digit code
                                val code = (100000..999999).random().toString()
                                val timestamp = System.currentTimeMillis()

                                // Store code with 10-minute expiration
                                verificationCodes[email] = Pair(code, timestamp)

                                println("🔐 Password reset code for $email: $code")

                                // Respond immediately to avoid client timeout
                                call.respond(
                                    HttpStatusCode.OK,
                                    ForgotPasswordResponse(
                                        success = true,
                                        message = "Verification code sent to your email address. Please check your inbox.",
                                        error = null
                                    )
                                )

                                // Send email asynchronously (don't block the response)
                                @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                                GlobalScope.launch(Dispatchers.IO) {
                                    try {
                                        val emailService = EmailService()
                                        val emailResult = emailService.sendOtpEmail(
                                            to = email,
                                            otp = code,
                                            purpose = "password reset",
                                            expiryMinutes = 10
                                        )

                                        if (emailResult.isSuccess) {
                                            println("✅ Password reset email sent successfully to $email")
                                        } else {
                                            println("⚠️ Failed to send email to $email: ${emailResult.exceptionOrNull()?.message}")
                                        }
                                    } catch (e: Exception) {
                                        println("❌ Error sending password reset email: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                println("❌ Error sending reset code: ${e.message}")
                                e.printStackTrace()
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ForgotPasswordResponse(
                                        success = false,
                                        message = "Failed to send verification code",
                                        error = "SEND_CODE_ERROR"
                                    )
                                )
                            }
                        }

                        post("/verify-code") {
                            @Serializable
                            data class VerifyCodeResponse(
                                val success: Boolean,
                                val message: String,
                                val error: String? = null
                            )

                            try {
                                val request = call.receive<Map<String, String>>()
                                val email = request["email"]?.trim()?.lowercase()
                                val code = request["code"]?.trim()

                                if (email.isNullOrBlank() || code.isNullOrBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        VerifyCodeResponse(
                                            success = false,
                                            message = "Email and code are required",
                                            error = "MISSING_FIELDS"
                                        )
                                    )
                                    return@post
                                }

                                val storedData = verificationCodes[email]
                                if (storedData == null) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        VerifyCodeResponse(
                                            success = false,
                                            message = "No verification code found. Please request a new one.",
                                            error = "CODE_NOT_FOUND"
                                        )
                                    )
                                    return@post
                                }

                                val (storedCode, timestamp) = storedData
                                val currentTime = System.currentTimeMillis()
                                val expirationTime = 10 * 60 * 1000 // 10 minutes

                                if (currentTime - timestamp > expirationTime) {
                                    verificationCodes.remove(email)
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        VerifyCodeResponse(
                                            success = false,
                                            message = "Verification code has expired. Please request a new one.",
                                            error = "CODE_EXPIRED"
                                        )
                                    )
                                    return@post
                                }

                                if (storedCode != code) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        VerifyCodeResponse(
                                            success = false,
                                            message = "Invalid verification code",
                                            error = "INVALID_CODE"
                                        )
                                    )
                                    return@post
                                }

                                call.respond(
                                    HttpStatusCode.OK,
                                    VerifyCodeResponse(
                                        success = true,
                                        message = "Code verified successfully",
                                        error = null
                                    )
                                )
                            } catch (e: Exception) {
                                println("❌ Error verifying code: ${e.message}")
                                e.printStackTrace()
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    VerifyCodeResponse(
                                        success = false,
                                        message = "Failed to verify code",
                                        error = "VERIFY_CODE_ERROR"
                                    )
                                )
                            }
                        }

                        post("/reset") {
                            @Serializable
                            data class ResetPasswordResponse(
                                val success: Boolean,
                                val message: String,
                                val error: String? = null
                            )

                            try {
                                val request = call.receive<Map<String, String>>()
                                val email = request["email"]?.trim()?.lowercase()
                                val code = request["code"]?.trim()
                                val newPassword = request["newPassword"]

                                if (email.isNullOrBlank() || code.isNullOrBlank() || newPassword.isNullOrBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ResetPasswordResponse(
                                            success = false,
                                            message = "Email, code, and new password are required",
                                            error = "MISSING_FIELDS"
                                        )
                                    )
                                    return@post
                                }

                                // Verify code one more time
                                val storedData = verificationCodes[email]
                                if (storedData == null) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ResetPasswordResponse(
                                            success = false,
                                            message = "No verification code found. Please start over.",
                                            error = "CODE_NOT_FOUND"
                                        )
                                    )
                                    return@post
                                }

                                val (storedCode, timestamp) = storedData
                                val currentTime = System.currentTimeMillis()
                                val expirationTime = 10 * 60 * 1000 // 10 minutes

                                if (currentTime - timestamp > expirationTime || storedCode != code) {
                                    verificationCodes.remove(email)
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ResetPasswordResponse(
                                            success = false,
                                            message = "Invalid or expired verification code",
                                            error = "INVALID_CODE"
                                        )
                                    )
                                    return@post
                                }

                                // Hash the new password
                                val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

                                // Update password in database
                                val updated = transaction {
                                    val isCustomer = Customers.select {
                                        (Customers.email.isNotNull()) and (Customers.email eq email)
                                    }.count() > 0

                                    if (isCustomer) {
                                        println("📝 Updating password for customer: $email")
                                        Customers.update({
                                            (Customers.email.isNotNull()) and (Customers.email.lowerCase() eq email.lowercase())
                                        }) {
                                            it[passwordHash] = hashedPassword
                                        } > 0
                                    } else {
                                        println("📝 Updating password for user: $email")
                                        Users.update({ Users.email.lowerCase() eq email.lowercase() }) {
                                            it[passwordHash] = hashedPassword
                                            it[salt] = BCrypt.gensalt() // Update salt for Users table
                                        } > 0
                                    }
                                }

                                if (updated) {
                                    // Remove used code
                                    verificationCodes.remove(email)

                                    // Get username for the user to help with login
                                    val username = transaction {
                                        val customer = Customers.select {
                                            (Customers.email.isNotNull()) and (Customers.email.lowerCase() eq email.lowercase())
                                        }.singleOrNull()

                                        val currentUsername = customer?.get(Customers.username)

                                        // If username is NULL, set it to the email prefix (part before @)
                                        if (currentUsername == null && customer != null) {
                                            val emailPrefix = email.substringBefore("@")
                                            println("⚠️ Username is NULL, setting it to: $emailPrefix")

                                            Customers.update({
                                                (Customers.email.isNotNull()) and (Customers.email.lowerCase() eq email.lowercase())
                                            }) {
                                                it[Customers.username] = emailPrefix
                                            }

                                            emailPrefix
                                        } else {
                                            currentUsername
                                        }
                                    }

                                    println("✅ Password reset successfully for: $email (Username: $username)")

                                    call.respond(
                                        HttpStatusCode.OK,
                                        ResetPasswordResponse(
                                            success = true,
                                            message = "Password reset successfully. You can now login with your email or username: $username",
                                            error = null
                                        )
                                    )
                                } else {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        ResetPasswordResponse(
                                            success = false,
                                            message = "Failed to update password",
                                            error = "UPDATE_FAILED"
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                println("❌ Error resetting password: ${e.message}")
                                e.printStackTrace()
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ResetPasswordResponse(
                                        success = false,
                                        message = "Failed to reset password",
                                        error = "RESET_ERROR"
                                    )
                                )
                            }
                        }
                    }

                    // Customer Login endpoint
                    post("/login") {
                        try {
                            val req = call.receive<LoginRequest>()

                            println("🔐 Login attempt for username: ${req.username}")

                            // Try to find customer by username first, then by email if not found
                            val row = transaction {
                                println("🔍 Searching for username/email: '${req.username}'")
                                println("🔍 After trim().lowercase(): '${req.username.trim().lowercase()}'")

                                var customer = Customers.select {
                                    (Customers.username.isNotNull()) and (Customers.username eq req.username)
                                }.firstOrNull()

                                if (customer != null) {
                                    println("✅ Customer found by username")
                                } else {
                                    println("❌ Customer NOT found by username, trying email...")
                                }

                                // If not found by username, try email
                                if (customer == null) {
                                    customer = Customers.select {
                                        (Customers.email.isNotNull()) and (Customers.email.lowerCase() eq req.username.trim().lowercase())
                                    }.orderBy(Customers.createdAt, SortOrder.DESC).firstOrNull()

                                    if (customer != null) {
                                        println("✅ Customer found by email instead of username - Using most recent record")
                                    } else {
                                        println("❌ Customer NOT found by email either")
                                    }
                                }

                                customer
                            }

                            println("🔍 Row result: ${if (row != null) "Found" else "NULL"}")

                            if (row != null) {
                                val hash = row[Customers.passwordHash]
                                val kycStatus = row[Customers.kycStatus]
                                val actualUsername = row[Customers.username]
                                val email = row[Customers.email]
                                println("📋 Customer found - Username: $actualUsername, Email: $email, KYC Status: $kycStatus")

                                if (hash == null || !BCrypt.checkpw(req.password, hash)) {
                                    println("❌ Password verification failed for: ${req.username}")
                                    call.respond(
                                        HttpStatusCode.Unauthorized,
                                        CustomerLoginResponse(
                                            success = false,
                                            message = "Invalid username or password",
                                            error = "AUTH_FAILED"
                                        )
                                    )
                                    return@post
                                }

                                println("✅ Login successful for: ${req.username}, KYC Status: $kycStatus")

                                // Successful, generate token (for now, random UUID)
                                val token = UUID.randomUUID().toString()

                                // Update last login time
                                transaction {
                                    Customers.update({ Customers.id eq row[Customers.id] }) {
                                        it[lastLoginAt] = Instant.now()
                                    }
                                }

                                // Calculate loan statistics
                                val loanStats = transaction {
                                    val customerId = row[Customers.id].value
                                    val activeLoansData = Loans.select {
                                        (Loans.customerId eq customerId) and (Loans.status eq LoanStatus.ACTIVE)
                                    }.toList()

                                    val totalBorrowed = activeLoansData.sumOf { it[Loans.originalAmount] }
                                    val totalRepaid = activeLoansData.sumOf {
                                        it[Loans.totalPrincipalPaid].add(it[Loans.totalInterestPaid])
                                    }
                                    val activeLoansCount = activeLoansData.count()

                                    Triple(totalBorrowed.toString(), totalRepaid.toString(), activeLoansCount)
                                }

                                val data = CustomerLoginData(
                                    id = row[Customers.id].toString(),
                                    username = row[Customers.username] ?: "",
                                    email = row[Customers.email] ?: "",
                                    firstName = row[Customers.firstName] ?: "",
                                    lastName = row[Customers.lastName] ?: "",
                                    phoneNumber = row[Customers.phoneNumber] ?: "",
                                    customerNumber = row[Customers.customerNumber],
                                    status = row[Customers.status].name,
                                    kycStatus = row[Customers.kycStatus],
                                    creditScore = row[Customers.creditScore],
                                    createdAt = row[Customers.createdAt].toString(),
                                    lastLoginAt = row[Customers.lastLoginAt]?.toString(),
                                    totalBorrowed = loanStats.first,
                                    totalRepaid = loanStats.second,
                                    activeLoans = loanStats.third
                                )

                                call.respond(
                                    HttpStatusCode.OK,
                                    CustomerLoginResponse(
                                        success = true,
                                        message = "Login successful",
                                        data = data,
                                        token = token,
                                        error = null
                                    )
                                )

                            } else {
                                println("❌ Login failed: Customer not found for: ${req.username}")
                                call.respond(
                                    HttpStatusCode.Unauthorized,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Invalid username or password",
                                        error = "AUTH_FAILED"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println("❌ Customer login error: ${e.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                CustomerLoginResponse(
                                    success = false,
                                    message = "Failed to process login: ${e.message}",
                                    error = "LOGIN_ERROR"
                                )
                            )
                        }
                    }

                    // Customer Profile endpoint - for refreshing user data
                    get("/profile/{customerId}") {
                        try {
                            val customerId = call.parameters["customerId"]
                                ?: throw IllegalArgumentException("Customer ID is required")

                            val row = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }
                                    .singleOrNull()
                            }

                            if (row != null) {
                                val currentKycStatus = row[Customers.kycStatus]
                                println("📋 Profile fetch - Customer: ${row[Customers.id]}, KYC Status in DB: $currentKycStatus")

                                // Calculate loan statistics
                                val loanStats = transaction {
                                    val custId = row[Customers.id].value
                                    val activeLoansData = Loans.select {
                                        (Loans.customerId eq custId) and (Loans.status eq LoanStatus.ACTIVE)
                                    }.toList()

                                    val totalBorrowed = activeLoansData.sumOf { it[Loans.originalAmount] }
                                    val totalRepaid = activeLoansData.sumOf {
                                        it[Loans.totalPrincipalPaid].add(it[Loans.totalInterestPaid])
                                    }
                                    val activeLoansCount = activeLoansData.count()

                                    Triple(totalBorrowed.toString(), totalRepaid.toString(), activeLoansCount)
                                }

                                val data = CustomerLoginData(
                                    id = row[Customers.id].toString(),
                                    username = row[Customers.username] ?: "",
                                    email = row[Customers.email] ?: "",
                                    firstName = row[Customers.firstName] ?: "",
                                    lastName = row[Customers.lastName] ?: "",
                                    phoneNumber = row[Customers.phoneNumber] ?: "",
                                    customerNumber = row[Customers.customerNumber],
                                    status = row[Customers.status].name,
                                    kycStatus = row[Customers.kycStatus],
                                    creditScore = row[Customers.creditScore],
                                    createdAt = row[Customers.createdAt].toString(),
                                    lastLoginAt = row[Customers.lastLoginAt]?.toString(),
                                    totalBorrowed = loanStats.first,
                                    totalRepaid = loanStats.second,
                                    activeLoans = loanStats.third
                                )

                                call.respond(
                                    HttpStatusCode.OK,
                                    CustomerLoginResponse(
                                        success = true,
                                        message = "Profile retrieved successfully",
                                        data = data,
                                        token = null,
                                        error = null
                                    )
                                )
                            } else {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Customer not found",
                                        error = "CUSTOMER_NOT_FOUND"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println("❌ Customer profile fetch error: ${e.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                CustomerLoginResponse(
                                    success = false,
                                    message = "Failed to fetch profile: ${e.message}",
                                    error = "PROFILE_ERROR"
                                )
                            )
                        }
                    }

                    // Customer KYC Document Upload endpoint
                    post("/kyc/upload") {
                        try {
                            val request = call.receive<Map<String, String>>()

                            val customerId = request["customerId"]
                                ?: throw IllegalArgumentException("Customer ID is required")
                            val documentType = request["documentType"]
                                ?: throw IllegalArgumentException("Document type is required")
                            val fileName = request["fileName"]
                                ?: throw IllegalArgumentException("File name is required")
                            val fileData = request["fileData"]
                                ?: throw IllegalArgumentException("File data is required")

                            println("📤 Uploading KYC document for customer: $customerId")
                            println("   Document type: $documentType")
                            println("   File name: $fileName")

                            // Create upload directory if it doesn't exist
                            val uploadDir = java.io.File("uploads/kyc/$customerId")
                            if (!uploadDir.exists()) {
                                uploadDir.mkdirs()
                            }

                            // Save the file
                            val file = java.io.File(uploadDir, fileName)
                            val decodedData = java.util.Base64.getDecoder().decode(fileData)
                            file.writeBytes(decodedData)

                            println("   File saved to: ${file.absolutePath}")

                            // Get customer info
                            val customerRow = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }.singleOrNull()
                            }

                            if (customerRow == null) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Customer not found",
                                        error = "CUSTOMER_NOT_FOUND"
                                    )
                                )
                                return@post
                            }

                            val customerName = "${customerRow[Customers.firstName]} ${customerRow[Customers.lastName]}"
                            val filePath = "/uploads/kyc/$customerId/$fileName"

                            // Map frontend document types to backend enum values
                            val mappedDocumentType = when (documentType.uppercase()) {
                                "NATIONAL_ID" -> KycDocumentType.OTHER_GOVERNMENT_ID
                                "DRIVING_LICENSE" -> KycDocumentType.DRIVERS_LICENSE
                                "PASSPORT" -> KycDocumentType.PASSPORT
                                "UTILITY_BILL" -> KycDocumentType.UTILITY_BILL
                                "BANK_STATEMENT" -> KycDocumentType.BANK_STATEMENT
                                "SELFIE" -> KycDocumentType.OTHER_GOVERNMENT_ID // Map selfie to other
                                else -> {
                                    println("⚠️ KYC upload validation error: Unknown document type '$documentType'")
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ApiResponse<String>(
                                            success = false,
                                            message = "Invalid document type: $documentType",
                                            error = "INVALID_DOCUMENT_TYPE"
                                        )
                                    )
                                    return@post
                                }
                            }

                            // Insert document record into database
                            val documentId = transaction {
                                KycDocuments.insert { insert ->
                                    insert[KycDocuments.customerId] = UUID.fromString(customerId)
                                    insert[KycDocuments.customerName] = customerName
                                    insert[KycDocuments.documentType] = mappedDocumentType
                                    insert[KycDocuments.fileName] = fileName
                                    insert[KycDocuments.originalFileName] = fileName
                                    insert[KycDocuments.filePath] = filePath
                                    insert[KycDocuments.fileSize] = file.length()
                                    insert[KycDocuments.mimeType] = when (file.extension.lowercase()) {
                                        "jpg", "jpeg" -> "image/jpeg"
                                        "png" -> "image/png"
                                        "pdf" -> "application/pdf"
                                        else -> "application/octet-stream"
                                    }
                                    insert[KycDocuments.uploadedBy] = UUID.fromString(customerId)
                                    insert[KycDocuments.status] = KycDocumentStatus.PENDING_REVIEW
                                    insert[KycDocuments.priority] = DocumentPriority.MEDIUM
                                    insert[KycDocuments.complianceLevel] = ComplianceLevel.BASIC
                                    insert[KycDocuments.requiresManualReview] = true
                                    insert[KycDocuments.isRequired] = true
                                    insert[KycDocuments.isConfidential] = true
                                }[KycDocuments.id]
                            }

                            println("   ✅ Document saved to database with ID: $documentId")

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "KYC document uploaded successfully",
                                    data = mapOf(
                                        "id" to documentId.toString(),
                                        "customerId" to customerId,
                                        "documentType" to documentType,
                                        "fileName" to fileName,
                                        "filePath" to filePath,
                                        "status" to "PENDING_REVIEW"
                                    )
                                )
                            )
                        } catch (e: IllegalArgumentException) {
                            println("❌ KYC upload validation error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = e.message ?: "Invalid request",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ KYC upload error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to upload KYC document: ${e.message}",
                                    error = "UPLOAD_ERROR"
                                )
                            )
                        }
                    }

                    // Get KYC documents for a customer
                    get("/kyc/documents/{customerId}") {
                        try {
                            val customerId = call.parameters["customerId"]
                                ?: throw IllegalArgumentException("Customer ID is required")

    //                        println("📋 Fetching KYC documents for customer: $customerId")

                            val documents = transaction {
                                KycDocuments.select { KycDocuments.customerId eq UUID.fromString(customerId) }
                                    .map { row ->
                                        mapOf(
                                            "id" to row[KycDocuments.id].toString(),
                                            "customerId" to row[KycDocuments.customerId].toString(),
                                            "documentType" to row[KycDocuments.documentType].name,
                                            "fileName" to row[KycDocuments.fileName],
                                            "filePath" to row[KycDocuments.filePath],
                                            "uploadDate" to row[KycDocuments.uploadDate].toString(),
                                            "status" to row[KycDocuments.status].name
                                        )
                                    }
                            }

//                            println("   ✅ Found ${documents.size} documents")

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "KYC documents retrieved successfully",
                                    data = documents
                                )
                            )
                        } catch (e: IllegalArgumentException) {
                            println("❌ KYC fetch validation error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = e.message ?: "Invalid request",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ KYC fetch error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to fetch KYC documents: ${e.message}",
                                    error = "FETCH_ERROR"
                                )
                            )
                        }
                    }

                    // Get customers with pending KYC
                    get("/kyc/pending") {
                        try {
                            println("📋 Fetching customers with pending KYC")

                            val pendingCustomers: List<PendingKycCustomerDto> = transaction {
                                Customers.select { Customers.kycStatus eq "PENDING" }
                                    .map { customerRow ->
                                        val customerId = customerRow[Customers.id].value

                                        // Get all documents for this customer
                                        val documents: List<PendingKycDocumentDto> = KycDocuments.select { KycDocuments.customerId eq customerId }
                                            .map { docRow ->
                                                PendingKycDocumentDto(
                                                    id = docRow[KycDocuments.id].toString(),
                                                    documentType = docRow[KycDocuments.documentType].name,
                                                    fileName = docRow[KycDocuments.fileName],
                                                    filePath = docRow[KycDocuments.filePath] ?: "",
                                                    uploadDate = docRow[KycDocuments.uploadDate].toString()
                                                )
                                            }

                                        PendingKycCustomerDto(
                                            customerId = customerId.toString(),
                                            customerName = "${customerRow[Customers.firstName]} ${customerRow[Customers.lastName]}",
                                            customerNumber = customerRow[Customers.customerNumber],
                                            email = customerRow[Customers.email] ?: "",
                                            phoneNumber = customerRow[Customers.phoneNumber] ?: "",
                                            submittedDate = customerRow[Customers.createdAt]?.toString() ?: "",
                                            documents = documents
                                        )
                                    }
                            }

                            println("   ✅ Found ${pendingCustomers.size} customers with pending KYC")

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<List<PendingKycCustomerDto>>(
                                    success = true,
                                    message = "Pending KYC customers retrieved successfully",
                                    data = pendingCustomers
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Error fetching pending KYC: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to fetch pending KYC customers: ${e.message}",
                                    error = "FETCH_ERROR"
                                )
                            )
                        }
                    }

                    // Update customer KYC status
                    post("/kyc/update-status") {
                        try {
                            val request = call.receive<Map<String, String>>()
                            val customerId = request["customerId"] ?: throw IllegalArgumentException("Customer ID required")
                            val newStatus = request["status"] ?: throw IllegalArgumentException("Status required")

                            println("========================================")
                            println("📝 KYC STATUS UPDATE REQUEST RECEIVED")
                            println("   Customer ID: $customerId")
                            println("   New Status: $newStatus")
                            println("   Timestamp: ${LocalDateTime.now()}")
                            println("========================================")

                            // Update database with explicit transaction
                            val updateCount = transaction {
                                val count = Customers.update({ Customers.id eq UUID.fromString(customerId) }) {
                                    it[kycStatus] = newStatus
                                }
                                println("   🔄 Database UPDATE executed - Rows affected: $count")
                                count
                            }

                            // Verify the update
                            val verifiedStatus = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }
                                    .singleOrNull()?.get(Customers.kycStatus)
                            }

                            println("   ✅ KYC status updated successfully!")
                            println("   📊 Rows affected: $updateCount")
                            println("   🔍 Verified status in DB: $verifiedStatus")
                            println("========================================")

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "User verified successfully",
                                    data = mapOf(
                                        "customerId" to customerId,
                                        "status" to newStatus,
                                        "verifiedStatus" to verifiedStatus,
                                        "rowsAffected" to updateCount
                                    )
                                )
                            )
                        } catch (e: IllegalArgumentException) {
                            println("❌ Validation error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = e.message ?: "Invalid request",
                                    error = "VALIDATION_ERROR"
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Error updating KYC status: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to update KYC status: ${e.message}",
                                    error = "UPDATE_ERROR"
                                )
                            )
                        }
                    }

                    // Test endpoint to verify deployment
                    get("/test-deployment") {
                        call.respondText("KYC endpoint deployed - version c5e6e12", ContentType.Text.Plain, HttpStatusCode.OK)
                    }

                    // Profile endpoint (requires valid token, for demo just returns profile by username param)
                    get("/profile/{username}") {
                        try {
                            val username = call.parameters["username"] ?: ""
                            if (username.isBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Username required",
                                        error = "USERNAME_REQUIRED"
                                    )
                                )
                                return@get
                            }
                            val row = transaction {
                                Customers.select { Customers.username eq username }
                                    .singleOrNull()
                            }
                            if (row != null) {
                                // Calculate loan statistics
                                val loanStats = transaction {
                                    val customerId = row[Customers.id].value
                                    val activeLoansData = Loans.select {
                                        (Loans.customerId eq customerId) and (Loans.status eq LoanStatus.ACTIVE)
                                    }.toList()

                                    val totalBorrowed = activeLoansData.sumOf { it[Loans.originalAmount] }
                                    val totalRepaid = activeLoansData.sumOf {
                                        it[Loans.totalPrincipalPaid].add(it[Loans.totalInterestPaid])
                                    }
                                    val activeLoansCount = activeLoansData.count()

                                    Triple(totalBorrowed.toString(), totalRepaid.toString(), activeLoansCount)
                                }

                                val data = CustomerLoginData(
                                    id = row[Customers.id].toString(),
                                    username = row[Customers.username] ?: "",
                                    email = row[Customers.email] ?: "",
                                    firstName = row[Customers.firstName] ?: "",
                                    lastName = row[Customers.lastName] ?: "",
                                    phoneNumber = row[Customers.phoneNumber] ?: "",
                                    customerNumber = row[Customers.customerNumber],
                                    status = row[Customers.status].name,
                                    kycStatus = row[Customers.kycStatus],
                                    creditScore = row[Customers.creditScore],
                                    createdAt = row[Customers.createdAt].toString(),
                                    lastLoginAt = row[Customers.lastLoginAt]?.toString(),
                                    totalBorrowed = loanStats.first,
                                    totalRepaid = loanStats.second,
                                    activeLoans = loanStats.third
                                )
                                call.respond(
                                    HttpStatusCode.OK,
                                    CustomerLoginResponse(
                                        success = true,
                                        message = "Profile retrieved",
                                        data = data,
                                        token = null,
                                        error = null
                                    )
                                )
                            } else {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    CustomerLoginResponse(
                                        success = false,
                                        message = "Customer not found",
                                        error = "NOT_FOUND"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                CustomerLoginResponse(
                                    success = false,
                                    message = "Failed to retrieve profile: ${e.message}",
                                    error = "PROFILE_ERROR"
                                )
                            )
                        }
                    }
                }
            }



            // ==================== USER/EMPLOYEE MANAGEMENT ROUTES ==================== 
            route("/users") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val response = userService.getAllUsers(page, pageSize)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve users",
                                error = e.message
                            )
                        )
                    }
                }

                get("/employees") {
                    try {
                        val response = userService.getAllEmployees()
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve employees",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid user ID",
                                error = "User ID must be a valid UUID"
                            )
                        )
                        return@get
                    }

                    val response = userService.getUserById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                post("/employees") {
                    try {
                        val request = call.receive<CreateEmployeeRequest>()
                        val response = userService.createEmployee(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }

                // Update user permissions
                put("/{id}/permissions") {
                    try {
                        val userId = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (userId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid user ID"
                                )
                            )
                            return@put
                        }

                        @kotlinx.serialization.Serializable
                        data class UpdatePermissionsRequest(val permissions: List<String>)

                        val request = call.receive<UpdatePermissionsRequest>()
                        val response = userService.updateUserPermissions(userId, request.permissions)
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to update permissions",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== EMPLOYEE ROUTES ====================
            route("/employees") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val branchId = call.request.queryParameters["branchId"]?.let { UUID.fromString(it) }
                        val response = employeeService.getAllEmployees(page, pageSize, branchId)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve employees",
                                error = e.message
                            )
                        )
                    }
                }

                post {
                    try {
                        val request = call.receive<CreateEmployeeRequest>()
                        val response = employeeService.createEmployee(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body: ${e.message}",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid employee ID"
                            )
                        )
                        return@get
                    }

                    val response = employeeService.getEmployeeById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                patch("/{id}") {
                    try {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid employee ID"
                                )
                            )
                            return@patch
                        }

                        val request = call.receive<org.dals.project.models.UpdateEmployeeRequest>()
                        val response = employeeService.updateEmployee(id, request)
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}/attendance") {
                    try {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid employee ID"
                                )
                            )
                            return@get
                        }

                        val startDate = call.request.queryParameters["startDate"]
                        val endDate = call.request.queryParameters["endDate"]
                        val response = employeeService.getEmployeeAttendance(id, startDate, endDate)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve attendance",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}/leaves") {
                    try {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid employee ID"
                                )
                            )
                            return@get
                        }

                        val response = employeeService.getEmployeeLeaves(id)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve leaves",
                                error = e.message
                            )
                        )
                    }
                }

                post("/{id}/leaves") {
                    try {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid employee ID"
                                )
                            )
                            return@post
                        }

                        val request = call.receive<org.dals.project.models.CreateLeaveRequest>()
                        val response = employeeService.createLeaveRequest(id, request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }

                post("/leaves/{leaveId}/approve") {
                    try {
                        val leaveId = call.parameters["leaveId"]?.let { UUID.fromString(it) }
                        if (leaveId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid leave ID"
                                )
                            )
                            return@post
                        }

                        val approverIdStr = call.request.queryParameters["approverId"]
                        val approverId = approverIdStr?.let { UUID.fromString(it) } ?: UUID.randomUUID()

                        val response = employeeService.approveLeave(leaveId, approverId)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to approve leave",
                                error = e.message
                            )
                        )
                    }
                }

                post("/leaves/{leaveId}/reject") {
                    try {
                        val leaveId = call.parameters["leaveId"]?.let { UUID.fromString(it) }
                        if (leaveId == null) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Invalid leave ID"
                                )
                            )
                            return@post
                        }

                        val requestBody = call.receive<Map<String, String>>()
                        val reason = requestBody["reason"] ?: "No reason provided"

                        val response = employeeService.rejectLeave(leaveId, reason)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to reject leave",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== CUSTOMER ROUTES ====================
            route("/customers") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val branchId = call.request.queryParameters["branchId"]?.let { UUID.fromString(it) }
                        val response = customerService.getAllCustomers(page, pageSize, branchId)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve customers",
                                error = e.message
                            )
                        )
                    }
                }

                get("/search") {
                    try {
                        val query = call.request.queryParameters["q"] ?: ""
                        val branchId = call.request.queryParameters["branchId"]?.let { UUID.fromString(it) }

                        if (query.isBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Search query is required",
                                    error = "Query parameter 'q' cannot be empty"
                                )
                            )
                            return@get
                        }

                        val response = customerService.searchCustomers(query, branchId)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to search customers",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid customer ID",
                                error = "Customer ID must be a valid UUID"
                            )
                        )
                        return@get
                    }

                    val response = customerService.getCustomerById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                post {
                    try {
                        val request = call.receive<CreateCustomerRequest>()
                        val response = customerService.createCustomer(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== ACCOUNT ROUTES ==================== 
            route("/accounts") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val response = accountService.getAllAccounts(page, pageSize)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve accounts",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid account ID"
                            )
                        )
                        return@get
                    }

                    val response = accountService.getAccountById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                post {
                    try {
                        println("💰 Account creation request received...")
                        println("Content-Type: ${call.request.contentType()}")

                        // Use the original approach with better error handling
                        val request = call.receive<CreateAccountRequest>()
                        println("✅ Parsed account request: $request")

                        val response = accountService.createAccount(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest

                        println("✅ Account creation response: ${response.message}")
                        call.respond(statusCode, response)

                    } catch (e: Exception) {
                        println("❌ Account creation error: ${e.message}")
                        e.printStackTrace()
                        call.respondText(
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.BadRequest,
                            text = "{\"success\":false,\"message\":\"Failed to create account\",\"error\":\"${e.message}\"}"
                        )
                    }
                }
            }

            // ==================== TRANSACTION ROUTES ==================== 
            route("/transactions") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val response = transactionService.getAllTransactions(page, pageSize)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve transactions",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid transaction ID"
                            )
                        )
                        return@get
                    }

                    val response = transactionService.getTransactionById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                post {
                    try {
                        val request = call.receive<CreateTransactionRequest>()
                        val response = transactionService.createTransaction(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }

                // Get transactions by customer ID (for customer app)
                get("/customer/{customerId}") {
                    try {
                        val customerId = call.parameters["customerId"]
                            ?: throw IllegalArgumentException("Customer ID is required")

//                        println("🔍 Fetching transactions for customer: $customerId")

                        val customerUUID = try {
                            UUID.fromString(customerId)
                        } catch (e: IllegalArgumentException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Invalid customer ID format"
                                )
                            )
                            return@get
                        }

                        // Get all accounts for this customer
                        val customerAccounts = transaction {
                            Accounts.select { Accounts.customerId eq customerUUID }
                                .map { it[Accounts.id] }
                        }

                        if (customerAccounts.isEmpty()) {
                            call.respond(
                                HttpStatusCode.OK,
                                ListResponse(
                                    success = true,
                                    message = "No transactions found (no accounts)",
                                    data = emptyList<TransactionDto>(),
                                    total = 0,
                                    page = 1,
                                    pageSize = 10
                                )
                            )
                            return@get
                        }

                        // Get transactions for all customer accounts
                        val transactions = transaction {
                            Transactions
                                .select { Transactions.accountId inList customerAccounts.map { it.value } }
                                .orderBy(Transactions.createdAt, SortOrder.DESC)
                                .limit(50) // Limit to last 50 transactions
                                .map { row ->
                                    TransactionDto(
                                        id = row[Transactions.id].toString(),
                                        accountId = row[Transactions.accountId].toString(),
                                        type = row[Transactions.type].name,
                                        amount = row[Transactions.amount].toPlainString(),
                                        status = row[Transactions.status].name,
                                        description = row[Transactions.description],
                                        timestamp = row[Transactions.timestamp].toString(),
                                        balanceAfter = row[Transactions.balanceAfter].toPlainString(),
                                        fromAccountId = row[Transactions.fromAccountId]?.toString(),
                                        toAccountId = row[Transactions.toAccountId]?.toString(),
                                        reference = row[Transactions.reference],
                                        processedBy = row[Transactions.processedBy]?.toString(),
                                        branchId = row[Transactions.branchId]?.toString(),
                                        checkNumber = row[Transactions.checkNumber],
                                        merchantName = row[Transactions.merchantName],
                                        category = row[Transactions.category],
                                        createdAt = row[Transactions.createdAt].toString()
                                    )
                                }
                        }

//                        println("✅ Found ${transactions.size} transactions for customer $customerId")

                        call.respond(
                            HttpStatusCode.OK,
                            ListResponse(
                                success = true,
                                message = "Transactions retrieved successfully",
                                data = transactions,
                                total = transactions.size,
                                page = 1,
                                pageSize = transactions.size
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error fetching customer transactions: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve transactions",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== LOAN APPLICATION ROUTES ==================== 

            // Update loan application status
            put("/loans/applications/{id}/status") {
                try {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing loan application ID")
                    val request = call.receive<UpdateLoanApplicationStatusRequest>()

                    val response = loanService.updateLoanApplicationStatus(
                        id = java.util.UUID.fromString(id),
                        status = request.status,
                        reviewedBy = java.util.UUID.fromString(request.reviewedBy),
                        approvedAmount = request.approvedAmount,
                        interestRate = request.interestRate,
                        termMonths = request.termMonths,
                        comments = request.comments
                    )

                    if (response.success) {
                        call.respond(HttpStatusCode.OK, response)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, response)
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<LoanApplicationDto>(
                            success = false,
                            message = "Failed to update loan application status: ${e.message}",
                            error = e.message
                        )
                    )
                }
            }
            route("/loan-applications") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val response = loanService.getAllLoanApplications(page, pageSize)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve loan applications",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid loan application ID"
                            )
                        )
                        return@get
                    }

                    val response = loanService.getLoanApplicationById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                post {
                    try {
                        val request = call.receive<CreateLoanApplicationRequest>()
                        val response = loanService.createLoanApplication(request)
                        val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                        call.respond(statusCode, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid request body",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== LOAN ROUTES ====================
            route("/loans") {
                get {
                    try {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                        val response = loanService.getAllLoans(page, pageSize)
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve loans",
                                error = e.message
                            )
                        )
                    }
                }

                // Get all loan applications (for employee app)
                get("/applications") {
                    try {
                        println("🔍 GET /api/v1/loans/applications endpoint called")
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                        println("📊 Fetching loan applications: page=$page, pageSize=$pageSize")

                        val (applications, total) = newSuspendedTransaction {
                            try {
                                val count = LoanApplications.selectAll().count()
                                println("📋 Total loan applications in database: $count")

                                val apps = LoanApplications
                                    .selectAll()
                                    .orderBy(LoanApplications.createdAt, SortOrder.DESC)
                                    .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                                    .map { row ->
                                        LoanApplicationDto(
                                            id = row[LoanApplications.id].toString(),
                                            customerId = row[LoanApplications.customerId].toString(),
                                            loanType = row[LoanApplications.loanType].name,
                                            requestedAmount = row[LoanApplications.requestedAmount].toPlainString(),
                                            purpose = row[LoanApplications.purpose],
                                            status = row[LoanApplications.status].name,
                                            applicationDate = row[LoanApplications.createdAt].toString(),
                                            reviewedBy = row[LoanApplications.reviewedBy]?.toString(),
                                            reviewedDate = row[LoanApplications.reviewedDate]?.toString(),
                                            approvedAmount = row[LoanApplications.approvedAmount]?.toPlainString(),
                                            interestRate = row[LoanApplications.interestRate]?.toPlainString(),
                                            termMonths = row[LoanApplications.termMonths],
                                            collateralDescription = row[LoanApplications.collateralDescription],
                                            collateralValue = row[LoanApplications.collateralValue]?.toPlainString(),
                                            creditScore = row[LoanApplications.creditScore],
                                            annualIncome = row[LoanApplications.annualIncome]?.toPlainString(),
                                            employmentHistory = row[LoanApplications.employmentHistory],
                                            comments = row[LoanApplications.comments],
                                            createdAt = row[LoanApplications.createdAt].toString(),
                                            updatedAt = row[LoanApplications.updatedAt].toString()
                                        )
                                    }

                                println("✅ Successfully mapped ${apps.size} loan applications")
                                Pair(apps, count.toInt())
                            } catch (queryError: Exception) {
                                println("❌ Database query error: ${queryError.message}")
                                queryError.printStackTrace()
                                throw queryError
                            }
                        }

                        println("✅ Returning ${applications.size} loan applications to client")

                        call.respond(
                            HttpStatusCode.OK,
                            ListResponse(
                                success = true,
                                message = "Loan applications retrieved successfully",
                                data = applications,
                                total = total,
                                page = page,
                                pageSize = pageSize
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error in loan applications endpoint: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ListResponse<LoanApplicationDto>(
                                success = false,
                                message = "Failed to retrieve loan applications: ${e.message}",
                                data = emptyList(),
                                total = 0,
                                page = 1,
                                pageSize = 50
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid loan ID"
                            )
                        )
                        return@get
                    }

                    val response = loanService.getLoanById(id)
                    val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                    call.respond(statusCode, response)
                }

                // Get loans by customer ID (for customer app)
                get("/customer/{customerId}") {
                    try {
                        val customerId = call.parameters["customerId"]
                            ?: throw IllegalArgumentException("Customer ID is required")

//                        println("🔍 Fetching loans for customer: $customerId")

                        val customerUUID = try {
                            UUID.fromString(customerId)
                        } catch (e: IllegalArgumentException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Invalid customer ID format"
                                )
                            )
                            return@get
                        }

                        // Get all loans for this customer
                        val loans = newSuspendedTransaction(db = DatabaseFactory.database) {
                            Loans
                                .select { Loans.customerId eq customerUUID }
                                .orderBy(Loans.createdAt, SortOrder.DESC)
                                .map { row ->
                                    LoanDto(
                                        id = row[Loans.id].toString(),
                                        customerId = row[Loans.customerId].toString(),
                                        accountId = row[Loans.accountId]?.toString(),
                                        applicationId = row[Loans.applicationId].toString(),
                                        loanType = row[Loans.loanType].name,
                                        originalAmount = row[Loans.originalAmount].toPlainString(),
                                        currentBalance = row[Loans.currentBalance].toPlainString(),
                                        interestRate = row[Loans.interestRate].toPlainString(),
                                        termMonths = row[Loans.termMonths],
                                        monthlyPayment = row[Loans.monthlyPayment].toPlainString(),
                                        paymentFrequency = row[Loans.paymentFrequency].name,
                                        status = row[Loans.status].name,
                                        originationDate = row[Loans.originationDate].toString(),
                                        maturityDate = row[Loans.maturityDate]?.toString() ?: "",
                                        nextPaymentDate = row[Loans.nextPaymentDate]?.toString() ?: "",
                                        totalInterestPaid = row[Loans.totalInterestPaid].toPlainString(),
                                        totalPrincipalPaid = row[Loans.totalPrincipalPaid].toPlainString(),
                                        latePaymentFees = row[Loans.latePaymentFees].toPlainString(),
                                        loanOfficerId = row[Loans.loanOfficerId].toString(),
                                        branchId = row[Loans.branchId].toString(),
                                        createdAt = row[Loans.createdAt].toString(),
                                        updatedAt = row[Loans.updatedAt].toString()
                                    )
                                }
                        }

//                        println("✅ Found ${loans.size} loans for customer $customerId")

                        call.respond(
                            HttpStatusCode.OK,
                            ListResponse(
                                success = true,
                                message = "Loans retrieved successfully",
                                data = loans,
                                total = loans.size,
                                page = 1,
                                pageSize = loans.size
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error fetching customer loans: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve loans",
                                error = e.message
                            )
                        )
                    }
                }

                // Get loan applications by customer ID (for customer app)
                get("/applications/customer/{customerId}") {
                    try {
                        val customerId = call.parameters["customerId"]
                            ?: throw IllegalArgumentException("Customer ID is required")

//                        println("🔍 Fetching loan applications for customer: $customerId")

                        val customerUUID = try {
                            UUID.fromString(customerId)
                        } catch (e: IllegalArgumentException) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Invalid customer ID format"
                                )
                            )
                            return@get
                        }

                        // Get all loan applications for this customer
                        val applications = transaction {
                            LoanApplications
                                .select { LoanApplications.customerId eq customerUUID }
                                .orderBy(LoanApplications.createdAt, SortOrder.DESC)
                                .map { row ->
                                    LoanApplicationDto(
                                        id = row[LoanApplications.id].toString(),
                                        customerId = row[LoanApplications.customerId].toString(),
                                        loanType = row[LoanApplications.loanType].name,
                                        requestedAmount = row[LoanApplications.requestedAmount].toPlainString(),
                                        purpose = row[LoanApplications.purpose],
                                        status = row[LoanApplications.status].name,
                                        applicationDate = row[LoanApplications.createdAt].toString(),
                                        reviewedBy = row[LoanApplications.reviewedBy]?.toString(),
                                        reviewedDate = row[LoanApplications.reviewedDate]?.toString(),
                                        approvedAmount = row[LoanApplications.approvedAmount]?.toPlainString(),
                                        interestRate = row[LoanApplications.interestRate]?.toPlainString(),
                                        termMonths = row[LoanApplications.termMonths],
                                        collateralDescription = row[LoanApplications.collateralDescription],
                                        collateralValue = row[LoanApplications.collateralValue]?.toPlainString(),
                                        creditScore = row[LoanApplications.creditScore],
                                        annualIncome = row[LoanApplications.annualIncome]?.toPlainString(),
                                        employmentHistory = row[LoanApplications.employmentHistory],
                                        comments = row[LoanApplications.comments],
                                        createdAt = row[LoanApplications.createdAt].toString(),
                                        updatedAt = row[LoanApplications.updatedAt].toString()
                                    )
                                }
                        }

//                        println("✅ Found ${applications.size} loan applications for customer $customerId")

                        call.respond(
                            HttpStatusCode.OK,
                            ListResponse(
                                success = true,
                                message = "Loan applications retrieved successfully",
                                data = applications,
                                total = applications.size,
                                page = 1,
                                pageSize = applications.size
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error fetching customer loan applications: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve loan applications",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== BRANCH ROUTES ==================== 
            route("/branches") {
                get {
                    try {
                        val branches = adminService.getAllBranches()
                        call.respond(HttpStatusCode.OK, branches)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve branches",
                                error = e.message
                            )
                        )
                    }
                }

                get("/{id}") {
                    val id = call.parameters["id"]?.let { UUID.fromString(it) }
                    if (id == null) {
                        call.respond(
                            HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Invalid branch ID"
                            )
                        )
                        return@get
                    }

                    try {
                        val branch = transaction {
                            Branches.select { Branches.id eq id }
                                .singleOrNull()
                                ?.let { row ->
                                    BranchDetails(
                                        id = row[Branches.id].toString(),
                                        branchCode = row[Branches.branchCode],
                                        name = row[Branches.name],
                                        street = row[Branches.street],
                                        city = row[Branches.city],
                                        state = row[Branches.state],
                                        zipCode = row[Branches.zipCode],
                                        country = row[Branches.country],
                                        phoneNumber = row[Branches.phoneNumber],
                                        managerUserId = row[Branches.managerUserId]?.toString(),
                                        operatingHours = row[Branches.operatingHours] ?: "9:00 AM - 5:00 PM",
                                        status = row[Branches.status],
                                        totalCustomers = row[Branches.totalCustomers],
                                        totalAccounts = row[Branches.totalAccounts],
                                        totalDeposits = row[Branches.totalDeposits].toPlainString(),
                                        totalLoans = row[Branches.totalLoans].toPlainString(),
                                        createdAt = row[Branches.createdAt].toString(),
                                        updatedAt = row[Branches.updatedAt].toString()
                                    )
                                }
                        }

                        if (branch != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Branch retrieved successfully",
                                    data = branch
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound, ApiResponse<String>(
                                    success = false,
                                    message = "Branch not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve branch",
                                error = e.message
                            )
                        )
                    }
                }
            }

            // ==================== CUSTOMER CARE ROUTES ==================== 
            route("/customer-care") {
                // Customer management - WORKING ✅
                route("/customers") {
                    get {
                        try {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100
                            val response = customerCareService.getAllCustomers(page, pageSize)
                            call.respond(HttpStatusCode.OK, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve customers",
                                    error = e.message
                                )
                            )
                        }
                    }

                    get("/{id}") {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                CustomerCareResponse<String>(
                                    success = false,
                                    message = "Invalid customer ID",
                                    timestamp = java.time.LocalDateTime.now()
                                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                            )
                            return@get
                        }
                        try {
                            val response = customerCareService.getCustomerById(id)
                            val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                            call.respond(statusCode, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve customer",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Customer profile endpoint - NEW
                    get("/{id}/profile") {
                        val id = call.parameters["id"]?.let { UUID.fromString(it) }
                        if (id == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                CustomerCareResponse<String>(
                                    success = false,
                                    message = "Invalid customer ID",
                                    timestamp = java.time.LocalDateTime.now()
                                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                            )
                            return@get
                        }
                        try {
                            println("🔍 Fetching customer profile for ID: $id")

                            val customerProfile = transaction {
                                Customers
                                    .join(Branches, JoinType.LEFT, Customers.branchId, Branches.id)
                                    .select { Customers.id eq id }
                                    .singleOrNull()
                                    ?.let { row ->
                                        CustomerProfileData(
                                            id = row[Customers.id].toString(),
                                            customerNumber = row[Customers.customerNumber],
                                            username = row[Customers.username] ?: "",
                                            type = row[Customers.type].name,
                                            status = row[Customers.status].name,
                                            firstName = row[Customers.firstName] ?: "",
                                            lastName = row[Customers.lastName] ?: "",
                                            middleName = row[Customers.middleName],
                                            dateOfBirth = row[Customers.dateOfBirth]?.toString(),
                                            ssn = row[Customers.ssn],
                                            email = row[Customers.email] ?: "",
                                            phoneNumber = row[Customers.phoneNumber] ?: "",
                                            alternatePhone = row[Customers.alternatePhone],
                                            primaryAddress = AddressInfo(
                                                street = row[Customers.primaryStreet],
                                                city = row[Customers.primaryCity],
                                                state = row[Customers.primaryState],
                                                zipCode = row[Customers.primaryZipCode],
                                                country = row[Customers.primaryCountry]
                                            ),
                                            mailingAddress = AddressInfo(
                                                street = row[Customers.mailingStreet],
                                                city = row[Customers.mailingCity],
                                                state = row[Customers.mailingState],
                                                zipCode = row[Customers.mailingZipCode],
                                                country = row[Customers.mailingCountry]
                                            ),
                                            occupation = row[Customers.occupation],
                                            employer = row[Customers.employer],
                                            annualIncome = row[Customers.annualIncome]?.toString(),
                                            creditScore = row[Customers.creditScore],
                                            branchId = row[Customers.branchId].toString(),
                                            branchName = row.getOrNull(Branches.name) ?: "Unknown Branch",
                                            accountManagerId = row[Customers.accountManagerId]?.toString(),
                                            onboardedDate = row[Customers.onboardedDate].toString(),
                                            lastContactDate = row[Customers.lastContactDate]?.toString(),
                                            lastLoginAt = row[Customers.lastLoginAt]?.toString(),
                                            riskLevel = row[Customers.riskLevel],
                                            kycStatus = row[Customers.kycStatus],
                                            businessInfo = if (row[Customers.type] == CustomerType.BUSINESS || row[Customers.type] == CustomerType.CORPORATE) {
                                                BusinessInfo(
                                                    businessName = row[Customers.businessName],
                                                    businessType = row[Customers.businessType],
                                                    taxId = row[Customers.taxId],
                                                    businessLicenseNumber = row[Customers.businessLicenseNumber]
                                                )
                                            } else null,
                                            createdAt = row[Customers.createdAt].toString(),
                                            updatedAt = row[Customers.updatedAt].toString()
                                        )
                                    }
                            }

                            if (customerProfile != null) {
                                println("✅ Successfully retrieved customer profile")

                                // Get customer accounts
                                val accounts = transaction {
                                    Accounts
                                        .join(Branches, JoinType.LEFT, Accounts.branchId, Branches.id)
                                        .select { Accounts.customerId eq id }
                                        .map { row ->
                                            ServerAccountDataDto(
                                                id = row[Accounts.id].toString(),
                                                accountNumber = row[Accounts.accountNumber],
                                                customerId = row[Accounts.customerId].toString(),
                                                type = row[Accounts.type].name,
                                                status = row[Accounts.status].name,
                                                balance = row[Accounts.balance].toDouble(),
                                                availableBalance = row[Accounts.availableBalance].toDouble(),
                                                minimumBalance = row[Accounts.minimumBalance].toDouble(),
                                                interestRate = row[Accounts.interestRate].toDouble(),
                                                branchId = row[Accounts.branchId].toString(),
                                                openedDate = row[Accounts.createdAt].toString(),
                                                closedDate = row[Accounts.closedDate]?.toString()
                                            )
                                        }
                                }

                                // Get KYC documents
                                val kycDocuments = transaction {
                                    KycDocuments
                                        .select { KycDocuments.customerId eq id }
                                        .map { row ->
                                            KYCDocumentDto(
                                                id = row[KycDocuments.id].toString(),
                                                customerId = row[KycDocuments.customerId].toString(),
                                                customerName = customerProfile.firstName + " " + customerProfile.lastName,
                                                documentType = row[KycDocuments.documentType].name,
                                                documentNumber = row[KycDocuments.documentNumber] ?: "N/A",
                                                fileName = row[KycDocuments.fileName],
                                                filePath = row[KycDocuments.filePath] ?: "",
                                                uploadedAt = row[KycDocuments.uploadDate].toString(),
                                                uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                                status = row[KycDocuments.status].name,
                                                verifiedAt = row[KycDocuments.verificationDate]?.toString(),
                                                verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                                expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                                rejectionReason = row[KycDocuments.rejectionReason],
                                                notes = row[KycDocuments.notes]
                                            )
                                        }
                                }

                                // Get service requests (if any)
                                val serviceRequests = transaction {
                                    ServiceRequests
                                        .select { ServiceRequests.customerId eq id }
                                        .map { row ->
                                            ServiceRequestDto(
                                                id = row[ServiceRequests.id].toString(),
                                                customerId = row[ServiceRequests.customerId].toString(),
                                                requestType = row[ServiceRequests.requestType],
                                                title = row[ServiceRequests.title],
                                                description = row[ServiceRequests.description],
                                                status = row[ServiceRequests.status],
                                                priority = row[ServiceRequests.priority],
                                                createdBy = row[ServiceRequests.createdBy].toString(),
                                                assignedTo = row[ServiceRequests.assignedTo]?.toString(),
                                                completedBy = row[ServiceRequests.completedBy]?.toString(),
                                                estimatedCompletionDate = row[ServiceRequests.estimatedCompletionDate]?.toString(),
                                                actualCompletionDate = row[ServiceRequests.actualCompletionDate]?.toString(),
                                                rejectionReason = row[ServiceRequests.rejectionReason],
                                                approvalRequired = row[ServiceRequests.approvalRequired],
                                                approvedBy = row[ServiceRequests.approvedBy]?.toString(),
                                                approvedAt = row[ServiceRequests.approvedAt]?.toString(),
                                                createdAt = row[ServiceRequests.createdAt].toString(),
                                                updatedAt = row[ServiceRequests.updatedAt].toString(),
                                                completedAt = row[ServiceRequests.completedAt]?.toString()
                                            )
                                        }
                                }

                                // Get complaints (if any)
                                val complaints = transaction {
                                    Complaints
                                        .select { Complaints.customerId eq id }
                                        .map { row ->
                                            ComplaintResult(
                                                id = row[Complaints.id].toString(),
                                                customerId = row[Complaints.customerId].toString(),
                                                complaintType = row[Complaints.complaintType],
                                                description = row[Complaints.description],
                                                reportedBy = row[Complaints.reportedBy].toString(),
                                                status = row[Complaints.status],
                                                createdAt = row[Complaints.createdAt].toString()
                                            )
                                        }
                                }

                                // Create the comprehensive profile response
                                val profileResponse = ServerCustomerProfileResponseDto(
                                    customer = customerProfile,
                                    accounts = accounts,
                                    serviceRequests = serviceRequests,
                                    complaints = complaints,
                                    kycDocuments = kycDocuments,
                                    complianceChecks = emptyList()
                                )

                                call.respond(
                                    HttpStatusCode.OK,
                                    CustomerCareResponse(
                                        success = true,
                                        message = "Customer profile retrieved successfully",
                                        data = profileResponse,
                                        error = null,
                                        timestamp = java.time.LocalDateTime.now()
                                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    )
                                )
                            } else {
                                println("❌ Customer not found for ID: $id")
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    CustomerCareResponse<String>(
                                        success = false,
                                        message = "Customer not found",
                                        timestamp = java.time.LocalDateTime.now()
                                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            println("❌ Customer profile error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError, CustomerCareResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve customer profile",
                                    error = e.message,
                                    timestamp = java.time.LocalDateTime.now()
                                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                )
                            )
                        }
                    }

                    post {
                        try {
                            val request = call.receive<CreateCustomerRequest>()
                            val response = customerService.createCustomer(request)
                            val statusCode =
                                if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                            call.respond(statusCode, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Invalid request body",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Customer search endpoint - EXISTING
                    post("/search") {
                        try {
                            val searchCriteria = call.receive<Map<String, Any>>()
                            val query = searchCriteria["name"]?.toString()
                                ?: searchCriteria["phoneNumber"]?.toString()
                                ?: searchCriteria["email"]?.toString()
                                ?: searchCriteria["customerNumber"]?.toString()
                                ?: ""

                            if (query.isBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Search query is required",
                                        error = "MISSING_QUERY"
                                    )
                                )
                                return@post
                            }

                            // Search customers using fuzzy matching
                            val customers = transaction {
                                Customers
                                    .selectAll()
                                    .where {
                                        (Customers.firstName.lowerCase() like "%${query.lowercase()}%") or
                                                (Customers.lastName.lowerCase() like "%${query.lowercase()}%") or
                                                (Customers.phoneNumber like "%${query}%") or
                                                (Customers.email.lowerCase() like "%${query.lowercase()}%") or
                                                (Customers.customerNumber like "%${query}%")
                                    }
                                    .limit(50)
                                    .map { row ->
                                        CustomerSearchResult(
                                            id = row[Customers.id].toString(),
                                            customerNumber = row[Customers.customerNumber],
                                            type = row[Customers.type].name,
                                            status = row[Customers.status].name,
                                            firstName = row[Customers.firstName] ?: "",
                                            lastName = row[Customers.lastName] ?: "",
                                            middleName = row[Customers.middleName],
                                            email = row[Customers.email] ?: "",
                                            phoneNumber = row[Customers.phoneNumber] ?: "",
                                            branchId = row[Customers.branchId].toString(),
                                            kycStatus = row[Customers.kycStatus],
                                            riskLevel = row[Customers.riskLevel],
                                            createdAt = row[Customers.createdAt].toString(),
                                            fullName = "${row[Customers.firstName] ?: ""} ${row[Customers.lastName] ?: ""}".trim()
                                        )
                                    }
                            }

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Customer search completed",
                                    data = customers
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Customer search error: ${e.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Search failed: ${e.message}",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // Service requests - WORKING ✅
                route("/service-requests") {
                    get {
                        try {
                            val response = customerCareService.getAllServiceRequests()
                            call.respond(HttpStatusCode.OK, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve service requests",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // Complaints - WORKING ✅
                route("/complaints") {
                    get {
                        try {
                            val response = customerCareService.getAllComplaints()
                            call.respond(HttpStatusCode.OK, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve complaints",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Create complaint endpoint - NEW
                    post {
                        try {
                            val request = call.receive<Map<String, Any>>()

                            val customerId = request["customerId"]?.toString()
                                ?: throw IllegalArgumentException("Customer ID is required")
                            val complaintType = request["complaintType"]?.toString()
                                ?: throw IllegalArgumentException("Complaint type is required")
                            val description = request["description"]?.toString()
                                ?: throw IllegalArgumentException("Complaint description is required")
                            val reportedBy = request["reportedBy"]?.toString()
                                ?: throw IllegalArgumentException("Reported by user ID is required")

                            val complaintId = transaction {
                                Complaints.insert { insert ->
                                    insert[Complaints.customerId] = UUID.fromString(customerId)
                                    insert[Complaints.complaintType] = complaintType
                                    insert[Complaints.description] = description
                                    insert[Complaints.reportedBy] = UUID.fromString(reportedBy)
                                    insert[Complaints.status] = "OPEN"
                                }[Complaints.id]
                            }

                            val createdComplaint = transaction {
                                Complaints
                                    .select { Complaints.id eq complaintId }
                                    .singleOrNull()
                                    ?.let { row ->
                                        ComplaintResult(
                                            id = row[Complaints.id].toString(),
                                            customerId = row[Complaints.customerId].toString(),
                                            complaintType = row[Complaints.complaintType],
                                            description = row[Complaints.description],
                                            reportedBy = row[Complaints.reportedBy].toString(),
                                            status = row[Complaints.status],
                                            createdAt = row[Complaints.createdAt].toString()
                                        )
                                    }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Complaint filed successfully",
                                    data = createdComplaint
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Complaint creation error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to create complaint",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // Transaction Reversals - NEW
                route("/transaction-reversals") {
                    // Create transaction reversal request
                    post {
                        try {
                            val request = call.receive<Map<String, Any>>()

                            val transactionId = request["transactionId"]?.toString()
                                ?: throw IllegalArgumentException("Transaction ID is required")
                            val customerId = request["customerId"]?.toString()
                                ?: throw IllegalArgumentException("Customer ID is required")
                            val reason = request["reason"]?.toString()
                                ?: throw IllegalArgumentException("Reason is required")
                            val reversalType = request["reversalType"]?.toString() ?: "FULL"
                            val createdBy = request["createdBy"]?.toString()
                                ?: throw IllegalArgumentException("Created by user ID is required")

                            println("🔄 Creating reversal request for transaction: $transactionId")

                            // Verify transaction exists
                            val transactionExists = transaction {
                                Transactions.select { Transactions.id eq UUID.fromString(transactionId) }.count() > 0
                            }

                            if (!transactionExists) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Transaction not found",
                                        error = "TRANSACTION_NOT_FOUND"
                                    )
                                )
                                return@post
                            }

                            // Verify customer exists
                            val customerExists = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }.count() > 0
                            }

                            if (!customerExists) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Customer not found",
                                        error = "CUSTOMER_NOT_FOUND"
                                    )
                                )
                                return@post
                            }

                            // Create reversal request (storing in ServiceRequests table for now)
                            val reversalId = transaction {
                                ServiceRequests.insert { insert ->
                                    insert[ServiceRequests.customerId] = UUID.fromString(customerId)
                                    insert[ServiceRequests.requestType] = "TRANSACTION_REVERSAL"
                                    insert[ServiceRequests.description] = "Reversal Type: $reversalType | Reason: $reason | Transaction ID: $transactionId"
                                    insert[ServiceRequests.priority] = "HIGH"
                                    insert[ServiceRequests.status] = "PENDING"
                                    insert[ServiceRequests.assignedTo] = UUID.fromString(createdBy)
                                }[ServiceRequests.id]
                            }

                            println("✅ Reversal request created with ID: $reversalId")

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Transaction reversal request created successfully",
                                    data = mapOf(
                                        "id" to reversalId.toString(),
                                        "transactionId" to transactionId,
                                        "customerId" to customerId,
                                        "reversalType" to reversalType,
                                        "status" to "PENDING"
                                    )
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Reversal creation error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to create reversal request: ${e.message}",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // KYC Documents - WORKING ✅
                route("/kyc-documents") {
                    // Upload new KYC document
                    post {
                        try {
                            val request = call.receive<Map<String, Any>>()

                            val customerId = request["customerId"]?.toString()
                                ?: throw IllegalArgumentException("Customer ID is required")
                            val documentType = request["documentType"]?.toString()
                                ?: throw IllegalArgumentException("Document type is required")
                            val fileName = request["fileName"]?.toString()
                                ?: throw IllegalArgumentException("File name is required")
                            val uploadedBy = request["uploadedBy"]?.toString()
                                ?: throw IllegalArgumentException("Uploaded by is required")

                            // Get customer name
                            val customerRow = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }.singleOrNull()
                            }
                            val customerName =
                                if (customerRow != null) "${customerRow[Customers.firstName]} ${customerRow[Customers.lastName]}"
                                else "Unknown Customer"

                            // Insert document into database
                            val documentId = transaction {
                                KycDocuments.insert { insert ->
                                    insert[KycDocuments.customerId] = UUID.fromString(customerId)
                                    insert[KycDocuments.customerName] = customerName
                                    insert[KycDocuments.documentType] = KycDocumentType.valueOf(documentType)
                                    insert[KycDocuments.fileName] = fileName
                                    insert[KycDocuments.filePath] =
                                        request["filePath"]?.toString() ?: "/uploads/kyc/$customerId/$fileName"
                                    insert[KycDocuments.documentNumber] = request["documentNumber"]?.toString()
                                    insert[KycDocuments.uploadedBy] = UUID.randomUUID() // Should be current user ID
                                    insert[KycDocuments.status] = KycDocumentStatus.PENDING_REVIEW
                                    insert[KycDocuments.priority] = DocumentPriority.MEDIUM
                                    insert[KycDocuments.expiryDate] = request["expiryDate"]?.toString()?.let {
                                        java.time.LocalDate.parse(it)
                                    }
                                    insert[KycDocuments.notes] = request["notes"]?.toString()
                                    insert[KycDocuments.requiresManualReview] = true
                                }[KycDocuments.id]
                            }

                            // Get the created document
                            val createdDocument = transaction {
                                KycDocuments
                                    .join(Customers, JoinType.INNER, KycDocuments.customerId, Customers.id)
                                    .select { KycDocuments.id eq documentId }
                                    .single()
                                    .let { row ->
                                        KYCDocumentDto(
                                            id = row[KycDocuments.id].toString(),
                                            customerId = row[KycDocuments.customerId].toString(),
                                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                                            documentType = row[KycDocuments.documentType].name,
                                            documentNumber = row[KycDocuments.documentNumber] ?: "N/A",
                                            fileName = row[KycDocuments.fileName],
                                            filePath = row[KycDocuments.filePath] ?: "",
                                            uploadedAt = row[KycDocuments.uploadDate].toString(),
                                            uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                            status = row[KycDocuments.status].name,
                                            verifiedAt = row[KycDocuments.verificationDate]?.toString(),
                                            verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                            expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                            rejectionReason = row[KycDocuments.rejectionReason],
                                            notes = row[KycDocuments.notes]
                                        )
                                    }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "KYC document uploaded successfully",
                                    data = createdDocument
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ KYC upload error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to upload KYC document",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Get all KYC documents for customer care dashboard
                    get {
                        try {
                            println("🔍 GET KYC documents request received")
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                            val kycDocuments = transaction {
                                KycDocuments
                                    .join(Customers, JoinType.INNER, KycDocuments.customerId, Customers.id)
                                    .selectAll()
                                    .orderBy(KycDocuments.uploadDate to SortOrder.DESC)
                                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                                    .map { row ->
                                        KYCDocumentDto(
                                            id = row[KycDocuments.id].toString(),
                                            customerId = row[KycDocuments.customerId].toString(),
                                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                                            documentType = row[KycDocuments.documentType].name,
                                            documentNumber = row[KycDocuments.documentNumber] ?: "N/A",
                                            fileName = row[KycDocuments.fileName],
                                            filePath = row[KycDocuments.filePath] ?: "",
                                            uploadedAt = row[KycDocuments.uploadDate].toString(),
                                            uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                            status = row[KycDocuments.status].name,
                                            verifiedAt = row[KycDocuments.verificationDate]?.toString(),
                                            verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                            expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                            rejectionReason = row[KycDocuments.rejectionReason],
                                            notes = row[KycDocuments.notes]
                                        )
                                    }
                            }

                            println("✅ Retrieved ${kycDocuments.size} KYC documents from database")

                            @kotlinx.serialization.Serializable
                            data class KYCDocumentsResponse(
                                val documents: List<KYCDocumentDto>,
                                val total: Int,
                                val page: Int,
                                val pageSize: Int
                            )

                            val responseData = KYCDocumentsResponse(
                                documents = kycDocuments,
                                total = kycDocuments.size,
                                page = page,
                                pageSize = pageSize
                            )

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "KYC documents retrieved successfully",
                                    data = responseData
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ KYC documents retrieval error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve KYC documents",
                                    error = e.message
                                )
                            )
                        }
                    }

                    route("/customer") {
                        get("/{customerId}") {
                            try {
                                val customerId = call.parameters["customerId"]
                                    ?: throw IllegalArgumentException("Customer ID is required")

                                val kycDocuments = transaction {
                                    KycDocuments.select { KycDocuments.customerId eq UUID.fromString(customerId) }
                                        .map { row ->
                                            KYCDocumentDto(
                                                id = row[KycDocuments.id].toString(),
                                                customerId = row[KycDocuments.customerId].toString(),
                                                documentType = row[KycDocuments.documentType].name,
                                                documentNumber = row[KycDocuments.documentNumber] ?: "N/A",
                                                fileName = row[KycDocuments.fileName],
                                                filePath = row[KycDocuments.filePath] ?: "",
                                                uploadedAt = row[KycDocuments.uploadDate].toString(),
                                                uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                                status = row[KycDocuments.status].name,
                                                verifiedAt = row[KycDocuments.verificationDate]?.toString(),
                                                verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                                expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                                rejectionReason = row[KycDocuments.rejectionReason],
                                                notes = row[KycDocuments.notes]
                                            )
                                        }
                                }

                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        message = "Customer KYC documents retrieved successfully",
                                        data = kycDocuments
                                    )
                                )
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.InternalServerError, ApiResponse<String>(
                                        success = false,
                                        message = "Failed to retrieve customer KYC documents",
                                        error = e.message
                                    )
                                )
                            }
                        }
                    }

                    // Update KYC document status
                    patch("/{documentId}/status") {
                        try {
                            val documentId = call.parameters["documentId"]
                                ?: throw IllegalArgumentException("Document ID is required")

                            @kotlinx.serialization.Serializable
                            data class KYCStatusUpdateRequest(
                                val status: String,
                                val rejectionReason: String? = null
                            )

                            val req = call.receive<KYCStatusUpdateRequest>()
                            val newStatus = req.status
                            val rejectionReasonText = req.rejectionReason

                            // Get the document and customer info first
                            val documentInfo = transaction {
                                KycDocuments
                                    .join(Customers, JoinType.INNER, KycDocuments.customerId, Customers.id)
                                    .select { KycDocuments.id eq UUID.fromString(documentId) }
                                    .singleOrNull()
                            }

                            if (documentInfo == null) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "KYC document not found",
                                        error = "DOCUMENT_NOT_FOUND"
                                    )
                                )
                                return@patch
                            }

                            val customerId = documentInfo[KycDocuments.customerId]
                            val customerKycStatus = documentInfo[Customers.kycStatus]

                            // Update the document status
                            transaction {
                                KycDocuments.update({ KycDocuments.id eq UUID.fromString(documentId) }) { update ->
                                    update[status] = KycDocumentStatus.valueOf(newStatus)
                                    update[verificationDate] =
                                        if (newStatus == "VERIFIED") java.time.LocalDate.now() else null
                                    update[verifiedBy] =
                                        if (newStatus == "VERIFIED") UUID.randomUUID() else null
                                    if (rejectionReasonText != null) {
                                        update[rejectionReason] = rejectionReasonText
                                    }
                                }
                            }

                            // If document is verified and customer KYC is not already verified, update customer status
                            var accountCreated = false
                            if (newStatus == "VERIFIED" && customerKycStatus != "VERIFIED") {
                                transaction {
                                    // Update customer KYC status to VERIFIED
                                    Customers.update({ Customers.id eq customerId }) {
                                        it[kycStatus] = "VERIFIED"
                                    }
                                }

                                // Check if customer has any active accounts
                                val existingAccounts = transaction {
                                    Accounts.select {
                                        (Accounts.customerId eq customerId) and
                                                (Accounts.status eq AccountStatus.ACTIVE)
                                    }.count()
                                }

                                // If no active accounts exist, create a default checking account
                                if (existingAccounts == 0L) {
                                    // Generate 7-digit numeric account number (1000000 to 9999999)
                                    val accountNumber = (1000000..9999999).random().toString()
                                    val customerBranchId = documentInfo[Customers.branchId]

                                    transaction {
                                        Accounts.insert { insert ->
                                            insert[Accounts.customerId] = customerId
                                            insert[Accounts.accountNumber] = accountNumber
                                            insert[Accounts.type] = AccountType.CHECKING
                                            insert[Accounts.balance] = java.math.BigDecimal("0.00")
                                            insert[Accounts.availableBalance] = java.math.BigDecimal("0.00")
                                            insert[Accounts.status] = AccountStatus.ACTIVE
                                            insert[Accounts.branchId] = customerBranchId
                                            insert[Accounts.interestRate] = java.math.BigDecimal("0.02") // 2% default
                                            insert[Accounts.minimumBalance] = java.math.BigDecimal("50.00")
                                            insert[Accounts.openedDate] = java.time.LocalDate.now()
                                        }
                                    }

                                    println("✅ Auto-created checking account $accountNumber for verified customer $customerId")
                                    accountCreated = true
                                }
                            }

                            val updateResult = KYCStatusUpdateResult(
                                documentId = documentId,
                                newStatus = newStatus,
                                customerKycStatus = if (newStatus == "VERIFIED") "VERIFIED" else customerKycStatus,
                                accountCreated = accountCreated
                            )

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "KYC document status updated successfully" +
                                            if (newStatus == "VERIFIED" && customerKycStatus != "VERIFIED") " and customer account activated" else "",
                                    data = updateResult
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ KYC status update error: ${e.message}")
                            call.respond(
                                HttpStatusCode.BadRequest, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to update KYC document status: ${e.message}",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // Dashboard metrics - WORKING ✅
                route("/dashboard") {
                    get("/metrics") {
                        try {
                            val response = customerCareService.getCustomerServiceMetrics()
                            call.respond(HttpStatusCode.OK, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError, ApiResponse<String>(
                                    success = false,
                                    message = "Failed to retrieve customer service metrics",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                // Utility endpoint to create accounts for verified customers without accounts
                post("/create-missing-accounts") {
                    try {
                        // Find all verified customers without active accounts
                        val customersNeedingAccounts = transaction {
                            Customers.select {
                                Customers.kycStatus eq "VERIFIED"
                            }.mapNotNull { customerRow ->
                                val customerId = customerRow[Customers.id].value
                                val hasAccount = Accounts.select {
                                    (Accounts.customerId eq customerId) and
                                    (Accounts.status eq AccountStatus.ACTIVE)
                                }.count() > 0

                                if (!hasAccount) {
                                    customerId to customerRow[Customers.branchId]
                                } else {
                                    null
                                }
                            }
                        }

                        val createdAccounts = mutableListOf<String>()

                        customersNeedingAccounts.forEach { (customerId, branchId) ->
                            try {
                                val accountNumber = (1000000..9999999).random().toString()
                                transaction {
                                    Accounts.insert { insert ->
                                        insert[Accounts.customerId] = customerId
                                        insert[Accounts.accountNumber] = accountNumber
                                        insert[Accounts.type] = AccountType.CHECKING
                                        insert[Accounts.balance] = java.math.BigDecimal("0.00")
                                        insert[Accounts.availableBalance] = java.math.BigDecimal("0.00")
                                        insert[Accounts.status] = AccountStatus.ACTIVE
                                        insert[Accounts.branchId] = branchId
                                        insert[Accounts.interestRate] = java.math.BigDecimal("0.02")
                                        insert[Accounts.minimumBalance] = java.math.BigDecimal("50.00")
                                        insert[Accounts.openedDate] = java.time.LocalDate.now()
                                    }
                                }
                                createdAccounts.add("Customer $customerId -> Account $accountNumber")
                                println("✅ Created account $accountNumber for customer $customerId")
                            } catch (e: Exception) {
                                println("❌ Failed to create account for customer $customerId: ${e.message}")
                            }
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                message = "Created ${createdAccounts.size} accounts for verified customers",
                                data = createdAccounts
                            )
                        )
                    } catch (e: Exception) {
                        println("❌ Error creating missing accounts: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(
                                success = false,
                                message = "Failed to create missing accounts: ${e.message}",
                                error = e.message
                            )
                        )
                    }
                }

                // Bank Account Opening Service - NEW ✅
                route("/accounts") {
                    // Create account endpoint (alias for /open for compatibility)
                    post {
                        try {
                            val request = call.receive<AccountOpeningRequest>()

                            val customerId = request.customerId
                            val accountType = request.accountType
                            val initialDeposit = request.initialDeposit
                            val branchId = request.branchId
                            val openedBy = request.openedBy

                            // Validate customer exists
                            val customerExists = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }
                                    .singleOrNull() != null
                            }

                            if (!customerExists) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Customer not found",
                                        error = "CUSTOMER_NOT_FOUND"
                                    )
                                )
                                return@post
                            }

                            // Generate account number
                            val accountNumber = IdGenerator.generateAccountNumber()

                            // Create account in database
                            val accountId = transaction {
                                Accounts.insert { insert ->
                                    insert[Accounts.customerId] = UUID.fromString(customerId)
                                    insert[Accounts.accountNumber] = accountNumber
                                    insert[Accounts.type] = AccountType.valueOf(accountType)
                                    insert[Accounts.balance] = java.math.BigDecimal(initialDeposit)
                                    insert[Accounts.availableBalance] = java.math.BigDecimal(initialDeposit)
                                    insert[Accounts.status] = AccountStatus.ACTIVE
                                    insert[Accounts.branchId] = UUID.fromString(branchId)
                                    insert[Accounts.interestRate] = java.math.BigDecimal("0.02") // 2% default
                                    insert[Accounts.minimumBalance] = when (AccountType.valueOf(accountType)) {
                                        AccountType.SAVINGS -> java.math.BigDecimal("100.00")
                                        AccountType.CHECKING -> java.math.BigDecimal("50.00")
                                        AccountType.BUSINESS_CHECKING -> java.math.BigDecimal("500.00")
                                        AccountType.BUSINESS_SAVINGS -> java.math.BigDecimal("1000.00")
                                        else -> java.math.BigDecimal("0.00")
                                    }
                                    insert[Accounts.openedDate] = java.time.LocalDate.now()
                                }[Accounts.id]
                            }

                            // If initial deposit > 0, create initial deposit transaction
                            if (initialDeposit > 0) {
                                transaction {
                                    Transactions.insert { insert ->
                                        insert[Transactions.accountId] = accountId as UUID
                                        insert[Transactions.type] = TransactionType.DEPOSIT
                                        insert[Transactions.amount] = java.math.BigDecimal(initialDeposit)
                                        insert[Transactions.description] = "Initial deposit - Account opening"
                                        insert[Transactions.status] = TransactionStatus.COMPLETED
                                        insert[Transactions.processedBy] = UUID.fromString(openedBy)
                                        insert[Transactions.balanceAfter] = java.math.BigDecimal(initialDeposit)
                                    }
                                }
                            }

                            // Get created account details with customer info
                            val accountDetails = transaction {
                                Accounts
                                    .join(Customers, JoinType.INNER, Accounts.customerId, Customers.id)
                                    .join(Branches, JoinType.LEFT, Accounts.branchId, Branches.id)
                                    .select { Accounts.id eq accountId }
                                    .single()
                                    .let { row ->
                                        AccountOpeningResult(
                                            accountId = row[Accounts.id].toString(),
                                            accountNumber = row[Accounts.accountNumber],
                                            type = row[Accounts.type].name,
                                            customerId = row[Accounts.customerId].toString(),
                                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                                            balance = row[Accounts.balance].toPlainString(),
                                            status = row[Accounts.status].name,
                                            branchId = row[Accounts.branchId].toString(),
                                            branchName = row.getOrNull(Branches.name) ?: "Unknown Branch",
                                            interestRate = row[Accounts.interestRate].toPlainString(),
                                            minimumBalance = row[Accounts.minimumBalance].toPlainString(),
                                            createdAt = row[Accounts.createdAt].toString(),
                                            initialDeposit = initialDeposit
                                        )
                                    }
                            }

                            // Create audit log
                            transaction {
                                AuditLogs.insert { insert ->
                                    insert[AuditLogs.userId] = UUID.fromString(openedBy)
                                    insert[AuditLogs.action] = "ACCOUNT_OPENED"
                                    insert[AuditLogs.entityType] = "accounts"
                                    insert[AuditLogs.entityId] = accountId.toString()
                                    insert[AuditLogs.description] =
                                        "Opened $accountType account $accountNumber for customer $customerId with initial deposit $initialDeposit"
                                    insert[AuditLogs.ipAddress] = call.request.local.remoteHost
                                }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Bank account opened successfully",
                                    data = accountDetails
                                )
                            )

                        } catch (e: Exception) {
                            println("❌ Account opening error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to open bank account",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Open new bank account for customer
                    post("/open") {
                        try {
                            val request = call.receive<AccountOpeningRequest>()

                            val customerId = request.customerId
                            val accountType = request.accountType
                            val initialDeposit = request.initialDeposit
                            val branchId = request.branchId
                            val openedBy = request.openedBy

                            // Validate customer exists
                            val customerExists = transaction {
                                Customers.select { Customers.id eq UUID.fromString(customerId) }
                                    .singleOrNull() != null
                            }

                            if (!customerExists) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Customer not found",
                                        error = "CUSTOMER_NOT_FOUND"
                                    )
                                )
                                return@post
                            }

                            // Generate account number
                            val accountNumber = IdGenerator.generateAccountNumber()

                            // Create account in database
                            val accountId = transaction {
                                Accounts.insert { insert ->
                                    insert[Accounts.customerId] = UUID.fromString(customerId)
                                    insert[Accounts.accountNumber] = accountNumber
                                    insert[Accounts.type] = AccountType.valueOf(accountType)
                                    insert[Accounts.balance] = java.math.BigDecimal(initialDeposit)
                                    insert[Accounts.availableBalance] = java.math.BigDecimal(initialDeposit)
                                    insert[Accounts.status] = AccountStatus.ACTIVE
                                    insert[Accounts.branchId] = UUID.fromString(branchId)
                                    insert[Accounts.interestRate] = java.math.BigDecimal("0.02") // 2% default
                                    insert[Accounts.minimumBalance] = when (AccountType.valueOf(accountType)) {
                                        AccountType.SAVINGS -> java.math.BigDecimal("100.00")
                                        AccountType.CHECKING -> java.math.BigDecimal("50.00")
                                        AccountType.BUSINESS_CHECKING -> java.math.BigDecimal("500.00")
                                        AccountType.BUSINESS_SAVINGS -> java.math.BigDecimal("1000.00")
                                        else -> java.math.BigDecimal("0.00")
                                    }
                                    insert[Accounts.openedDate] = java.time.LocalDate.now()
                                }[Accounts.id]
                            }

                            // If initial deposit > 0, create initial deposit transaction
                            if (initialDeposit > 0) {
                                transaction {
                                    Transactions.insert { insert ->
                                        insert[Transactions.accountId] = accountId as UUID
                                        insert[Transactions.type] = TransactionType.DEPOSIT
                                        insert[Transactions.amount] = java.math.BigDecimal(initialDeposit)
                                        insert[Transactions.description] = "Initial deposit - Account opening"
                                        insert[Transactions.status] = TransactionStatus.COMPLETED
                                        insert[Transactions.processedBy] = UUID.fromString(openedBy)
                                        insert[Transactions.balanceAfter] = java.math.BigDecimal(initialDeposit)
                                    }
                                }
                            }

                            // Get created account details with customer info
                            val accountDetails = transaction {
                                Accounts
                                    .join(Customers, JoinType.INNER, Accounts.customerId, Customers.id)
                                    .join(Branches, JoinType.LEFT, Accounts.branchId, Branches.id)
                                    .select { Accounts.id eq accountId }
                                    .single()
                                    .let { row ->
                                        AccountOpeningResult(
                                            accountId = row[Accounts.id].toString(),
                                            accountNumber = row[Accounts.accountNumber],
                                            type = row[Accounts.type].name,
                                            customerId = row[Accounts.customerId].toString(),
                                            customerName = "${row[Customers.firstName]} ${row[Customers.lastName]}",
                                            balance = row[Accounts.balance].toPlainString(),
                                            status = row[Accounts.status].name,
                                            branchId = row[Accounts.branchId].toString(),
                                            branchName = row.getOrNull(Branches.name) ?: "Unknown Branch",
                                            interestRate = row[Accounts.interestRate].toPlainString(),
                                            minimumBalance = row[Accounts.minimumBalance].toPlainString(),
                                            createdAt = row[Accounts.createdAt].toString(),
                                            initialDeposit = initialDeposit
                                        )
                                    }
                            }

                            // Create audit log
                            transaction {
                                AuditLogs.insert { insert ->
                                    insert[AuditLogs.userId] = UUID.fromString(openedBy)
                                    insert[AuditLogs.action] = "ACCOUNT_OPENED"
                                    insert[AuditLogs.entityType] = "accounts"
                                    insert[AuditLogs.entityId] = accountId.toString()
                                    insert[AuditLogs.description] =
                                        "Opened $accountType account $accountNumber for customer $customerId with initial deposit $initialDeposit"
                                    insert[AuditLogs.ipAddress] = call.request.local.remoteHost
                                }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Bank account opened successfully",
                                    data = accountDetails
                                )
                            )

                        } catch (e: Exception) {
                            println("❌ Account opening error: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to open bank account",
                                    error = e.message
                                )
                            )
                        }
                    }

                    // Get customer accounts
                    get("/customer/{customerId}") {
                        try {
                            val customerId = call.parameters["customerId"]
                                ?: throw IllegalArgumentException("Customer ID is required")

//                            println("🔍 Fetching accounts for customer: $customerId")

                            // Validate that the customer ID is a valid UUID
                            val customerUUID = try {
                                UUID.fromString(customerId)
                            } catch (e: IllegalArgumentException) {
                                println("❌ Invalid UUID format for customer ID: $customerId")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    CustomerCareListResponse<ServerAccountDataDto>(
                                        success = false,
                                        message = "Invalid customer ID format",
                                        data = emptyList(),
                                        total = 0,
                                        page = 1,
                                        pageSize = 10,
                                        timestamp = java.time.LocalDateTime.now().toString()
                                    )
                                )
                                return@get
                            }

                            // First, verify the customer exists
                            val customerExists = transaction {
                                Customers.select { Customers.id eq customerUUID }.singleOrNull()
                            }

                            if (customerExists == null) {
                                println("❌ Customer not found: $customerId")
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    CustomerCareListResponse<ServerAccountDataDto>(
                                        success = false,
                                        message = "Customer not found",
                                        data = emptyList(),
                                        total = 0,
                                        page = 1,
                                        pageSize = 10,
                                        timestamp = java.time.LocalDateTime.now().toString()
                                    )
                                )
                                return@get
                            }

                            val customerKycStatus = customerExists[Customers.kycStatus]
//                            println("📋 Customer $customerId found with KYC status: $customerKycStatus")

                            var accounts: List<ServerAccountDataDto> = transaction {
                                Accounts
                                    .select { Accounts.customerId eq customerUUID }
                                    .map { row ->
                                        ServerAccountDataDto(
                                            id = row[Accounts.id].toString(),
                                            accountNumber = row[Accounts.accountNumber],
                                            customerId = row[Accounts.customerId].toString(),
                                            type = row[Accounts.type].name,
                                            status = row[Accounts.status].name,
                                            balance = row[Accounts.balance].toDouble(),
                                            availableBalance = row[Accounts.availableBalance].toDouble(),
                                            minimumBalance = row[Accounts.minimumBalance].toDouble(),
                                            interestRate = row[Accounts.interestRate].toDouble(),
                                            branchId = row[Accounts.branchId].toString(),
                                            openedDate = row[Accounts.createdAt].toString(),
                                            closedDate = row[Accounts.closedDate]?.toString()
                                        )
                                    }
                            }

//                            println("✅ Found ${accounts.size} accounts for customer $customerId")

                            // If customer is verified but has no accounts, create one automatically
                            var accountAutoCreated = false
                            if (accounts.isEmpty() && customerKycStatus == "VERIFIED") {
//                                println("🔧 Creating default account for verified customer...")

                                val accountNumber = IdGenerator.generateAccountNumber()
                                val customerBranchId = customerExists[Customers.branchId]

                                try {
                                    val newAccountId = transaction {
                                        Accounts.insert { insert ->
                                            insert[Accounts.customerId] = customerUUID
                                            insert[Accounts.accountNumber] = accountNumber
                                            insert[Accounts.type] = AccountType.CHECKING
                                            insert[Accounts.balance] = java.math.BigDecimal("0.00")
                                            insert[Accounts.availableBalance] = java.math.BigDecimal("0.00")
                                            insert[Accounts.status] = AccountStatus.ACTIVE
                                            insert[Accounts.branchId] = customerBranchId
                                            insert[Accounts.interestRate] = java.math.BigDecimal("0.02")
                                            insert[Accounts.minimumBalance] = java.math.BigDecimal("50.00")
                                            insert[Accounts.openedDate] = java.time.LocalDate.now()
                                        }[Accounts.id]
                                    }

                                    println("✅ Auto-created account $accountNumber for customer $customerId")

                                    // Return the newly created account
                                    val newAccount = transaction {
                                        Accounts
                                            .select { Accounts.id eq newAccountId }
                                            .single()
                                            .let { row ->
                                                ServerAccountDataDto(
                                                    id = row[Accounts.id].toString(),
                                                    accountNumber = row[Accounts.accountNumber],
                                                    customerId = row[Accounts.customerId].toString(),
                                                    type = row[Accounts.type].name,
                                                    status = row[Accounts.status].name,
                                                    balance = row[Accounts.balance].toDouble(),
                                                    availableBalance = row[Accounts.availableBalance].toDouble(),
                                                    minimumBalance = row[Accounts.minimumBalance].toDouble(),
                                                    interestRate = row[Accounts.interestRate].toDouble(),
                                                    branchId = row[Accounts.branchId].toString(),
                                                    openedDate = row[Accounts.createdAt].toString(),
                                                    closedDate = row[Accounts.closedDate]?.toString()
                                                )
                                            }
                                    }
                                    accounts = listOf(newAccount)
                                    accountAutoCreated = true
                                } catch (accountCreationError: Exception) {
                                    println("❌ Failed to auto-create account: ${accountCreationError.message}")
                                    accountCreationError.printStackTrace()
                                }
                            }

                            call.respond(
                                HttpStatusCode.OK,
                                CustomerCareListResponse(
                                    success = true,
                                    message = if (accountAutoCreated) "Customer accounts retrieved (account auto-created)" else "Customer accounts retrieved successfully",
                                    data = accounts,
                                    total = accounts.size,
                                    page = 1,
                                    pageSize = accounts.size,
                                    timestamp = java.time.LocalDateTime.now().toString()
                                )
                            )

                        } catch (e: Exception) {
                            println("❌ Error fetching customer accounts: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                CustomerCareListResponse<ServerAccountDataDto>(
                                    success = false,
                                    message = "Failed to retrieve customer accounts: ${e.message}",
                                    data = emptyList(),
                                    total = 0,
                                    page = 1,
                                    pageSize = 10,
                                    timestamp = java.time.LocalDateTime.now().toString()
                                )
                            )
                        }
                    }

                    // Get account types available for opening
                    get("/types") {
                        println("✅ Account types request received")
                        try {
                            val accountTypes = transaction {
                                AccountTypes.selectAll()
                                    .where { AccountTypes.isActive eq true }
                                    .map { row ->
                                        val typeName = row[AccountTypes.typeName]
                                        val displayName = row[AccountTypes.displayName]
                                        val minimumDeposit = row[AccountTypes.minimumDeposit].toPlainString()
                                        val interestRate = "${(row[AccountTypes.interestRate].toDouble() * 100)}%"

                                        // Parse features field properly  
                                        val rawFeatures = row[AccountTypes.features]
                                        val featuresArray = try {
                                            if (rawFeatures != null && rawFeatures.startsWith("[") && rawFeatures.endsWith(
                                                    "]"
                                                )
                                            ) {
                                                // Parse JSON array manually
                                                rawFeatures.removeSurrounding("[", "]")
                                                    .split(",")
                                                    .map { it.trim().removeSurrounding("\"") }
                                            } else if (rawFeatures != null) {
                                                // Single feature, create list
                                                listOf(rawFeatures)
                                            } else {
                                                // Default features
                                                listOf("Mobile Banking", "ATM Access", "Online Statements")
                                            }
                                        } catch (e: Exception) {
                                            println("Error parsing features for $typeName: ${e.message}")
                                            listOf("Standard Banking Features")
                                        }

                                        OpeningAccountType(
                                            type = typeName,
                                            displayName = displayName,
                                            minimumDeposit = minimumDeposit,
                                            interestRate = interestRate,
                                            features = featuresArray
                                        )
                                    }
                            }

                            println("✅ Found ${accountTypes.size} account types in database")

                            val responseData = AccountTypesResponse(
                                accountTypes = accountTypes,
                                availableTypes = accountTypes.size
                            )

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    message = "Account types retrieved successfully",
                                    data = responseData
                                )
                            )
                        } catch (e: Exception) {
                            println("❌ Error fetching account types: ${e.message}")
                            e.printStackTrace()

                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to fetch account types: ${e.message ?: "Unknown error"}",
                                    error = e.message ?: "Unknown error"
                                )
                            )
                        }
                    }

                    // Create customer and open account in one transaction
                    post("/open-with-customer") {
                        try {
                            val request = call.receive<OpenAccountForNewCustomerRequest>()

                            println("🏦 Creating customer and opening account...")

                            // Validate required fields
                            if (request.customer.firstName.isNullOrBlank() ||
                                request.customer.lastName.isNullOrBlank() ||
                                request.customer.email.isNullOrBlank() ||
                                request.customer.phoneNumber.isNullOrBlank()) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Missing required customer fields",
                                        error = "VALIDATION_ERROR"
                                    )
                                )
                                return@post
                            }

                            // Create customer first
                            val customerNumber = "CUST${System.currentTimeMillis()}"
                            val customerId = transaction {
                                Customers.insert { insert ->
                                    insert[Customers.customerNumber] = customerNumber
                                    insert[Customers.type] = CustomerType.valueOf(request.customer.type)
                                    insert[Customers.status] = CustomerStatus.ACTIVE
                                    insert[Customers.firstName] = request.customer.firstName
                                    insert[Customers.lastName] = request.customer.lastName
                                    insert[Customers.middleName] = request.customer.middleName
                                    insert[Customers.dateOfBirth] = request.customer.dateOfBirth?.let { java.time.LocalDate.parse(it) }
                                    insert[Customers.ssn] = request.customer.ssn
                                    insert[Customers.email] = request.customer.email
                                    insert[Customers.phoneNumber] = request.customer.phoneNumber
                                    insert[Customers.alternatePhone] = request.customer.alternatePhone
                                    insert[Customers.primaryStreet] = request.customer.primaryStreet
                                    insert[Customers.primaryCity] = request.customer.primaryCity
                                    insert[Customers.primaryState] = request.customer.primaryState
                                    insert[Customers.primaryZipCode] = request.customer.primaryZipCode
                                    insert[Customers.primaryCountry] = request.customer.primaryCountry
                                    insert[Customers.mailingStreet] = request.customer.mailingStreet
                                    insert[Customers.mailingCity] = request.customer.mailingCity
                                    insert[Customers.mailingState] = request.customer.mailingState
                                    insert[Customers.mailingZipCode] = request.customer.mailingZipCode
                                    insert[Customers.mailingCountry] = request.customer.mailingCountry
                                    insert[Customers.occupation] = request.customer.occupation
                                    insert[Customers.employer] = request.customer.employer
                                    insert[Customers.annualIncome] = request.customer.annualIncome?.let { java.math.BigDecimal(it) }
                                    insert[Customers.creditScore] = request.customer.creditScore
                                    insert[Customers.branchId] = UUID.fromString(request.customer.branchId)
                                    insert[Customers.businessName] = request.customer.businessName
                                    insert[Customers.businessType] = request.customer.businessType
                                    insert[Customers.taxId] = request.customer.taxId
                                    insert[Customers.businessLicenseNumber] = request.customer.businessLicenseNumber
                                    insert[Customers.kycStatus] = "PENDING"
                                }[Customers.id]
                            }

                            println("✅ Customer created: $customerId")

                            // Generate account number
                            val accountNumber = IdGenerator.generateAccountNumber()

                            // Create account for the new customer
                            val accountId = transaction {
                                Accounts.insert { insert ->
                                    insert[Accounts.customerId] = customerId as UUID
                                    insert[Accounts.accountNumber] = accountNumber
                                    insert[Accounts.type] = AccountType.valueOf(request.accountTypeId)
                                    insert[Accounts.balance] = java.math.BigDecimal(request.initialDeposit)
                                    insert[Accounts.availableBalance] = java.math.BigDecimal(request.initialDeposit)
                                    insert[Accounts.status] = AccountStatus.ACTIVE
                                    insert[Accounts.branchId] = UUID.fromString(request.customer.branchId)
                                    insert[Accounts.interestRate] = java.math.BigDecimal("0.02")
                                    insert[Accounts.minimumBalance] = java.math.BigDecimal("50.00")
                                    insert[Accounts.nickname] = request.nickname
                                    insert[Accounts.openedDate] = java.time.LocalDate.now()
                                }[Accounts.id]
                            }

                            println("✅ Account created: $accountId")

                            // If initial deposit > 0, create initial deposit transaction
                            if (request.initialDeposit > 0) {
                                transaction {
                                    Transactions.insert { insert ->
                                        insert[Transactions.accountId] = accountId as UUID
                                        insert[Transactions.type] = TransactionType.DEPOSIT
                                        insert[Transactions.amount] = java.math.BigDecimal(request.initialDeposit)
                                        insert[Transactions.description] = "Initial deposit - Account opening"
                                        insert[Transactions.status] = TransactionStatus.COMPLETED
                                        insert[Transactions.balanceAfter] = java.math.BigDecimal(request.initialDeposit)
                                    }
                                }
                            }

                            // Get created account details
                            val accountDetails = transaction {
                                Accounts
                                    .join(Customers, JoinType.INNER, Accounts.customerId, Customers.id)
                                    .join(Branches, JoinType.LEFT, Accounts.branchId, Branches.id)
                                    .select { Accounts.id eq accountId }
                                    .single()
                                    .let { row ->
                                        org.dals.project.models.AccountDto(
                                            id = row[Accounts.id].toString(),
                                            accountNumber = row[Accounts.accountNumber],
                                            customerId = row[Accounts.customerId].toString(),
                                            type = row[Accounts.type].name,
                                            status = row[Accounts.status].name,
                                            balance = row[Accounts.balance].toPlainString(),
                                            availableBalance = row[Accounts.availableBalance].toPlainString(),
                                            minimumBalance = row[Accounts.minimumBalance].toPlainString(),
                                            interestRate = row[Accounts.interestRate].toPlainString(),
                                            branchId = row[Accounts.branchId].toString(),
                                            openedDate = row[Accounts.createdAt].toString(),
                                            closedDate = row[Accounts.closedDate]?.toString(),
                                            nickname = row[Accounts.nickname],
                                            createdAt = row[Accounts.createdAt].toString(),
                                            updatedAt = row[Accounts.updatedAt].toString()
                                        )
                                    }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    message = "Customer registered and account opened successfully",
                                    data = accountDetails
                                )
                            )

                        } catch (e: Exception) {
                            println("❌ Error creating customer and opening account: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Failed to create customer and open account: ${e.message}",
                                    error = e.message
                                )
                            )
                        }
                    }
                }
            }

            // ==================== KYC SERVICES ROUTES ====================
            route("/kyc") {
                // KYC Dashboard Metrics
                route("/dashboard") {
                    get("/metrics") {
                        try {
                            val metrics = transaction {
                                val totalProfiles = KycProfiles.selectAll().count().toInt()
                                val pendingReview = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.PENDING_REVIEW }.count().toInt()
                                val underReview = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.UNDER_REVIEW }.count().toInt()
                                val verified = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.VERIFIED }.count().toInt()
                                val rejected = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.REJECTED }.count().toInt()
                                val expired = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.EXPIRED }.count().toInt()
                                val requiresAction = KycDocuments.select {
                                    (KycDocuments.status eq KycDocumentStatus.ADDITIONAL_INFO_REQUIRED) or
                                    (KycDocuments.status eq KycDocumentStatus.RESUBMISSION_REQUIRED)
                                }.count().toInt()
                                val highRisk = KycProfiles.select { KycProfiles.riskRating eq "HIGH" }.count().toInt()
                                val pendingDocs = KycDocuments.select { KycDocuments.status eq KycDocumentStatus.PENDING_UPLOAD }.count().toInt()

                                // Documents expiring in 30 days
                                val thirtyDaysFromNow = LocalDate.now().plusDays(30)
                                val expiringIn30Days = KycDocuments.select {
                                    (KycDocuments.expiryDate.isNotNull()) and
                                    (KycDocuments.expiryDate lessEq thirtyDaysFromNow) and
                                    (KycDocuments.expiryDate greaterEq LocalDate.now())
                                }.count().toInt()

                                KycDashboardMetricsDto(
                                    totalProfiles = totalProfiles,
                                    pendingReview = pendingReview,
                                    underReview = underReview,
                                    verified = verified,
                                    rejected = rejected,
                                    expired = expired,
                                    requiresAction = requiresAction,
                                    highRiskCount = highRisk,
                                    pendingDocumentsCount = pendingDocs,
                                    expiringIn30Days = expiringIn30Days
                                )
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "KYC metrics retrieved successfully", data = metrics))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, message = "Failed to retrieve KYC metrics", error = e.message))
                        }
                    }
                }

                // KYC Profiles
                route("/profiles") {
                    get {
                        try {
                            val page = call.parameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                            val statusFilter = call.parameters["status"]

                            val profiles = transaction {
                                val query = if (statusFilter != null) {
                                    KycProfiles.select { KycProfiles.overallKycStatus eq KycDocumentStatus.valueOf(statusFilter) }
                                } else {
                                    KycProfiles.selectAll()
                                }

                                val total = query.count().toInt()
                                val data = query
                                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                                    .map { row ->
                                        KycProfileDto(
                                            id = row[KycProfiles.id].toString(),
                                            customerId = row[KycProfiles.customerId].toString(),
                                            customerName = row[KycProfiles.customerName],
                                            customerType = row[KycProfiles.customerType].name,
                                            overallKycStatus = row[KycProfiles.overallKycStatus].name,
                                            complianceLevel = row[KycProfiles.complianceLevel].name,
                                            lastReviewDate = row[KycProfiles.lastReviewDate]?.toString(),
                                            nextReviewDate = row[KycProfiles.nextReviewDate]?.toString(),
                                            riskRating = row[KycProfiles.riskRating],
                                            completionPercentage = row[KycProfiles.completionPercentage].toDouble(),
                                            flags = row[KycProfiles.flags],
                                            notes = row[KycProfiles.notes],
                                            assignedOfficer = row[KycProfiles.assignedOfficer]?.toString(),
                                            createdDate = row[KycProfiles.createdDate].toString(),
                                            lastUpdatedDate = row[KycProfiles.lastUpdatedDate].toString()
                                        )
                                    }

                                CustomerCareListResponse(
                                    success = true,
                                    message = "KYC profiles retrieved successfully",
                                    data = data,
                                    total = total,
                                    page = page,
                                    pageSize = pageSize,
                                    timestamp = java.time.LocalDateTime.now().toString()
                                )
                            }

                            call.respond(HttpStatusCode.OK, profiles)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, CustomerCareListResponse<KycProfileDto>(
                                success = false,
                                message = "Failed to retrieve KYC profiles",
                                data = emptyList(),
                                total = 0,
                                page = 1,
                                pageSize = 50,
                                timestamp = java.time.LocalDateTime.now().toString()
                            ))
                        }
                    }

                    put("/{customerId}") {
                        try {
                            val customerId = UUID.fromString(call.parameters["customerId"])
                            val request = call.receive<UpdateKycProfileRequest>()

                            val updated = transaction {
                                KycProfiles.update({ KycProfiles.customerId eq customerId }) {
                                    request.overallKycStatus?.let { status ->
                                        it[overallKycStatus] = KycDocumentStatus.valueOf(status)
                                    }
                                    request.complianceLevel?.let { level ->
                                        it[complianceLevel] = ComplianceLevel.valueOf(level)
                                    }
                                    request.riskRating?.let { rating -> it[riskRating] = rating }
                                    request.flags?.let { f -> it[flags] = f }
                                    request.notes?.let { n -> it[notes] = n }
                                    request.assignedOfficer?.let { officer -> it[assignedOfficer] = UUID.fromString(officer) }
                                    it[lastUpdatedDate] = LocalDate.now()
                                }

                                KycProfiles.select { KycProfiles.customerId eq customerId }
                                    .single()
                                    .let { row ->
                                        KycProfileDto(
                                            id = row[KycProfiles.id].toString(),
                                            customerId = row[KycProfiles.customerId].toString(),
                                            customerName = row[KycProfiles.customerName],
                                            customerType = row[KycProfiles.customerType].name,
                                            overallKycStatus = row[KycProfiles.overallKycStatus].name,
                                            complianceLevel = row[KycProfiles.complianceLevel].name,
                                            lastReviewDate = row[KycProfiles.lastReviewDate]?.toString(),
                                            nextReviewDate = row[KycProfiles.nextReviewDate]?.toString(),
                                            riskRating = row[KycProfiles.riskRating],
                                            completionPercentage = row[KycProfiles.completionPercentage].toDouble(),
                                            flags = row[KycProfiles.flags],
                                            notes = row[KycProfiles.notes],
                                            assignedOfficer = row[KycProfiles.assignedOfficer]?.toString(),
                                            createdDate = row[KycProfiles.createdDate].toString(),
                                            lastUpdatedDate = row[KycProfiles.lastUpdatedDate].toString()
                                        )
                                    }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "KYC profile updated successfully", data = updated))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Failed to update KYC profile", error = e.message))
                        }
                    }
                }

                // KYC Documents
                route("/documents") {
                    get {
                        try {
                            val page = call.parameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 50
                            val statusFilter = call.parameters["status"]

                            val documents = transaction {
                                val query = if (statusFilter != null) {
                                    KycDocuments.select { KycDocuments.status eq KycDocumentStatus.valueOf(statusFilter) }
                                } else {
                                    KycDocuments.selectAll()
                                }

                                val total = query.count().toInt()
                                val data = query
                                    .limit(pageSize, ((page - 1) * pageSize).toLong())
                                    .map { row ->
                                        KycDocumentDetailsDto(
                                            id = row[KycDocuments.id].toString(),
                                            customerId = row[KycDocuments.customerId].toString(),
                                            customerName = row[KycDocuments.customerName],
                                            documentType = row[KycDocuments.documentType].name,
                                            fileName = row[KycDocuments.fileName],
                                            originalFileName = row[KycDocuments.originalFileName],
                                            filePath = row[KycDocuments.filePath],
                                            fileSize = row[KycDocuments.fileSize],
                                            mimeType = row[KycDocuments.mimeType],
                                            uploadDate = row[KycDocuments.uploadDate].toString(),
                                            lastModifiedDate = row[KycDocuments.lastModifiedDate]?.toString(),
                                            expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                            issueDate = row[KycDocuments.issueDate]?.toString(),
                                            issuingAuthority = row[KycDocuments.issuingAuthority],
                                            documentNumber = row[KycDocuments.documentNumber],
                                            status = row[KycDocuments.status].name,
                                            priority = row[KycDocuments.priority].name,
                                            complianceLevel = row[KycDocuments.complianceLevel].name,
                                            uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                            verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                            verificationDate = row[KycDocuments.verificationDate]?.toString(),
                                            rejectedBy = row[KycDocuments.rejectedBy]?.toString(),
                                            rejectionDate = row[KycDocuments.rejectionDate]?.toString(),
                                            rejectionReason = row[KycDocuments.rejectionReason],
                                            notes = row[KycDocuments.notes],
                                            internalNotes = row[KycDocuments.internalNotes],
                                            isRequired = row[KycDocuments.isRequired],
                                            isConfidential = row[KycDocuments.isConfidential],
                                            requiresManualReview = row[KycDocuments.requiresManualReview],
                                            riskScore = row[KycDocuments.riskScore]?.toDouble()
                                        )
                                    }

                                CustomerCareListResponse(
                                    success = true,
                                    message = "KYC documents retrieved successfully",
                                    data = data,
                                    total = total,
                                    page = page,
                                    pageSize = pageSize,
                                    timestamp = java.time.LocalDateTime.now().toString()
                                )
                            }

                            call.respond(HttpStatusCode.OK, documents)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, CustomerCareListResponse<KycDocumentDetailsDto>(
                                success = false,
                                message = "Failed to retrieve KYC documents",
                                data = emptyList(),
                                total = 0,
                                page = 1,
                                pageSize = 50,
                                timestamp = java.time.LocalDateTime.now().toString()
                            ))
                        }
                    }

                    get("/customer/{customerId}") {
                        try {
                            val customerId = UUID.fromString(call.parameters["customerId"])

                            val documents = transaction {
                                KycDocuments.select { KycDocuments.customerId eq customerId }
                                    .map { row ->
                                        KycDocumentDetailsDto(
                                            id = row[KycDocuments.id].toString(),
                                            customerId = row[KycDocuments.customerId].toString(),
                                            customerName = row[KycDocuments.customerName],
                                            documentType = row[KycDocuments.documentType].name,
                                            fileName = row[KycDocuments.fileName],
                                            originalFileName = row[KycDocuments.originalFileName],
                                            filePath = row[KycDocuments.filePath],
                                            fileSize = row[KycDocuments.fileSize],
                                            mimeType = row[KycDocuments.mimeType],
                                            uploadDate = row[KycDocuments.uploadDate].toString(),
                                            lastModifiedDate = row[KycDocuments.lastModifiedDate]?.toString(),
                                            expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                            issueDate = row[KycDocuments.issueDate]?.toString(),
                                            issuingAuthority = row[KycDocuments.issuingAuthority],
                                            documentNumber = row[KycDocuments.documentNumber],
                                            status = row[KycDocuments.status].name,
                                            priority = row[KycDocuments.priority].name,
                                            complianceLevel = row[KycDocuments.complianceLevel].name,
                                            uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                            verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                            verificationDate = row[KycDocuments.verificationDate]?.toString(),
                                            rejectedBy = row[KycDocuments.rejectedBy]?.toString(),
                                            rejectionDate = row[KycDocuments.rejectionDate]?.toString(),
                                            rejectionReason = row[KycDocuments.rejectionReason],
                                            notes = row[KycDocuments.notes],
                                            internalNotes = row[KycDocuments.internalNotes],
                                            isRequired = row[KycDocuments.isRequired],
                                            isConfidential = row[KycDocuments.isConfidential],
                                            requiresManualReview = row[KycDocuments.requiresManualReview],
                                            riskScore = row[KycDocuments.riskScore]?.toDouble()
                                        )
                                    }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "Customer KYC documents retrieved successfully", data = documents))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Failed to retrieve customer documents", error = e.message))
                        }
                    }

                    put("/{documentId}/status") {
                        try {
                            val documentId = UUID.fromString(call.parameters["documentId"])
                            val request = call.receive<UpdateKycDocumentStatusRequest>()

                            val updated = transaction {
                                KycDocuments.update({ KycDocuments.id eq documentId }) {
                                    it[status] = KycDocumentStatus.valueOf(request.status)
                                    request.notes?.let { n -> it[notes] = n }
                                    request.internalNotes?.let { in_ -> it[internalNotes] = in_ }
                                    request.rejectionReason?.let { reason -> it[rejectionReason] = reason }

                                    if (request.status == "VERIFIED") {
                                        it[verificationDate] = LocalDate.now()
                                        // verifiedBy would come from auth token
                                    } else if (request.status == "REJECTED") {
                                        it[rejectionDate] = LocalDate.now()
                                        // rejectedBy would come from auth token
                                    }
                                }

                                KycDocuments.select { KycDocuments.id eq documentId }
                                    .single()
                                    .let { row ->
                                        KycDocumentDetailsDto(
                                            id = row[KycDocuments.id].toString(),
                                            customerId = row[KycDocuments.customerId].toString(),
                                            customerName = row[KycDocuments.customerName],
                                            documentType = row[KycDocuments.documentType].name,
                                            fileName = row[KycDocuments.fileName],
                                            originalFileName = row[KycDocuments.originalFileName],
                                            filePath = row[KycDocuments.filePath],
                                            fileSize = row[KycDocuments.fileSize],
                                            mimeType = row[KycDocuments.mimeType],
                                            uploadDate = row[KycDocuments.uploadDate].toString(),
                                            lastModifiedDate = row[KycDocuments.lastModifiedDate]?.toString(),
                                            expiryDate = row[KycDocuments.expiryDate]?.toString(),
                                            issueDate = row[KycDocuments.issueDate]?.toString(),
                                            issuingAuthority = row[KycDocuments.issuingAuthority],
                                            documentNumber = row[KycDocuments.documentNumber],
                                            status = row[KycDocuments.status].name,
                                            priority = row[KycDocuments.priority].name,
                                            complianceLevel = row[KycDocuments.complianceLevel].name,
                                            uploadedBy = row[KycDocuments.uploadedBy].toString(),
                                            verifiedBy = row[KycDocuments.verifiedBy]?.toString(),
                                            verificationDate = row[KycDocuments.verificationDate]?.toString(),
                                            rejectedBy = row[KycDocuments.rejectedBy]?.toString(),
                                            rejectionDate = row[KycDocuments.rejectionDate]?.toString(),
                                            rejectionReason = row[KycDocuments.rejectionReason],
                                            notes = row[KycDocuments.notes],
                                            internalNotes = row[KycDocuments.internalNotes],
                                            isRequired = row[KycDocuments.isRequired],
                                            isConfidential = row[KycDocuments.isConfidential],
                                            requiresManualReview = row[KycDocuments.requiresManualReview],
                                            riskScore = row[KycDocuments.riskScore]?.toDouble()
                                        )
                                    }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(success = true, message = "Document status updated successfully", data = updated))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, message = "Failed to update document status", error = e.message))
                        }
                    }
                }
            }

            // ==================== ADMIN ROUTES ====================
            // ==================== NEW MODULAR ADMIN ROUTES ====================
            // Dashboard routes with real-time database fetching
            dashboardRoutes()

            // Alerts routes (system and compliance)
            alertsRoutes()

            // Workflow approval routes
            workflowRoutes()

            // Audit and system logs routes
            auditRoutes()

            // User management routes
            userManagementRoutes()

            // System configuration routes
            systemRoutes()

            // Reports and analytics routes
            reportsRoutes()

            // Savings accounts routes
            savingsRoutes()

            // Teller dashboard routes
//            println("=== ABOUT TO REGISTER TELLER ROUTES ===")
            try {
                tellerRoutes()
//                println("=== TELLER ROUTES REGISTERED SUCCESSFULLY ===")
            } catch (e: Exception) {
//                println("=== ERROR REGISTERING TELLER ROUTES: ${e.message} ===")
                e.printStackTrace()
            }

            // Loan routes
            loanRoutes(loanService)

            // Notification routes
            notificationRoutes()

            // Card routes
            cardRoutes(cardService)

            // Card issuance routes (Mastercard integration)
            cardIssuanceRoutes(mastercardIssuanceService, cardService)

            // Card transaction routes
            val cardTransactionService = CardTransactionService()
            cardTransactionRoutes(cardTransactionService)

            // Credit assessment routes
            creditAssessmentRoutes(creditAssessmentService)

            // Master wallet dashboard routes
            masterWalletDashboardRoutes()

            // ==================== LEGACY ADMIN ROUTES (Keep for compatibility) ====================
            route("/admin") {

                route("/dashboard") {
                    get("/metrics") {
                        val response = adminService.getDashboardMetrics()
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }
                }

                // Advertisement Management
                route("/advertisements") {
                    // Get all advertisements
                    get {
                        try {
                            val includeInactive = call.request.queryParameters["includeInactive"]?.toBoolean() ?: false

                            val ads = transaction {
                                val query = if (includeInactive) {
                                    Advertisements.selectAll()
                                } else {
                                    Advertisements.select { Advertisements.isActive eq true }
                                }

                                query.orderBy(Advertisements.displayOrder to SortOrder.ASC)
                                    .map { row ->
                                        AdvertisementDto(
                                            id = row[Advertisements.id].toString(),
                                            title = row[Advertisements.title],
                                            description = row[Advertisements.description],
                                            imageUrl = row[Advertisements.imageUrl],
                                            linkUrl = row[Advertisements.linkUrl],
                                            displayOrder = row[Advertisements.displayOrder],
                                            isActive = row[Advertisements.isActive],
                                            startDate = row[Advertisements.startDate].toString(),
                                            endDate = row[Advertisements.endDate]?.toString(),
                                            createdBy = row[Advertisements.createdBy].toString(),
                                            createdAt = row[Advertisements.createdAt].toString(),
                                            updatedAt = row[Advertisements.updatedAt].toString()
                                        )
                                    }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "Advertisements retrieved successfully",
                                data = ads
                            ))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve advertisements: ${e.message}",
                                error = e.message
                            ))
                        }
                    }

                    // Create new advertisement
                    post {
                        try {
                            val request = call.receive<CreateAdvertisementRequest>()
                            val sessionId = call.request.headers["Authorization"]?.removePrefix("Bearer ")

                            val userId = sessionId?.let {
                                transaction {
                                    UserSessions.select {
                                        (UserSessions.id eq UUID.fromString(it)) and (UserSessions.isActive eq true)
                                    }.singleOrNull()?.get(UserSessions.userId)
                                }
                            }

                            if (userId == null) {
                                call.respond(HttpStatusCode.Unauthorized, ApiResponse<String>(
                                    success = false,
                                    message = "Unauthorized",
                                    error = "UNAUTHORIZED"
                                ))
                                return@post
                            }

                            val adId = transaction {
                                Advertisements.insert {
                                    it[title] = request.title
                                    it[description] = request.description
                                    it[imageUrl] = request.imageUrl
                                    it[linkUrl] = request.linkUrl
                                    it[displayOrder] = request.displayOrder
                                    it[isActive] = true
                                    it[startDate] = request.startDate?.let { date ->
                                        java.time.Instant.parse(date)
                                    } ?: java.time.Instant.now()
                                    it[endDate] = request.endDate?.let { date ->
                                        java.time.Instant.parse(date)
                                    }
                                    it[createdBy] = userId
                                }[Advertisements.id]
                            }

                            call.respond(HttpStatusCode.Created, ApiResponse(
                                success = true,
                                message = "Advertisement created successfully",
                                data = mapOf("id" to adId.toString())
                            ))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to create advertisement: ${e.message}",
                                error = e.message
                            ))
                        }
                    }

                    // Update advertisement
                    put("/{id}") {
                        try {
                            val adId = call.parameters["id"] ?: throw IllegalArgumentException("Advertisement ID required")
                            val request = call.receive<UpdateAdvertisementRequest>()

                            transaction {
                                Advertisements.update({ Advertisements.id eq UUID.fromString(adId) }) { update ->
                                    request.title?.let { update[title] = it }
                                    request.description?.let { update[description] = it }
                                    request.imageUrl?.let { update[imageUrl] = it }
                                    request.linkUrl?.let { update[linkUrl] = it }
                                    request.displayOrder?.let { update[displayOrder] = it }
                                    request.isActive?.let { update[isActive] = it }
                                    request.endDate?.let { update[endDate] = java.time.Instant.parse(it) }
                                    update[updatedAt] = java.time.Instant.now()
                                }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "Advertisement updated successfully",
                                data = mapOf("id" to adId)
                            ))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to update advertisement: ${e.message}",
                                error = e.message
                            ))
                        }
                    }

                    // Delete advertisement
                    delete("/{id}") {
                        try {
                            val adId = call.parameters["id"] ?: throw IllegalArgumentException("Advertisement ID required")

                            transaction {
                                Advertisements.deleteWhere { Advertisements.id eq UUID.fromString(adId) }
                            }

                            call.respond(HttpStatusCode.OK, ApiResponse(
                                success = true,
                                message = "Advertisement deleted successfully",
                                data = mapOf("id" to adId)
                            ))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(
                                success = false,
                                message = "Failed to delete advertisement: ${e.message}",
                                error = e.message
                            ))
                        }
                    }
                }

                // Roles and Permissions management
                route("/roles") {
                    get {
                        val response = adminService.getAllRoles()
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }

                    post {
                        try {
                            val request = call.receive<CreateRoleRequest>()
                            val response = adminService.createRole(request)
                            val statusCode = if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                            call.respond(statusCode, response)
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    message = "Invalid request body",
                                    error = e.message
                                )
                            )
                        }
                    }
                }

                route("/permissions") {
                    get {
                        val response = adminService.getAllPermissions()
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }
                }

                // Drawer management routes
                route("/drawers") {
                    // Get all drawers
                    get {
                        try {
                            val sessionId = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (sessionId == null) {
                                call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("success" to false, "message" to "No session token provided")
                                )
                                return@get
                            }

                            // Get user's branch from session
                            val userBranchId = newSuspendedTransaction {
                                UserSessions
                                    .join(Users, JoinType.INNER, UserSessions.userId, Users.id)
                                    .select {
                                        (UserSessions.id eq UUID.fromString(sessionId)) and
                                                (UserSessions.isActive eq true)
                                    }
                                    .singleOrNull()
                                    ?.get(Users.branchId)?.toString()
                            }

                            if (userBranchId == null) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    mapOf("success" to false, "message" to "Branch information not found")
                                )
                                return@get
                            }

                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                            val drawers = newSuspendedTransaction {
                                Drawers.select { Drawers.branchId eq UUID.fromString(userBranchId) }.map { row ->
                                    DrawerDto(
                                        id = row[Drawers.id].toString(),
                                        drawerNumber = row[Drawers.drawerNumber],
                                        branchId = row[Drawers.branchId].toString(),
                                        drawerName = row[Drawers.drawerName],
                                        location = row[Drawers.location],
                                        maxFloatAmount = row[Drawers.maxFloatAmount].toDouble(),
                                        minFloatAmount = row[Drawers.minFloatAmount].toDouble(),
                                        status = row[Drawers.status],
                                        isActive = row[Drawers.isActive],
                                        createdBy = row[Drawers.createdBy]?.toString(),
                                        createdAt = row[Drawers.createdAt].toString(),
                                        updatedAt = row[Drawers.updatedAt].toString()
                                    )
                                }
                            }

                            call.respond(HttpStatusCode.OK, ListResponse(
                                success = true,
                                message = "Drawers retrieved successfully",
                                data = drawers,
                                total = drawers.size,
                                page = page,
                                pageSize = pageSize
                            ))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<String>(
                                success = false,
                                message = "Failed to retrieve drawers: ${e.message}",
                                error = e.message
                            ))
                        }
                    }

                    // Create new drawer
                    post {
                        try {
                            @kotlinx.serialization.Serializable
                            data class CreateDrawerRequest(
                                val drawerNumber: String,
                                val branchId: String,
                                val drawerName: String,
                                val location: String? = null,
                                val maxFloatAmount: Double = 50000.0,
                                val minFloatAmount: Double = 5000.0,
                                val createdBy: String? = null
                            )

                            val request = call.receive<CreateDrawerRequest>()
                            
                            val createdDrawer = newSuspendedTransaction {
                                val drawerId = Drawers.insert {
                                    it[drawerNumber] = request.drawerNumber
                                    it[branchId] = UUID.fromString(request.branchId)
                                    it[drawerName] = request.drawerName
                                    request.location?.let { loc -> it[location] = loc }
                                    it[maxFloatAmount] = java.math.BigDecimal(request.maxFloatAmount)
                                    it[minFloatAmount] = java.math.BigDecimal(request.minFloatAmount)
                                    it[status] = "AVAILABLE"
                                    it[isActive] = true
                                    request.createdBy?.let { creator -> it[createdBy] = UUID.fromString(creator) }
                                } get Drawers.id

                                // Fetch the created drawer to return complete object
                                Drawers.select { Drawers.id eq drawerId }.single().let { row ->
                                    DrawerDto(
                                        id = row[Drawers.id].toString(),
                                        drawerNumber = row[Drawers.drawerNumber],
                                        branchId = row[Drawers.branchId].toString(),
                                        drawerName = row[Drawers.drawerName],
                                        location = row[Drawers.location],
                                        maxFloatAmount = row[Drawers.maxFloatAmount].toDouble(),
                                        minFloatAmount = row[Drawers.minFloatAmount].toDouble(),
                                        status = row[Drawers.status],
                                        isActive = row[Drawers.isActive],
                                        createdBy = row[Drawers.createdBy]?.toString(),
                                        createdAt = row[Drawers.createdAt].toString(),
                                        updatedAt = row[Drawers.updatedAt].toString()
                                    )
                                }
                            }

                            call.respond(HttpStatusCode.Created, ApiResponse(
                                success = true,
                                message = "Drawer created successfully",
                                data = createdDrawer
                            ))
                        } catch (e: Exception) {
                            val errorMessage = when {
                                e.message?.contains("duplicate key value violates unique constraint \"drawers_drawer_number_unique\"") == true ->
                                    "A drawer with this number already exists. Please use a different drawer number."
                                else -> "Failed to create drawer: ${e.message}"
                            }

                            call.respond(HttpStatusCode.BadRequest, ApiResponse<DrawerDto>(
                                success = false,
                                message = errorMessage,
                                error = e.message
                            ))
                        }
                    }
                }

                // Drawer assignment routes
                route("/drawer-assignments") {
                    // Create new drawer assignment - PUT BEFORE GET
                    post {
                        println("🔵 POST /drawer-assignments endpoint called")
                        try {
                            val sessionId = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                            println("🔵 Session ID: $sessionId")
                            if (sessionId == null) {
                                call.respond(
                                    HttpStatusCode.Unauthorized,
                                    ApiResponse<DrawerAssignmentDto>(
                                        success = false,
                                        message = "No session token provided"
                                    )
                                )
                                return@post
                            }

                            // Get assignedBy from session
                            val assignedBy = newSuspendedTransaction {
                                UserSessions
                                    .join(Users, JoinType.INNER, UserSessions.userId, Users.id)
                                    .select {
                                        (UserSessions.id eq UUID.fromString(sessionId)) and
                                                (UserSessions.isActive eq true)
                                    }
                                    .singleOrNull()
                                    ?.get(Users.id)?.toString()
                            }

                            if (assignedBy == null) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ApiResponse<DrawerAssignmentDto>(
                                        success = false,
                                        message = "Invalid session"
                                    )
                                )
                                return@post
                            }

                            val request = call.receive<AssignDrawerRequest>()

                            val assignment = newSuspendedTransaction {
                                val assignmentId = DrawerAssignments.insert {
                                    it[drawerId] = UUID.fromString(request.drawerId)
                                    it[userId] = UUID.fromString(request.userId)
                                    it[branchId] = UUID.fromString(request.branchId)
                                    it[DrawerAssignments.assignedBy] = UUID.fromString(assignedBy)
                                    it[status] = "ACTIVE"
                                    it[accessLevel] = request.accessLevel
                                    request.notes?.let { n -> it[notes] = n }
                                } get DrawerAssignments.id

                                // Fetch the created assignment to return
                                DrawerAssignments.select { DrawerAssignments.id eq assignmentId }.single().let { row ->
                                    DrawerAssignmentDto(
                                        id = row[DrawerAssignments.id].toString(),
                                        drawerId = row[DrawerAssignments.drawerId].toString(),
                                        userId = row[DrawerAssignments.userId].toString(),
                                        branchId = row[DrawerAssignments.branchId].toString(),
                                        assignedBy = row[DrawerAssignments.assignedBy].toString(),
                                        assignedDate = row[DrawerAssignments.assignedDate].toString(),
                                        revokedDate = row[DrawerAssignments.revokedDate]?.toString(),
                                        revokedBy = row[DrawerAssignments.revokedBy]?.toString(),
                                        status = row[DrawerAssignments.status],
                                        accessLevel = row[DrawerAssignments.accessLevel],
                                        notes = row[DrawerAssignments.notes],
                                        createdAt = row[DrawerAssignments.createdAt].toString(),
                                        updatedAt = row[DrawerAssignments.updatedAt].toString()
                                    )
                                }
                            }

                            call.respond(HttpStatusCode.Created, ApiResponse(
                                success = true,
                                message = "Drawer assignment created successfully",
                                data = assignment
                            ))
                        } catch (e: Exception) {
                            println("❌ Error in POST /drawer-assignments: ${e.message}")
                            e.printStackTrace()
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<DrawerAssignmentDto>(
                                success = false,
                                message = "Failed to create drawer assignment: ${e.message}",
                                error = e.message
                            ))
                        }
                    }

                    // Get all drawer assignments
                    get {
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                        try {
                            val assignments = newSuspendedTransaction {
                                DrawerAssignments.selectAll().map { row ->
                                    DrawerAssignmentDto(
                                        id = row[DrawerAssignments.id].toString(),
                                        drawerId = row[DrawerAssignments.drawerId].toString(),
                                        userId = row[DrawerAssignments.userId].toString(),
                                        branchId = row[DrawerAssignments.branchId].toString(),
                                        assignedBy = row[DrawerAssignments.assignedBy].toString(),
                                        assignedDate = row[DrawerAssignments.assignedDate].toString(),
                                        revokedDate = row[DrawerAssignments.revokedDate]?.toString(),
                                        revokedBy = row[DrawerAssignments.revokedBy]?.toString(),
                                        status = row[DrawerAssignments.status],
                                        accessLevel = row[DrawerAssignments.accessLevel],
                                        notes = row[DrawerAssignments.notes],
                                        createdAt = row[DrawerAssignments.createdAt].toString(),
                                        updatedAt = row[DrawerAssignments.updatedAt].toString()
                                    )
                                }
                            }

                            call.respond(HttpStatusCode.OK, ListResponse(
                                success = true,
                                message = "Drawer assignments retrieved successfully",
                                data = assignments,
                                total = assignments.size,
                                page = page,
                                pageSize = pageSize
                            ))
                        } catch (e: Exception) {
                            println("❌ Error in GET /drawer-assignments: ${e.message}")
                            e.printStackTrace()
                            call.respond(HttpStatusCode.InternalServerError, ListResponse<DrawerAssignmentDto>(
                                success = false,
                                message = "Failed to retrieve drawer assignments: ${e.message}",
                                data = emptyList(),
                                total = 0,
                                page = page,
                                pageSize = pageSize
                            ))
                        }
                    }
                }

                route("/alerts") {
                    get("/system") {
                        val response = adminService.getSystemAlerts()
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }

                    get("/compliance") {
                        val response = adminService.getComplianceAlerts()
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }
                }

                route("/workflow") {
                    get("/approvals") {
                        val status = call.request.queryParameters["status"]
                        val response = if (status != null) {
                            adminService.getWorkflowApprovalsByStatus(status)
                        } else {
                            adminService.getWorkflowApprovals()
                        }
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }
                }

                route("/audit") {
                    get("/logs") {
                        val startDate = call.request.queryParameters["startDate"]
                        val endDate = call.request.queryParameters["endDate"]
                        val userId = call.request.queryParameters["userId"]
                        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                        val response = adminService.getAuditLogs(startDate, endDate, userId, limit)
                        val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                        call.respond(statusCode, response)
                    }
                }

                // Master Wallet API routes
                route("/master-wallet") {
                    // Main master wallet endpoint (working)
                    get {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                message = "Master Wallet API endpoint",
                                data = "Master Wallet Service Available"
                            )
                        )
                    }

                    // Wallet management routes
                    route("/wallets") {
                        get {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                            println("📊 Getting wallets: page=$page, pageSize=$pageSize")
                            val response = masterWalletService.getAllWallets(page, pageSize)
                            call.respond(HttpStatusCode.OK, response)
                        }

                        post {
                            try {
                                val request = call.receive<CreateWalletRequest>()
                                println("💼 Creating wallet: ${request.walletName}")
                                val response = masterWalletService.createWallet(request)
                                val statusCode =
                                    if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                                call.respond(statusCode, response)
                            } catch (e: Exception) {
                                println("❌ Error creating wallet: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid request body",
                                        error = e.message
                                    )
                                )
                            }
                        }

                        get("/{id}") {
                            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                            if (id == null) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid wallet ID"
                                    )
                                )
                                return@get
                            }

                            val response = masterWalletService.getWalletById(id)
                            val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                            call.respond(statusCode, response)
                        }

                        patch("/{id}/status") {
                            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                            if (id == null) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid wallet ID"
                                    )
                                )
                                return@patch
                            }

                            try {
                                val request = call.receive<Map<String, Any>>()
                                val status = request["status"] as? String ?: throw Exception("Status is required")
                                val isActive = request["isActive"] as? Boolean ?: true

                                println("🔄 Updating wallet $id status to: $status, active: $isActive")

                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        message = "Wallet status updated successfully",
                                        data = mapOf(
                                            "walletId" to id.toString(),
                                            "status" to status,
                                            "isActive" to isActive
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid request body",
                                        error = e.message
                                    )
                                )
                            }
                        }

                        delete("/{id}") {
                            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                            if (id == null) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid wallet ID"
                                    )
                                )
                                return@delete
                            }

                            val response = masterWalletService.deleteWallet(id)
                            val statusCode = if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound
                            call.respond(statusCode, response)
                        }
                    }

                    // Wallet transactions routes
                    route("/transactions") {
                        get {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                            println("📊 Getting wallet transactions: page=$page, pageSize=$pageSize")
                            val response = masterWalletService.getAllWalletTransactions(page, pageSize)
                            call.respond(HttpStatusCode.OK, response)
                        }

                        post {
                            try {
                                val request = call.receive<CreateWalletTransactionRequest>()
                                println("💳 Creating wallet transaction: ${request.description}")
                                val response = masterWalletService.createWalletTransaction(request)
                                val statusCode =
                                    if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                                call.respond(statusCode, response)
                            } catch (e: Exception) {
                                println("❌ Error creating transaction: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid request body",
                                        error = e.message
                                    )
                                )
                            }
                        }
                    }

                    // Wallet allocations routes
                    route("/allocations") {
                        get {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                            println("📊 Getting allocations: page=$page, pageSize=$pageSize")
                            val response = masterWalletService.getAllAllocations(page, pageSize)
                            call.respond(HttpStatusCode.OK, response)
                        }

                        post {
                            try {
                                val request = call.receive<CreateWalletAllocationRequest>()
                                println("📋 Creating allocation: ${request.purpose}")
                                val response = masterWalletService.createAllocation(request)
                                val statusCode =
                                    if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                                call.respond(statusCode, response)
                            } catch (e: Exception) {
                                println("❌ Error creating allocation: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid request body",
                                        error = e.message
                                    )
                                )
                            }
                        }
                    }

                    // Reconciliations routes
                    route("/reconciliations") {
                        get {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                            println("📊 Getting reconciliations: page=$page, pageSize=$pageSize")
                            val response = masterWalletService.getAllReconciliations(page, pageSize)
                            call.respond(HttpStatusCode.OK, response)
                        }

                        post {
                            try {
                                val request = call.receive<CreateWalletReconciliationRequest>()
                                println("🔍 Creating reconciliation for wallet: ${request.walletId}")
                                val response = masterWalletService.createReconciliation(request)
                                val statusCode =
                                    if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest
                                call.respond(statusCode, response)
                            } catch (e: Exception) {
                                println("❌ Error creating reconciliation: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        message = "Invalid request body",
                                        error = e.message
                                    )
                                )
                            }
                        }
                    }

                    // Wallet-related security alerts routes
                    route("/security-alerts") {
                        get {
                            println("🚨 Getting security alerts")
                            val response = masterWalletService.getAllSecurityAlerts()
                            call.respond(HttpStatusCode.OK, response)
                        }
                    }

                    // M-Pesa wallet funding endpoints
                    route("/mpesa-deposit") {
                        // Initiate M-Pesa deposit to a specific wallet
                        post("/{walletId}") {
                            try {
                                val walletId =
                                    call.parameters["walletId"] ?: throw IllegalArgumentException("Wallet ID required")
                                val request = call.receive<MpesaWalletDepositRequest>()

                                println("🏦 Processing M-Pesa wallet deposit: WalletID=$walletId, Phone=${request.phoneNumber}, Amount=${request.amount}")

                                // Validate request
                                if (request.phoneNumber.isBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ApiResponse<Unit>(
                                            success = false,
                                            message = "Phone number is required",
                                            error = "VALIDATION_ERROR"
                                        )
                                    )
                                    return@post
                                }

                                if (request.amount <= 0) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ApiResponse<Unit>(
                                            success = false,
                                            message = "Amount must be greater than zero",
                                            error = "VALIDATION_ERROR"
                                        )
                                    )
                                    return@post
                                }

                                // Validate wallet exists and admin has permission
                                val walletResponse = masterWalletService.getWalletById(UUID.fromString(walletId))
                                if (!walletResponse.success || walletResponse.data == null) {
                                    call.respond(
                                        HttpStatusCode.NotFound,
                                        ApiResponse<Unit>(
                                            success = false,
                                            message = "Wallet not found",
                                            error = "WALLET_NOT_FOUND"
                                        )
                                    )
                                    return@post
                                }
                                val walletData = walletResponse.data

                                // Initiate M-Pesa STK Push for wallet funding
                                val result = mpesaService.initiateWalletDeposit(
                                    walletId = walletId,
                                    phoneNumber = request.phoneNumber,
                                    amount = request.amount,
                                    description = request.description?.ifBlank { "Deposit to ${walletData.walletName}" } ?: "Deposit to ${walletData.walletName}"
                                )

                                val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                                // Create proper response data structure
                                val responseData = MpesaDepositResponse(
                                    success = result.success,
                                    message = result.message,
                                    transactionId = result.transactionId,
                                    checkoutRequestID = result.checkoutRequestID,
                                    merchantRequestID = result.merchantRequestID,
                                    customerMessage = result.customerMessage,
                                    error = result.error
                                )

                                call.respond(
                                    statusCode, ApiResponse(
                                        success = result.success,
                                        message = result.message,
                                        data = responseData
                                    )
                                )
                            } catch (e: Exception) {
                                println("❌ M-Pesa wallet deposit error: ${e.message}")
                                e.printStackTrace()
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Failed to initiate M-Pesa deposit: ${e.message}",
                                        error = "MPESA_DEPOSIT_ERROR"
                                    )
                                )
                            }
                        }

                        // Check status of wallet deposit transaction
                        get("/status/{transactionId}") {
                            try {
                                val transactionId = call.parameters["transactionId"]
                                    ?: throw IllegalArgumentException("Transaction ID required")

                                println("🔍 Checking M-Pesa wallet deposit status: $transactionId")

                                val result = mpesaService.checkWalletDepositStatus(transactionId)

                                // Create proper response data structure
                                val responseData = if (result.success) {
                                    MpesaDepositStatus(
                                        transactionId = result.transactionId,
                                        status = result.status ?: "UNKNOWN"
                                    )
                                } else null

                                call.respond(
                                    HttpStatusCode.OK, ApiResponse(
                                        success = result.success,
                                        message = if (result.success) "Transaction status retrieved successfully" else "Transaction status check failed",
                                        data = responseData,
                                        error = result.error
                                    )
                                )
                            } catch (e: Exception) {
                                println("❌ M-Pesa status check error: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Failed to check transaction status: ${e.message}",
                                        error = "TRANSACTION_STATUS_ERROR"
                                    )
                                )
                            }
                        }

                        // Manually register completed transaction using M-Pesa receipt
                        post("/register-completed") {
                            try {
                                val request = call.receive<ManualTransactionRegistrationRequest>()

                                println("📝 Manual registration request: ${request.mpesaReceiptNumber} for wallet ${request.walletId}")

                                // Validate request
                                if (request.mpesaReceiptNumber.isBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ApiResponse<Unit>(
                                            success = false,
                                            message = "M-Pesa receipt number is required",
                                            error = "VALIDATION_ERROR"
                                        )
                                    )
                                    return@post
                                }

                                if (request.walletId.isBlank()) {
                                    call.respond(
                                        HttpStatusCode.BadRequest,
                                        ApiResponse<Unit>(
                                            success = false,
                                            message = "Wallet ID is required",
                                            error = "VALIDATION_ERROR"
                                        )
                                    )
                                    return@post
                                }

                                // Register the completed transaction
                                val result = mpesaService.registerCompletedTransaction(
                                    mpesaReceiptNumber = request.mpesaReceiptNumber,
                                    walletId = request.walletId,
                                    phoneNumber = request.phoneNumber,
                                    amount = request.amount,
                                    description = request.description ?: "Manual registration via admin panel"
                                )

                                val statusCode =
                                    if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest

                                call.respond(
                                    statusCode, ApiResponse(
                                        success = result.success,
                                        message = if (result.success) "Transaction registered successfully" else "Failed to register transaction",
                                        data = if (result.success) result else null,
                                        error = result.error
                                    )
                                )
                            } catch (e: Exception) {
                                println("❌ Manual transaction registration error: ${e.message}")
                                e.printStackTrace()
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Failed to register completed transaction: ${e.message}",
                                        error = "MANUAL_REGISTRATION_ERROR"
                                    )
                                )
                            }
                        }

                        // Manually complete a pending transaction (for testing)
                        post("/complete-transaction/{checkoutRequestID}") {
                            try {
                                val checkoutRequestID = call.parameters["checkoutRequestID"]
                                    ?: throw IllegalArgumentException("Checkout Request ID required")

                                println("🔧 Manual completion request for: $checkoutRequestID")

                                val result = mpesaService.manuallyCompleteTransaction(checkoutRequestID)

                                val statusCode = if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest

                                call.respond(
                                    statusCode, ApiResponse(
                                        success = result.success,
                                        message = if (result.success) "Transaction completed successfully" else "Failed to complete transaction",
                                        data = if (result.success) TransactionStatusResponse(
                                            success = result.success,
                                            message = "Transaction completed",
                                            transactionId = result.transactionId,
                                            status = result.status,
                                            amount = result.amount,
                                            phoneNumber = result.phoneNumber,
                                            walletId = result.walletId,
                                            createdAt = result.createdAt
                                        ) else null,
                                        error = result.error
                                    )
                                )
                            } catch (e: Exception) {
                                println("❌ Manual completion error: ${e.message}")
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Failed to complete transaction: ${e.message}",
                                        error = "MANUAL_COMPLETION_ERROR"
                                    )
                                )
                            }
                        }

                        // Generic /mpesa-deposit endpoint for general wallet funding
                        post {
                            try {
                                val request = call.receive<MpesaWalletDepositRequest>()

                                // For now, respond with an error asking for wallet ID to be specified
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Please specify a wallet ID in the URL path: /mpesa-deposit/{walletId}",
                                        error = "WALLET_ID_REQUIRED"
                                    )
                                )
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        message = "Invalid request: ${e.message}",
                                        error = "INVALID_REQUEST"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ==================== TELLER ROUTES AT /api LEVEL ====================
            // Client expects routes at /api/teller/* not /api/v1/teller/*
        }

        // ==================== API LEVEL ROUTES ====================
        // These routes already include the "/api" prefix in their definitions
//        println("🏦 Registering teller routes at /api/teller...")
        tellerRoutes()
//        println("✅ Teller routes registered successfully at /api/teller")

        // ==================== CUSTOMER CARE ROUTES ====================
//        println("👥 Registering customer care access routes...")
        customerCareAccessRoutes()
//        println("✅ Customer care access routes registered successfully")

//        println("🔄 Registering transaction reversal routes...")
        transactionReversalRoutes()
//        println("✅ Transaction reversal routes registered successfully")

//        println("📄 Registering statement routes...")
        statementRoutes()
//        println("✅ Statement routes registered successfully")
    }
}