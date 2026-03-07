package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DisputeStatus {
    PENDING, UNDER_REVIEW, RESOLVED, REJECTED
}

enum class DisputeReason {
    UNAUTHORIZED, DUPLICATE, NOT_RECEIVED, WRONG_AMOUNT, FRAUD, OTHER
}

data class TransactionDispute(
    val id: String,
    val transactionId: String,
    val transactionAmount: Double,
    val merchantName: String,
    val transactionDate: String,
    val reason: DisputeReason,
    val description: String,
    val status: DisputeStatus,
    val createdAt: String,
    val resolvedAt: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDisputeScreen(
    onNavigateBack: () -> Unit
) {
    var disputes by remember { mutableStateOf(listOf(
        TransactionDispute("1", "TXN001", 150.0, "Unknown Merchant", "2024-02-15", DisputeReason.UNAUTHORIZED, "I did not make this transaction", DisputeStatus.UNDER_REVIEW, "2024-02-16"),
        TransactionDispute("2", "TXN002", 50.0, "Amazon", "2024-02-10", DisputeReason.DUPLICATE, "Charged twice for same order", DisputeStatus.RESOLVED, "2024-02-11", "2024-02-18")
    )) }
    var showNewDisputeDialog by remember { mutableStateOf(false) }
    var selectedDispute by remember { mutableStateOf<TransactionDispute?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Disputes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewDisputeDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Dispute") }
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
            // Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dispute a Transaction",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Challenge unauthorized or incorrect charges. Most disputes are resolved within 7-10 business days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (disputes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Disputes",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "You haven't filed any transaction disputes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Your Disputes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(disputes) { dispute ->
                    DisputeCard(
                        dispute = dispute,
                        onClick = { selectedDispute = dispute }
                    )
                }
            }
        }
    }

    // New Dispute Dialog
    if (showNewDisputeDialog) {
        NewDisputeDialog(
            onDismiss = { showNewDisputeDialog = false },
            onSubmit = { transactionId, amount, merchant, reason, description ->
                scope.launch {
                    val newDispute = TransactionDispute(
                        id = (disputes.size + 1).toString(),
                        transactionId = transactionId,
                        transactionAmount = amount,
                        merchantName = merchant,
                        transactionDate = "2024-02-28",
                        reason = reason,
                        description = description,
                        status = DisputeStatus.PENDING,
                        createdAt = "2024-02-28"
                    )
                    disputes = disputes + newDispute
                    showNewDisputeDialog = false
                }
            }
        )
    }

    // Dispute Details Dialog
    selectedDispute?.let { dispute ->
        DisputeDetailsDialog(
            dispute = dispute,
            onDismiss = { selectedDispute = null }
        )
    }
}

@Composable
private fun DisputeCard(
    dispute: TransactionDispute,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dispute.merchantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Transaction: ${dispute.transactionId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DisputeStatusChip(status = dispute.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", dispute.transactionAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dispute.reason.name.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Filed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dispute.createdAt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DisputeStatusChip(status: DisputeStatus) {
    val (color, text) = when (status) {
        DisputeStatus.PENDING -> Pair(Color(0xFFFFA000), "Pending")
        DisputeStatus.UNDER_REVIEW -> Pair(Color(0xFF2196F3), "Under Review")
        DisputeStatus.RESOLVED -> Pair(Color(0xFF4CAF50), "Resolved")
        DisputeStatus.REJECTED -> Pair(Color(0xFFF44336), "Rejected")
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
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

@Composable
private fun NewDisputeDialog(
    onDismiss: () -> Unit,
    onSubmit: (transactionId: String, amount: Double, merchant: String, reason: DisputeReason, description: String) -> Unit
) {
    var transactionId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(DisputeReason.UNAUTHORIZED) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File a Dispute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = { Text("Transaction ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("$") }
                )

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Reason Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedReason.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DisputeReason.values().forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.name.replace("_", " ")) },
                                onClick = {
                                    selectedReason = reason
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onSubmit(transactionId, amount.toDoubleOrNull() ?: 0.0, merchant, selectedReason, description)
                },
                enabled = !isLoading && transactionId.isNotBlank() && amount.isNotBlank() && merchant.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisputeDetailsDialog(
    dispute: TransactionDispute,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dispute Details")
                Spacer(modifier = Modifier.width(8.dp))
                DisputeStatusChip(status = dispute.status)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Transaction ID", dispute.transactionId)
                DetailRow("Merchant", dispute.merchantName)
                DetailRow("Amount", "$${String.format("%.2f", dispute.transactionAmount)}")
                DetailRow("Transaction Date", dispute.transactionDate)
                DetailRow("Reason", dispute.reason.name.replace("_", " "))
                DetailRow("Filed On", dispute.createdAt)
                dispute.resolvedAt?.let { DetailRow("Resolved On", it) }
                
                HorizontalDivider()
                
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dispute.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium
        )
    }
}
