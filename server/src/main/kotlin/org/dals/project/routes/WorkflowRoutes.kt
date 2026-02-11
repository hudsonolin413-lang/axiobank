package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.AdminWorkflowService

/**
 * Workflow routes for approval management
 */
fun Route.workflowRoutes() {
    val workflowService = AdminWorkflowService()

    route("/admin/workflow") {
        /**
         * GET /admin/workflow/approvals
         * Get all workflow approvals
         */
        get("/approvals") {
            try {
                val status = call.request.queryParameters["status"]

                val response = if (status != null) {
                    workflowService.getWorkflowApprovalsByStatus(status)
                } else {
                    workflowService.getWorkflowApprovals()
                }

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/workflow/approvals: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error retrieving workflow approvals: ${e.message}"
                ))
            }
        }

        /**
         * POST /admin/workflow/approvals/{approvalId}/process
         * Process a workflow approval (approve/reject)
         */
        post("/approvals/{approvalId}/process") {
            try {
                val approvalId = call.parameters["approvalId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("success" to false, "message" to "Approval ID is required")
                )

                val requestBody = call.receive<Map<String, String>>()
                val action = requestBody["action"] ?: "PENDING"
                val comments = requestBody["comments"] ?: ""

                val response = workflowService.processWorkflowApproval(approvalId, action, comments)
                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }
            } catch (e: Exception) {
                println("Error in /admin/workflow/approvals/process: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "success" to false,
                    "message" to "Error processing workflow approval: ${e.message}"
                ))
            }
        }
    }
}
