package org.dals.project.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dals.project.model.*
import org.dals.project.API_BASE_URL

@Serializable
data class ServerNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val category: String,
    val priority: String,
    val isRead: Boolean,
    val actionUrl: String? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
data class NotificationsResponse(
    val success: Boolean,
    val message: String,
    val data: List<ServerNotification>? = null,
    val unreadCount: Int = 0,
    val timestamp: String
)

class NotificationRepository(
    private val authRepository: AuthRepository
) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val baseUrl = API_BASE_URL

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationSettings = MutableStateFlow<NotificationSettings?>(null)
    val notificationSettings: StateFlow<NotificationSettings?> = _notificationSettings.asStateFlow()

    init {
        // Initialize notification settings when user logs in
        CoroutineScope(Dispatchers.Default).launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    println("🔔 User logged in, fetching real notifications")
                    initializeSettings(user.id)
                    fetchNotificationsFromServer(user.id)
                } else {
                    println("🔔 User logged out, clearing notifications")
                    clearData()
                }
            }
        }
    }

    private suspend fun fetchNotificationsFromServer(userId: String) {
        try {
//            println("📬 Fetching notifications for user: $userId")

            val response = httpClient.get("$baseUrl/notifications/user/$userId") {
                contentType(ContentType.Application.Json)
                headers {
                    authRepository.getAuthToken()?.let { token ->
                        append("Authorization", "Bearer $token")
                    }
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val notificationsResponse = response.body<NotificationsResponse>()

                if (notificationsResponse.success && !notificationsResponse.data.isNullOrEmpty()) {
                    val serverNotifications = notificationsResponse.data

                    // Convert server notifications to app model
                    val appNotifications = serverNotifications.map { serverNotif ->
                        Notification(
                            id = serverNotif.id,
                            userId = serverNotif.userId,
                            title = serverNotif.title,
                            message = serverNotif.message,
                            type = mapNotificationType(serverNotif.type),
                            timestamp = serverNotif.createdAt,
                            isRead = serverNotif.isRead,
                            actionUrl = serverNotif.actionUrl,
                            data = emptyMap()
                        )
                    }

                    _notifications.value = appNotifications
//                    println("✅ Fetched ${appNotifications.size} notifications")
                } else {
//                    println("📭 No notifications found")
                    _notifications.value = emptyList()
                }
            }
        } catch (e: Exception) {
            println("❌ Error fetching notifications: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun mapNotificationType(serverType: String): NotificationType {
        return when (serverType.uppercase()) {
            "TRANSACTION_COMPLETED", "LARGE_DEPOSIT", "DEPOSIT", "WITHDRAWAL", "TRANSFER" -> NotificationType.TRANSACTION_COMPLETED
            "TRANSACTION_FAILED" -> NotificationType.TRANSACTION_FAILED
            "LOAN_APPROVED", "LOAN_REJECTED", "LOAN_PAID_OFF" -> NotificationType.LOAN_APPROVED
            "BILL_DUE", "PAYMENT_RECEIVED", "PAYMENT_REMINDER" -> NotificationType.PAYMENT_REMINDER
            "INVESTMENT_UPDATE" -> NotificationType.INVESTMENT_UPDATE
            "LOW_BALANCE", "ACCOUNT_FROZEN", "SECURITY_ALERT" -> NotificationType.SECURITY_ALERT
            "PROMOTIONAL" -> NotificationType.PROMOTIONAL
            else -> NotificationType.SYSTEM_UPDATE
        }
    }

    private fun initializeSettings(userId: String) {
        // Initialize with default notification settings
        _notificationSettings.value = NotificationSettings(
            userId = userId,
            transactionNotifications = true,
            billReminders = true,
            loanUpdates = true,
            investmentUpdates = true,
            securityAlerts = true,
            promotionalOffers = false,
            pushNotifications = true,
            emailNotifications = true,
            smsNotifications = false
        )
    }

    private fun clearData() {
        _notifications.value = emptyList()
        _notificationSettings.value = null
    }

    private fun getCurrentDateString(): String {
        return java.time.Instant.now().toString()
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            println("📬 Marking notification as read: $notificationId")

            val response = httpClient.put("$baseUrl/notifications/$notificationId/read") {
                contentType(ContentType.Application.Json)
            }

            if (response.status == HttpStatusCode.OK) {
                // Update local state
                val currentNotifications = _notifications.value.toMutableList()
                val index = currentNotifications.indexOfFirst { it.id == notificationId }

                if (index != -1) {
                    currentNotifications[index] = currentNotifications[index].copy(isRead = true)
                    _notifications.value = currentNotifications
                }

                println("✅ Notification marked as read")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as read"))
            }
        } catch (e: Exception) {
            println("❌ Error marking as read: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            println("📬 Marking all notifications as read")

            val response = httpClient.put("$baseUrl/notifications/user/${currentUser.id}/read-all") {
                contentType(ContentType.Application.Json)
            }

            if (response.status == HttpStatusCode.OK) {
                // Update local state
                val updatedNotifications = _notifications.value.map { it.copy(isRead = true) }
                _notifications.value = updatedNotifications

                println("✅ All notifications marked as read")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all as read"))
            }
        } catch (e: Exception) {
            println("❌ Error marking all as read: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun refreshNotifications() {
        val currentUser = authRepository.currentUser.value
        if (currentUser != null) {
            fetchNotificationsFromServer(currentUser.id)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            delay(200) // Simulate network delay

            val currentNotifications = _notifications.value.toMutableList()
            currentNotifications.removeAll { it.id == notificationId }
            _notifications.value = currentNotifications

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings): Result<Unit> {
        return try {
            delay(500) // Simulate network delay

            _notificationSettings.value = settings

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        title: String,
        message: String,
        type: NotificationType,
        actionUrl: String? = null
    ): Result<String> {
        return try {
            val currentUser = authRepository.currentUser.value
                ?: return Result.failure(Exception("User not authenticated"))

            val notification = Notification(
                id = "notif-${kotlin.random.Random.nextLong()}",
                userId = currentUser.id,
                title = title,
                message = message,
                type = type,
                timestamp = getCurrentDateString(),
                isRead = false,
                actionUrl = actionUrl
            )

            val currentNotifications = _notifications.value.toMutableList()
            currentNotifications.add(0, notification)
            _notifications.value = currentNotifications

            Result.success(notification.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUnreadCount(): Int {
        return _notifications.value.count { !it.isRead }
    }

    fun getNotificationById(id: String): Notification? {
        return _notifications.value.find { it.id == id }
    }
}