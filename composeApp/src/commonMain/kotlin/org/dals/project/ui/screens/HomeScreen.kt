package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.utils.SettingsManager
import org.dals.project.utils.CurrencyUtils
import org.dals.project.services.MpesaService
import kotlinx.coroutines.launch
// Removed NetworkConnectivityManager - no longer needed
import androidx.compose.material.icons.filled.Refresh
import org.dals.project.ui.components.TransactionCardSkeleton
import org.dals.project.ui.components.BalanceCardSkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateToTransact: () -> Unit,
    onNavigateToAccountDetails: () -> Unit,
    onNavigateToStatement: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onNavigateToCards: () -> Unit = {},
    onNavigateToBankAccounts: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()

    // Get current currency from settings
    val settingsRepository = SettingsManager.settingsRepository
    val appSettings by settingsRepository.appSettings.collectAsStateWithLifecycle()

    // State for M-Pesa deposit dialog
    var showDepositDialog by remember { mutableStateOf(false) }
    var isProcessingDeposit by remember { mutableStateOf(false) }
    var depositStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val mpesaService = remember { MpesaService() }

    // Network connectivity state
    // Network connectivity check removed - always assume connected
    var isRefreshing by remember { mutableStateOf(false) }

    // Auto-refresh when app starts
    LaunchedEffect(Unit) {
        transactionViewModel.refreshAllData()
    }

    // Manual refresh function
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            transactionViewModel.refreshAllData()
            kotlinx.coroutines.delay(500) // Small delay for UX
            isRefreshing = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Refresh indicator
        if (isRefreshing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refreshing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Welcome Card
        item {
            authUiState.currentUser?.let { user ->
                WelcomeCard(userName = user.fullName.split(" ").first())
            }
        }

        // Main Wallet Card - now positioned after welcome message (COMPACT VERSION)
        item {
            if (transactionUiState.isLoading && transactionUiState.walletBalance == null) {
                BalanceCardSkeleton()
            } else {
                // Use real-time balance from server, no mock fallback
                val displayBalance = transactionUiState.walletBalance ?: WalletBalance(
                    userId = authUiState.currentUser?.id ?: "default",
                    totalBalance = 0.0,
                    availableBalance = 0.0,
                    pendingAmount = 0.0,
                    currency = "USD",
                    lastUpdated = System.currentTimeMillis().toString()
                )

                CompactWalletCard(
                    balance = displayBalance,
                    currentCurrency = appSettings.currency,
                    onAccountDetailsClick = onNavigateToAccountDetails
                )
            }
        }

        // Quick Actions Section (COMPACT VERSION)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            CompactQuickActionsCard(
                onNavigateToTransact = onNavigateToTransact,
                onNavigateToCards = onNavigateToCards,
                onNavigateToBankAccounts = onNavigateToBankAccounts
            )
        }

        // Advertisement Carousel
        item {
            AdvertisementCarousel()
        }

        // Recent Transactions Section
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (transactionUiState.isLoading && transactionUiState.transactions.isEmpty()) {
            // Show skeleton loaders while loading
            items(3) {
                TransactionCardSkeleton()
            }
        } else if (transactionUiState.transactions.isEmpty()) {
            item {
                EmptyTransactionsCard()
            }
        } else {
            items(transactionUiState.transactions.take(5)) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    currentCurrency = appSettings.currency,
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }

        // View All Transactions Button
        if (transactionUiState.transactions.size > 5) {
            item {
                OutlinedButton(
                    onClick = onNavigateToStatement,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("View All Transactions")
                }
            }
        }
    }

    // Show M-Pesa deposit dialog
    if (showDepositDialog) {
        MpesaDepositDialog(
            onDismiss = {
                showDepositDialog = false
                depositStatusMessage = null
            },
            onConfirm = { phoneNumber, amount ->
                isProcessingDeposit = true
                coroutineScope.launch {
                    try {
                        val stkResult = mpesaService.initiateDeposit(
                            phoneNumber = phoneNumber,
                            amount = amount,
                            accountNumber = authUiState.currentUser?.accountNumber ?: "WALLET"
                        )

                        if (stkResult.isSuccess) {
                            val stkResponse = stkResult.getOrNull()
                            val checkoutRequestId = stkResponse?.CheckoutRequestID

                            if (checkoutRequestId != null) {
                                depositStatusMessage = "STK push sent! Check your phone..."

                                val confirmResult = mpesaService.waitForDepositConfirmation(checkoutRequestId)

                                if (confirmResult.isSuccess) {
                                    depositStatusMessage = "Payment confirmed! Recording transaction..."

                                    // Record the deposit on the server
                                    val recordResult = transactionViewModel.recordMpesaDeposit(
                                        amount = amount,
                                        phoneNumber = phoneNumber,
                                        mpesaReference = checkoutRequestId,
                                        accountNumber = authUiState.currentUser?.accountNumber ?: "WALLET"
                                    )

                                    if (recordResult.isSuccess) {
                                        depositStatusMessage = "Deposit successful! Your wallet has been updated."
                                        // Refresh wallet balance to show real-time update
                                        transactionViewModel.refreshData()
                                    } else {
                                        depositStatusMessage = "Warning: Payment confirmed but failed to record: ${recordResult.exceptionOrNull()?.message}"
                                    }

                                    kotlinx.coroutines.delay(2000)
                                    showDepositDialog = false
                                } else {
                                    val errorMsg = confirmResult.exceptionOrNull()?.message ?: "Payment failed"
                                    depositStatusMessage = "Payment status: $errorMsg"
                                }
                            } else {
                                depositStatusMessage = "Failed to initiate STK push"
                            }
                        } else {
                            val errorMsg = stkResult.exceptionOrNull()?.message ?: "Failed to send STK push"
                            depositStatusMessage = "Error: $errorMsg"
                        }
                    } catch (e: Exception) {
                        depositStatusMessage = "Error: ${e.message}"
                    } finally {
                        isProcessingDeposit = false
                    }
                }
            },
            currentUser = authUiState.currentUser
        )
    }

    // Show processing/status dialog
    if (isProcessingDeposit || depositStatusMessage != null) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingDeposit) depositStatusMessage = null },
            title = { Text(if (isProcessingDeposit) "Processing Payment..." else "Payment Status") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (isProcessingDeposit) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    if (depositStatusMessage != null) Text(depositStatusMessage!!)
                }
            },
            confirmButton = {
                if (!isProcessingDeposit) {
                    TextButton(onClick = { depositStatusMessage = null }) {
                        Text("OK")
                    }
                }
            }
        )
    }
}

@Composable
private fun MainWalletCard(
    balance: WalletBalance,
    transactions: List<Transaction>,
    currentCurrency: String,
    currentUser: User?,
    onAccountDetailsClick: () -> Unit,
    onDepositClick: () -> Unit,
    onViewStatementClick: () -> Unit
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var showMoreOptions by remember { mutableStateOf(false) }

    // Calculate current month cashflow for the three dots menu
    val currentMonth = "December 2024"
    val cashFlow = remember(transactions) {
        calculateCashFlowFromTransactions(transactions, currentMonth)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header with three dots menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency Display
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = currentCurrency,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Three dots menu with enhanced options
                Box {
                    IconButton(onClick = { showMoreOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreOptions,
                        onDismissRequest = { showMoreOptions = false }
                    ) {
                        // Account Details with comprehensive info
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Account Details", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showMoreOptions = false
                                onAccountDetailsClick()
                            }
                        )

                        HorizontalDivider()
                        
                        // Show account summary in dropdown
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "Account Balance",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        CurrencyUtils.formatAmount(
                                            CurrencyUtils.convertFromUSD(balance.totalBalance, currentCurrency),
                                            currentCurrency
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = { /* Info only, no action */ },
                            enabled = false
                        )
                        
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        "This Month Cashflow",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "In: ${
                                                CurrencyUtils.formatAmount(
                                                    CurrencyUtils.convertFromUSD(cashFlow.moneyIn, currentCurrency),
                                                    currentCurrency
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            "Out: ${
                                                CurrencyUtils.formatAmount(
                                                    CurrencyUtils.convertFromUSD(cashFlow.moneyOut, currentCurrency),
                                                    currentCurrency
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            onClick = { /* Info only, no action */ },
                            enabled = false
                        )

                        HorizontalDivider()
                        
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Recent Transactions", color = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                showMoreOptions = false
                                onViewStatementClick()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Wallet Title and Balance Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Main Wallet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Show account number if KYC is verified and account exists
                    if (currentUser?.kycStatus == KycStatus.VERIFIED && currentUser.accountNumber != null) {
                        Text(
                            text = "Account: ${currentUser.accountNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Your digital wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Hide/Show balance toggle
                IconButton(onClick = { isBalanceVisible = !isBalanceVisible }) {
                    Icon(
                        if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Display - More prominent
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isBalanceVisible) {
                            CurrencyUtils.formatAmount(
                                CurrencyUtils.convertFromUSD(balance.totalBalance, currentCurrency),
                                currentCurrency
                            )
                        } else {
                            "••••••••"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Available and Pending balances - Enhanced visibility
            if (isBalanceVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceItemCard(
                        label = "Available",
                        amount = balance.availableBalance,
                        currency = currentCurrency,
                        iconVector = Icons.Filled.AttachMoney,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceItemCard(
                        label = "Pending",
                        amount = balance.pendingAmount,
                        currency = currentCurrency,
                        iconVector = Icons.Filled.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons - Enhanced design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Deposit Money - using dark/black primary color
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deposit", style = MaterialTheme.typography.labelLarge)
                }

                // View Statement
                OutlinedButton(
                    onClick = onViewStatementClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Statement", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Welcome back, $userName!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        Icons.Default.WavingHand,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ready to manage your finances today?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simple static time-based greeting cycling through a list
                val greetings = listOf("Good morning", "Good afternoon", "Good evening", "Good night")
                val greetingIndex = remember { (userName.hashCode() and 0xFF) % greetings.size }
                val timeGreeting = greetings[greetingIndex]

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = timeGreeting,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Welcome icon/illustration
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Rocket,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BalanceItemCard(
    label: String,
    amount: Double,
    currency: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyUtils.formatAmount(
                    CurrencyUtils.convertFromUSD(amount, currency),
                    currency
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onNavigateToTransact: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToBankAccounts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = "Send Money",
                    iconVector = Icons.Filled.Send,
                    onClick = onNavigateToTransact,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    text = "Pay Bills",
                    iconVector = Icons.Filled.Payment,
                    onClick = onNavigateToTransact,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    text = "Invest",
                    iconVector = Icons.Filled.TrendingUp,
                    onClick = onNavigateToTransact,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = "My Cards",
                    iconVector = Icons.Filled.CreditCard,
                    onClick = onNavigateToCards,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    text = "Accounts",
                    iconVector = Icons.Filled.AccountBalance,
                    onClick = onNavigateToBankAccounts,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = text,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    currentCurrency: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.recipientName ?: "Internal Transfer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.timestamp.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transaction.reference?.let { ref ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ref,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val convertedAmount = CurrencyUtils.convertFromUSD(transaction.amount, currentCurrency)
                Text(
                    text = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT ->
                            "-${CurrencyUtils.formatAmount(convertedAmount, currentCurrency)}"

                        TransactionType.RECEIVE, TransactionType.DEPOSIT ->
                            "+${CurrencyUtils.formatAmount(convertedAmount, currentCurrency)}"

                        else -> CurrencyUtils.formatAmount(convertedAmount, currentCurrency)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.SEND, TransactionType.BILL_PAYMENT,
                        TransactionType.WITHDRAWAL, TransactionType.INVESTMENT -> MaterialTheme.colorScheme.error

                        TransactionType.RECEIVE, TransactionType.DEPOSIT -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Surface(
                    color = when (transaction.status) {
                        TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                        TransactionStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
                        TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                        TransactionStatus.REVERSED -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (transaction.status) {
                            TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                            TransactionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            TransactionStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                            TransactionStatus.REVERSED -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No Transactions Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
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
        currency = "USD"
    )
}

@Composable
fun MpesaDepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (phoneNumber: String, amount: Double) -> Unit,
    currentUser: User?
) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Deposit via M-Pesa",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show account number if available
                if (currentUser?.accountNumber != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Deposit to Account:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = currentUser.accountNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Phone number input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        errorMessage = null
                    },
                    label = { Text("M-Pesa Phone Number") },
                    placeholder = { Text("0712345678 or 254712345678") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                // Amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        errorMessage = null
                    },
                    label = { Text("Amount (KES)") },
                    placeholder = { Text("Enter amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "ℹ️")
                        Column {
                            Text(
                                text = "You will receive an STK push on your phone. Enter your M-Pesa PIN to complete the deposit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate inputs
                    if (phoneNumber.isBlank()) {
                        errorMessage = "Phone number is required"
                        return@Button
                    }
                    if (amount.isBlank()) {
                        errorMessage = "Amount is required"
                        return@Button
                    }
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || amountValue <= 0) {
                        errorMessage = "Invalid amount"
                        return@Button
                    }
                    if (amountValue < 1) {
                        errorMessage = "Minimum deposit is KES 1"
                        return@Button
                    }

                    onConfirm(phoneNumber, amountValue)
                }
            ) {
                Text("Send STK Push")
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
private fun CompactWalletCard(
    balance: WalletBalance,
    currentCurrency: String,
    onAccountDetailsClick: () -> Unit
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { isBalanceVisible = !isBalanceVisible }) {
                    Icon(
                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isBalanceVisible) "Hide balance" else "Show balance",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = if (isBalanceVisible) {
                    CurrencyUtils.formatAmount(
                        CurrencyUtils.convertFromUSD(balance.totalBalance, currentCurrency),
                        currentCurrency
                    )
                } else "••••••",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAccountDetailsClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text("View Details", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun CompactQuickActionsCard(
    onNavigateToTransact: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToBankAccounts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactQuickActionButton(
                text = "Send",
                iconVector = Icons.Filled.Send,
                onClick = onNavigateToTransact,
                modifier = Modifier.weight(1f)
            )
            CompactQuickActionButton(
                text = "Cards",
                iconVector = Icons.Filled.CreditCard,
                onClick = onNavigateToCards,
                modifier = Modifier.weight(1f)
            )
            CompactQuickActionButton(
                text = "Accounts",
                iconVector = Icons.Filled.AccountBalance,
                onClick = onNavigateToBankAccounts,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactQuickActionButton(
    text: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .padding(2.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdvertisementCarousel() {
    // TODO: Fetch ads from server
    var currentAdIndex by remember { mutableStateOf(0) }
    val sampleAds = remember {
        listOf(
            "Sample Ad 1",
            "Sample Ad 2",
            "Sample Ad 3"
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Auto-scroll every 5 seconds
            currentAdIndex = (currentAdIndex + 1) % sampleAds.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sampleAds[currentAdIndex],
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                sampleAds.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentAdIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}
