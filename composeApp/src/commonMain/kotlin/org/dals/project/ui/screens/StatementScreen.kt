package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.dals.project.model.*
import org.dals.project.utils.DateRangePeriod
import org.dals.project.utils.DateTimeUtils
import org.dals.project.utils.StatementFormat
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("Current Month") }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showDownloadSuccess by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }
    var downloadStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val periods = listOf("Current Month", "Last 3 Months", "Last 6 Months", "Last Year", "All Time")

    // Calculate statement summary
    val statementSummary = remember(transactionUiState.transactions) {
        calculateStatementSummary(transactionUiState.transactions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Statement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showEmailDialog = true
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download & Email Statement")
                    }
                }
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
            // Statement Header
            item {
                StatementHeaderCard(
                    user = authUiState.currentUser,
                    balance = transactionUiState.walletBalance
                )
            }

            // Period Selection
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Statement Period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = showPeriodDropdown,
                            onExpandedChange = { showPeriodDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedPeriod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Period") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPeriodDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = showPeriodDropdown,
                                onDismissRequest = { showPeriodDropdown = false }
                            ) {
                                periods.forEach { period ->
                                    DropdownMenuItem(
                                        text = { Text(period) },
                                        onClick = {
                                            selectedPeriod = period
                                            showPeriodDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Statement Summary
            item {
                StatementSummaryCard(
                    summary = statementSummary,
                    period = selectedPeriod
                )
            }

            // Transaction Details Header
            item {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Transactions Table Header
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Transaction List
            if (transactionUiState.transactions.isEmpty()) {
                item {
                    EmptyStatementCard()
                }
            } else {
                items(transactionUiState.transactions) { transaction ->
                    StatementTransactionRow(transaction = transaction)
                }
            }

            // Statement Footer
            item {
                StatementFooterCard()
            }
        }

        // Show download status message
        if (downloadStatusMessage != null) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(5000)
                downloadStatusMessage = null
            }
        }
    }

    // Snackbar for download status
    if (downloadStatusMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { downloadStatusMessage = null }) {
                    Text("OK")
                }
            }
        ) {
            Text(downloadStatusMessage!!)
        }
    }

    // Email Dialog
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Send Statement to Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your account statement will be sent as a password-protected PDF attachment. The password is the last 4 digits of your account number.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("Email Address (optional)") },
                        placeholder = { Text("Leave blank to use account email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val user = authUiState.currentUser
                                if (user == null) {
                                    downloadStatusMessage = "Error: User not logged in"
                                    showEmailDialog = false
                                    return@launch
                                }

                                // Calculate date range based on selected period
                                val currentTimestamp = DateTimeUtils.getCurrentTimestamp()
                                val currentDate = DateTimeUtils.parseDateTime(currentTimestamp)

                                if (currentDate == null) {
                                    downloadStatusMessage = "Error: Could not determine current date"
                                    showEmailDialog = false
                                    return@launch
                                }

                                val periodStart = when (selectedPeriod) {
                                    "Last 3 Months" -> currentDate.date.minusMonths(3)
                                    "Last 6 Months" -> currentDate.date.minusMonths(6)
                                    "Last Year" -> currentDate.date.minusYears(1)
                                    "All Time" -> LocalDate(2020, 1, 1)
                                    else -> currentDate.date.minusMonths(1) // Current Month
                                }

                                val periodStartDateTime = LocalDateTime(
                                    periodStart.year, periodStart.monthNumber, periodStart.dayOfMonth, 0, 0
                                )
                                val periodEndDateTime = currentDate

                                // Call the API to send statement via email using viewModelScope to avoid cancellation on disposal
                                transactionViewModel.downloadAndEmailStatement(
                                    period = when (selectedPeriod) {
                                        "Last 3 Months" -> DateRangePeriod.LAST_30_DAYS
                                        "Last 6 Months" -> DateRangePeriod.LAST_30_DAYS
                                        "Last Year" -> DateRangePeriod.THIS_YEAR
                                        "All Time" -> DateRangePeriod.THIS_YEAR
                                        else -> DateRangePeriod.THIS_MONTH
                                    },
                                    email = if (customEmail.isBlank()) null else customEmail
                                )

                                downloadStatusMessage = "Processing your request..."
                                customEmail = ""
                                showEmailDialog = false
                            } catch (e: Exception) {
                                downloadStatusMessage = "Error: ${e.message}"
                                showEmailDialog = false
                            }
                        }
                    }
                ) {
                    Text("Send to Email")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    customEmail = ""
                    showEmailDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatementHeaderCard(
    user: User?,
    balance: WalletBalance?
) {
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
            Text(
                text = "ACCOUNT STATEMENT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            user?.let {
                Text(
                    text = "Account Holder: ${it.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Username: @${it.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            balance?.let {
                Text(
                    text = "Current Balance: $${formatAmount(it.totalBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = "Statement Date: ${getCurrentDateFormatted()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatementSummaryCard(
    summary: StatementSummary,
    period: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Summary for $period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Inflow
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Inflow",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$${formatAmount(summary.totalInflow)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total Inflow",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Total Outflow
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Outflow",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "$${formatAmount(summary.totalOutflow)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Total Outflow",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Net Change
            val netChange = summary.totalInflow - summary.totalOutflow
            val isPositive = netChange >= 0

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPositive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Change",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}$${formatAmount(netChange)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total Transactions: ${summary.totalTransactions}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatementTransactionRow(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTransactionDate(transaction.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTransactionTime(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Description
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                transaction.recipientName?.let { recipient ->
                    Text(
                        text = "To/From: $recipient",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                Surface(
                    color = when (transaction.status) {
                        TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        TransactionStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                        TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                        TransactionStatus.REVERSED -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = transaction.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Amount
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> "-$${formatAmount(transaction.amount)}"

                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> "+$${formatAmount(transaction.amount)}"

                        else -> "$${formatAmount(transaction.amount)}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> MaterialTheme.colorScheme.error

                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.End
                )

                if (transaction.fee > 0) {
                    Text(
                        text = "Fee: $${formatAmount(transaction.fee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatementCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = "No transactions",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Transactions Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No transactions available for the selected period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatementFooterCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "--- End of Statement ---",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Generated on ${getCurrentDateFormatted()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Axio Bank - Digital Financial Services",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Data class for statement summary
private data class StatementSummary(
    val totalInflow: Double,
    val totalOutflow: Double,
    val totalTransactions: Int
)

// Helper functions
private fun calculateStatementSummary(transactions: List<Transaction>): StatementSummary {
    var totalInflow = 0.0
    var totalOutflow = 0.0

    transactions.forEach { transaction ->
        when (transaction.type) {
            TransactionType.RECEIVE, TransactionType.DEPOSIT -> totalInflow += transaction.amount
            TransactionType.SEND, TransactionType.BILL_PAYMENT,
            TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> totalOutflow += transaction.amount

            else -> {}
        }
    }

    return StatementSummary(
        totalInflow = totalInflow,
        totalOutflow = totalOutflow,
        totalTransactions = transactions.size
    )
}

private fun getCurrentDateFormatted(): String {
    // Use DateTimeUtils to get current timestamp
    val currentTimestamp = DateTimeUtils.getCurrentTimestamp()
    val dateTime = DateTimeUtils.parseDateTime(currentTimestamp)

    if (dateTime != null) {
        val months = listOf("January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December")
        val monthName = months[dateTime.monthNumber - 1]
        return "$monthName ${dateTime.dayOfMonth}, ${dateTime.year}"
    }

    // Fallback to simple formatting
    return currentTimestamp.take(10)
}

private fun formatTransactionDate(timestamp: String): String {
    return timestamp.take(10).replace("-", "/") // Simple date formatting
}

private fun formatTransactionTime(timestamp: String): String {
    return if (timestamp.length > 11) {
        timestamp.substring(11, 16) // Extract time portion
    } else {
        "00:00"
    }
}

private fun formatAmount(amount: Double): String {
    return (kotlin.math.round(amount * 100) / 100).toString()
}

/**
 * Download statement functionality - sends via email using server API
 */
private suspend fun downloadStatement(
    user: User?,
    transactions: List<Transaction>,
    balance: WalletBalance?,
    selectedPeriod: String
) {
    // This function is deprecated and should not be called
    // The email functionality should be implemented through the server API
    println("⚠️ Warning: downloadStatement is deprecated. Use server API to send statements via email.")
}

// Extension function to subtract months from LocalDate
private fun LocalDate.minusMonths(months: Int): LocalDate {
    var year = this.year
    var month = this.monthNumber - months

    while (month <= 0) {
        month += 12
        year -= 1
    }

    val daysInMonth = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    val day = minOf(this.dayOfMonth, daysInMonth)
    return LocalDate(year, month, day)
}

// Extension function to subtract years from LocalDate
private fun LocalDate.minusYears(years: Int): LocalDate {
    return LocalDate(this.year - years, this.monthNumber, this.dayOfMonth)
}