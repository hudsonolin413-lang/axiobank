package org.dals.project.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dals.project.models.*
import org.dals.project.services.NotificationService
import java.time.LocalDateTime
import java.util.*

fun Route.notificationRoutes() {
    val notificationService = NotificationService()
    val emailService = org.dals.project.services.EmailService()
    val smsService = org.dals.project.services.SmsService()

    route("/notifications") {

        // Get all notifications for a user
        get("/user/{userId}") {
            try {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID is required")

                val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBoolean() ?: false

                val notifications = notificationService.getNotifications(UUID.fromString(userId), unreadOnly)
                val unreadCount = notificationService.getUnreadCount(UUID.fromString(userId))

                call.respond(
                    HttpStatusCode.OK,
                    NotificationsResponse(
                        success = true,
                        message = "Notifications retrieved successfully",
                        data = notifications,
                        unreadCount = unreadCount,
                        timestamp = LocalDateTime.now().toString()
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    NotificationsResponse(
                        success = false,
                        message = "Failed to retrieve notifications: ${e.message}",
                        timestamp = LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Get unread count
        get("/user/{userId}/unread-count") {
            try {
                val userId = call.parameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "User ID is required")

                val count = notificationService.getUnreadCount(UUID.fromString(userId))

                call.respond(
                    HttpStatusCode.OK,
                    UnreadCountResponse(
                        success = true,
                        unreadCount = count,
                        timestamp = LocalDateTime.now().toString()
                    )
                )
            } catch (e: Exception) {
                println("❌ Error getting unread count: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to get unread count: ${e.message}",
                        "timestamp" to LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Mark notification as read
        put("/{notificationId}/read") {
            try {
                val notificationId = call.parameters["notificationId"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Notification ID is required")

                val success = notificationService.markAsRead(UUID.fromString(notificationId))

                if (success) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "Notification marked as read",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "success" to false,
                            "message" to "Notification not found",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to mark as read: ${e.message}",
                        "timestamp" to LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Mark all notifications as read for a user
        put("/user/{userId}/read-all") {
            try {
                val userId = call.parameters["userId"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "User ID is required")

                val count = notificationService.markAllAsRead(UUID.fromString(userId))

                call.respond(
                    HttpStatusCode.OK,
                    MarkAllReadResponse(
                        success = true,
                        message = "$count notifications marked as read",
                        count = count,
                        timestamp = LocalDateTime.now().toString()
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    MarkAllReadResponse(
                        success = false,
                        message = "Failed to mark all as read: ${e.message}",
                        count = 0,
                        timestamp = LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Delete notification
        delete("/{notificationId}") {
            try {
                val notificationId = call.parameters["notificationId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Notification ID is required")

                val success = notificationService.deleteNotification(UUID.fromString(notificationId))

                if (success) {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "Notification deleted",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "success" to false,
                            "message" to "Notification not found",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "success" to false,
                        "message" to "Failed to delete notification: ${e.message}",
                        "timestamp" to LocalDateTime.now().toString()
                    )
                )
            }
        }

        // Admin: Send custom notification(s)
        post("/admin/send") {
            try {
                val request = call.receive<SendNotificationRequest>()

                // Handle custom email/SMS (direct sending without in-app notification)
                if ((request.customEmail != null && request.channels.contains("EMAIL")) ||
                    (request.customPhoneNumber != null && request.channels.contains("SMS"))) {

                    var emailSent = false
                    var smsSent = false
                    val errors = mutableListOf<String>()

                    // Send custom email
                    if (request.customEmail != null && request.channels.contains("EMAIL")) {
                        val emailResult = emailService.sendNotificationEmail(
                            to = request.customEmail,
                            title = request.title,
                            message = request.message,
                            priority = request.priority,
                            attachments = request.attachments
                        )
                        if (emailResult.isSuccess) {
                            emailSent = true
                        } else {
                            errors.add("Email failed: ${emailResult.exceptionOrNull()?.message}")
                        }
                    }

                    // Send custom SMS
                    if (request.customPhoneNumber != null && request.channels.contains("SMS")) {
                        val smsResult = kotlinx.coroutines.runBlocking {
                            smsService.sendNotificationSms(
                                to = request.customPhoneNumber,
                                title = request.title,
                                message = request.message,
                                priority = request.priority
                            )
                        }
                        if (smsResult.isSuccess) {
                            smsSent = true
                        } else {
                            errors.add("SMS failed: ${smsResult.exceptionOrNull()?.message}")
                        }
                    }

                    val successMessage = buildString {
                        if (emailSent) append("Email sent to ${request.customEmail}. ")
                        if (smsSent) append("SMS sent to ${request.customPhoneNumber}.")
                        if (errors.isNotEmpty()) append(" Errors: ${errors.joinToString(", ")}")
                    }

                    call.respond(HttpStatusCode.OK, SendNotificationResponse(
                        success = emailSent || smsSent,
                        message = successMessage.ifEmpty { "Failed to send: ${errors.joinToString(", ")}" },
                        recipientCount = if (emailSent || smsSent) 1 else 0
                    ))
                    return@post
                }

                val result = when {
                    // Broadcast to all users or specific user type
                    request.recipientId == null && request.recipientIds == null -> {
                        val count = notificationService.broadcastNotification(
                            userType = request.userType ?: "ALL",
                            title = request.title,
                            message = request.message,
                            type = request.type,
                            category = request.category,
                            priority = request.priority,
                            actionUrl = request.actionUrl,
                            targetRole = request.targetRole,
                            channels = request.channels
                        )
                        SendNotificationResponse(
                            success = true,
                            message = "Broadcast notification sent to $count users",
                            recipientCount = count
                        )
                    }
                    // Send to single user
                    request.recipientId != null -> {
                        val notificationId = notificationService.sendCustomNotification(
                            recipientId = UUID.fromString(request.recipientId),
                            userType = request.userType ?: "ALL",
                            title = request.title,
                            message = request.message,
                            type = request.type,
                            category = request.category,
                            priority = request.priority,
                            actionUrl = request.actionUrl
                        )
                        SendNotificationResponse(
                            success = true,
                            message = "Notification sent successfully",
                            notificationId = notificationId,
                            recipientCount = 1
                        )
                    }
                    // Send to multiple users
                    request.recipientIds != null && request.recipientIds.isNotEmpty() -> {
                        val count = notificationService.sendBulkNotifications(
                            recipientIds = request.recipientIds.map { UUID.fromString(it) },
                            userType = request.userType ?: "ALL",
                            title = request.title,
                            message = request.message,
                            type = request.type,
                            category = request.category,
                            priority = request.priority,
                            actionUrl = request.actionUrl
                        )
                        SendNotificationResponse(
                            success = true,
                            message = "Notifications sent to $count users",
                            recipientCount = count
                        )
                    }
                    else -> {
                        SendNotificationResponse(
                            success = false,
                            message = "Invalid request: Must provide recipientId, recipientIds, or use broadcast mode"
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, result)

            } catch (e: Exception) {
                println("❌ Error sending notification: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SendNotificationResponse(
                        success = false,
                        message = "Failed to send notification: ${e.message}"
                    )
                )
            }
        }

        // Send OTP via email
        post("/admin/send-otp") {
            try {
                val request = call.receive<SendOtpRequest>()

                // Generate 6-digit OTP
                val otp = (100000..999999).random().toString()
                val otpId = UUID.randomUUID().toString()
                val expiresAt = LocalDateTime.now().plusMinutes(request.expiryMinutes.toLong())

                // Send OTP email
                val result = emailService.sendOtpEmail(
                    to = request.email,
                    otp = otp,
                    purpose = request.purpose,
                    expiryMinutes = request.expiryMinutes
                )

                if (result.isSuccess) {
                    println("✅ OTP sent to ${request.email}: $otp (ID: $otpId)")
                    call.respond(HttpStatusCode.OK, SendOtpResponse(
                        success = true,
                        message = "OTP sent successfully to ${request.email}",
                        otpId = otpId,
                        expiresAt = expiresAt.toString()
                    ))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, SendOtpResponse(
                        success = false,
                        message = "Failed to send OTP: ${result.exceptionOrNull()?.message}"
                    ))
                }

            } catch (e: Exception) {
                println("❌ Error sending OTP: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SendOtpResponse(
                        success = false,
                        message = "Failed to send OTP: ${e.message}"
                    )
                )
            }
        }
    }
}
