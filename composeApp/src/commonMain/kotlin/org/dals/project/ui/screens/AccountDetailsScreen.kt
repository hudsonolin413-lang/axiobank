package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    var selectedAccount by remember { mutableStateOf<Account?>(null) }

    // Get real accounts from ViewModel
    val accounts = transactionUiState.accounts

    // Initialize with first account if none selected
    LaunchedEffect(accounts) {
        if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.firstOrNull()
        }
    }

    // Calculate current month cashflow
    val currentMonth = remember {
        getCurrentMonthYear()
    }

    val cashFlow = remember(transactionUiState.transactions) {
        calculateCashFlowFromTransactions(transactionUiState.transactions, currentMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Show loading state
            if (transactionUiState.isLoading && accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (accounts.isEmpty()) {
                // No accounts available
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Accounts Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Please complete KYC verification to open an account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // Account Selection
                item {
                    AccountSelectionCard(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        onAccountSelected = { selectedAccount = it }
                    )
                }

                // Account Balance
                selectedAccount?.let { account ->
                    item {
                        AccountBalanceCard(account = account)
                    }
                }
            }

            // Cash Flow
            item {
                CashFlowCard(cashFlow = cashFlow)
            }

            // Recent Transactions
            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (transactionUiState.transactions.isEmpty()) {
                item {
                    EmptyTransactionsCard()
                }
            } else {
                items(transactionUiState.transactions.take(10)) { transaction ->
                    TransactionCard(transaction = transaction)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectionCard(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelected: (Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${account.type} • ${account.accountNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onAccountSelected(account)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceCard(account: Account) {
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
                text = account.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = account.accountNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = formatBalanceWithCurrency(account.balance, account.currency),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Current Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CashFlowCard(cashFlow: CashFlow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cash Flow - ${cashFlow.monthYear}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Money In
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
                            Icons.Default.ArrowDownward,
                            contentDescription = "Money In",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatBalanceWithCurrency(cashFlow.moneyIn, cashFlow.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Money In",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Money Out
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
                            Icons.Default.ArrowUpward,
                            contentDescription = "Money Out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatBalanceWithCurrency(cashFlow.moneyOut, cashFlow.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Money Out",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Net Flow
            val netFlow = cashFlow.moneyIn - cashFlow.moneyOut
            val isPositive = netFlow >= 0

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
                        text = "Net Flow",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${formatBalanceWithCurrency(netFlow, cashFlow.currency)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.recipientName ?: "Internal Transfer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = transaction.timestamp.take(19).replace("T", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> "-${
                            formatBalanceWithCurrency(
                                transaction.amount,
                                transaction.currency
                            )
                        }"

                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> "+${
                            formatBalanceWithCurrency(
                                transaction.amount,
                                transaction.currency
                            )
                        }"

                        else -> formatBalanceWithCurrency(transaction.amount, transaction.currency)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> MaterialTheme.colorScheme.error

                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Surface(
                    color = when (transaction.status) {
                        TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        TransactionStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                        TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                        TransactionStatus.REVERSED -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Transactions Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start by sending money or paying bills",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatBalanceWithCurrency(amount: Double, currency: String): String {
    val formattedAmount = (kotlin.math.round(amount * 100) / 100).toString()
    return when (currency) {
        "KSH" -> "KSh $formattedAmount"
        "USD" -> "$formattedAmount"
        else -> "$currency $formattedAmount"
    }
}

private fun getCurrentMonthYear(): String {
    // Use simple date calculation without kotlinx.datetime
    val currentTimeMillis = System.currentTimeMillis()
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = currentTimeMillis
    
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH)
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    return "${monthNames[month]} $year"
}

private fun calculateCashFlowFromTransactions(transactions: List<Transaction>, monthYear: String): CashFlow {
    var moneyIn = 0.0
    var moneyOut = 0.0

    transactions.forEach { transaction ->
        when (transaction.type) {
            TransactionType.RECEIVE, TransactionType.DEPOSIT -> moneyIn += transaction.amount
            TransactionType.SEND, TransactionType.BILL_PAYMENT,
            TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> moneyOut += transaction.amount

            else -> {}
        }
    }

    return CashFlow(
        monthYear = monthYear,
        moneyIn = moneyIn,
        moneyOut = moneyOut,
        currency = "USD" // Default currency
    )
}