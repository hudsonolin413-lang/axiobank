package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.*
import org.dals.project.services.CardTransactionService

fun Route.cardTransactionRoutes(cardTransactionService: CardTransactionService) {
    route("/card-transactions") {

        // Process online payment
        post("/online-payment") {
            try {
                val request = call.receive<OnlinePaymentRequest>()

                println("🌐 Processing online payment: ${request.merchantName}, Amount: ${request.amount}")

                val response = cardTransactionService.processOnlinePayment(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                println("❌ Online payment failed: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardTransactionResponse(
                        success = false,
                        message = e.message ?: "Online payment failed"
                    )
                )
            } catch (e: Exception) {
                println("❌ Online payment error: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardTransactionResponse(
                        success = false,
                        message = "An error occurred while processing online payment"
                    )
                )
            }
        }

        // Process POS transaction
        post("/pos-transaction") {
            try {
                val request = call.receive<POSTransactionRequest>()

                println("🌐 Processing POS transaction: ${request.merchantName}, Amount: ${request.amount}")

                val response = cardTransactionService.processPOSTransaction(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                println("❌ POS transaction failed: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardTransactionResponse(
                        success = false,
                        message = e.message ?: "POS transaction failed"
                    )
                )
            } catch (e: Exception) {
                println("❌ POS transaction error: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardTransactionResponse(
                        success = false,
                        message = "An error occurred while processing POS transaction"
                    )
                )
            }
        }

        // Process bill payment
        post("/bill-payment") {
            try {
                val request = call.receive<BillPaymentRequest>()

                println("🌐 Processing bill payment: ${request.billType} to ${request.billerName}, Amount: ${request.amount}")

                val response = cardTransactionService.processBillPayment(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                println("❌ Bill payment failed: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardTransactionResponse(
                        success = false,
                        message = e.message ?: "Bill payment failed"
                    )
                )
            } catch (e: Exception) {
                println("❌ Bill payment error: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardTransactionResponse(
                        success = false,
                        message = "An error occurred while processing bill payment"
                    )
                )
            }
        }

        // Process card transfer
        post("/transfer") {
            try {
                val request = call.receive<CardTransferRequest>()

                println("🌐 Processing card transfer to ${request.destinationAccountNumber}, Amount: ${request.amount}")

                val response = cardTransactionService.processCardTransfer(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                println("❌ Card transfer failed: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardTransactionResponse(
                        success = false,
                        message = e.message ?: "Card transfer failed"
                    )
                )
            } catch (e: Exception) {
                println("❌ Card transfer error: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardTransactionResponse(
                        success = false,
                        message = "An error occurred while processing card transfer"
                    )
                )
            }
        }

        // Process ATM withdrawal
        post("/atm-withdrawal") {
            try {
                val request = call.receive<ATMWithdrawalRequest>()

                println("🌐 Processing ATM withdrawal at ${request.atmLocation}, Amount: ${request.amount}")

                val response = cardTransactionService.processATMWithdrawal(request)

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                println("❌ ATM withdrawal failed: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    CardTransactionResponse(
                        success = false,
                        message = e.message ?: "ATM withdrawal failed"
                    )
                )
            } catch (e: Exception) {
                println("❌ ATM withdrawal error: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    CardTransactionResponse(
                        success = false,
                        message = "An error occurred while processing ATM withdrawal"
                    )
                )
            }
        }
    }
}
