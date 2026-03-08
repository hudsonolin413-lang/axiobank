package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.viewmodel.QRPaymentViewModel
import org.dals.project.viewmodel.QRPaymentTab
import org.jetbrains.compose.resources.painterResource
import org.dals.project.resources.Res
import org.dals.project.resources.AxioBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRPaymentScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    viewModel: QRPaymentViewModel = viewModel { QRPaymentViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Generate QR code when screen loads
    LaunchedEffect(customerId) {
        if (uiState.myQRCode == null) {
            viewModel.generateMyQRCode(customerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = if (uiState.currentTab == QRPaymentTab.MY_QR) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.currentTab == QRPaymentTab.MY_QR,
                    onClick = { viewModel.switchTab(QRPaymentTab.MY_QR) },
                    text = { Text("My QR Code") },
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null) }
                )
                Tab(
                    selected = uiState.currentTab == QRPaymentTab.SCAN_PAY,
                    onClick = { viewModel.switchTab(QRPaymentTab.SCAN_PAY) },
                    text = { Text("Scan to Pay") },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                )
            }

            // Content
            when (uiState.currentTab) {
                QRPaymentTab.MY_QR -> MyQRCodeContent(
                    uiState = uiState,
                    viewModel = viewModel
                )
                QRPaymentTab.SCAN_PAY -> ScanToPayContent(
                    customerId = customerId,
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    // Error Snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    // Success Dialog
    if (uiState.paymentSuccess) {
        PaymentSuccessDialog(
            recipientName = uiState.validatedRecipient ?: "",
            amount = uiState.paymentAmount,
            transactionId = uiState.transactionId ?: "",
            onDismiss = {
                viewModel.resetPayment()
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun MyQRCodeContent(
    uiState: org.dals.project.viewmodel.QRPaymentUiState,
    viewModel: QRPaymentViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading && uiState.myQRCode == null) {
            CircularProgressIndicator(modifier = Modifier.padding(48.dp))
        } else if (uiState.myQRCode != null) {
            val qrData = uiState.myQRCode

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Show this QR code to receive payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // QR Code Container with Logo
            Card(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Axio Bank Logo
                        Image(
                            painter = painterResource(Res.drawable.AxioBank),
                            contentDescription = "Axio Bank Logo",
                            modifier = Modifier.size(60.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Code placeholder (In real implementation, generate actual QR code image)
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(140.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Scan to Pay",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    InfoRow("Account Name", qrData.accountName)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Account Number", qrData.accountNumber)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Bank", qrData.bankName)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share/Save buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { /* TODO: Share QR code */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }

                Button(
                    onClick = { /* TODO: Save QR code */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ask the payer to scan this QR code using their banking app to transfer money to your account instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ScanToPayContent(
    customerId: String,
    uiState: org.dals.project.viewmodel.QRPaymentUiState,
    viewModel: QRPaymentViewModel
) {
    var showPaymentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.validatedRecipient == null) {
            // Scanner View
            Spacer(modifier = Modifier.height(48.dp))

            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Scan QR Code to Pay",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Point your camera at the recipient's QR code to make a secure payment.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { /* TODO: Open Camera Scanner */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Camera")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* TODO: Upload from Gallery */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload from Gallery")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo: Manual QR input for testing
            OutlinedButton(
                onClick = {
                    // Simulate QR scan - for demo purposes
                    val demoQRData = """{"accountNumber":"123456789","accountName":"John Doe","bankName":"Axio Bank","customerId":"550e8400-e29b-41d4-a716-446655440000","timestamp":"${System.currentTimeMillis()}"}"""
                    viewModel.validateScannedQR(demoQRData)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Demo: Test QR Scan")
            }
        } else {
            // Payment Details Form
            PaymentDetailsForm(
                recipientName = uiState.validatedRecipient,
                amount = uiState.paymentAmount,
                description = uiState.paymentDescription,
                isLoading = uiState.isLoading,
                onAmountChange = { viewModel.updateAmount(it) },
                onDescriptionChange = { viewModel.updateDescription(it) },
                onPayClick = { viewModel.processPayment(customerId) },
                onCancelClick = { viewModel.resetPayment() }
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
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
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailsForm(
    recipientName: String,
    amount: String,
    description: String,
    isLoading: Boolean,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPayClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Payment Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recipient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Paying to",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = recipientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount") },
            leadingIcon = {
                Text(
                    "$",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onPayClick,
                modifier = Modifier.weight(1f),
                enabled = !isLoading && amount.toDoubleOrNull() != null && amount.toDouble() > 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Pay Now")
                }
            }
        }
    }
}

@Composable
private fun PaymentSuccessDialog(
    recipientName: String,
    amount: String,
    transactionId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Text(
                text = "Payment Successful!",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You have successfully sent $$amount to $recipientName",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transaction ID: $transactionId",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            fontWeight = FontWeight.SemiBold
        )
    }
}
