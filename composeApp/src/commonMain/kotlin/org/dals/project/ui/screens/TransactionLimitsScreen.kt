package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.dals.project.model.TransactionLimits
import org.dals.project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionLimitsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(true) }
    var dailyLimit by remember { mutableStateOf("10000") }
    var monthlyLimit by remember { mutableStateOf("100000") }
    var singleTransactionLimit by remember { mutableStateOf("5000") }
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Limits") },
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
            // Header
            item {
                TransactionLimitsHeader()
            }

            // Enable/Disable Toggle
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Transaction Limits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Set limits to control your spending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                }
            }

            if (isEnabled) {
                // Daily Limit
                item {
                    LimitSettingCard(
                        title = "Daily Limit",
                        description = "Maximum amount you can spend per day",
                        value = dailyLimit,
                        onValueChange = { dailyLimit = it },
                        icon = ""
                    )
                }

                // Monthly Limit
                item {
                    LimitSettingCard(
                        title = "Monthly Limit",
                        description = "Maximum amount you can spend per month",
                        value = monthlyLimit,
                        onValueChange = { monthlyLimit = it },
                        icon = ""
                    )
                }

                // Single Transaction Limit
                item {
                    LimitSettingCard(
                        title = "Single Transaction Limit",
                        description = "Maximum amount for a single transaction",
                        value = singleTransactionLimit,
                        onValueChange = { singleTransactionLimit = it },
                        icon = ""
                    )
                }

                // Current Usage
                item {
                    CurrentUsageCard()
                }
            }

            // Save Button
            item {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = if (isEnabled) {
                        dailyLimit.isNotEmpty() && monthlyLimit.isNotEmpty() && singleTransactionLimit.isNotEmpty()
                    } else true
                ) {
                    Text("Save Limits")
                }
            }

            // Emergency Override Section
            item {
                EmergencyOverrideCard()
            }
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Transaction Limits") },
            text = {
                Column {
                    if (isEnabled) {
                        Text("Your new transaction limits:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Daily: $${dailyLimit}")
                        Text("• Monthly: $${monthlyLimit}")
                        Text("• Single: $${singleTransactionLimit}")
                    } else {
                        Text("Transaction limits will be disabled. You can spend without restrictions.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Save limits to preferences/database
                        showSaveDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionLimitsHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Transaction Limits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Set spending limits to help manage your finances",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LimitSettingCard(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Amount (USD)") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CurrentUsageCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Current Usage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            UsageProgressItem(
                label = "Daily Usage",
                used = 2500.0,
                limit = 10000.0
            )

            Spacer(modifier = Modifier.height(12.dp))

            UsageProgressItem(
                label = "Monthly Usage",
                used = 15000.0,
                limit = 100000.0
            )
        }
    }
}

@Composable
private fun UsageProgressItem(
    label: String,
    used: Double,
    limit: Double
) {
    val progress = (used / limit).coerceIn(0.0, 1.0)
    val percentage = (progress * 100).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$${used.toInt()} / $${limit.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                progress > 0.8 -> MaterialTheme.colorScheme.error
                progress > 0.6 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )

        Text(
            text = "$percentage% used",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun EmergencyOverrideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🚨 Emergency Override",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "If you need to exceed your limits for emergencies, you can temporarily disable them using biometric authentication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedButton(
                onClick = { /* TODO: Implement emergency override */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Request Emergency Override")
            }
        }
    }
}