package org.dals.project.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.repository.*
import org.dals.project.viewmodel.SubAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubAccountsScreen(
    customerId: String,
    parentAccountId: String,
    onNavigateBack: () -> Unit,
    viewModel: SubAccountViewModel = viewModel { SubAccountViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.loadSubAccounts(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sub-Accounts & Goals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "Add Sub-Account")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Sub-Account")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Overall Summary Card
            if (uiState.activeSubAccounts.isNotEmpty()) {
                OverallSummaryCard(
                    totalSaved = uiState.totalSaved,
                    totalTarget = uiState.totalTarget,
                    overallProgress = uiState.overallProgress
                )
            }

            // Sub-Accounts List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error!!,
                            onRetry = { viewModel.loadSubAccounts(customerId) },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                    uiState.activeSubAccounts.isEmpty() -> {
                        EmptyState(onAddClick = { viewModel.showAddDialog() })
                    }
                    else -> {
                        SubAccountsList(
                            subAccounts = uiState.activeSubAccounts,
                            onSubAccountClick = { viewModel.selectSubAccount(it) },
                            onTransfer = { viewModel.showTransferDialog(it) },
                            onWithdraw = { viewModel.showWithdrawDialog(it) },
                            onEdit = { viewModel.startEdit(it) },
                            onDelete = { viewModel.deleteSubAccount(customerId, it.id) },
                            onToggleLock = { viewModel.toggleLock(customerId, it.id) }
                        )
                    }
                }
            }
        }

        // Add Dialog
        if (uiState.showAddDialog) {
            AddSubAccountDialog(
                customerId = customerId,
                parentAccountId = parentAccountId,
                viewModel = viewModel,
                onDismiss = { viewModel.hideAddDialog() }
            )
        }

        // Transfer Dialog
        if (uiState.showTransferDialog && uiState.selectedSubAccount != null) {
            TransferDialog(
                customerId = customerId,
                subAccount = uiState.selectedSubAccount!!,
                viewModel = viewModel,
                onDismiss = { viewModel.hideTransferDialog() }
            )
        }

        // Withdraw Dialog
        if (uiState.showWithdrawDialog && uiState.selectedSubAccount != null) {
            WithdrawDialog(
                customerId = customerId,
                subAccount = uiState.selectedSubAccount!!,
                viewModel = viewModel,
                onDismiss = { viewModel.hideWithdrawDialog() }
            )
        }

        // Edit Dialog
        if (uiState.editingSubAccount != null) {
            EditSubAccountDialog(
                customerId = customerId,
                subAccount = uiState.editingSubAccount!!,
                viewModel = viewModel,
                onDismiss = { viewModel.cancelEdit() }
            )
        }

        // Success Snackbar
        if (uiState.successMessage != null) {
            LaunchedEffect(uiState.successMessage) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccess()
            }
        }
    }
}

@Composable
private fun OverallSummaryCard(
    totalSaved: Double,
    totalTarget: Double,
    overallProgress: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$${String.format("%.2f", totalSaved)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$${String.format("%.2f", totalTarget)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Overall Progress",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${String.format("%.1f", overallProgress)}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (overallProgress / 100f).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun SubAccountsList(
    subAccounts: List<SubAccountRecord>,
    onSubAccountClick: (SubAccountRecord) -> Unit,
    onTransfer: (SubAccountRecord) -> Unit,
    onWithdraw: (SubAccountRecord) -> Unit,
    onEdit: (SubAccountRecord) -> Unit,
    onDelete: (SubAccountRecord) -> Unit,
    onToggleLock: (SubAccountRecord) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(subAccounts) { subAccount ->
            SubAccountCard(
                subAccount = subAccount,
                onClick = { onSubAccountClick(subAccount) },
                onTransfer = { onTransfer(subAccount) },
                onWithdraw = { onWithdraw(subAccount) },
                onEdit = { onEdit(subAccount) },
                onDelete = { onDelete(subAccount) },
                onToggleLock = { onToggleLock(subAccount) }
            )
        }
    }
}

@Composable
private fun SubAccountCard(
    subAccount: SubAccountRecord,
    onClick: () -> Unit,
    onTransfer: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleLock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardColor = try {
        val colorLong = subAccount.colorHex.removePrefix("0x").toLong(16)
        Color(colorLong)
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(cardColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(subAccount.iconName),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Name and Description
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = subAccount.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (subAccount.isLocked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (subAccount.description != null) {
                            Text(
                                text = subAccount.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (subAccount.isLocked) "Unlock" else "Lock") },
                            onClick = {
                                showMenu = false
                                onToggleLock()
                            },
                            leadingIcon = {
                                Icon(
                                    if (subAccount.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance and Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", subAccount.currentBalance.toDoubleOrNull() ?: 0.0)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                }
                if (subAccount.targetAmount != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", subAccount.targetAmount.toDoubleOrNull() ?: 0.0)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Bar (if target exists)
            if (subAccount.targetAmount != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.1f", subAccount.progressPercentage)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (subAccount.progressPercentage / 100f).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = cardColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Target Date (if exists)
            if (subAccount.targetDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: ${subAccount.targetDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTransfer,
                    modifier = Modifier.weight(1f),
                    enabled = !subAccount.isLocked
                ) {
                    Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Funds")
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    enabled = !subAccount.isLocked && (subAccount.currentBalance.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw")
                }
            }
        }
    }
}

@Composable
private fun getIconForName(iconName: String) = when (iconName.lowercase()) {
    "savings" -> Icons.Default.Savings
    "home" -> Icons.Default.Home
    "car" -> Icons.Default.DirectionsCar
    "vacation" -> Icons.Default.BeachAccess
    "education" -> Icons.Default.School
    "emergency" -> Icons.Default.LocalHospital
    "shopping" -> Icons.Default.ShoppingCart
    "investment" -> Icons.Default.TrendingUp
    else -> Icons.Default.AccountBalance
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Sub-Accounts Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create sub-accounts to save for specific goals like vacation, emergency fund, or a new car",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Sub-Account")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AddSubAccountDialog(
    customerId: String,
    parentAccountId: String,
    viewModel: SubAccountViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Savings") }
    var selectedColor by remember { mutableStateOf("0xFF2196F3") }

    val icons = listOf(
        "Savings" to Icons.Default.Savings,
        "Home" to Icons.Default.Home,
        "Car" to Icons.Default.DirectionsCar,
        "Vacation" to Icons.Default.BeachAccess,
        "Education" to Icons.Default.School,
        "Emergency" to Icons.Default.LocalHospital,
        "Shopping" to Icons.Default.ShoppingCart,
        "Investment" to Icons.Default.TrendingUp
    )

    val colors = listOf(
        "Blue" to "0xFF2196F3",
        "Green" to "0xFF4CAF50",
        "Orange" to "0xFFFF9800",
        "Purple" to "0xFF9C27B0",
        "Red" to "0xFFF44336",
        "Teal" to "0xFF009688",
        "Pink" to "0xFFE91E63",
        "Indigo" to "0xFF3F51B5"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Sub-Account") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name *") },
                        placeholder = { Text("e.g., Vacation Fund") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("Target Amount (Optional)") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Target Date (Optional)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Choose Icon", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons.forEach { (iconName, icon) ->
                            FilterChip(
                                selected = selectedIcon == iconName,
                                onClick = { selectedIcon = iconName },
                                label = { Icon(icon, null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
                item {
                    Text("Choose Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { (colorName, colorHex) ->
                            val color = try {
                                val colorLong = colorHex.removePrefix("0x").toLong(16)
                                Color(colorLong)
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            FilterChip(
                                selected = selectedColor == colorHex,
                                onClick = { selectedColor = colorHex },
                                label = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createSubAccount(
                        customerId,
                        CreateSubAccountRequest(
                            customerId = customerId,
                            parentAccountId = parentAccountId,
                            name = name,
                            description = description.ifBlank { null },
                            targetAmount = targetAmount.ifBlank { null },
                            iconName = selectedIcon,
                            colorHex = selectedColor,
                            targetDate = targetDate.ifBlank { null }
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TransferDialog(
    customerId: String,
    subAccount: SubAccountRecord,
    viewModel: SubAccountViewModel,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to ${subAccount.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current Balance: $${String.format("%.2f", subAccount.currentBalance.toDoubleOrNull() ?: 0.0)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.transferToSubAccount(
                        customerId,
                        TransferToSubAccountRequest(
                            subAccountId = subAccount.id,
                            amount = amount,
                            description = description.ifBlank { null },
                            isDirectDeposit = true // Direct deposit (e.g., M-Pesa)
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null && amount.toDouble() > 0
            ) {
                Text("Add Funds")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun WithdrawDialog(
    customerId: String,
    subAccount: SubAccountRecord,
    viewModel: SubAccountViewModel,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val currentBalance = subAccount.currentBalance.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw from ${subAccount.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Available Balance: $${String.format("%.2f", currentBalance)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.withdrawFromSubAccount(
                        customerId,
                        TransferToSubAccountRequest(
                            subAccountId = subAccount.id,
                            amount = amount,
                            description = description.ifBlank { null }
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = amount.isNotBlank() &&
                         amount.toDoubleOrNull() != null &&
                         amount.toDouble() > 0 &&
                         amount.toDouble() <= currentBalance
            ) {
                Text("Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditSubAccountDialog(
    customerId: String,
    subAccount: SubAccountRecord,
    viewModel: SubAccountViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(subAccount.name) }
    var description by remember { mutableStateOf(subAccount.description ?: "") }
    var targetAmount by remember { mutableStateOf(subAccount.targetAmount ?: "") }
    var targetDate by remember { mutableStateOf(subAccount.targetDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sub-Account") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("Target Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = { targetDate = it },
                        label = { Text("Target Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSubAccount(
                        customerId,
                        subAccount.id,
                        UpdateSubAccountRequest(
                            name = if (name != subAccount.name) name else null,
                            description = if (description != subAccount.description) description else null,
                            targetAmount = if (targetAmount != subAccount.targetAmount) targetAmount else null,
                            targetDate = if (targetDate != subAccount.targetDate) targetDate else null
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
