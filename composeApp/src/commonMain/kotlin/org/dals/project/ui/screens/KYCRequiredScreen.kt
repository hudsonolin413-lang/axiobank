package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dals.project.model.DocumentStatus
import org.dals.project.model.DocumentType
import org.dals.project.model.KYCDocument
import org.dals.project.utils.FileValidator
import org.dals.project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KYCRequiredScreen(
    authViewModel: AuthViewModel,
    onNavigateToMainApp: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showUploadOptions by remember { mutableStateOf(false) }
    var selectedDocumentType by remember { mutableStateOf<DocumentType?>(null) }
    var uploadedDocuments by remember { mutableStateOf(emptyList<KYCDocument>()) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Platform-specific file picker and camera managers
    val filePickerManager = rememberFilePickerManager()
    val cameraManager = rememberCameraManager()

    // Fetch KYC documents when screen loads
    LaunchedEffect(authUiState.currentUser?.id) {
        authUiState.currentUser?.id?.let { customerId ->
            authViewModel.getKYCDocuments(
                customerId = customerId,
                onSuccess = { documents ->
                    // Map backend document format to frontend KYCDocument model
                    uploadedDocuments = documents.map { doc ->
                        val docType = when (doc["documentType"]) {
                            "OTHER_GOVERNMENT_ID" -> DocumentType.NATIONAL_ID
                            "DRIVERS_LICENSE" -> DocumentType.DRIVING_LICENSE
                            "PASSPORT" -> DocumentType.PASSPORT
                            "UTILITY_BILL" -> DocumentType.UTILITY_BILL
                            "BANK_STATEMENT" -> DocumentType.BANK_STATEMENT
                            else -> DocumentType.NATIONAL_ID
                        }
                        val status = when (doc["status"]) {
                            "PENDING_REVIEW" -> DocumentStatus.PENDING
                            "APPROVED" -> DocumentStatus.APPROVED
                            "REJECTED" -> DocumentStatus.REJECTED
                            else -> DocumentStatus.PENDING
                        }
                        KYCDocument(
                            id = doc["id"] ?: "",
                            type = docType,
                            fileName = doc["fileName"] ?: "",
                            filePath = doc["filePath"] ?: "",
                            uploadDate = doc["uploadDate"] ?: "",
                            status = status
                        )
                    }

                    // If user has already uploaded documents, allow them to proceed
                    if (uploadedDocuments.isNotEmpty()) {
                        // User has submitted documents, allow access to main app
                        // They will be redirected if needed based on KYC status in MainAppScreen
                    }
                },
                onError = { error ->
                    println("Failed to fetch KYC documents: $error")
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Header Icon
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Title
            item {
                Text(
                    text = "Identity Verification Required",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Description
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Welcome to Axio Bank!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Before you can start using your account, we need to verify your identity. This is a one-time process required by financial regulations.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // What You'll Need
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "You'll Need:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        RequirementItem(
                            icon = Icons.Default.Badge,
                            text = "Government-issued ID (Passport, National ID, or Driver's License)"
                        )
                        RequirementItem(
                            icon = Icons.Default.Description,
                            text = "Proof of address (Utility bill or Bank statement)"
                        )
                        RequirementItem(
                            icon = Icons.Default.CameraAlt,
                            text = "A clear selfie with your ID document"
                        )
                    }
                }
            }

            // Benefits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "After Verification You Can:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        BenefitItem("Send and receive money")
                        BenefitItem("Apply for loans")
                        BenefitItem("Access investment opportunities")
                        BenefitItem("Use all banking features")
                    }
                }
            }

            // Status Display (if documents uploaded)
            if (uploadedDocuments.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Documents Submitted!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You've uploaded ${uploadedDocuments.size} document(s). You can now access your account while we review your submission.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Verification Button
                    Button(
                        onClick = { showUploadOptions = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uploadedDocuments.isEmpty()) "Upload Photo of Details" else "Upload More Documents",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Continue to App Button (only if documents uploaded)
                    if (uploadedDocuments.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onNavigateToMainApp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Continue to App",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Privacy Notice
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Your information is encrypted and secure. We comply with all data protection regulations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                        text = "This will return you to the login screen.",
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

    // Upload Options Bottom Sheet
    if (showUploadOptions) {
        ModalBottomSheet(
            onDismissRequest = {
                showUploadOptions = false
                selectedDocumentType = null
            }
        ) {
            KYCUploadOptionsSheet(
                authViewModel = authViewModel,
                selectedDocumentType = selectedDocumentType,
                uploadedDocuments = uploadedDocuments,
                filePickerManager = filePickerManager,
                cameraManager = cameraManager,
                onDocumentTypeSelected = { type ->
                    selectedDocumentType = type
                },
                onDocumentUploaded = { newDocument ->
                    uploadedDocuments = uploadedDocuments + newDocument
                },
                onNavigateToMainApp = onNavigateToMainApp,
                onDismiss = {
                    showUploadOptions = false
                    selectedDocumentType = null
                }
            )
        }
    }
}

@Composable
private fun RequirementItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun KYCUploadOptionsSheet(
    authViewModel: AuthViewModel,
    selectedDocumentType: DocumentType?,
    uploadedDocuments: List<KYCDocument>,
    filePickerManager: org.dals.project.utils.FilePickerManager,
    cameraManager: org.dals.project.utils.CameraManager,
    onDocumentTypeSelected: (DocumentType) -> Unit,
    onDocumentUploaded: (KYCDocument) -> Unit,
    onNavigateToMainApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Upload Document",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Document Type Selection (if not already selected)
        if (selectedDocumentType == null) {
            Text(
                text = "Select Document Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DocumentType.values().forEach { docType ->
                val isAlreadyUploaded = uploadedDocuments.any { it.type == docType }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAlreadyUploaded) {
                            if (!isAlreadyUploaded) {
                                onDocumentTypeSelected(docType)
                            }
                        }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlreadyUploaded)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = getDocumentTypeIcon(docType),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            Column {
                                Text(
                                    text = getDocumentTypeName(docType),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = getDocumentTypeDescription(docType),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isAlreadyUploaded) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Uploaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Upload method selection
            Text(
                text = "Choose Upload Method for ${getDocumentTypeName(selectedDocumentType)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Camera Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            try {
                                val hasPermission = cameraManager.requestCameraPermission()
                                if (hasPermission) {
                                    val photoData = cameraManager.capturePhoto()
                                    val currentUser = authUiState.currentUser

                                    if (photoData != null && currentUser != null) {
                                        val userId = currentUser.id
                                        val documentType = selectedDocumentType

                                        val fileName = FileValidator.generateFileName(
                                            prefix = documentType.name.lowercase(),
                                            extension = "jpg"
                                        )

                                        authViewModel.uploadKYCDocument(
                                            customerId = userId,
                                            documentType = documentType.name,
                                            fileName = fileName,
                                            fileData = photoData,
                                            onSuccess = {
                                                val timestamp = java.time.Instant.now().toString()
                                                val newDocument = KYCDocument(
                                                    id = "uploaded_${fileName.hashCode()}_${System.currentTimeMillis()}",
                                                    type = documentType,
                                                    fileName = fileName,
                                                    filePath = "/uploads/kyc/$userId/$fileName",
                                                    uploadDate = timestamp,
                                                    status = DocumentStatus.PENDING
                                                )
                                                onDocumentUploaded(newDocument)
                                                // After successful upload, navigate to main app as requested
                                                onNavigateToMainApp()
                                            },
                                            onError = { error ->
                                                println("Upload failed: $error")
                                            }
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                println("Camera error: ${e.message}")
                            }
                            onDismiss()
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = "Take Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use your device camera to capture the document",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Gallery Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            try {
                                val hasPermission = filePickerManager.requestPermissions()
                                if (hasPermission) {
                                    val imageData = filePickerManager.pickImage()
                                    val currentUser = authUiState.currentUser

                                    if (imageData != null && currentUser != null) {
                                        val userId = currentUser.id
                                        val documentType = selectedDocumentType

                                        val fileName = FileValidator.generateFileName(
                                            prefix = documentType.name.lowercase(),
                                            extension = "jpg"
                                        )

                                        authViewModel.uploadKYCDocument(
                                            customerId = userId,
                                            documentType = documentType.name,
                                            fileName = fileName,
                                            fileData = imageData,
                                            onSuccess = {
                                                val timestamp = java.time.Instant.now().toString()
                                                val newDocument = KYCDocument(
                                                    id = "uploaded_${fileName.hashCode()}_${System.currentTimeMillis()}",
                                                    type = documentType,
                                                    fileName = fileName,
                                                    filePath = "/uploads/kyc/$userId/$fileName",
                                                    uploadDate = timestamp,
                                                    status = DocumentStatus.PENDING
                                                )
                                                onDocumentUploaded(newDocument)
                                                // After successful upload, navigate to main app as requested
                                                onNavigateToMainApp()
                                            },
                                            onError = { error ->
                                                println("Upload failed: $error")
                                            }
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                println("Gallery error: ${e.message}")
                            }
                            onDismiss()
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = "Choose from Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select an existing photo from your device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Utility functions from KYCVerificationScreen
private fun getDocumentTypeIcon(type: DocumentType): String {
    return when (type) {
        DocumentType.NATIONAL_ID -> "ID"
        DocumentType.PASSPORT -> "PP"
        DocumentType.DRIVING_LICENSE -> "DL"
        DocumentType.UTILITY_BILL -> "UB"
        DocumentType.BANK_STATEMENT -> "BS"
        DocumentType.SELFIE -> "SF"
    }
}

private fun getDocumentTypeName(type: DocumentType): String {
    return when (type) {
        DocumentType.NATIONAL_ID -> "National ID"
        DocumentType.PASSPORT -> "Passport"
        DocumentType.DRIVING_LICENSE -> "Driving License"
        DocumentType.UTILITY_BILL -> "Utility Bill"
        DocumentType.BANK_STATEMENT -> "Bank Statement"
        DocumentType.SELFIE -> "Selfie Verification"
    }
}

private fun getDocumentTypeDescription(type: DocumentType): String {
    return when (type) {
        DocumentType.NATIONAL_ID -> "Front and back of your national ID card"
        DocumentType.PASSPORT -> "Photo page of your passport"
        DocumentType.DRIVING_LICENSE -> "Front of your driving license"
        DocumentType.UTILITY_BILL -> "Recent utility bill (last 3 months)"
        DocumentType.BANK_STATEMENT -> "Recent bank statement (last 3 months)"
        DocumentType.SELFIE -> "Clear photo of yourself holding your ID"
    }
}
