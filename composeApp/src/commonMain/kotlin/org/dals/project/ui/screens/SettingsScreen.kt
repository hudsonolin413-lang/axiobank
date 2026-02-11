package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.NotificationSettings
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.NotificationViewModel
import org.dals.project.utils.themeManager
import org.dals.project.utils.ThemeMode
import org.dals.project.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToKYC: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToPaymentMethods: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToTransactionLimits: () -> Unit = {},
    onNavigateToAutoPaySettings: () -> Unit = {},
    onNavigateToLocationSettings: () -> Unit = {},
    onNavigateToDateTimeSettings: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by themeManager.themeMode.collectAsState()
    val isDarkMode by themeManager.isDarkMode.collectAsState()

    // Settings repository for language and currency
    val settingsRepository = SettingsManager.settingsRepository
    val appSettings by settingsRepository.appSettings.collectAsStateWithLifecycle()

    // Mock settings state - in a real app this would come from a settings repository/viewmodel
    var isLocationEnabled by remember { mutableStateOf(false) }
    var selectedTimeFormat by remember { mutableStateOf("24-hour") }
    var selectedDateFormat by remember { mutableStateOf("DD/MM/YYYY") }
    var isAutoBackupEnabled by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings Header
            item {
                SettingsHeaderCard(user = authUiState.currentUser)
            }

            // Account Settings Section
            item {
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        title = "Profile Information",
                        subtitle = "Update your personal details",
                        icon = Icons.Filled.Person,
                        onClick = onNavigateToProfile
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "KYC Verification",
                        subtitle = "Complete identity verification with document upload",
                        icon = Icons.Filled.Lock,
                        onClick = onNavigateToKYC
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Security & Privacy",
                        subtitle = "Password, biometrics, privacy settings",
                        icon = Icons.Filled.Shield,
                        onClick = onNavigateToSecurity
                    )
                }
            }

            // Login Preferences Section
            item {
                Text(
                    text = "Login Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    // Auto Login Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Auto Login",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (authUiState.isRememberMeEnabled)
                                        "Automatically sign in when app opens"
                                    else
                                        "Require manual sign-in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = authUiState.isRememberMeEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    // Keep current credentials and enable auto-login
                                    authViewModel.setAutoLogin(true)
                                } else {
                                    // Disable auto-login but keep credentials for manual login
                                    authViewModel.setAutoLogin(false)
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Biometric Unlock
                    val appSettings by settingsRepository.appSettings.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsRepository.updateBiometricEnabled(!appSettings.isBiometricEnabled) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Biometric Unlock",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Use fingerprint to unlock the app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = appSettings.isBiometricEnabled,
                            onCheckedChange = { settingsRepository.updateBiometricEnabled(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Clear Saved Credentials
                    if (authUiState.savedCredentials.first != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authViewModel.clearSavedCredentials()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Saved Credentials",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Remove saved username and password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Saved Account Info (if credentials are saved)
                    authUiState.savedCredentials.first?.let { savedUsername ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Saved Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Username: $savedUsername",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // App Preferences Section
            item {
                Text(
                    text = "App Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeManager.toggleDarkMode() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (themeMode) {
                                        ThemeMode.LIGHT -> "Light theme"
                                        ThemeMode.DARK -> "Dark theme"
                                        ThemeMode.SYSTEM -> "System default"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { themeManager.toggleDarkMode() }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Language",
                        subtitle = appSettings.language,
                        icon = Icons.Filled.Language,
                        onClick = onNavigateToLanguage
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Currency",
                        subtitle = appSettings.currency,
                        icon = Icons.Filled.AttachMoney,
                        onClick = onNavigateToCurrency
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Date & Time",
                        subtitle = "$selectedDateFormat, $selectedTimeFormat",
                        icon = Icons.Filled.AccessTime,
                        onClick = onNavigateToDateTimeSettings
                    )
                }
            }

            // Location & Privacy Section
            item {
                Text(
                    text = "Location & Privacy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isLocationEnabled = !isLocationEnabled
                                if (isLocationEnabled) {
                                    // Request location permission
                                }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Location Services",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isLocationEnabled) "Allow app to access location" else "Location access disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isLocationEnabled,
                            onCheckedChange = {
                                isLocationEnabled = it
                                if (it) {
                                    // Request location permission
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Location Settings",
                        subtitle = "Configure location preferences",
                        icon = Icons.Filled.Map,
                        onClick = onNavigateToLocationSettings
                    )
                }
            }

            // Data & Backup Section
            item {
                Text(
                    text = "Data & Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAutoBackupEnabled = !isAutoBackupEnabled }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Auto Backup",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isAutoBackupEnabled) "Automatic data backup enabled" else "Manual backup only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { isAutoBackupEnabled = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Export Data",
                        subtitle = "Download your data as CSV/PDF",
                        icon = Icons.Filled.BarChart,
                        onClick = { /* TODO: Export data functionality */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Clear Cache",
                        subtitle = "Free up storage space",
                        icon = Icons.Filled.Delete,
                        onClick = { /* TODO: Clear cache functionality */ }
                    )
                }
            }

            // Notification Settings Section
            item {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                notificationUiState.notificationSettings?.let { settings ->
                    NotificationSettingsCard(
                        settings = settings,
                        onSettingsUpdate = { newSettings ->
                            notificationViewModel.updateNotificationSettings(newSettings)
                        }
                    )
                } ?: run {
                    // Default notification settings if none available
                    NotificationSettingsCard(
                        settings = NotificationSettings(
                            userId = authUiState.currentUser?.id ?: "",
                            pushNotifications = true,
                            emailNotifications = true,
                            transactionNotifications = true
                        ),
                        onSettingsUpdate = { newSettings ->
                            notificationViewModel.updateNotificationSettings(newSettings)
                        }
                    )
                }
            }

            // Financial Settings Section
            item {
                Text(
                    text = "Financial Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        title = "Payment Methods",
                        subtitle = "Manage cards and mobile money accounts",
                        icon = Icons.Filled.CreditCard,
                        onClick = onNavigateToPaymentMethods
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Transaction Limits",
                        subtitle = "Set daily and monthly spending limits",
                        icon = Icons.Filled.Balance,
                        onClick = onNavigateToTransactionLimits
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Auto-Pay Settings",
                        subtitle = "Manage recurring payments and auto-pay",
                        icon = Icons.Filled.Sync,
                        onClick = onNavigateToAutoPaySettings
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Investment Preferences",
                        subtitle = "Risk tolerance and portfolio settings",
                        icon = Icons.Filled.TrendingUp,
                        onClick = { /* TODO: Navigate to investment settings */ }
                    )
                }
            }

            // Device & Permissions Section
            item {
                Text(
                    text = "Device & Permissions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                DevicePermissionsCard()
            }

            // Support & Legal Section
            item {
                Text(
                    text = "Support & Legal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        title = "Help Center",
                        subtitle = "FAQ and support articles",
                        icon = Icons.Filled.Help,
                        onClick = onNavigateToHelp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Contact Support",
                        subtitle = "Get help from our team",
                        icon = Icons.Filled.Phone,
                        onClick = { /* TODO: Open support chat/email */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Send Bug Report",
                        subtitle = "Report issues and improve the app",
                        icon = Icons.Filled.BugReport,
                        onClick = { /* TODO: Bug report functionality */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "About Axio Bank",
                        subtitle = "Learn more about our platform",
                        icon = Icons.Filled.Info,
                        onClick = onNavigateToAbout
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "How we protect your data",
                        icon = Icons.Filled.Description,
                        onClick = { /* TODO: Navigate to privacy policy */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Terms of Service",
                        subtitle = "Legal terms and conditions",
                        icon = Icons.Filled.Assignment,
                        onClick = { /* TODO: Navigate to terms */ }
                    )
                }
            }

            // About Section
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SettingsCard {
                    SettingsItem(
                        title = "App Version",
                        subtitle = "v1.0.0 (Build 1)",
                        icon = Icons.Filled.Smartphone,
                        onClick = { /* Show version details */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Check for Updates",
                        subtitle = "Download the latest version",
                        icon = Icons.Filled.Sync,
                        onClick = { /* TODO: Check for app updates */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Rate App",
                        subtitle = "Share your feedback on the app store",
                        icon = Icons.Filled.Star,
                        onClick = { /* TODO: Navigate to app store */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsItem(
                        title = "Share App",
                        subtitle = "Invite friends to join Axio Bank",
                        icon = Icons.Filled.Share,
                        onClick = { /* TODO: Share app functionality */ }
                    )
                }
            }

            // Logout Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Sign out and clear all saved data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to logout?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will:",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("• Sign you out of your account")
                    Text("• Clear all saved login credentials")
                    Text("• Disable auto-login")
                    Text("• Require manual sign-in next time")

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can always sign back in with your username and password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsHeaderCard(user: org.dals.project.model.User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user?.fullName?.take(2)?.uppercase() ?: "??",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = user?.fullName ?: "Unknown User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "@${user?.username ?: "unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Credit Score: ${user?.creditScore ?: 0}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationSettingsCard(
    settings: NotificationSettings,
    onSettingsUpdate: (NotificationSettings) -> Unit
) {
    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
            Text(
                text = "Notification Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        NotificationToggleItem(
            title = "Push Notifications",
            subtitle = "Get notifications on your device",
            checked = settings.pushNotifications,
            onCheckedChange = {
                onSettingsUpdate(settings.copy(pushNotifications = it))
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NotificationToggleItem(
            title = "Email Notifications",
            subtitle = "Receive updates via email",
            checked = settings.emailNotifications,
            onCheckedChange = {
                onSettingsUpdate(settings.copy(emailNotifications = it))
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NotificationToggleItem(
            title = "Transaction Alerts",
            subtitle = "Get notified about transactions",
            checked = settings.transactionNotifications,
            onCheckedChange = {
                onSettingsUpdate(settings.copy(transactionNotifications = it))
            }
        )
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DevicePermissionsCard() {
    var cameraPermission by remember { mutableStateOf(false) }
    var storagePermission by remember { mutableStateOf(true) }
    var microphonePermission by remember { mutableStateOf(false) }
    var contactsPermission by remember { mutableStateOf(false) }

    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Smartphone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
            Text(
                text = "Device Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        PermissionToggleItem(
            title = "Camera",
            subtitle = "Required for document verification",
            icon = Icons.Filled.CameraAlt,
            checked = cameraPermission,
            onCheckedChange = { cameraPermission = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        PermissionToggleItem(
            title = "Storage",
            subtitle = "Save and access documents",
            icon = Icons.Filled.Save,
            checked = storagePermission,
            onCheckedChange = { storagePermission = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        PermissionToggleItem(
            title = "Microphone",
            subtitle = "Voice commands and support calls",
            icon = Icons.Filled.Mic,
            checked = microphonePermission,
            onCheckedChange = { microphonePermission = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        PermissionToggleItem(
            title = "Contacts",
            subtitle = "Quick money transfers to contacts",
            icon = Icons.Filled.People,
            checked = contactsPermission,
            onCheckedChange = { contactsPermission = it }
        )
    }
}

@Composable
private fun PermissionToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}