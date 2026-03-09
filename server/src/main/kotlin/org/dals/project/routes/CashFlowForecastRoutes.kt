package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.CashFlowForecastService
import org.dals.project.services.GenerateForecastRequest
import java.time.LocalDate

fun Route.cashFlowForecastRoutes() {
    route("/cash-flow-forecast") {

        // Generate forecasts
        post("/generate") {
            try {
                val request = call.receive<GenerateForecastRequest>()
                val result = CashFlowForecastService.generateForecasts(request)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to generate forecasts"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get forecasts for account
        get("/account/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Account ID is required")
                    )

                val startDate = call.request.queryParameters["startDate"]?.let { LocalDate.parse(it) }
                val endDate = call.request.queryParameters["endDate"]?.let { LocalDate.parse(it) }

                val result = CashFlowForecastService.getForecasts(accountId, startDate, endDate)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get forecasts"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Get cash flow analysis
        get("/analysis/{accountId}") {
            try {
                val accountId = call.parameters["accountId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Account ID is required")
                    )

                val customerId = call.request.queryParameters["customerId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Customer ID is required")
                    )

                val result = CashFlowForecastService.getCashFlowAnalysis(accountId, customerId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to get cash flow analysis"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }

        // Update actual values for a forecast
        post("/update-actual/{forecastId}") {
            try {
                val forecastId = call.parameters["forecastId"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Forecast ID is required")
                    )

                val result = CashFlowForecastService.updateActualValues(forecastId)

                if (result.isSuccess) {
                    call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (result.exceptionOrNull()?.message ?: "Failed to update forecast"))
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Internal server error"))
                )
            }
        }
    }
}
