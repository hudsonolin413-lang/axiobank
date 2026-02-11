package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.*
import org.dals.project.services.CustomerCareService

fun Route.customerCareAccessRoutes() {
    val customerCareService = CustomerCareService()

    route("/api/customer-care/access-requests") {

        // Create a new customer care access request (for customers)
        post {
            try {
                val request = call.receive<CreateCustomerCareAccessRequest>()
                val response = customerCareService.createAccessRequest(request)

                if (response.success) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<CustomerCareAccessRequestDto>(
                        success = false,
                        message = "Failed to create access request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Get all access requests (for admins)
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                val status = call.request.queryParameters["status"]

                val response = if (status != null) {
                    customerCareService.getAccessRequestsByStatus(status, page, pageSize)
                } else {
                    customerCareService.getAllAccessRequests(page, pageSize)
                }

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<CustomerCareAccessRequestDto>(
                        success = false,
                        message = "Failed to retrieve access requests",
                        data = emptyList(),
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Get access requests by customer ID
        get("/customer/{customerId}") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Customer ID is required")

                val response = customerCareService.getAccessRequestsByCustomerId(customerId)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<List<CustomerCareAccessRequestDto>>(
                        success = false,
                        message = "Failed to retrieve access requests",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Check active access for a customer
        get("/customer/{customerId}/active") {
            try {
                val customerId = call.parameters["customerId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Customer ID is required")

                val response = customerCareService.getActiveAccessForCustomer(customerId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<CustomerCareAccessRequestDto>(
                        success = false,
                        message = "Failed to check active access",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Review an access request (approve/reject) - Admin only
        post("/review") {
            try {
                val review = call.receive<ReviewCustomerCareAccessRequest>()
                // TODO: Extract reviewer ID from authenticated session
                val reviewerId = call.request.headers["X-User-Id"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "User ID not found in headers")

                val response = customerCareService.reviewAccessRequest(review, reviewerId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<CustomerCareAccessRequestDto>(
                        success = false,
                        message = "Failed to review access request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Revoke an access request - Admin only
        post("/revoke") {
            try {
                val revoke = call.receive<RevokeCustomerCareAccessRequest>()
                // TODO: Extract revoker ID from authenticated session
                val revokerId = call.request.headers["X-User-Id"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "User ID not found in headers")

                val response = customerCareService.revokeAccessRequest(revoke, revokerId)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareResponse<CustomerCareAccessRequestDto>(
                        success = false,
                        message = "Failed to revoke access request",
                        error = e.message,
                        timestamp = java.time.LocalDateTime.now().toString()
                    )
                )
            }
        }
    }
}
