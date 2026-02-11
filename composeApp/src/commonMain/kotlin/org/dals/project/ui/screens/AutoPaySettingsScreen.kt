package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.dals.project.model.AutoPaySettings
import org.dals.project.model.PaymentFrequency
import org.dals.project.model.RecurringPayment
import org.dals.project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPaySettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    var isAutoPayEnabled by remember { mutableStateOf(false) }
    var autoLoanPayment by remember { mutableStateOf(false) }
    var minimumBalance by remember { mutableStateOf("1000") }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    // Mock recurring payments data
    val recurringPayments = remember {
        mutableStateListOf(
            RecurringPayment(
                id = "rp1",
                name = "Netflix Subscription",
                amount = 15.99,
                frequency = PaymentFrequency.MONTHLY,
                nextPaymentDate = "2024-02-15",
                paymentMethod = "M-PESA Personal"
            ),
            RecurringPayment(
                id = "rp2",
                name = "Gym Membership",
                amount = 45.0,
                frequency = PaymentFrequency.MONTHLY,
                nextPaymentDate = "2024-02-01",
                paymentMethod = "Airtel Money"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Pay Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAutoPayEnabled) {
                        IconButton(onClick = { showAddPaymentDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Payment")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                AutoPayHeader()
            }

            // Main Auto-Pay Toggle
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
                                text = "Enable Auto-Pay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically pay bills and subscriptions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoPayEnabled,
                            onCheckedChange = { isAutoPayEnabled = it }
                        )
                    }
                }
            }

            if (isAutoPayEnabled) {
                // Minimum Balance Setting
                item {
                    MinimumBalanceCard(
                        value = minimumBalance,
                        onValueChange = { minimumBalance = it }
                    )
                }

                // Auto Loan Payment
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Loan Payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically pay loan installments",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoLoanPayment,
                                onCheckedChange = { autoLoanPayment = it }
                            )
                        }
                    }
                }

                // Recurring Payments Section
                item {
                    Text(
                        text = "Recurring Payments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recurringPayments.isEmpty()) {
                    item {
                        EmptyRecurringPaymentsCard(
                            onAddPayment = { showAddPaymentDialog = true }
                        )
                    }
                } else {
                    items(recurringPayments.size) { index ->
                        val payment = recurringPayments[index]
                        RecurringPaymentCard(
                            payment = payment,
                            onEdit = { /* TODO: Edit payment */ },
                            onDelete = { showDeleteDialog = payment.id },
                            onToggleActive = {
                                recurringPayments[index] = payment.copy(isActive = !payment.isActive)
                            }
                        )
                    }
                }

                // Safety Features
                item {
                    SafetyFeaturesCard()
                }
            } else {
                // Disabled State Info
                item {
                    DisabledAutoPayCard()
                }
            }
        }
    }

    // Add Payment Dialog
    if (showAddPaymentDialog) {
        AddRecurringPaymentDialog(
            onDismiss = { showAddPaymentDialog = false },
            onAdd = { name, amount, frequency, paymentMethod ->
                recurringPayments.add(
                    RecurringPayment(
                        id = "rp${kotlin.random.Random.nextInt(1000)}",
                        name = name,
                        amount = amount,
                        frequency = frequency,
                        nextPaymentDate = "2024-02-15", // Calculate based on frequency
                        paymentMethod = paymentMethod
                    )
                )
                showAddPaymentDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { paymentId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Recurring Payment") },
            text = { Text("Are you sure you want to delete this recurring payment?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val index = recurringPayments.indexOfFirst { it.id == paymentId }
                        if (index >= 0) {
                            recurringPayments.removeAt(index)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AutoPayHeader() {
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
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Auto-Pay Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Never miss a payment with automatic recurring payments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MinimumBalanceCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp).padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "Minimum Balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keep this amount as buffer for auto-payments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Minimum Balance (USD)") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecurringPaymentCard(
    payment: RecurringPayment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (payment.isActive)
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${payment.amount} • ${
                            payment.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Next: ${payment.nextPaymentDate} via ${payment.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (payment.isActive) "Active" else "Paused",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (payment.isActive)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = payment.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }
        }
    }
}

@Composable
private fun EmptyRecurringPaymentsCard(
    onAddPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Recurring Payments",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Add recurring payments to automate your bills",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAddPayment) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Recurring Payment")
            }
        }
    }
}

@Composable
private fun SafetyFeaturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
                Text(
                    text = "Safety Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            SafetyFeatureItem(
                title = "Payment Notifications",
                description = "Get notified before each automatic payment"
            )

            SafetyFeatureItem(
                title = "Insufficient Funds Protection",
                description = "Skip payments if balance is below minimum"
            )

            SafetyFeatureItem(
                title = "Monthly Summary",
                description = "Review all auto-payments in monthly reports"
            )
        }
    }
}

@Composable
private fun DisabledAutoPayCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏸️",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Auto-Pay Disabled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Enable auto-pay to automatically handle your recurring payments and never miss a due date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SafetyFeatureItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddRecurringPaymentDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, PaymentFrequency, String) -> Unit
) {
    var paymentName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf(PaymentFrequency.MONTHLY) }
    var selectedPaymentMethod by remember { mutableStateOf("M-PESA Personal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Payment") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = paymentName,
                    onValueChange = { paymentName = it },
                    label = { Text("Payment Name") },
                    placeholder = { Text("e.g., Netflix Subscription") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Selection
                Text(
                    text = "Payment Frequency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentFrequency.values().forEach { frequency ->
                        FilterChip(
                            onClick = { selectedFrequency = frequency },
                            label = { Text(frequency.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            selected = selectedFrequency == frequency
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (paymentName.isNotEmpty() && amount.isNotEmpty()) {
                        onAdd(paymentName, amount.toDoubleOrNull() ?: 0.0, selectedFrequency, selectedPaymentMethod)
                    }
                },
                enabled = paymentName.isNotEmpty() && amount.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}