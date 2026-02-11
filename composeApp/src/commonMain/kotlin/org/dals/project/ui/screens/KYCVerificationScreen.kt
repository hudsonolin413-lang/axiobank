package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dals.project.model.DocumentStatus
import org.dals.project.model.DocumentType
import org.dals.project.model.KYCDocument
import org.dals.project.utils.FileValidator
import org.dals.project.utils.PermissionStatus
import org.dals.project.viewmodel.AuthViewModel

// Expect declarations for platform-specific composable functions
@Composable
expect fun rememberFilePickerManager(): org.dals.project.utils.FilePickerManager

@Composable
expect fun rememberCameraManager(): org.dals.project.utils.CameraManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KYCVerificationScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showUploadOptions by remember { mutableStateOf(false) }
    var selectedDocumentType by remember { mutableStateOf<DocumentType?>(null) }
    var uploadedDocuments by remember { mutableStateOf(emptyList<KYCDocument>()) }

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
                title = { Text("KYC Verification") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadOptions = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Document")
            }
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
                KYCHeader()
            }

            // Verification Status
            item {
                VerificationStatusCard(uploadedDocuments)
            }

            // Required Documents Section
            item {
                Text(
                    text = "Required Documents",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                RequiredDocumentsCard(
                    uploadedDocuments = uploadedDocuments,
                    onUploadDocument = { documentType ->
                        selectedDocumentType = documentType
                        showUploadOptions = true
                    }
                )
            }

            // Uploaded Documents
            if (uploadedDocuments.isNotEmpty()) {
                item {
                    Text(
                        text = "Uploaded Documents",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uploadedDocuments) { document ->
                    UploadedDocumentCard(
                        document = document,
                        onDelete = { docToDelete ->
                            uploadedDocuments = uploadedDocuments.filter { it.id != docToDelete.id }
                        }
                    )
                }
            }

            // Tips Section
            item {
                DocumentTipsCard()
            }
        }
    }

    // Upload Options Bottom Sheet
    if (showUploadOptions) {
        ModalBottomSheet(
            onDismissRequest = {
                showUploadOptions = false
                selectedDocumentType = null
            }
        ) {
            UploadOptionsBottomSheet(
                selectedDocumentType = selectedDocumentType,
                onDocumentTypeSelected = { type ->
                    selectedDocumentType = type
                },
                onCameraSelected = {
                    scope.launch {
                        try {
                            val hasPermission = cameraManager.requestCameraPermission()
                            if (hasPermission) {
                                val photoData = cameraManager.capturePhoto()
                                val currentUser = authUiState.currentUser
                                val docType = selectedDocumentType

                                if (photoData != null && currentUser != null && docType != null) {
                                    // Capture values for lambda closures
                                    val userId = currentUser.id
                                    val documentType = docType

                                    val fileName = FileValidator.generateFileName(
                                        prefix = documentType.name.lowercase(),
                                        extension = "jpg"
                                    )

                                    // Upload to backend
                                    authViewModel.uploadKYCDocument(
                                        customerId = userId,
                                        documentType = documentType.name,
                                        fileName = fileName,
                                        fileData = photoData,
                                        onSuccess = {
                                            // Add to local list for display
                                            val timestamp = java.time.Instant.now().toString()
                                            val newDocument = KYCDocument(
                                                id = "uploaded_${fileName.hashCode()}_${System.currentTimeMillis()}",
                                                type = documentType,
                                                fileName = fileName,
                                                filePath = "/uploads/kyc/$userId/$fileName",
                                                uploadDate = timestamp,
                                                status = DocumentStatus.PENDING
                                            )
                                            uploadedDocuments = uploadedDocuments + newDocument
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
                        showUploadOptions = false
                        selectedDocumentType = null
                    }
                },
                onGallerySelected = {
                    scope.launch {
                        try {
                            val hasPermission = filePickerManager.requestPermissions()
                            if (hasPermission) {
                                val imageData = filePickerManager.pickImage()
                                val currentUser = authUiState.currentUser
                                val docType = selectedDocumentType

                                if (imageData != null && currentUser != null && docType != null) {
                                    // Capture values for lambda closures
                                    val userId = currentUser.id
                                    val documentType = docType

                                    val fileName = FileValidator.generateFileName(
                                        prefix = documentType.name.lowercase(),
                                        extension = "jpg"
                                    )

                                    // Upload to backend
                                    authViewModel.uploadKYCDocument(
                                        customerId = userId,
                                        documentType = documentType.name,
                                        fileName = fileName,
                                        fileData = imageData,
                                        onSuccess = {
                                            // Add to local list for display
                                            val timestamp = java.time.Instant.now().toString()
                                            val newDocument = KYCDocument(
                                                id = "uploaded_${fileName.hashCode()}_${System.currentTimeMillis()}",
                                                type = documentType,
                                                fileName = fileName,
                                                filePath = "/uploads/kyc/$userId/$fileName",
                                                uploadDate = timestamp,
                                                status = DocumentStatus.PENDING
                                            )
                                            uploadedDocuments = uploadedDocuments + newDocument
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
                        showUploadOptions = false
                        selectedDocumentType = null
                    }
                },
                onDismiss = {
                    showUploadOptions = false
                    selectedDocumentType = null
                }
            )
        }
    }
}

@Composable
private fun KYCHeader() {
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
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Identity Verification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Complete your KYC to unlock all features and increase your loan limits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun VerificationStatusCard(documents: List<KYCDocument>) {
    val totalRequired = DocumentType.values().size
    val uploaded = documents.size
    val approved = documents.count { it.status == DocumentStatus.APPROVED }
    val pending = documents.count { it.status == DocumentStatus.PENDING }
    val rejected = documents.count { it.status == DocumentStatus.REJECTED }

    val progress = (approved.toFloat() / totalRequired).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                approved == totalRequired -> MaterialTheme.colorScheme.tertiaryContainer
                rejected > 0 -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                approved == totalRequired -> Icons.Default.Check
                                rejected > 0 -> Icons.Default.Error
                                pending > 0 -> Icons.Default.Pending
                                else -> Icons.Default.CloudUpload
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = when {
                                approved == totalRequired -> MaterialTheme.colorScheme.tertiary
                                rejected > 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = when {
                                approved == totalRequired -> "Verification Complete"
                                rejected > 0 -> "Documents Need Attention"
                                pending > 0 -> "Verification in Progress"
                                else -> "Getting Started"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$approved of $totalRequired documents approved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    approved == totalRequired -> MaterialTheme.colorScheme.tertiary
                    rejected > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            if (pending > 0 || rejected > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pending > 0) {
                        StatusChip(
                            text = "$pending Pending",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (rejected > 0) {
                        StatusChip(
                            text = "$rejected Rejected",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequiredDocumentsCard(
    uploadedDocuments: List<KYCDocument>,
    onUploadDocument: (DocumentType) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            DocumentType.values().forEach { docType ->
                val uploadedDoc = uploadedDocuments.find { it.type == docType }

                RequiredDocumentItem(
                    documentType = docType,
                    uploadedDocument = uploadedDoc,
                    onUpload = { onUploadDocument(docType) }
                )

                if (docType != DocumentType.values().last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RequiredDocumentItem(
    documentType: DocumentType,
    uploadedDocument: KYCDocument?,
    onUpload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document Icon
        Text(
            text = getDocumentTypeIcon(documentType),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 12.dp)
        )

        // Document Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getDocumentTypeName(documentType),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = getDocumentTypeDescription(documentType),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status/Action
        if (uploadedDocument != null) {
            StatusIcon(status = uploadedDocument.status)
        } else {
            OutlinedButton(
                onClick = onUpload,
                modifier = Modifier.size(width = 80.dp, height = 36.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Upload", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun UploadedDocumentCard(
    document: KYCDocument,
    onDelete: (KYCDocument) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (document.status) {
                DocumentStatus.APPROVED -> MaterialTheme.colorScheme.tertiaryContainer
                DocumentStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document preview placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getDocumentTypeIcon(document.type),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Document Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getDocumentTypeName(document.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Uploaded: ${document.uploadDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status with description
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StatusIcon(status = document.status)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = getStatusText(document.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(document.status)
                    )
                }
            }

            // Delete button for pending/rejected documents
            if (document.status != DocumentStatus.APPROVED) {
                IconButton(onClick = { onDelete(document) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadOptionsBottomSheet(
    selectedDocumentType: DocumentType?,
    onDocumentTypeSelected: (DocumentType) -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onDismiss: () -> Unit
) {
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocumentTypeSelected(docType) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                    .clickable { onCameraSelected() }
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
                    .clickable { onGallerySelected() }
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

@Composable
private fun DocumentTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Document Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            val tips = listOf(
                "Ensure all text is clearly visible and readable",
                "Use good lighting when taking photos",
                "Keep the entire document within the frame",
                "Avoid glare, shadows, or blurred images",
                "Make sure the document is not expired"
            )

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: DocumentStatus) {
    when (status) {
        DocumentStatus.APPROVED -> Icon(
            Icons.Default.Check,
            contentDescription = "Approved",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )

        DocumentStatus.REJECTED -> Icon(
            Icons.Default.Error,
            contentDescription = "Rejected",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )

        DocumentStatus.PENDING -> Icon(
            Icons.Default.Pending,
            contentDescription = "Pending",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        DocumentStatus.EXPIRED -> Icon(
            Icons.Default.Error,
            contentDescription = "Expired",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// Utility functions
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

@Composable
private fun getStatusColor(status: DocumentStatus): Color {
    return when (status) {
        DocumentStatus.APPROVED -> MaterialTheme.colorScheme.tertiary
        DocumentStatus.REJECTED, DocumentStatus.EXPIRED -> MaterialTheme.colorScheme.error
        DocumentStatus.PENDING -> MaterialTheme.colorScheme.primary
    }
}

private fun getStatusText(status: DocumentStatus): String {
    return when (status) {
        DocumentStatus.APPROVED -> "Approved"
        DocumentStatus.REJECTED -> "Rejected"
        DocumentStatus.PENDING -> "Under Review"
        DocumentStatus.EXPIRED -> "Expired"
    }
}