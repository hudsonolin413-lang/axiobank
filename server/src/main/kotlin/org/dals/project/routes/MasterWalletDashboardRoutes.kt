package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminMasterWalletDashboardService
import org.dals.project.services.TransactionFeeService
import org.dals.project.models.ApiResponse
import org.dals.project.models.ListResponse
import java.util.*

/**
 * Master wallet dashboard routes
 */
fun Route.masterWalletDashboardRoutes() {
    val walletDashboardService = AdminMasterWalletDashboardService()
    val feeService = TransactionFeeService()

    route("/admin") {
        route("/master-wallet") {
        /**
         * GET /admin/master-wallet/dashboard
         * Get master wallet dashboard overview
         */
        get("/dashboard") {
            try {
                val response = walletDashboardService.getMasterWalletDashboard()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving master wallet dashboard: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/master-wallet/wallets
         * Get all master wallets with details
         */
        get("/wallets") {
            try {
                val response = walletDashboardService.getAllMasterWallets()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving master wallets: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/master-wallet/transactions
         * Get master wallet transactions
         * Query params: walletId, limit
         */
        get("/transactions") {
            try {
                val walletId = call.request.queryParameters["walletId"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

                val response = walletDashboardService.getMasterWalletTransactions(walletId, limit)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving master wallet transactions: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/master-wallet/float-allocations
         * Get float allocations
         * Query params: status
         */
        get("/float-allocations") {
            try {
                val status = call.request.queryParameters["status"]

                val response = walletDashboardService.getFloatAllocations(status)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ListResponse.empty<Unit>("Error retrieving float allocations: ${e.message}"))
            }
        }

        /**
         * GET /admin/master-wallet/security-alerts
         * Get wallet security alerts
         */
        get("/security-alerts") {
            try {
                val response = walletDashboardService.getWalletSecurityAlerts()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving wallet security alerts: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/master-wallet/reconciliations
         * Get reconciliation records
         * Query params: walletId
         */
        get("/reconciliations") {
            try {
                val walletId = call.request.queryParameters["walletId"]

                val response = walletDashboardService.getReconciliationRecords(walletId)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving reconciliation records: ${e.message}"
                ))
            }
        }

        // ==================== TRANSACTION FEE MANAGEMENT ROUTES ====================

        /**
         * GET /admin/master-wallet/fee-structures
         * Get all transaction fee structures
         */
        get("/fee-structures") {
            try {
                val response = feeService.getAllFeeStructures()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving fee structures: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/master-wallet/fee-structures/initialize
         * Initialize default fee structures
         */
        post("/fee-structures/initialize") {
            try {
                val request = call.receive<InitializeFeeStructuresRequest>()
                val createdBy = UUID.fromString(request.createdBy)

                val response = feeService.initializeFeeStructures(createdBy)
                call.respond(if (response.success) HttpStatusCode.Created else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error initializing fee structures: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/master-wallet/fee-structures
         * Create a new fee structure
         */
        post("/fee-structures") {
            try {
                val request = call.receive<CreateFeeStructureRequest>()
                val response = feeService.createFeeStructure(request)
                call.respond(if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error creating fee structure: ${e.message}"
                ))
            }
        }

        /**
         * PUT /admin/master-wallet/fee-structures/{id}
         * Update an existing fee structure
         */
        put("/fee-structures/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Fee structure ID is required")
                val request = call.receive<UpdateFeeStructureRequest>()
                val response = feeService.updateFeeStructure(UUID.fromString(id), request)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error updating fee structure: ${e.message}"
                ))
            }
        }

        /**
         * DELETE /admin/master-wallet/fee-structures/{id}
         * Delete (deactivate) a fee structure
         */
        delete("/fee-structures/{id}") {
            try {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Fee structure ID is required")
                val response = feeService.deleteFeeStructure(UUID.fromString(id))
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error deleting fee structure: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/master-wallet/profit-summary
         * Get company profit summary
         */
        get("/profit-summary") {
            try {
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]

                val response = feeService.getTotalFeesCollected(startDate, endDate)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error retrieving profit summary: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/master-wallet/create-profit-wallet
         * Create the company profit wallet
         */
        post("/create-profit-wallet") {
            try {
                val request = call.receive<CreateProfitWalletRequest>()
                val response = feeService.createCompanyProfitWallet(request)
                call.respond(if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(
                    success = false,
                    message = "Error creating profit wallet: ${e.message}"
                ))
            }
        }
        }
    }
}

// Request DTOs
@kotlinx.serialization.Serializable
data class InitializeFeeStructuresRequest(
    val createdBy: String
)

@kotlinx.serialization.Serializable
data class CreateFeeStructureRequest(
    val transactionType: String,
    val minAmount: Double,
    val maxAmount: Double,
    val feeAmount: Double,
    val feePercentage: Double? = null,
    val currency: String = "KES",
    val description: String? = null,
    val createdBy: String
)

@kotlinx.serialization.Serializable
data class UpdateFeeStructureRequest(
    val transactionType: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val feeAmount: Double? = null,
    val feePercentage: Double? = null,
    val isActive: Boolean? = null,
    val description: String? = null
)

@kotlinx.serialization.Serializable
data class CreateProfitWalletRequest(
    val walletName: String,
    val currency: String = "KES",
    val createdBy: String,
    val managedBy: String
)
