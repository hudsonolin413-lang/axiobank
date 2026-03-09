package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*
import java.util.UUID

fun Route.taxReportRoutes() {
    val taxReportService = TaxReportService()

    route("/tax-reports") {
        // Get all reports for a customer
        get("/{customerId}") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val reports = taxReportService.getAllReports(customerId)
                call.respond(HttpStatusCode.OK, reports)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid customer ID")))
            }
        }

        // Get reports by year
        get("/{customerId}/year/{year}") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val year = call.parameters["year"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                val reports = taxReportService.getReportsByYear(customerId, year)
                call.respond(HttpStatusCode.OK, reports)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid parameters")))
            }
        }

        // Get tax summary for a year
        get("/{customerId}/summary/{year}") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val year = call.parameters["year"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid year")
                val summary = taxReportService.getTaxSummary(customerId, year)
                call.respond(HttpStatusCode.OK, summary)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to get summary")))
            }
        }

        // Get available years
        get("/{customerId}/available-years") {
            try {
                val customerId = UUID.fromString(call.parameters["customerId"])
                val years = taxReportService.getAvailableYears(customerId)
                call.respond(HttpStatusCode.OK, mapOf("years" to years))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to get years")))
            }
        }

        // Get report by ID
        get("/detail/{reportId}") {
            try {
                val reportId = UUID.fromString(call.parameters["reportId"])
                val report = taxReportService.getReportById(reportId)
                if (report != null) {
                    call.respond(HttpStatusCode.OK, report)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid report ID")))
            }
        }

        // Generate new report
        post("/generate") {
            try {
                val request = call.receive<GenerateTaxReportRequest>()
                val report = taxReportService.generateReport(request)
                call.respond(HttpStatusCode.Created, report)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to generate report")))
            }
        }

        // Download report
        get("/download/{reportId}") {
            try {
                val reportId = UUID.fromString(call.parameters["reportId"])
                val documentUrl = taxReportService.downloadReport(reportId)
                if (documentUrl != null) {
                    call.respond(HttpStatusCode.OK, mapOf("documentUrl" to documentUrl))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to download report")))
            }
        }

        // Delete report
        delete("/{reportId}") {
            try {
                val reportId = UUID.fromString(call.parameters["reportId"])
                val deleted = taxReportService.deleteReport(reportId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Report deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to delete report")))
            }
        }
    }
}
