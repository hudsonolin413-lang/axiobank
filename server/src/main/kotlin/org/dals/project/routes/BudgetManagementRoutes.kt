package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.services.*

fun Route.budgetManagementRoutes() {
    route("/budget") {
        post("/create") {
            try {
                val request = call.receive<CreateBudgetRequest>()
                val result = BudgetManagementService.createBudget(request)
                if (result.isSuccess) call.respond(HttpStatusCode.Created, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/customer/{customerId}") {
            try {
                val result = BudgetManagementService.getBudgets(call.parameters["customerId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        get("/summary/{customerId}") {
            try {
                val result = BudgetManagementService.getBudgetSummary(call.parameters["customerId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        post("/update-spending/{budgetId}") {
            try {
                val transactionId = call.request.queryParameters["transactionId"]!!
                val amount = call.request.queryParameters["amount"]!!.toDouble()
                val category = call.request.queryParameters["category"]!!
                val result = BudgetManagementService.updateBudgetSpending(
                    call.parameters["budgetId"]!!, transactionId, amount, category
                )
                if (result.isSuccess) call.respond(HttpStatusCode.OK, result.getOrNull()!!)
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        delete("/{budgetId}") {
            try {
                val result = BudgetManagementService.deleteBudget(call.parameters["budgetId"]!!)
                if (result.isSuccess) call.respond(HttpStatusCode.OK, mapOf("message" to "Budget deleted"))
                else call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.exceptionOrNull()?.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
