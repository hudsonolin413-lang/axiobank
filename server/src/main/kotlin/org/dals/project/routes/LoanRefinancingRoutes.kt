package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*

fun Route.loanRefinancingRoutes() {
    route("/loan-refinancing") {
        post("/analyze") {
            try {
                val loanId = call.request.queryParameters["loanId"]!!
                val rate = call.request.queryParameters["proposedRate"]!!.toDouble()
                val term = call.request.queryParameters["proposedTerm"]!!.toInt()
                val costs = call.request.queryParameters["closingCosts"]?.toDouble() ?: 0.0

                val result = LoanRefinancingService.analyzeRefinancingOpportunity(loanId, rate, term, costs)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/apply") {
            try {
                val request = call.receive<CreateRefinancingApplicationRequest>()
                val result = LoanRefinancingService.createRefinancingApplication(request)
                if (result.isSuccess) call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/{refinancingId}") {
            try {
                val result = LoanRefinancingService.getRefinancingApplication(call.parameters["refinancingId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/customer/{customerId}") {
            try {
                val result = LoanRefinancingService.getCustomerRefinancingApplications(call.parameters["customerId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/approve/{refinancingId}") {
            try {
                val reviewerId = call.request.queryParameters["reviewerId"]!!
                val result = LoanRefinancingService.approveRefinancing(call.parameters["refinancingId"]!!, reviewerId)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
