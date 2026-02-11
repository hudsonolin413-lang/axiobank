package org.dals.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dals.project.model.*
import org.dals.project.repository.AuthRepository
import org.dals.project.repository.NotificationRepository

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val notificationSettings: NotificationSettings? = null,
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class NotificationViewModel(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository = NotificationRepository(authRepository)
) : ViewModel() {

    private val repository = notificationRepository
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Collect data from repository
                launch {
                    repository.notifications.collect { notifications ->
                        _uiState.value = _uiState.value.copy(
                            notifications = notifications,
                            unreadCount = repository.getUnreadCount()
                        )
                    }
                }

                launch {
                    repository.notificationSettings.collect { settings ->
                        _uiState.value = _uiState.value.copy(notificationSettings = settings)
                    }
                }

            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        unreadCount = repository.getUnreadCount()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to mark notification as read: ${error.message}"
                    )
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.markAllAsRead()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        unreadCount = 0,
                        successMessage = "All notifications marked as read"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to mark all as read: ${error.message}"
                    )
                }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        unreadCount = repository.getUnreadCount(),
                        successMessage = "Notification deleted"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete notification: ${error.message}"
                    )
                }
        }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.updateNotificationSettings(settings)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Notification settings updated"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update settings: ${error.message}"
                    )
                }
        }
    }

    fun createNotification(
        title: String,
        message: String,
        type: NotificationType,
        actionUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.createNotification(title, message, type, actionUrl)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        unreadCount = repository.getUnreadCount()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to create notification: ${error.message}"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun getNotificationById(id: String): Notification? {
        return repository.getNotificationById(id)
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.refreshNotifications()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}