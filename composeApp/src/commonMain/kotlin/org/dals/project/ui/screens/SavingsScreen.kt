package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dals.project.model.*
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.TransactionViewModel

enum class SavingsScreenTab {
    MY_SAVINGS, PLANS, HISTORY
}

data class SavingsPlan(
    val lockPeriod: LockPeriod,
    val interestRate: Double,
    val minDeposit: Double,
    val description: String,
    val features: List<String>
)

// Available savings plans
private val savingsPlans = listOf(
    SavingsPlan(
        lockPeriod = LockPeriod.ONE_MONTH,
        interestRate = 2.5,
        minDeposit = 50.0,
        description = "Short-term savings with quick access",
        features = listOf("Low minimum deposit", "Monthly returns", "Flexible terms")
    ),
    SavingsPlan(
        lockPeriod = LockPeriod.THREE_MONTHS,
        interestRate = 4.0,
        minDeposit = 100.0,
        description = "Build your savings with better rates",
        features = listOf("Better interest rate", "Quarterly returns", "Good liquidity")
    ),
    SavingsPlan(
        lockPeriod = LockPeriod.SIX_MONTHS,
        interestRate = 6.5,
        minDeposit = 200.0,
        description = "Mid-term savings for your goals",
        features = listOf("Competitive rates", "Bi-annual returns", "Goal-oriented")
    ),
    SavingsPlan(
        lockPeriod = LockPeriod.ONE_YEAR,
        interestRate = 9.0,
        minDeposit = 500.0,
        description = "One year to grow your money",
        features = listOf("High interest rate", "Annual maturity", "Guaranteed returns")
    ),
    SavingsPlan(
        lockPeriod = LockPeriod.TWO_YEARS,
        interestRate = 11.5,
        minDeposit = 1000.0,
        description = "Long-term savings with excellent returns",
        features = listOf("Premium rates", "Long-term growth", "Wealth building")
    ),
    SavingsPlan(
        lockPeriod = LockPeriod.FIVE_YEARS,
        interestRate = 15.0,
        minDeposit = 2000.0,
        description = "Maximum returns for patient savers",
        features = listOf("Highest rates", "Maximum returns", "Future security")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(SavingsScreenTab.MY_SAVINGS) }
    var showNewSavingsDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf<SavingsAccount?>(null) }
    var showSavingsDetailsDialog by remember { mutableStateOf<SavingsAccount?>(null) }

    // Load savings accounts on start
    LaunchedEffect(Unit) {
        transactionViewModel.loadSavingsAccounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locked Savings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewSavingsDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Savings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                SavingsScreenTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    SavingsScreenTab.MY_SAVINGS -> "My Savings"
                                    SavingsScreenTab.PLANS -> "Plans"
                                    SavingsScreenTab.HISTORY -> "History"
                                }
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                SavingsScreenTab.MY_SAVINGS -> {
                    MySavingsTab(
                        savingsAccounts = transactionUiState.savingsAccounts,
                        isLoading = transactionUiState.isLoading,
                        onWithdraw = { showWithdrawDialog = it },
                        onViewDetails = { showSavingsDetailsDialog = it }
                    )
                }
                SavingsScreenTab.PLANS -> {
                    SavingsPlansTab(
                        plans = savingsPlans,
                        onCreateSavings = { showNewSavingsDialog = true }
                    )
                }
                SavingsScreenTab.HISTORY -> {
                    SavingsHistoryTab(savingsAccounts = transactionUiState.savingsAccounts)
                }
            }
        }
    }

    // New Savings Dialog
    if (showNewSavingsDialog) {
        NewSavingsDialog(
            plans = savingsPlans,
            isLoading = transactionUiState.isLoading,
            onDismiss = { showNewSavingsDialog = false },
            onConfirm = { accountName, amount, plan ->
                scope.launch {
                    transactionViewModel.createSavingsAccount(accountName, amount, plan.lockPeriod)
                    showNewSavingsDialog = false
                }
            }
        )
    }

    // Withdraw Dialog
    showWithdrawDialog?.let { savings ->
        WithdrawSavingsDialog(
            savings = savings,
            isLoading = transactionUiState.isLoading,
            onDismiss = { showWithdrawDialog = null },
            onConfirm = {
                scope.launch {
                    transactionViewModel.withdrawSavings(savings.id)
                    showWithdrawDialog = null
                }
            }
        )
    }

    // Details Dialog
    showSavingsDetailsDialog?.let { savings ->
        SavingsDetailsDialog(
            savings = savings,
            onDismiss = { showSavingsDetailsDialog = null },
            onWithdraw = {
                showSavingsDetailsDialog = null
                showWithdrawDialog = savings
            }
        )
    }
}

@Composable
private fun MySavingsTab(
    savingsAccounts: List<SavingsAccount>,
    isLoading: Boolean,
    onWithdraw: (SavingsAccount) -> Unit,
    onViewDetails: (SavingsAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Overview Card
            item {
                SavingsOverviewCard(savingsAccounts)
            }

            // Earnings Summary
            item {
                EarningsSummaryCard(savingsAccounts)
            }

            // My Savings Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Locked Savings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }.size} active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Savings List
            if (savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }.isEmpty()) {
                item {
                    EmptySavingsCard()
                }
            } else {
                items(savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }) { savings ->
                    SavingsAccountCard(
                        savings = savings,
                        onWithdraw = { onWithdraw(savings) },
                        onViewDetails = { onViewDetails(savings) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsPlansTab(
    plans: List<SavingsPlan>,
    onCreateSavings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Locked Savings Plans",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Lock your money and earn guaranteed interest. The longer you save, the more you earn!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(plans) { plan ->
            SavingsPlanCard(plan = plan, onSelect = onCreateSavings)
        }
    }
}

@Composable
private fun SavingsHistoryTab(
    savingsAccounts: List<SavingsAccount>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Savings History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        val completedSavings = savingsAccounts.filter {
            it.status == SavingsStatus.MATURED || it.status == SavingsStatus.WITHDRAWN
        }

        if (completedSavings.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No History Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed and withdrawn savings will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(completedSavings) { savings ->
                SavingsHistoryCard(savings = savings)
            }
        }
    }
}

// Card Components

@Composable
private fun SavingsOverviewCard(savingsAccounts: List<SavingsAccount>) {
    val totalSaved = savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }.sumOf { it.amount }
    val totalEarnings = savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }.sumOf { it.accruedInterest }
    val projectedEarnings = savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }.sumOf { it.projectedEarnings }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$${"%.2f".format(totalSaved)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Earned: $${"%.2f".format(totalEarnings)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Projected: $${"%.2f".format(projectedEarnings)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EarningsSummaryCard(savingsAccounts: List<SavingsAccount>) {
    val activeSavings = savingsAccounts.filter { it.status == SavingsStatus.ACTIVE }
    val averageRate = if (activeSavings.isNotEmpty())
        activeSavings.sumOf { it.interestRate } / activeSavings.size else 0.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Earnings Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Active Plans",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${activeSavings.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Avg Interest Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.1f".format(averageRate)}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsAccountCard(
    savings: SavingsAccount,
    onWithdraw: () -> Unit,
    onViewDetails: () -> Unit
) {
    val daysRemaining = calculateDaysRemaining(savings.maturityDate)
    val progress = calculateProgress(savings.startDate, savings.maturityDate)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = savings.accountName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = savings.lockPeriod.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Locked: $${"%.2f".format(savings.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${savings.interestRate}% APY",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${"%.2f".format(savings.accruedInterest)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "earned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$daysRemaining days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Matures: ${savings.maturityDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }

                Button(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw")
                }
            }
        }
    }
}

@Composable
private fun EmptySavingsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Savings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Locked Savings Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start saving and earning interest today!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavingsPlanCard(
    plan: SavingsPlan,
    onSelect: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.lockPeriod.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${plan.interestRate}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "APY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Features
            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Min: $${"%.0f".format(plan.minDeposit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onSelect) {
                    Text("Start Saving")
                }
            }
        }
    }
}

@Composable
private fun SavingsHistoryCard(savings: SavingsAccount) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (savings.status == SavingsStatus.MATURED) Icons.Default.TrendingUp else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (savings.status == SavingsStatus.MATURED)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = savings.accountName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${savings.lockPeriod.displayName} • ${savings.interestRate}% APY",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${savings.startDate} - ${savings.maturityDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%.2f".format(savings.amount + savings.accruedInterest)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+$${"%.2f".format(savings.accruedInterest)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Dialogs

@Composable
private fun NewSavingsDialog(
    plans: List<SavingsPlan>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, SavingsPlan) -> Unit
) {
    var accountName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf(plans.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Savings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Savings Name") },
                    placeholder = { Text("e.g., Emergency Fund, Vacation") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Plan selector
                Column {
                    Text(
                        text = "Select Lock Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    plans.forEach { plan ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlan == plan,
                                onClick = { if (!isLoading) selectedPlan = plan },
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${plan.lockPeriod.displayName} - ${plan.interestRate}% APY",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Min: $${"%.0f".format(plan.minDeposit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Projected earnings
                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                if (amountDouble > 0) {
                    val projected = calculateProjectedEarnings(
                        amountDouble,
                        selectedPlan.interestRate,
                        selectedPlan.lockPeriod.days
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Projected Earnings",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$${"%.2f".format(projected)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total at maturity: $${"%.2f".format(amountDouble + projected)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (accountName.isNotEmpty() && amountDouble >= selectedPlan.minDeposit) {
                        onConfirm(accountName, amountDouble, selectedPlan)
                    }
                },
                enabled = !isLoading &&
                        accountName.isNotEmpty() &&
                        (amount.toDoubleOrNull() ?: 0.0) >= selectedPlan.minDeposit
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
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
private fun WithdrawSavingsDialog(
    savings: SavingsAccount,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isMatured = calculateDaysRemaining(savings.maturityDate) <= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Savings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isMatured) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Early Withdrawal Warning",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Withdrawing before maturity will incur a penalty of $${"%.2f".format(savings.earlyWithdrawalPenalty)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Principal: $${"%.2f".format(savings.amount)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Accrued Interest: $${"%.2f".format(savings.accruedInterest)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!isMatured) {
                    Text(
                        text = "Penalty: -$${"%.2f".format(savings.earlyWithdrawalPenalty)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Total Withdrawal: $${"%.2f".format(
                        savings.amount + savings.accruedInterest -
                                if (!isMatured) savings.earlyWithdrawalPenalty else 0.0
                    )}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Confirm Withdrawal")
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
private fun SavingsDetailsDialog(
    savings: SavingsAccount,
    onDismiss: () -> Unit,
    onWithdraw: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(savings.accountName) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Lock Period", savings.lockPeriod.displayName)
                DetailRow("Interest Rate", "${savings.interestRate}% APY")
                DetailRow("Locked Amount", "$${"%.2f".format(savings.amount)}")
                DetailRow("Accrued Interest", "$${"%.2f".format(savings.accruedInterest)}")
                DetailRow("Projected Earnings", "$${"%.2f".format(savings.projectedEarnings)}")
                DetailRow("Start Date", savings.startDate)
                DetailRow("Maturity Date", savings.maturityDate)
                DetailRow("Days Remaining", "${calculateDaysRemaining(savings.maturityDate)} days")
                DetailRow("Status", savings.status.name)
            }
        },
        confirmButton = {
            Button(onClick = onWithdraw) {
                Text("Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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

// Helper functions
private fun calculateProjectedEarnings(amount: Double, annualRate: Double, days: Int): Double {
    val rate = annualRate / 100.0
    val years = days / 365.0
    return amount * rate * years
}

private fun calculateDaysRemaining(maturityDate: String): Int {
    // TODO: Implement proper date calculation
    return 90
}

private fun calculateProgress(startDate: String, maturityDate: String): Float {
    // TODO: Implement proper progress calculation
    return 0.5f
}
