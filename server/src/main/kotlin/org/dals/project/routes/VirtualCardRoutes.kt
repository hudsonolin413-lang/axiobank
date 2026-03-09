package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*

fun Route.virtualCardRoutes() {
    route("/virtual-card") {
        post("/create") {
            try {
                val request = call.receive<CreateVirtualCardRequest>()
                val result = VirtualCardService.createVirtualCard(request)
                if (result.isSuccess) call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/customer/{customerId}") {
            try {
                val result = VirtualCardService.getVirtualCards(call.parameters["customerId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/freeze/{cardId}") {
            try {
                val result = VirtualCardService.freezeCard(call.parameters["cardId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/unfreeze/{cardId}") {
            try {
                val result = VirtualCardService.unfreezeCard(call.parameters["cardId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/cancel/{cardId}") {
            try {
                val result = VirtualCardService.cancelCard(call.parameters["cardId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
