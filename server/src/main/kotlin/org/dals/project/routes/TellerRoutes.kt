package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*
import org.dals.project.models.*
import org.dals.project.models.ApiResponse
import org.dals.project.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

fun Route.tellerRoutes() {
    println("=== REGISTERING TELLER ROUTES ===")
    val tellerService = TellerCashDrawerService()
    val accountService = AccountService()
    val transactionService = TransactionService()
    val customerService = CustomerService()

    route("/teller") {
        println("Teller routes registered at: /teller (will be /api/v1/teller when called from /api/v1)")

        // Test endpoint to verify routes are working
        get("/test") {
            println("TEST ENDPOINT HIT!")
            call.respond(HttpStatusCode.OK, mapOf(
                "success" to true,
                "message" to "Teller routes are working!",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        // Initialize or get cash drawer
        post("/cash-drawer/initialize") {
            try {
                val request = call.receive<CreateTellerCashDrawerRequest>()
                val result = tellerService.initializeTellerCashDrawer(
                    tellerId = request.tellerId,
                    branchId = request.branchId,
                    drawerNumber = request.drawerNumber,
                    floatAmount = request.floatAmount,
                    openingBalance = request.openingBalance
                )
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to initialize cash drawer",
                    data = null as String?
                ))
            }
        }

        // Get teller's cash drawer
        get("/cash-drawer/{tellerId}") {
            try {
                val tellerId = call.parameters["tellerId"] ?: throw IllegalArgumentException("Teller ID is required")
                val result = tellerService.getTellerCashDrawer(tellerId)
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.OK, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to get cash drawer: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Process cash transaction
        post("/cash-drawer/transaction") {
            try {
                val request = call.receive<TellerTransactionRequest>()
                val result = tellerService.processCashTransaction(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to process transaction",
                    data = null as String?
                ))
            }
        }

        // Get teller transactions
        get("/transactions/{tellerId}") {
            try {
                val tellerId = call.parameters["tellerId"] ?: throw IllegalArgumentException("Teller ID is required")
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val result = tellerService.getTellerTransactions(tellerId, limit)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to get transactions",
                    data = null as String?
                ))
            }
        }

        // Get user's active drawer assignment
        get("/my-drawer-assignment") {
            try {
                val userId = call.request.queryParameters["userId"]
                    ?: throw IllegalArgumentException("User ID is required")

                val assignment = newSuspendedTransaction {
                    DrawerAssignments
                        .innerJoin(Drawers)
                        .select {
                            (DrawerAssignments.userId eq UUID.fromString(userId)) and
                            (DrawerAssignments.status eq "ACTIVE")
                        }
                        .orderBy(DrawerAssignments.assignedDate to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                        ?.let { row ->
                            DrawerAssignmentDto(
                                id = row[DrawerAssignments.id].toString(),
                                drawerId = row[DrawerAssignments.drawerId].toString(),
                                drawerNumber = row[Drawers.drawerNumber],
                                drawerName = row[Drawers.drawerName],
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

                if (assignment != null) {
                    call.respond(HttpStatusCode.OK, org.dals.project.models.ApiResponse(
                        success = true,
                        message = "Drawer assignment found",
                        data = assignment
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, org.dals.project.models.ApiResponse(
                        success = false,
                        message = "No active drawer assignment found for this user",
                        data = null as DrawerAssignmentDto?
                    ))
                }
            } catch (e: Exception) {
                println("❌ Error in GET /my-drawer-assignment: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to get drawer assignment: ${e.message}",
                    data = null as DrawerAssignmentDto?,
                    error = e.message
                ))
            }
        }

        // Close cash drawer
        post("/cash-drawer/close") {
            try {
                val request = call.receive<TellerReconciliationRequest>()
                val result = tellerService.closeTellerCashDrawer(
                    tellerId = call.request.queryParameters["tellerId"] ?: throw IllegalArgumentException("Teller ID is required"),
                    finalBalance = request.actualBalance,
                    notes = request.notes
                )
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to close cash drawer",
                    data = null as String?
                ))
            }
        }

        // Allocate float to teller
        post("/float/allocate") {
            try {
                val request = call.receive<TellerFloatAllocationRequest>()
                val result = tellerService.allocateFloatToTeller(
                    tellerId = request.tellerId,
                    branchId = request.branchId,
                    sourceWalletId = request.sourceWalletId,
                    amount = request.amount,
                    allocatedBy = request.allocatedBy,
                    purpose = request.purpose
                )
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to allocate float",
                    data = null as String?
                ))
            }
        }

        // Customer account lookup
        get("/account/lookup") {
            try {
                val accountNumber = call.request.queryParameters["accountNumber"]
                val customerId = call.request.queryParameters["customerId"]

                when {
                    accountNumber != null -> {
                        val result = accountService.getAccountByNumber(accountNumber)
                        call.respond(HttpStatusCode.OK, result)
                    }
                    customerId != null -> {
                        val accounts = accountService.getAccountsByCustomerId(UUID.fromString(customerId))
                        val result = ApiResponse(
                            success = accounts.success,
                            message = accounts.message,
                            data = accounts.data.firstOrNull()
                        )
                        call.respond(HttpStatusCode.OK, result)
                    }
                    else -> {
                        val result = ApiResponse(
                            success = false,
                            message = "Account number or customer ID is required",
                            data = null as AccountDto?
                        )
                        call.respond(HttpStatusCode.BadRequest, result)
                    }
                }
            } catch (e: Exception) {
                val errorResponse = ApiResponse(
                    success = false,
                    message = "Failed to lookup account: ${e.message}",
                    data = null as AccountDto?,
                    error = e.message
                )
                call.respond(HttpStatusCode.BadRequest, errorResponse)
            }
        }

        // Get customer details
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"] ?: throw IllegalArgumentException("Customer ID is required")
                val result = customerService.getCustomerById(UUID.fromString(customerId))
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to get customer: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Get customer accounts
        get("/customer/{customerId}/accounts") {
            try {
                val customerId = call.parameters["customerId"] ?: throw IllegalArgumentException("Customer ID is required")
                val result = accountService.getAccountsByCustomerId(UUID.fromString(customerId))
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to get customer accounts: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Search customers  
        get("/customer/search") {
            println("=== TELLER CUSTOMER SEARCH ENDPOINT HIT ===")
            try {
                val query = call.request.queryParameters["q"] ?: ""
                val branchId = call.request.queryParameters["branchId"]

                println("Query: '$query'")
                println("BranchId: $branchId")
                println("Full URL: ${call.request.uri}")
                println("Request headers: ${call.request.headers.names()}")

                if (query.isBlank()) {
                    val emptyResponse = org.dals.project.models.ApiResponse(
                        success = true,
                        message = "Please enter a search query",
                        data = emptyList<CustomerDto>()
                    )
                    println("Empty query - returning empty list")
                    call.respond(HttpStatusCode.OK, emptyResponse)
                    return@get
                }

                // Try without branch filter first to see if we get any results
                println("Searching ALL customers first (ignoring branch filter)...")
                val resultWithoutBranch = customerService.searchCustomers(
                    query = query,
                    branchId = null  // Search all branches
                )

                println("Search without branch filter: found ${resultWithoutBranch.data?.size ?: 0} customers")

                // Now try with branch filter if provided
                val result = if (branchId != null) {
                    println("Now searching with branch filter: $branchId")
                    customerService.searchCustomers(
                        query = query,
                        branchId = UUID.fromString(branchId)
                    )
                } else {
                    resultWithoutBranch
                }

                println("Final search result - success=${result.success}, found=${result.data?.size ?: 0} customers")
                if (result.data != null && result.data.isNotEmpty()) {
                    println("Found customers: ${result.data.map { "${it.firstName} ${it.lastName} (${it.customerNumber})" }}")
                }

                // ALWAYS return 200 OK
                call.respond(HttpStatusCode.OK, result)
            } catch (e: Exception) {
                println("ERROR in customer search: ${e.message}")
                e.printStackTrace()
                val errorResponse = org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to search customers: ${e.message}",
                    data = emptyList<CustomerDto>(),
                    error = e.message
                )
                call.respond(HttpStatusCode.OK, errorResponse)
            }
        }

        // Process deposit
        post("/transaction/deposit") {
            try {
                val request = call.receive<CreateTransactionRequest>()
                val result = transactionService.createTransaction(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to process deposit: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Process withdrawal
        post("/transaction/withdrawal") {
            try {
                val request = call.receive<CreateTransactionRequest>()
                val result = transactionService.createTransaction(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to process withdrawal: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Process transfer
        post("/transaction/transfer") {
            try {
                val request = call.receive<CreateTransactionRequest>()
                val result = transactionService.createTransaction(request)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to process transfer: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Get account transactions
        get("/account/{accountId}/transactions") {
            try {
                val accountId = call.parameters["accountId"] ?: throw IllegalArgumentException("Account ID is required")
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val result = transactionService.getTransactionsByAccountId(UUID.fromString(accountId), limit)
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = "Failed to get transactions: ${e.message}",
                    data = null as String?
                ))
            }
        }

        // Get teller dashboard stats
        get("/dashboard/{tellerId}") {
            try {
                val tellerId = call.parameters["tellerId"] ?: throw IllegalArgumentException("Teller ID is required")

                // Get cash drawer info
                val drawer = tellerService.getTellerCashDrawer(tellerId)

                // Get recent transactions
                val transactions = tellerService.getTellerTransactions(tellerId, 10)

                val totalDeposits = drawer.data?.totalDeposits?.toInt() ?: 0
                val totalWithdrawals = drawer.data?.totalWithdrawals?.toInt() ?: 0

                val dashboardData = mapOf(
                    "cashDrawer" to drawer.data,
                    "recentTransactions" to transactions.data,
                    "stats" to mapOf(
                        "totalTransactions" to (totalDeposits + totalWithdrawals),
                        "totalCashIn" to drawer.data?.totalCashIn,
                        "totalCashOut" to drawer.data?.totalCashOut,
                        "currentBalance" to drawer.data?.currentBalance
                    )
                )

                call.respond(org.dals.project.models.ApiResponse(
                    success = true,
                    message = "Dashboard data retrieved",
                    data = dashboardData
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, org.dals.project.models.ApiResponse(
                    success = false,
                    message = e.message ?: "Failed to get dashboard data",
                    data = null as String?
                ))
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class TellerFloatAllocationRequest(
    val tellerId: String,
    val branchId: String,
    val sourceWalletId: String,
    val amount: Double,
    val allocatedBy: String,
    val purpose: String? = null
)
