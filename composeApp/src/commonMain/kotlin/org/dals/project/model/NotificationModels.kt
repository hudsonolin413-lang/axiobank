package org.dals.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: String,
    val isRead: Boolean = false,
    val actionUrl: String? = null,
    val data: Map<String, String> = emptyMap()
)

@Serializable
enum class NotificationType {
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,
    BILL_DUE,
    LOAN_APPROVED,
    LOAN_REJECTED,
    PAYMENT_REMINDER,
    INVESTMENT_UPDATE,
    SECURITY_ALERT,
    SYSTEM_UPDATE,
    PROMOTIONAL
}

@Serializable
data class NotificationSettings(
    val userId: String,
    val transactionNotifications: Boolean = true,
    val billReminders: Boolean = true,
    val loanUpdates: Boolean = true,
    val investmentUpdates: Boolean = true,
    val securityAlerts: Boolean = true,
    val promotionalOffers: Boolean = false,
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val smsNotifications: Boolean = false
)