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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dals.project.model.Card
import org.dals.project.model.CardType
import org.dals.project.viewmodel.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualCardsScreen(
    cardViewModel: CardViewModel,
    onNavigateBack: () -> Unit
) {
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var showCardDetails by remember { mutableStateOf(false) }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var cardToFreeze by remember { mutableStateOf<Card?>(null) }
    
    val scope = rememberCoroutineScope()

    // Load cards on screen load
    LaunchedEffect(Unit) {
        cardViewModel.loadCards()
    }

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
                    IconButton(onClick = { cardViewModel.loadCards() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
        if (cardUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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

                // Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Cards",
                            value = cardUiState.cards.size.toString(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Active",
                            value = cardUiState.cards.count { it.isActive }.toString(),
                            color = Color(0xFF4CAF50)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Frozen",
                            value = cardUiState.cards.count { !it.isActive }.toString(),
                            color = Color(0xFFFFA000)
                        )
                    }
                }

                // Cards Section
                item {
                    Text(
                        text = "Your Cards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (cardUiState.cards.isEmpty()) {
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
                                    imageVector = Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No cards yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Create a virtual card for secure online payments",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showCreateDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Card")
                                }
                            }
                        }
                    }
                } else {
                    items(cardUiState.cards) { card ->
                        VirtualCardItem(
                            card = card,
                            onViewDetails = {
                                selectedCard = card
                                showCardDetails = true
                            },
                            onFreeze = {
                                cardToFreeze = card
                                showFreezeDialog = true
                            },
                            onDelete = {
                                cardViewModel.removeCard(card.id)
                            }
                        )
                    }
                }

                // Error message
                cardUiState.errorMessage?.let { error ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Card Details Dialog
    if (showCardDetails && selectedCard != null) {
        CardDetailsDialog(
            card = selectedCard!!,
            onDismiss = { 
                showCardDetails = false
                selectedCard = null
            }
        )
    }

    // Freeze Confirmation Dialog
    if (showFreezeDialog && cardToFreeze != null) {
        AlertDialog(
            onDismissRequest = { 
                showFreezeDialog = false
                cardToFreeze = null
            },
            title = { Text(if (cardToFreeze!!.isActive) "Freeze Card?" else "Unfreeze Card?") },
            text = { 
                Text(
                    if (cardToFreeze!!.isActive) 
                        "This will temporarily disable the card. You can unfreeze it anytime."
                    else 
                        "This will reactivate the card for transactions."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // In a real app, this would call an API to freeze/unfreeze
                        showFreezeDialog = false
                        cardToFreeze = null
                    }
                ) {
                    Text(if (cardToFreeze!!.isActive) "Freeze" else "Unfreeze")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showFreezeDialog = false
                    cardToFreeze = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Card Dialog
    if (showCreateDialog) {
        CreateVirtualCardDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { cardType, nickname ->
                // In a real app, this would call an API to create a virtual card
                showCreateDialog = false
            }
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
private fun VirtualCardItem(
    card: Card,
    onViewDetails: () -> Unit,
    onFreeze: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = if (card.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = card.nickname ?: "${card.cardBrand.name} Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "**** **** **** ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Surface(
                    color = if (card.isActive) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (card.isActive) "Active" else "Frozen",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (card.isActive) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
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
                        text = "${card.expiryMonth.toString().padStart(2, '0')}/${card.expiryYear.toString().takeLast(2)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = card.cardType.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", card.linkedAccountBalance ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                OutlinedButton(
                    onClick = onFreeze,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (card.isActive) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (card.isActive) "Freeze" else "Unfreeze")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CardDetailsDialog(
    card: Card,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Card Holder", card.cardHolderName)
                DetailRow("Card Number", "**** **** **** ${card.lastFourDigits}")
                DetailRow("Expiry", "${card.expiryMonth.toString().padStart(2, '0')}/${card.expiryYear}")
                DetailRow("Type", card.cardType.name)
                DetailRow("Brand", card.cardBrand.name)
                DetailRow("Status", if (card.isActive) "Active" else "Frozen")
                card.linkedAccountBalance?.let {
                    DetailRow("Balance", "$${String.format("%.2f", it)}")
                }
                DetailRow("Added", card.addedDate)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVirtualCardDialog(
    onDismiss: () -> Unit,
    onCreate: (cardType: CardType, nickname: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(CardType.DEBIT) }
    var nickname by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Virtual Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Create a new virtual card for secure online transactions.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Card Type Selection
                Text(
                    text = "Card Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedType == CardType.DEBIT,
                        onClick = { selectedType = CardType.DEBIT },
                        label = { Text("Debit") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == CardType.CREDIT,
                        onClick = { selectedType = CardType.CREDIT },
                        label = { Text("Credit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Card Nickname (Optional)") },
                    placeholder = { Text("e.g., Shopping Card") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    onCreate(selectedType, nickname)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Card")
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
