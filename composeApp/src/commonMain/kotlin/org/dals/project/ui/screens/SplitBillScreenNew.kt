package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.model.*
import org.dals.project.viewmodel.SplitBillViewModel
import org.dals.project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreenNew(
    authViewModel: AuthViewModel,
    viewModel: SplitBillViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = authUiState.currentUser

    // Load data on start
    LaunchedEffect(currentUser) {
        currentUser?.let {
            viewModel.loadSplitBills(it.id)
        }
    }

    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Bills") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { currentUser?.let { viewModel.loadSplitBills(it.id) } }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (currentUser != null) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Create Split")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Please log in to view split bills")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            if (uiState.summary != null) {
                item {
                    SplitBillSummaryCard(uiState.summary!!)
                }
            }

            // Filter Tabs
            item {
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("All", "Pending", "Partial", "Completed")

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // Split Bills List
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.splitBills.isEmpty()) {
                item {
                    EmptySplitBillsState()
                }
            } else {
                items(uiState.splitBills) { splitBill ->
                    SplitBillCard(
                        splitBill = splitBill,
                        currentUserId = currentUser.id,
                        onCardClick = { viewModel.loadSplitBillDetails(splitBill.id) },
                        onPayClick = { participant ->
                            viewModel.showPayDialog(participant)
                        },
                        onRemindClick = { participant ->
                            viewModel.sendReminder(participant.id)
                        },
                        onCancelClick = {
                            viewModel.cancelSplitBill(splitBill.id, currentUser.id)
                        }
                    )
                }
            }
        }

        // Create Dialog
        if (uiState.showCreateDialog) {
            CreateSplitBillDialog(
                currentUser = currentUser,
                onDismiss = { viewModel.hideCreateDialog() },
                onCreate = { request ->
                    viewModel.createSplitBill(request)
                }
            )
        }

        // Pay Dialog
        if (uiState.showPayDialog && uiState.selectedParticipant != null) {
            PaySplitBillDialog(
                participant = uiState.selectedParticipant!!,
                currentUser = currentUser,
                onDismiss = { viewModel.hidePayDialog() },
                onPay = { request ->
                    viewModel.paySplitBill(request, currentUser.id)
                }
            )
        }
    }
}

@Composable
private fun SplitBillSummaryCard(summary: SplitBillSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total Splits",
                    value = "${summary.totalSplits}",
                    icon = Icons.Default.CallSplit,
                    color = Color(0xFF2196F3)
                )
                SummaryItem(
                    label = "Active",
                    value = "${summary.activeSplits}",
                    icon = Icons.Default.PendingActions,
                    color = Color(0xFFFF9800)
                )
                SummaryItem(
                    label = "Completed",
                    value = "${summary.completedSplits}",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You Owe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "$${String.format("%.2f", summary.totalOwing)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You're Owed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "$${String.format("%.2f", summary.totalOwed)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Net Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        "$${String.format("%.2f", summary.netBalance)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SplitBillCard(
    splitBill: SplitBill,
    currentUserId: String,
    onCardClick: () -> Unit,
    onPayClick: (SplitBillParticipant) -> Unit,
    onRemindClick: (SplitBillParticipant) -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        splitBill.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Created by ${splitBill.creatorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                StatusBadge(splitBill.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "$${String.format("%.2f", splitBill.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "$${String.format("%.2f", splitBill.remainingAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (splitBill.remainingAmount > 0) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { if (splitBill.totalAmount > 0) (splitBill.paidAmount / splitBill.totalAmount).toFloat() else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Participants Summary
            Text(
                "${splitBill.participants.size} participants • ${splitBill.participants.count { it.isPaid }} paid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Current User's Participation
            val userParticipant = splitBill.participants.find { it.customerId == currentUserId }
            if (userParticipant != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (userParticipant.isPaid) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (userParticipant.isPaid) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Your share: $${String.format("%.2f", userParticipant.amount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!userParticipant.isPaid && splitBill.status != SplitBillStatus.CANCELLED) {
                        Button(
                            onClick = { onPayClick(userParticipant) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Pay Now")
                        }
                    }
                }
            }

            // Creator Actions
            if (splitBill.creatorId == currentUserId && splitBill.status == SplitBillStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelClick) {
                        Text("Cancel Split", color = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SplitBillStatus) {
    val (color, text) = when (status) {
        SplitBillStatus.PENDING -> Color(0xFFFF9800) to "Pending"
        SplitBillStatus.PARTIAL -> Color(0xFF2196F3) to "Partial"
        SplitBillStatus.COMPLETED -> Color(0xFF4CAF50) to "Completed"
        SplitBillStatus.CANCELLED -> Color.Gray to "Cancelled"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptySplitBillsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CallSplit,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Split Bills Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create a split bill to share expenses with friends",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CreateSplitBillDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onCreate: (CreateSplitBillRequest) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var splitEqually by remember { mutableStateOf(true) }
    var participants by remember { mutableStateOf(listOf<CreateParticipantRequest>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Split Bill") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Dinner at restaurant") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = { Text("Total Amount") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = splitEqually,
                        onCheckedChange = { splitEqually = it }
                    )
                    Text("Split equally among all participants")
                }

                Text(
                    "Add participants in the next step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = totalAmount.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amount > 0) {
                        onCreate(
                            CreateSplitBillRequest(
                                creatorId = currentUser.id,
                                description = description,
                                totalAmount = amount,
                                splitEqually = splitEqually,
                                participants = emptyList() // TODO: Add participant management
                            )
                        )
                    }
                },
                enabled = description.isNotBlank() && (totalAmount.toDoubleOrNull() ?: 0.0) > 0
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
private fun PaySplitBillDialog(
    participant: SplitBillParticipant,
    currentUser: User,
    onDismiss: () -> Unit,
    onPay: (PaySplitBillRequest) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Split Bill") },
        text = {
            Column {
                Text("Amount to pay: $${String.format("%.2f", participant.amount)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Payment will be processed from your account",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPay(
                        PaySplitBillRequest(
                            participantId = participant.id,
                            splitBillId = participant.splitBillId,
                            fromAccountId = currentUser.id, // Should be actual account ID
                            amount = participant.amount
                        )
                    )
                }
            ) {
                Text("Pay Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
