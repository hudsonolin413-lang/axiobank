package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    notificationViewModel: NotificationViewModel,
    onNavigateBack: () -> Unit
) {
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    // Refresh notifications when screen appears
    LaunchedEffect(Unit) {
        notificationViewModel.refreshNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notifications")
                        if (notificationUiState.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = notificationUiState.unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    if (notificationUiState.unreadCount > 0) {
                        TextButton(
                            onClick = { notificationViewModel.markAllAsRead() }
                        ) {
                            Text("Mark All Read")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success/Error Messages
            item {
                notificationUiState.successMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                notificationUiState.errorMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (notificationUiState.notifications.isEmpty()) {
                item {
                    EmptyNotificationsCard()
                }
            } else {
                items(notificationUiState.notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { notificationViewModel.markAsRead(notification.id) },
                        onDelete = { notificationViewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!notification.isRead) onMarkAsRead() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                        )

                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatNotificationTime(notification.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = getNotificationTypeColor(notification.type),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = getNotificationTypeLabel(notification.type),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Actions
                Column {
                    if (!notification.isRead) {
                        TextButton(
                            onClick = onMarkAsRead
                        ) {
                            Text("Mark Read")
                        }
                    }

                    TextButton(
                        onClick = onDelete
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔔",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "You're all caught up! New notifications will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun getNotificationTypeColor(type: NotificationType): androidx.compose.ui.graphics.Color {
    return when (type) {
        NotificationType.TRANSACTION_COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        NotificationType.TRANSACTION_FAILED -> MaterialTheme.colorScheme.errorContainer
        NotificationType.BILL_DUE -> MaterialTheme.colorScheme.secondaryContainer
        NotificationType.LOAN_APPROVED -> MaterialTheme.colorScheme.primaryContainer
        NotificationType.LOAN_REJECTED -> MaterialTheme.colorScheme.errorContainer
        NotificationType.PAYMENT_REMINDER -> MaterialTheme.colorScheme.secondaryContainer
        NotificationType.INVESTMENT_UPDATE -> MaterialTheme.colorScheme.tertiaryContainer
        NotificationType.SECURITY_ALERT -> MaterialTheme.colorScheme.errorContainer
        NotificationType.SYSTEM_UPDATE -> MaterialTheme.colorScheme.surfaceVariant
        NotificationType.PROMOTIONAL -> MaterialTheme.colorScheme.primaryContainer
    }
}

private fun getNotificationTypeLabel(type: NotificationType): String {
    return when (type) {
        NotificationType.TRANSACTION_COMPLETED -> "Transaction"
        NotificationType.TRANSACTION_FAILED -> "Failed"
        NotificationType.BILL_DUE -> "Bill Due"
        NotificationType.LOAN_APPROVED -> "Loan"
        NotificationType.LOAN_REJECTED -> "Loan"
        NotificationType.PAYMENT_REMINDER -> "Reminder"
        NotificationType.INVESTMENT_UPDATE -> "Investment"
        NotificationType.SECURITY_ALERT -> "Security"
        NotificationType.SYSTEM_UPDATE -> "System"
        NotificationType.PROMOTIONAL -> "Promo"
    }
}

private fun formatNotificationTime(timestamp: String): String {
    // Simple time formatting - in real app would use proper date formatting
    return timestamp.take(16).replace('T', ' ')
}