package org.dals.project.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SmsService {
    // SMS configuration - Hardcoded for AxioBank with Africastalking
    private val provider = System.getenv("SMS_PROVIDER") ?: "africas_talking"

    // Africastalking configuration
    private val atApiKey = System.getenv("AT_API_KEY") ?: "atsk_51be0f8b1e3d2f690b17ee3c08947ed49db44194c4b960a8599b0ace7a6c686912b07e3f"
    private val atUsername = System.getenv("AT_USERNAME") ?: "axiobank"
    private val atShortcode = System.getenv("AT_SHORTCODE") ?: "AxioBank" // Your sender ID

    // Legacy Twilio configuration (fallback)
    private val accountSid = System.getenv("TWILIO_ACCOUNT_SID") ?: ""
    private val authToken = System.getenv("TWILIO_AUTH_TOKEN") ?: ""
    private val fromNumber = System.getenv("TWILIO_FROM_NUMBER") ?: ""

    private val client = HttpClient(CIO)

    suspend fun sendSms(
        to: String,
        message: String
    ): Result<Unit> {
        return try {
            when (provider.lowercase()) {
                "twilio" -> {
                    if (accountSid.isEmpty() || authToken.isEmpty() || fromNumber.isEmpty()) {
                        println("⚠️ SMS not sent: Twilio not configured")
                        return Result.failure(Exception("Twilio not configured"))
                    }
                    sendViaTwilio(to, message)
                }
                "africas_talking" -> sendViaAfricasTalking(to, message)
                else -> {
                    println("⚠️ Unknown SMS provider: $provider")
                    Result.failure(Exception("Unknown SMS provider"))
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to send SMS to $to: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun sendViaTwilio(to: String, message: String): Result<Unit> {
        return try {
            val response = client.post("https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json") {
                basicAuth(accountSid, authToken)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("From=$fromNumber&To=$to&Body=${message.encodeURLParameter()}")
            }

            if (response.status.isSuccess()) {
                println("✅ SMS sent to: $to via Twilio")
                Result.success(Unit)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Twilio SMS failed: ${response.status} - $errorBody")
                Result.failure(Exception("SMS failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Twilio SMS error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun sendViaAfricasTalking(to: String, message: String): Result<Unit> {
        return try {
            if (atApiKey.isEmpty() || atUsername.isEmpty()) {
                println("⚠️ SMS not sent: Africastalking not configured")
                return Result.failure(Exception("Africastalking not configured"))
            }

            // Format phone number for Africastalking (must include country code)
            val formattedTo = if (!to.startsWith("+")) "+$to" else to

            println("📱 Sending SMS via Africastalking...")
            println("   Username: $atUsername")
            println("   To: $formattedTo")
            println("   API Key: ${atApiKey.take(20)}...")

            val response = client.post("https://api.africastalking.com/version1/messaging") {
                header("apiKey", atApiKey)
                header("Accept", "application/json")
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody("username=${atUsername.encodeURLParameter()}&to=${formattedTo.encodeURLParameter()}&message=${message.encodeURLParameter()}")
            }

            val responseBody = response.bodyAsText()
            println("📱 Africastalking HTTP status: ${response.status}")
            println("📱 Africastalking response: $responseBody")

            if (response.status.isSuccess()) {
                // Parse response to check if message was accepted
                val json = Json.parseToJsonElement(responseBody).jsonObject
                val smsMessageData = json["SMSMessageData"]?.jsonObject
                val recipients = smsMessageData?.get("Recipients")?.toString() ?: ""

                if (recipients.contains("Success")) {
                    println("✅ SMS sent to: $to via Africastalking")
                    Result.success(Unit)
                } else {
                    println("❌ Africastalking SMS rejected: $recipients")
                    Result.failure(Exception("SMS rejected by Africastalking"))
                }
            } else {
                println("❌ Africastalking SMS failed: ${response.status} - $responseBody")
                Result.failure(Exception("SMS failed: ${response.status}"))
            }
        } catch (e: Exception) {
            println("❌ Africastalking SMS error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendBulkSms(
        recipients: List<String>,
        message: String
    ): Pair<Int, Int> {
        var successCount = 0
        var failureCount = 0

        recipients.forEach { phoneNumber ->
            val result = sendSms(phoneNumber, message)
            if (result.isSuccess) {
                successCount++
            } else {
                failureCount++
            }
        }

        return Pair(successCount, failureCount)
    }

    suspend fun sendNotificationSms(
        to: String,
        title: String,
        message: String,
        priority: String
    ): Result<Unit> {
        val priorityPrefix = when (priority) {
            "URGENT" -> "[URGENT] "
            "HIGH" -> "[IMPORTANT] "
            else -> ""
        }

        val smsBody = "$priorityPrefix$title: $message - AxioBank"

        // SMS has character limits (160 for standard SMS)
        val truncatedBody = if (smsBody.length > 160) {
            smsBody.substring(0, 157) + "..."
        } else {
            smsBody
        }

        return sendSms(to, truncatedBody)
    }

    fun close() {
        client.close()
    }
}
