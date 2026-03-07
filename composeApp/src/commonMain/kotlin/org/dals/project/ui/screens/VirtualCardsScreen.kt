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

data class VirtualCard(
    val id: String,
    val cardNumber: String,
    val expiryDate: String,
    val cvv: String,
    val cardholderName: String,
    val balance: Double,
    val spendingLimit: Double,
    val isActive: Boolean,
    val isTemporary: Boolean,
    val createdAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualCardsScreen(
    onNavigateBack: () -> Unit
) {
    var virtualCards by remember { mutableStateOf(listOf(
        VirtualCard("1", "**** **** **** 4521", "12/26", "***", "John Doe", 500.0, 1000.0, true, false, "2024-01-15"),
        VirtualCard("2", "**** **** **** 8734", "03/25", "***", "John Doe", 0.0, 200.0, true, true, "2024-02-20")
    )) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<VirtualCard?>(null) }
    var showCardDetails by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Cards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Card")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Card") }
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
            item {
                // Info Card
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
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Virtual Cards",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Create disposable cards for secure online shopping",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (virtualCards.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Virtual Cards",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Create your first virtual card for secure online payments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(virtualCards) { card ->
                    VirtualCardItem(
                        card = card,
                        onCardClick = {
                            selectedCard = card
                            showCardDetails = true
                        },
                        onToggleActive = { isActive ->
                            virtualCards = virtualCards.map {
                                if (it.id == card.id) it.copy(isActive = isActive) else it
                            }
                        },
                        onDelete = {
                            virtualCards = virtualCards.filter { it.id != card.id }
                        }
                    )
                }
            }
        }
    }

    // Create Card Dialog
    if (showCreateDialog) {
        CreateVirtualCardDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { isTemporary, limit ->
                scope.launch {
                    isLoading = true
                    delay(1500) // Simulate API call
                    val newCard = VirtualCard(
                        id = (virtualCards.size + 1).toString(),
                        cardNumber = "**** **** **** ${(1000..9999).random()}",
                        expiryDate = if (isTemporary) "24h" else "12/${(25..28).random()}",
                        cvv = "***",
                        cardholderName = "John Doe",
                        balance = 0.0,
                        spendingLimit = limit,
                        isActive = true,
                        isTemporary = isTemporary,
                        createdAt = "2024-02-28"
                    )
                    virtualCards = virtualCards + newCard
                    isLoading = false
                    showCreateDialog = false
                }
            },
            isLoading = isLoading
        )
    }

    // Card Details Dialog
    if (showCardDetails && selectedCard != null) {
        VirtualCardDetailsDialog(
            card = selectedCard!!,
            onDismiss = { showCardDetails = false }
        )
    }
}

@Composable
private fun VirtualCardItem(
    card: VirtualCard,
    onCardClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isActive) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (card.isTemporary) Icons.Filled.Timer else Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (card.isTemporary) "Temporary Card" else "Virtual Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = card.cardNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = card.isActive,
                    onCheckedChange = onToggleActive
                )
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
                        text = "Expires",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = card.expiryDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", card.spendingLimit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (card.isActive) "Active" else "Frozen",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (card.isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Card?") },
            text = { Text("This action cannot be undone. The card will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CreateVirtualCardDialog(
    onDismiss: () -> Unit,
    onCreate: (isTemporary: Boolean, limit: Double) -> Unit,
    isLoading: Boolean
) {
    var isTemporary by remember { mutableStateOf(false) }
    var spendingLimit by remember { mutableStateOf("500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Virtual Card") },
        text = {
            Column {
                Text(
                    text = "Configure your new virtual card",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Temporary Card",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Expires in 24 hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTemporary,
                        onCheckedChange = { isTemporary = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = spendingLimit,
                    onValueChange = { spendingLimit = it.filter { c -> c.isDigit() } },
                    label = { Text("Spending Limit ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("$") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(isTemporary, spendingLimit.toDoubleOrNull() ?: 500.0) },
                enabled = !isLoading && spendingLimit.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create")
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

@Composable
private fun VirtualCardDetailsDialog(
    card: VirtualCard,
    onDismiss: () -> Unit
) {
    var showCvv by remember { mutableStateOf(false) }
    var showCardNumber by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card Details") },
        text = {
            Column {
                // Card Visual
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = if (card.isTemporary) "TEMPORARY" else "VIRTUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (showCardNumber) "4532 1234 5678 ${card.cardNumber.takeLast(4)}" else card.cardNumber,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "EXPIRES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = card.expiryDate,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "CVV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = if (showCvv) "123" else "***",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.cardholderName.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { showCardNumber = !showCardNumber }) {
                        Icon(
                            if (showCardNumber) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showCardNumber) "Hide Number" else "Show Number")
                    }
                    TextButton(onClick = { showCvv = !showCvv }) {
                        Icon(
                            if (showCvv) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showCvv) "Hide CVV" else "Show CVV")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
