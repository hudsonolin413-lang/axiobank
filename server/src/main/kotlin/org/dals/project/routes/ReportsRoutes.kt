package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminReportsService

/**
 * Reports and analytics routes
 */
fun Route.reportsRoutes() {
    val reportsService = AdminReportsService()

    route("/admin/reports") {
        /**
         * POST /admin/reports/generate
         * Generate a report
         * Request body: { "reportType": "USER_ACTIVITY", "startDate": "2025-01-01", "endDate": "2025-12-31" }
         */
        post("/generate") {
            try {
                val requestBody = call.receive<Map<String, String>>()
                val reportType = requestBody["reportType"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Report type is required")
                )
                val startDate: String? = requestBody["startDate"]
                val endDate: String? = requestBody["endDate"]

                val response = reportsService.generateReport(reportType, startDate, endDate)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error generating report: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/reports/financial-summary
         * Get financial summary report
         * Query params: startDate, endDate
         */
        get("/financial-summary") {
            try {
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]

                val response = reportsService.getFinancialSummary(startDate, endDate)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving financial summary: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/reports/transaction-analytics
         * Get transaction analytics
         * Query params: days (default: 30)
         */
        get("/transaction-analytics") {
            try {
                val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30

                val response = reportsService.getTransactionAnalytics(days)
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving transaction analytics: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/reports/customer-analytics
         * Get customer analytics
         */
        get("/customer-analytics") {
            try {
                val response = reportsService.getCustomerAnalytics()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving customer analytics: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/reports/loan-portfolio
         * Get loan portfolio report
         */
        get("/loan-portfolio") {
            try {
                val response = reportsService.getLoanPortfolioReport()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving loan portfolio: ${e.message}"
                ))
            }
        }

        /**
         * GET /admin/reports/branch-performance
         * Get branch performance report
         */
        get("/branch-performance") {
            try {
                val response = reportsService.getBranchPerformanceReport()
                call.respond(if (response.success) HttpStatusCode.OK else HttpStatusCode.InternalServerError, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving branch performance: ${e.message}"
                ))
            }
        }
    }
}
