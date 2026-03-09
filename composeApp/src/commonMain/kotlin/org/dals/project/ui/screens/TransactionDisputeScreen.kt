package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dals.project.model.Transaction
import org.dals.project.model.TransactionStatus
import org.dals.project.viewmodel.TransactionViewModel

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
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    
    // Local state for disputes (in a real app, this would come from a DisputeViewModel/Repository)
    var disputes by remember { mutableStateOf<List<TransactionDispute>>(emptyList()) }
    var showNewDisputeDialog by remember { mutableStateOf(false) }
    var selectedDispute by remember { mutableStateOf<TransactionDispute?>(null) }
    var showTransactionPicker by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    val scope = rememberCoroutineScope()

    // Refresh transactions on load
    LaunchedEffect(Unit) {
        transactionViewModel.refreshAllData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Disputes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { transactionViewModel.refreshAllData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTransactionPicker = true },
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

            // Statistics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Active",
                        value = disputes.count { it.status == DisputeStatus.PENDING || it.status == DisputeStatus.UNDER_REVIEW }.toString(),
                        color = Color(0xFFFFA000)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Resolved",
                        value = disputes.count { it.status == DisputeStatus.RESOLVED }.toString(),
                        color = Color(0xFF4CAF50)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total",
                        value = disputes.size.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Disputes List
            item {
                Text(
                    text = "Your Disputes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (disputes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No disputes filed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "If you notice any unauthorized transactions, tap the button below to file a dispute",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(disputes) { dispute ->
                    DisputeCard(
                        dispute = dispute,
                        onClick = { selectedDispute = dispute }
                    )
                }
            }
        }
    }

    // Transaction Picker Dialog
    if (showTransactionPicker) {
        TransactionPickerDialog(
            transactions = transactionUiState.transactions,
            isLoading = transactionUiState.isLoading,
            onDismiss = { showTransactionPicker = false },
            onSelectTransaction = { transaction ->
                selectedTransaction = transaction
                showTransactionPicker = false
                showNewDisputeDialog = true
            }
        )
    }

    // New Dispute Dialog
    if (showNewDisputeDialog && selectedTransaction != null) {
        NewDisputeDialog(
            transaction = selectedTransaction!!,
            onDismiss = { 
                showNewDisputeDialog = false
                selectedTransaction = null
            },
            onSubmit = { reason, description ->
                val newDispute = TransactionDispute(
                    id = "DSP${disputes.size + 1}",
                    transactionId = selectedTransaction!!.id,
                    transactionAmount = selectedTransaction!!.amount,
                    merchantName = selectedTransaction!!.recipientName ?: selectedTransaction!!.description,
                    transactionDate = selectedTransaction!!.timestamp,
                    reason = reason,
                    description = description,
                    status = DisputeStatus.PENDING,
                    createdAt = "Just now"
                )
                disputes = disputes + newDispute
                showNewDisputeDialog = false
                selectedTransaction = null
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
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun DisputeCard(
    dispute: TransactionDispute,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionPickerDialog(
    transactions: List<Transaction>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelectTransaction: (Transaction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Transaction to Dispute") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions.take(20)) { transaction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTransaction(transaction) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = transaction.recipientName ?: transaction.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = transaction.timestamp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$${String.format("%.2f", transaction.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewDisputeDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSubmit: (reason: DisputeReason, description: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(DisputeReason.UNAUTHORIZED) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File a Dispute") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Transaction Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Transaction Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.recipientName ?: transaction.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$${String.format("%.2f", transaction.amount)} • ${transaction.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                    placeholder = { Text("Describe the issue with this transaction...") },
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
                    onSubmit(selectedReason, description)
                },
                enabled = !isLoading && description.isNotBlank()
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
        },
        dismissButton = {}
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
