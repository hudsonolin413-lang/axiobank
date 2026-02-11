package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.CreateLoanApplicationRequest
import org.dals.project.models.CustomerCareListResponse
import org.dals.project.models.LoanApplicationDto
import org.dals.project.models.LoanDto
import org.dals.project.services.LoanService
import java.util.*

fun Route.loanRoutes(loanService: LoanService) {
    // Loan Applications - must come before /loans/{id} to avoid route conflicts
    route("/loans/applications") {
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100
                val response = loanService.getAllLoanApplications(page, pageSize)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<LoanApplicationDto>(
                        success = false,
                        message = "Failed to fetch loan applications: ${e.message}",
                        data = emptyList(),
                        total = 0,
                        page = 1,
                        pageSize = 100,
                        timestamp = java.time.Instant.now().toString()
                    )
                )
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            val response = loanService.getLoanApplicationById(id)
            call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
        }

        get("/customer/{customerId}") {
            val customerId = call.parameters["customerId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid customer ID")
            val response = loanService.getLoanApplicationsByCustomerId(customerId)
            call.respond(HttpStatusCode.OK, response)
        }

        post {
            val request = call.receive<CreateLoanApplicationRequest>()
            val response = loanService.createLoanApplication(request)
            call.respond(
                if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                response
            )
        }

        put("/{id}/status") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val body = call.receive<Map<String, Any?>>()
            val status = body["status"] as? String
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Status is required")

            val reviewedBy = (body["reviewedBy"] as? String)?.let { UUID.fromString(it) }
            val approvedAmount = body["approvedAmount"] as? String
            val interestRate = body["interestRate"] as? String
            val termMonths = body["termMonths"] as? Int
            val comments = body["comments"] as? String

            val response = loanService.updateLoanApplicationStatus(
                id, status, reviewedBy, approvedAmount, interestRate, termMonths, comments
            )
            call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, response)
        }
    }

    // Loans
    route("/loans") {
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100
                val response = loanService.getAllLoans(page, pageSize)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<LoanDto>(
                        success = false,
                        message = "Failed to fetch loans: ${e.message}",
                        data = emptyList(),
                        total = 0,
                        page = 1,
                        pageSize = 100,
                        timestamp = java.time.Instant.now().toString()
                    )
                )
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")
            val response = loanService.getLoanById(id)
            call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
        }

        get("/customer/{customerId}") {
            val customerId = call.parameters["customerId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid customer ID")
            val response = loanService.getLoansByCustomerId(customerId)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/from-application") {
            val body = call.receive<Map<String, String>>()
            val applicationId = body["applicationId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Application ID is required")
            val loanOfficerId = body["loanOfficerId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Loan Officer ID is required")
            val branchId = body["branchId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Branch ID is required")

            val response = loanService.createLoanFromApplication(applicationId, loanOfficerId, branchId)
            call.respond(
                if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                response
            )
        }

        // Make loan payment
        post("/{loanId}/payments") {
            try {
                val request = call.receive<org.dals.project.models.MakeLoanPaymentRequest>()
                val response = loanService.makeLoanPayment(request)
                call.respond(
                    if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                    response
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    org.dals.project.models.ApiResponse<org.dals.project.models.LoanPaymentDto>(
                        success = false,
                        message = "Failed to process loan payment: ${e.message}",
                        error = e.message
                    )
                )
            }
        }

        // Get loan payment history
        get("/{loanId}/payments") {
            try {
                val loanId = call.parameters["loanId"]?.let { UUID.fromString(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid loan ID")
                val response = loanService.getLoanPaymentHistory(loanId)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<org.dals.project.models.LoanPaymentDto>(
                        success = false,
                        message = "Failed to retrieve payment history: ${e.message}",
                        data = emptyList(),
                        total = 0,
                        page = 1,
                        pageSize = 10,
                        timestamp = java.time.Instant.now().toString()
                    )
                )
            }
        }
    }
}
