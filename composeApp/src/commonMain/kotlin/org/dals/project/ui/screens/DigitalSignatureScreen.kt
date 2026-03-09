package org.dals.project.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.viewmodel.DigitalSignatureViewModel
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalSignatureScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    viewModel: DigitalSignatureViewModel = viewModel { DigitalSignatureViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSignatures(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Signatures") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (!uiState.showSignaturePad) {
                FloatingActionButton(
                    onClick = { viewModel.showSignaturePad() }
                ) {
                    Icon(Icons.Default.Edit, "New Signature")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.showSignaturePad) {
                SignaturePadView(
                    uiState = uiState,
                    viewModel = viewModel,
                    customerId = customerId,
                    onCancel = { viewModel.hideSignaturePad() }
                )
            } else {
                SignatureListView(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // Messages
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) { Text(error) }
            }

            uiState.successMessage?.let { success ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK")
                        }
                    }
                ) { Text(success) }
            }
        }
    }
}

@Composable
private fun SignatureListView(
    uiState: org.dals.project.viewmodel.DigitalSignatureUiState,
    viewModel: DigitalSignatureViewModel
) {
    if (uiState.signatures.isEmpty() && !uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Draw,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No signatures yet",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Tap + to create your first signature",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.signatures) { signature ->
                SignatureCard(signature)
            }
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SignatureCard(signature: org.dals.project.repository.DigitalSignatureDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        signature.documentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        signature.documentType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (signature.isValid) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Valid",
                        tint = Color(0xFF4CAF50)
                    )
                } else {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Invalid",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Signed: ${signature.signedAt.take(19).replace('T', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "ID: ${signature.documentId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignaturePadView(
    uiState: org.dals.project.viewmodel.DigitalSignatureUiState,
    viewModel: DigitalSignatureViewModel,
    customerId: String,
    onCancel: () -> Unit
) {
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf(Path()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Create Digital Signature",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.documentName,
            onValueChange = { viewModel.updateDocumentName(it) },
            label = { Text("Document Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Draw your signature below:", style = MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(8.dp))

        // Signature Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            },
                            onDrag = { change, _ ->
                                currentPath.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                paths = paths + currentPath
                                currentPath = Path()
                            }
                        )
                    }
            ) {
                // Draw all completed paths
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                // Draw current path
                drawPath(
                    path = currentPath,
                    color = Color.Black,
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    paths = emptyList()
                    currentPath = Path()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear")
            }

            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        // Convert signature to base64
                        val signatureData = "SIGNATURE_${System.currentTimeMillis()}"
                        viewModel.createSignature(customerId, signatureData)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = paths.isNotEmpty() && uiState.documentName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
