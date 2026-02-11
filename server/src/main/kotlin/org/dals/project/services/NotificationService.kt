package org.dals.project.services

import org.dals.project.database.*
import org.dals.project.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.util.*

class NotificationService {
    private val emailService = EmailService()
    private val smsService = SmsService()

    // Create a notification
    fun createNotification(
        userId: UUID,
        userType: String,
        title: String,
        message: String,
        type: String,
        category: String = "GENERAL",
        priority: String = "NORMAL",
        actionUrl: String? = null,
        relatedEntityType: String? = null,
        relatedEntityId: UUID? = null,
        metadata: Map<String, String>? = null
    ): String {
        return transaction {
            val notificationId = UUID.randomUUID()

            Notifications.insert {
                it[id] = notificationId
                it[Notifications.userId] = userId
                it[Notifications.userType] = userType
                it[Notifications.title] = title
                it[Notifications.message] = message
                it[Notifications.type] = type
                it[Notifications.category] = category
                it[Notifications.priority] = priority
                it[Notifications.actionUrl] = actionUrl
                it[Notifications.relatedEntityType] = relatedEntityType
                it[Notifications.relatedEntityId] = relatedEntityId
                it[Notifications.metadata] = metadata?.let { map -> Json.encodeToString(map) }
            }

            notificationId.toString()
        }
    }

    // Notification generator methods for different events

    fun notifyTransactionCompleted(customerId: UUID, transactionId: UUID, amount: String, type: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Transaction Successful",
            message = "Your $type transaction of $$amount has been completed successfully.",
            type = "TRANSACTION_COMPLETED",
            category = "TRANSACTION",
            priority = "NORMAL",
            relatedEntityType = "TRANSACTION",
            relatedEntityId = transactionId
        )
    }

    fun notifyTransactionFailed(customerId: UUID, transactionId: UUID, amount: String, reason: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Transaction Failed",
            message = "Your transaction of $$amount failed. Reason: $reason",
            type = "TRANSACTION_FAILED",
            category = "TRANSACTION",
            priority = "HIGH",
            relatedEntityType = "TRANSACTION",
            relatedEntityId = transactionId
        )
    }

    fun notifyLoanApproved(customerId: UUID, loanId: UUID, amount: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Loan Approved!",
            message = "Congratulations! Your loan application for $$amount has been approved and funds have been disbursed to your account.",
            type = "LOAN_APPROVED",
            category = "LOAN",
            priority = "HIGH",
            actionUrl = "/loans/$loanId",
            relatedEntityType = "LOAN",
            relatedEntityId = loanId
        )
    }

    fun notifyLoanRejected(customerId: UUID, loanId: UUID, reason: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Loan Application Update",
            message = "Your loan application has been reviewed. Reason: $reason",
            type = "LOAN_REJECTED",
            category = "LOAN",
            priority = "NORMAL",
            relatedEntityType = "LOAN",
            relatedEntityId = loanId
        )
    }

    fun notifyLoanPaymentReceived(customerId: UUID, loanId: UUID, amount: String, remainingBalance: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Loan Payment Received",
            message = "Your loan payment of $$amount has been received. Remaining balance: $$remainingBalance",
            type = "PAYMENT_RECEIVED",
            category = "PAYMENT",
            priority = "NORMAL",
            actionUrl = "/loans/$loanId",
            relatedEntityType = "LOAN",
            relatedEntityId = loanId
        )
    }

    fun notifyLoanPaidOff(customerId: UUID, loanId: UUID) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Loan Paid Off! 🎉",
            message = "Congratulations! You have successfully paid off your loan. Thank you for your timely payments.",
            type = "LOAN_PAID_OFF",
            category = "LOAN",
            priority = "HIGH",
            relatedEntityType = "LOAN",
            relatedEntityId = loanId
        )
    }

    fun notifyPaymentDue(customerId: UUID, loanId: UUID, amount: String, dueDate: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Payment Reminder",
            message = "Your loan payment of $$amount is due on $dueDate. Please ensure sufficient funds in your account.",
            type = "PAYMENT_REMINDER",
            category = "PAYMENT",
            priority = "HIGH",
            actionUrl = "/loans/$loanId",
            relatedEntityType = "LOAN",
            relatedEntityId = loanId
        )
    }

    fun notifyLargeDeposit(customerId: UUID, accountId: UUID, amount: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Large Deposit Received",
            message = "A deposit of $$amount has been credited to your account.",
            type = "LARGE_DEPOSIT",
            category = "TRANSACTION",
            priority = "NORMAL",
            relatedEntityType = "ACCOUNT",
            relatedEntityId = accountId
        )
    }

    fun notifyLowBalance(customerId: UUID, accountId: UUID, balance: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Low Balance Alert",
            message = "Your account balance is $$balance. Please add funds to avoid service interruptions.",
            type = "LOW_BALANCE",
            category = "SECURITY",
            priority = "HIGH",
            relatedEntityType = "ACCOUNT",
            relatedEntityId = accountId
        )
    }

    fun notifyAccountFrozen(customerId: UUID, accountId: UUID, reason: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Account Frozen",
            message = "Your account has been temporarily frozen. Reason: $reason. Please contact customer support.",
            type = "ACCOUNT_FROZEN",
            category = "SECURITY",
            priority = "URGENT",
            relatedEntityType = "ACCOUNT",
            relatedEntityId = accountId
        )
    }

    fun notifySecurityAlert(customerId: UUID, alertType: String, message: String) {
        createNotification(
            userId = customerId,
            userType = "CUSTOMER",
            title = "Security Alert",
            message = message,
            type = "SECURITY_ALERT",
            category = "SECURITY",
            priority = "URGENT"
        )
    }

    // Get notifications for a user
    fun getNotifications(userId: UUID, unreadOnly: Boolean = false): List<NotificationDto> {
        return transaction {
            val query = if (unreadOnly) {
                Notifications.select {
                    (Notifications.userId eq userId) and (Notifications.isRead eq false)
                }
            } else {
                Notifications.select { Notifications.userId eq userId }
            }

            query.orderBy(Notifications.createdAt, SortOrder.DESC)
                .map { mapToDto(it) }
        }
    }

    // Mark notification as read
    fun markAsRead(notificationId: UUID): Boolean {
        return transaction {
            val updated = Notifications.update({ Notifications.id eq notificationId }) {
                it[isRead] = true
                it[readAt] = Instant.now()
            }
            updated > 0
        }
    }

    // Mark all as read
    fun markAllAsRead(userId: UUID): Int {
        return transaction {
            Notifications.update({
                (Notifications.userId eq userId) and (Notifications.isRead eq false)
            }) {
                it[isRead] = true
                it[readAt] = Instant.now()
            }
        }
    }

    // Delete notification
    fun deleteNotification(notificationId: UUID): Boolean {
        return transaction {
            Notifications.deleteWhere { id eq notificationId } > 0
        }
    }

    // Get unread count
    fun getUnreadCount(userId: UUID): Int {
        return transaction {
            Notifications.select {
                (Notifications.userId eq userId) and (Notifications.isRead eq false)
            }.count().toInt()
        }
    }

    // Admin broadcast notification to all users of specific type
    fun broadcastNotification(
        userType: String, // "EMPLOYEE", "CUSTOMER", or "ALL"
        title: String,
        message: String,
        type: String = "SYSTEM_ANNOUNCEMENT",
        category: String = "SYSTEM",
        priority: String = "NORMAL",
        actionUrl: String? = null,
        targetRole: String? = null, // Optional: target specific role like "TELLER", "LOAN_OFFICER"
        channels: List<String> = listOf("IN_APP") // "IN_APP", "EMAIL", "SMS"
    ): Int {
        return transaction {
            var count = 0

            // Get target users based on userType
            when (userType) {
                "EMPLOYEE", "ALL_EMPLOYEES" -> {
                    val employeesQuery = if (targetRole != null) {
                        Users.select { Users.role eq UserRole.valueOf(targetRole) }
                    } else {
                        Users.selectAll()
                    }

                    employeesQuery.forEach { user ->
                        // In-app notification
                        if (channels.contains("IN_APP")) {
                            createNotification(
                                userId = user[Users.id].value,
                                userType = "EMPLOYEE",
                                title = title,
                                message = message,
                                type = type,
                                category = category,
                                priority = priority,
                                actionUrl = actionUrl
                            )
                        }

                        // Email notification (async to avoid blocking)
                        if (channels.contains("EMAIL")) {
                            CoroutineScope(Dispatchers.IO).launch {
                                emailService.sendNotificationEmail(
                                    to = user[Users.email],
                                    title = title,
                                    message = message,
                                    priority = priority
                                )
                            }
                        }

                        // SMS notification (async to avoid blocking)
                        if (channels.contains("SMS")) {
                            user[Users.phoneNumber]?.let { phone ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    smsService.sendNotificationSms(
                                        to = phone,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        count++
                    }
                }
                "CUSTOMER", "ALL_CUSTOMERS" -> {
                    Customers.selectAll().forEach { customer ->
                        // In-app notification
                        if (channels.contains("IN_APP")) {
                            createNotification(
                                userId = customer[Customers.id].value,
                                userType = "CUSTOMER",
                                title = title,
                                message = message,
                                type = type,
                                category = category,
                                priority = priority,
                                actionUrl = actionUrl
                            )
                        }

                        // Email notification
                        if (channels.contains("EMAIL")) {
                            customer[Customers.email]?.let { email ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    emailService.sendNotificationEmail(
                                        to = email,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        // SMS notification (async to avoid blocking)
                        if (channels.contains("SMS")) {
                            customer[Customers.phoneNumber]?.let { phone ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    smsService.sendNotificationSms(
                                        to = phone,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        count++
                    }
                }
                "ALL" -> {
                    // Send to all employees
                    Users.selectAll().forEach { user ->
                        // In-app notification
                        if (channels.contains("IN_APP")) {
                            createNotification(
                                userId = user[Users.id].value,
                                userType = "EMPLOYEE",
                                title = title,
                                message = message,
                                type = type,
                                category = category,
                                priority = priority,
                                actionUrl = actionUrl
                            )
                        }

                        // Email notification (async to avoid blocking)
                        if (channels.contains("EMAIL")) {
                            CoroutineScope(Dispatchers.IO).launch {
                                emailService.sendNotificationEmail(
                                    to = user[Users.email],
                                    title = title,
                                    message = message,
                                    priority = priority
                                )
                            }
                        }

                        // SMS notification (async to avoid blocking)
                        if (channels.contains("SMS")) {
                            user[Users.phoneNumber]?.let { phone ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    smsService.sendNotificationSms(
                                        to = phone,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        count++
                    }
                    // Send to all customers
                    Customers.selectAll().forEach { customer ->
                        // In-app notification
                        if (channels.contains("IN_APP")) {
                            createNotification(
                                userId = customer[Customers.id].value,
                                userType = "CUSTOMER",
                                title = title,
                                message = message,
                                type = type,
                                category = category,
                                priority = priority,
                                actionUrl = actionUrl
                            )
                        }

                        // Email notification
                        if (channels.contains("EMAIL")) {
                            customer[Customers.email]?.let { email ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    emailService.sendNotificationEmail(
                                        to = email,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        // SMS notification (async to avoid blocking)
                        if (channels.contains("SMS")) {
                            customer[Customers.phoneNumber]?.let { phone ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    smsService.sendNotificationSms(
                                        to = phone,
                                        title = title,
                                        message = message,
                                        priority = priority
                                    )
                                }
                            }
                        }

                        count++
                    }
                }
            }

            println("✅ Broadcast notification sent to $count users")
            count
        }
    }

    // Send notification to specific user (can be employee or customer)
    fun sendCustomNotification(
        recipientId: UUID,
        userType: String, // "EMPLOYEE" or "CUSTOMER"
        title: String,
        message: String,
        type: String = "CUSTOM",
        category: String = "GENERAL",
        priority: String = "NORMAL",
        actionUrl: String? = null
    ): String {
        return createNotification(
            userId = recipientId,
            userType = userType,
            title = title,
            message = message,
            type = type,
            category = category,
            priority = priority,
            actionUrl = actionUrl
        )
    }

    // Send notification to multiple specific users
    fun sendBulkNotifications(
        recipientIds: List<UUID>,
        userType: String,
        title: String,
        message: String,
        type: String = "CUSTOM",
        category: String = "GENERAL",
        priority: String = "NORMAL",
        actionUrl: String? = null
    ): Int {
        return transaction {
            var count = 0
            recipientIds.forEach { recipientId ->
                createNotification(
                    userId = recipientId,
                    userType = userType,
                    title = title,
                    message = message,
                    type = type,
                    category = category,
                    priority = priority,
                    actionUrl = actionUrl
                )
                count++
            }
            count
        }
    }

    private fun mapToDto(row: ResultRow): NotificationDto {
        return NotificationDto(
            id = row[Notifications.id].toString(),
            userId = row[Notifications.userId].toString(),
            title = row[Notifications.title],
            message = row[Notifications.message],
            type = row[Notifications.type],
            category = row[Notifications.category],
            priority = row[Notifications.priority],
            isRead = row[Notifications.isRead],
            actionUrl = row[Notifications.actionUrl],
            relatedEntityType = row[Notifications.relatedEntityType],
            relatedEntityId = row[Notifications.relatedEntityId]?.toString(),
            readAt = row[Notifications.readAt]?.toString(),
            createdAt = row[Notifications.createdAt].toString()
        )
    }
}
