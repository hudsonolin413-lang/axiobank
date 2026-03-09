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
import org.dals.project.utils.QRCodeGenerator
import org.dals.project.utils.QRCodeScanner
import org.dals.project.utils.PlatformFilePickerManager
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.launch
import java.io.File

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
    // Generate QR code image with logo
    val qrCodeImage: ImageBitmap? = remember(uiState.myQRCode) {
        uiState.myQRCode?.let { qrData ->
            val qrJsonData = viewModel.getQRCodeJsonString()
            if (qrJsonData != null) {
                // Get the logo path from resources
                val logoPath = getLogoPath()
                QRCodeGenerator.generateQRCode(
                    data = qrJsonData,
                    size = 512,
                    logoPath = logoPath
                )
            } else null
        }
    }

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

            // QR Code Container with Generated QR Code
            Card(
                modifier = Modifier
                    .size(340.dp)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCodeImage != null) {
                        // Display actual QR code with embedded logo
                        Image(
                            bitmap = qrCodeImage,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback if QR generation fails
                        CircularProgressIndicator()
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
            var showSaveDialog by remember { mutableStateOf(false) }
            var showShareDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val qrJsonData = viewModel.getQRCodeJsonString()
                        if (qrJsonData != null) {
                            shareQRCode(qrJsonData, qrData.accountName)
                            showShareDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        val qrJsonData = viewModel.getQRCodeJsonString()
                        if (qrJsonData != null) {
                            val success = downloadQRCode(qrJsonData, qrData.accountName)
                            showSaveDialog = success
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }

            // Save success dialog
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
                    title = { Text("QR Code Saved!") },
                    text = { Text("Your QR code has been saved to your Downloads folder.") },
                    confirmButton = {
                        Button(onClick = { showSaveDialog = false }) {
                            Text("OK")
                        }
                    }
                )
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
    val scope = rememberCoroutineScope()
    val filePickerManager = remember { PlatformFilePickerManager() }
    var scanningError by remember { mutableStateOf<String?>(null) }

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
                text = "Upload a QR code image to make a secure payment.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Camera Scan Button
            Button(
                onClick = {
                    scope.launch {
                        scanningError = null
                        try {
                            openCameraScanner { qrData ->
                                if (qrData != null && qrData.isNotBlank()) {
                                    if (QRCodeScanner.validateQRPaymentData(qrData)) {
                                        println("✅ QR Code scanned from camera: $qrData")
                                        viewModel.validateScannedQR(qrData)
                                    } else {
                                        scanningError = "Invalid QR code. Please scan a valid Axio Bank payment QR code."
                                    }
                                } else if (qrData != null) {
                                    scanningError = "No camera available or camera access denied."
                                }
                            }
                        } catch (e: Exception) {
                            scanningError = "Camera error: ${e.message}"
                            println("❌ Camera error: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Camera to Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upload Image Button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        scanningError = null
                        val imageBytes = filePickerManager.pickImage()
                        if (imageBytes != null) {
                            try {
                                // Save to temp file for scanning
                                val tempDir = System.getProperty("java.io.tmpdir")
                                val tempFile = File(tempDir, "qr_temp_${System.currentTimeMillis()}.png")
                                tempFile.writeBytes(imageBytes)

                                // Scan QR code
                                val scannedData = QRCodeScanner.scanQRCodeFromFile(tempFile.absolutePath)

                                // Clean up temp file
                                tempFile.delete()

                                if (scannedData != null && QRCodeScanner.validateQRPaymentData(scannedData)) {
                                    println("✅ QR Code scanned successfully: $scannedData")
                                    viewModel.validateScannedQR(scannedData)
                                } else {
                                    scanningError = "Invalid QR code. Please scan a valid Axio Bank payment QR code."
                                    println("❌ Invalid QR code data: $scannedData")
                                }
                            } catch (e: Exception) {
                                scanningError = "Failed to scan QR code: ${e.message}"
                                println("❌ QR Scan error: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload QR Code Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show scanning error if any
            scanningError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
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
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Enter Password to Confirm") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None
                                   else androidx.compose.ui.text.input.PasswordVisualTransformation(),
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
                onClick = { showConfirmDialog = true },
                modifier = Modifier.weight(1f),
                enabled = !isLoading &&
                         amount.toDoubleOrNull() != null &&
                         amount.toDouble() > 0 &&
                         password.isNotBlank()
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

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Confirm Payment") },
            text = {
                Column {
                    Text("Are you sure you want to send:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$$amount to $recipientName",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        // TODO: Validate password before processing
                        onPayClick()
                    }
                ) {
                    Text("Confirm & Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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

// Helper functions for QR code operations

/**
 * Get the path to the Axio Bank logo
 */
private fun getLogoPath(): String? {
    return try {
        val possiblePaths = listOf(
            // Try resource path
            System.getProperty("compose.application.resources.dir")?.let { "$it/composeResources/drawable/AxioBank.png" },
            // Try user.dir
            System.getProperty("user.dir")?.let { "$it/composeApp/src/commonMain/composeResources/drawable/AxioBank.png" },
            // Try relative paths
            "composeApp/src/commonMain/composeResources/drawable/AxioBank.png",
            "src/commonMain/composeResources/drawable/AxioBank.png",
            "../composeApp/src/commonMain/composeResources/drawable/AxioBank.png",
            // Try absolute path
            "C:/Users/ADMIN/AxionBank/Axio Bank/composeApp/src/commonMain/composeResources/drawable/AxioBank.png"
        )

        for (path in possiblePaths) {
            if (path != null && File(path).exists()) {
                println("✅ Logo found at: $path")
                return path
            }
        }

        println("⚠️ Logo not found in any of the expected locations")
        null
    } catch (e: Exception) {
        println("⚠️ Error locating logo: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Download QR code to user's device
 */
private fun downloadQRCode(qrJsonData: String, accountName: String): Boolean {
    return try {
        val downloadsDir = System.getProperty("user.home") + "/Downloads"
        val fileName = "AxioBank_QR_${accountName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val outputPath = "$downloadsDir/$fileName"

        val logoPath = getLogoPath()
        val success = QRCodeGenerator.saveQRCodeToFile(
            data = qrJsonData,
            size = 1024, // Higher resolution for saving
            logoPath = logoPath,
            outputPath = outputPath
        )

        if (success) {
            println("✅ QR Code saved to: $outputPath")
        }
        success
    } catch (e: Exception) {
        println("❌ Failed to save QR code: ${e.message}")
        e.printStackTrace()
        false
    }
}

/**
 * Share QR code (opens system share dialog)
 */
private fun shareQRCode(qrJsonData: String, accountName: String) {
    try {
        val logoPath = getLogoPath()
        val qrBytes = QRCodeGenerator.getQRCodeBytes(
            data = qrJsonData,
            size = 1024,
            logoPath = logoPath
        )

        if (qrBytes != null) {
            // Save to temp file for sharing
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "AxioBank_QR_${accountName.replace(" ", "_")}.png"
            val tempFile = File(tempDir, fileName)
            tempFile.writeBytes(qrBytes)

            // Open file location (platform-specific)
            val desktop = java.awt.Desktop.getDesktop()
            if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                desktop.open(tempFile.parentFile)
            }

            println("✅ QR Code ready to share at: ${tempFile.absolutePath}")
        }
    } catch (e: Exception) {
        println("❌ Failed to prepare QR code for sharing: ${e.message}")
        e.printStackTrace()
    }
}
