package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dals.project.viewmodel.NfcPaymentViewModel
import org.dals.project.repository.NfcPaymentResponse
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcPaymentScreen(
    customerId: String,
    accountId: String,
    onNavigateBack: () -> Unit,
    viewModel: NfcPaymentViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPaymentDetails by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    // Load payment history on screen load
    LaunchedEffect(customerId) {
        viewModel.loadPaymentHistory(customerId)
        viewModel.loadActivePayments(customerId)

        // Check NFC availability (platform-specific)
        checkNfcAvailability { available, enabled ->
            viewModel.setNfcAvailability(available, enabled)
        }
    }

    // Auto-clear messages after 5 seconds
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            delay(5000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Default.History, contentDescription = "Payment History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showHistory) {
                PaymentHistoryView(
                    payments = uiState.paymentHistory,
                    isLoading = uiState.isLoading,
                    onClose = { showHistory = false },
                    onPaymentClick = { payment ->
                        showPaymentDetails = true
                    }
                )
            } else if (uiState.currentPayment != null && showPaymentDetails) {
                PaymentResultView(
                    payment = uiState.currentPayment!!,
                    onDismiss = {
                        showPaymentDetails = false
                        viewModel.clearCurrentPayment()
                    },
                    onNewPayment = {
                        showPaymentDetails = false
                        viewModel.clearCurrentPayment()
                        viewModel.startNfcScan()
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error/Success Messages
                    uiState.errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    uiState.successMessage?.let { success ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // NFC Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Nfc,
                                contentDescription = "NFC",
                                modifier = Modifier.size(64.dp),
                                tint = if (uiState.nfcAvailable && uiState.nfcEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when {
                                    !uiState.nfcAvailable -> "NFC Not Available"
                                    !uiState.nfcEnabled -> "NFC Disabled"
                                    uiState.isScanning -> "Ready to Scan"
                                    else -> "NFC Ready"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    !uiState.nfcAvailable -> "This device doesn't support NFC"
                                    !uiState.nfcEnabled -> "Please enable NFC in your device settings"
                                    uiState.isScanning -> "Hold your device near the NFC terminal"
                                    else -> "Tap the button below to start payment"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Active Payments
                    if (uiState.activePayments.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Pending Payments",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.activePayments.forEach { payment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = payment.merchantName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "$${payment.amount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(
                                            onClick = { viewModel.cancelPayment(payment.id) }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons
                    if (uiState.nfcAvailable && uiState.nfcEnabled) {
                        if (uiState.isScanning) {
                            Button(
                                onClick = { viewModel.stopNfcScan() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Scan")
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.startNfcScan()
                                    // Start listening for NFC tags
                                    enableNfcForegroundDispatch()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Contactless, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start NFC Payment")
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { /* Open NFC settings */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open NFC Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryView(
    payments: List<NfcPaymentResponse>,
    isLoading: Boolean,
    onClose: () -> Unit,
    onPaymentClick: (NfcPaymentResponse) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Payment History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (payments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No payment history", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payments) { payment ->
                    PaymentHistoryItem(payment = payment, onClick = { onPaymentClick(payment) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentHistoryItem(
    payment: NfcPaymentResponse,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = payment.initiatedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                payment.authorizationCode?.let { authCode ->
                    Text(
                        text = "Auth: $authCode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${payment.amount} ${payment.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (payment.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.tertiaryContainer
                        "PENDING" -> MaterialTheme.colorScheme.secondaryContainer
                        "FAILED", "DECLINED", "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = payment.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (payment.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "PENDING" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "FAILED", "DECLINED", "CANCELLED" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentResultView(
    payment: NfcPaymentResponse,
    onDismiss: () -> Unit,
    onNewPayment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (payment.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = if (payment.status == "COMPLETED") {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (payment.status == "COMPLETED") "Payment Successful!" else "Payment ${payment.status}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                PaymentDetailRow("Merchant", payment.merchantName)
                PaymentDetailRow("Amount", "$${payment.amount} ${payment.currency}")
                payment.authorizationCode?.let {
                    PaymentDetailRow("Authorization Code", it)
                }
                payment.transactionId?.let {
                    PaymentDetailRow("Transaction ID", it.take(16) + "...")
                }
                payment.completedAt?.let {
                    PaymentDetailRow("Completed At", it)
                }
                payment.failureReason?.let {
                    PaymentDetailRow("Failure Reason", it)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Done")
            }
            Button(
                onClick = onNewPayment,
                modifier = Modifier.weight(1f)
            ) {
                Text("New Payment")
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// Platform-specific functions (expect declarations)
expect fun checkNfcAvailability(callback: (available: Boolean, enabled: Boolean) -> Unit)
expect fun enableNfcForegroundDispatch()
