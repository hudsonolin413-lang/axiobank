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
import org.dals.project.viewmodel.OverdraftProtectionViewModel
import org.dals.project.repository.EnrollOverdraftProtectionRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdraftProtectionScreen(
    customerId: String,
    accountId: String,
    viewModel: OverdraftProtectionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEnrollDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId, accountId) {
        viewModel.loadData(customerId, accountId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overdraft Protection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.protection?.let { protection ->
                    ActiveProtectionView(
                        protection = protection,
                        onCancel = { viewModel.cancel(protection.id, customerId, accountId) }
                    )
                } ?: run {
                    NoProtectionView(onEnrollClick = { showEnrollDialog = true })
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Recent Overdraft Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.transactions) { tx ->
                        OverdraftTransactionItem(tx)
                    }
                    if (uiState.transactions.isEmpty()) {
                        item {
                            Text(
                                "No recent transactions",
                                modifier = Modifier.padding(vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEnrollDialog) {
        EnrollOverdraftDialog(
            onDismiss = { showEnrollDialog = false },
            onConfirm = { type, linkedId, limit ->
                viewModel.enroll(
                    EnrollOverdraftProtectionRequest(
                        customerId = customerId,
                        accountId = accountId,
                        protectionType = type,
                        linkedAccountId = linkedId,
                        creditLimit = limit
                    )
                )
                showEnrollDialog = false
            }
        )
    }
}

@Composable
fun ActiveProtectionView(
    protection: org.dals.project.repository.OverdraftProtectionDto,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Protection Active",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Type: ${protection.protectionType}", style = MaterialTheme.typography.bodyLarge)
            Text("Status: ${protection.status}", style = MaterialTheme.typography.bodyMedium)
            
            protection.creditLimit?.let {
                Text("Credit Limit: $${it}", style = MaterialTheme.typography.bodyMedium)
                Text("Used: $${protection.usedAmount}", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel Protection")
            }
        }
    }
}

@Composable
fun NoProtectionView(onEnrollClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ShieldMoon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Overdraft Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Protect your account from declined transactions and fees by enabling overdraft protection.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onEnrollClick) {
                Text("Enable Protection")
            }
        }
    }
}

@Composable
fun OverdraftTransactionItem(tx: org.dals.project.repository.OverdraftTransactionDto) {
    ListItem(
        headlineContent = { Text("Transfer: $${tx.amount}") },
        supportingContent = { Text("${tx.createdAt} • ${tx.status}") },
        trailingContent = { 
            Text(
                "-$${tx.totalAmount}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollOverdraftDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, linkedId: String?, limit: Double?) -> Unit
) {
    var selectedType by remember { mutableStateOf("LINKED_ACCOUNT") }
    var linkedId by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Overdraft Protection") },
        text = {
            Column {
                Text("Choose protection type:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedType == "LINKED_ACCOUNT", onClick = { selectedType = "LINKED_ACCOUNT" })
                    Text("Linked Account")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedType == "LINE_OF_CREDIT", onClick = { selectedType = "LINE_OF_CREDIT" })
                    Text("Line of Credit")
                }
                
                if (selectedType == "LINKED_ACCOUNT") {
                    OutlinedTextField(
                        value = linkedId,
                        onValueChange = { linkedId = it },
                        label = { Text("Linked Account ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = { Text("Credit Limit") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(selectedType, if (selectedType == "LINKED_ACCOUNT") linkedId else null, creditLimit.toDoubleOrNull())
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
