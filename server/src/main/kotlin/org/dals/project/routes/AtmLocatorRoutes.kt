package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*

fun Route.atmLocatorRoutes() {
    route("/atm") {
        post("/create") {
            try {
                val request = call.receive<CreateAtmRequest>()
                val result = AtmLocatorService.createAtm(request)
                if (result.isSuccess) call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/nearby") {
            try {
                val lat = call.request.queryParameters["latitude"]!!.toDouble()
                val lon = call.request.queryParameters["longitude"]!!.toDouble()
                val radius = call.request.queryParameters["radius"]?.toDouble() ?: 10.0
                val limit = call.request.queryParameters["limit"]?.toInt() ?: 20

                val result = AtmLocatorService.findNearbyAtms(lat, lon, radius, limit)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/search") {
            try {
                val city = call.request.queryParameters["city"]
                val state = call.request.queryParameters["state"]
                val zipCode = call.request.queryParameters["zipCode"]
                val atmType = call.request.queryParameters["atmType"]
                val features = call.request.queryParameters.getAll("features")

                val result = AtmLocatorService.searchAtms(city, state, zipCode, atmType, features)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/{atmId}") {
            try {
                val result = AtmLocatorService.getAtmById(call.parameters["atmId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/update-status/{atmId}") {
            try {
                val status = call.request.queryParameters["status"]!!
                val cashAvailable = call.request.queryParameters["cashAvailable"]?.toBoolean()
                val result = AtmLocatorService.updateAtmStatus(call.parameters["atmId"]!!, status, cashAvailable)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
