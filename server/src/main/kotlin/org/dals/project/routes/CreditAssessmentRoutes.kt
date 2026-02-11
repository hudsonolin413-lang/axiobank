package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.CreateCreditAssessmentRequest
import org.dals.project.models.CustomerCareListResponse
import org.dals.project.models.CreditAssessmentDto
import org.dals.project.services.CreditAssessmentService
import java.util.*

fun Route.creditAssessmentRoutes(creditAssessmentService: CreditAssessmentService) {
    route("/credit-assessments") {

        // Specific routes must come before parameterized routes
        post("/sync-credit-scores") {
            try {
                val response = creditAssessmentService.syncCreditScoresToCustomers()
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to sync credit scores: ${e.message}"
                    )
                )
            }
        }

        get("/customer/{customerId}") {
            val customerId = call.parameters["customerId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid customer ID")
            val response = creditAssessmentService.getCreditAssessmentsByCustomerId(customerId)
            call.respond(HttpStatusCode.OK, response)
        }

        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 100
                val response = creditAssessmentService.getAllCreditAssessments(page, pageSize)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CustomerCareListResponse<CreditAssessmentDto>(
                        success = false,
                        message = "Failed to fetch credit assessments: ${e.message}",
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
            val response = creditAssessmentService.getCreditAssessmentById(id)
            call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.NotFound, response)
        }

        post {
            val request = call.receive<CreateCreditAssessmentRequest>()
            val response = creditAssessmentService.createCreditAssessment(request)
            call.respond(
                if (response.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                response
            )
        }
    }
}
