package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*

fun Route.internationalTransferRoutes() {
    route("/international-transfer") {
        get("/quote") {
            try {
                val amount = call.request.queryParameters["amount"]!!.toDouble()
                val fromCurrency = call.request.queryParameters["fromCurrency"] ?: "USD"
                val toCurrency = call.request.queryParameters["toCurrency"]!!
                val country = call.request.queryParameters["country"]!!

                val result = InternationalTransferService.getTransferQuote(amount, fromCurrency, toCurrency, country)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/create") {
            try {
                val request = call.receive<CreateInternationalTransferRequest>()
                val result = InternationalTransferService.createInternationalTransfer(request)
                if (result.isSuccess) call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/{transferId}") {
            try {
                val result = InternationalTransferService.getInternationalTransfer(call.parameters["transferId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/customer/{customerId}") {
            try {
                val result = InternationalTransferService.getCustomerTransfers(call.parameters["customerId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/update-status/{transferId}") {
            try {
                val status = call.request.queryParameters["status"]!!
                val reason = call.request.queryParameters["reason"]
                val result = InternationalTransferService.updateTransferStatus(call.parameters["transferId"]!!, status, reason)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/exchange-rate") {
            try {
                val fromCurrency = call.request.queryParameters["from"]!!
                val toCurrency = call.request.queryParameters["to"]!!
                val rate = call.request.queryParameters["rate"]!!.toDouble()
                val buyRate = call.request.queryParameters["buyRate"]!!.toDouble()
                val sellRate = call.request.queryParameters["sellRate"]!!.toDouble()

                val result = InternationalTransferService.setExchangeRate(fromCurrency, toCurrency, rate, buyRate, sellRate)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
