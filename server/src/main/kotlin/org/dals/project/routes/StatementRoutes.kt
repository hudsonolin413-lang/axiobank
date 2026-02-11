package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.GenerateStatementRequest
import org.dals.project.services.StatementService

fun Route.statementRoutes() {
    val statementService = StatementService()

    route("/api/statements") {
        // Generate and send statement via email
        post("/generate") {
            try {
                val request = call.receive<GenerateStatementRequest>()
                val response = statementService.generateAndSendStatement(request)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to generate statement",
                        "error" to e.message
                    )
                )
            }
        }

        // Alternative endpoint for downloading statement (without email)
        post("/download") {
            try {
                val request = call.receive<GenerateStatementRequest>()
                val downloadRequest = request.copy(sendEmail = false)
                val response = statementService.generateAndSendStatement(downloadRequest)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to download statement",
                        "error" to e.message
                    )
                )
            }
        }
    }
}
