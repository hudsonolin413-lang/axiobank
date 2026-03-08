package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Bill") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CallSplit,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Split Expenses with Friends",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Easily divide bills, rent, or dinner expenses with your contacts.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Split")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ATMLocatorScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ATM Locator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Find Nearest ATM or Branch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Use our interactive map to find the closest Axio Bank location.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Map")
            }
        }
    }
}

// BeneficiaryManagementScreen moved to BeneficiaryManagementScreen.kt (real implementation)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Secured Devices",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Manage the devices authorized to access your account.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh List")
            }
        }
    }
}

// QRPaymentScreen moved to QRPaymentScreen.kt (real implementation with QR code generation and payment processing)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkTransferScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Transfer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Batch Payments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Send money to multiple recipients at once.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Recipient List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No recipients added", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { /* TODO */ }) {
                        Text("Upload CSV/Excel")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text("Continue to Payment")
            }
        }
    }
}

// SubAccountsScreen moved to SubAccountsScreen.kt (real implementation)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoWalletScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crypto Wallet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Total Crypto Balance", color = Color.White.copy(alpha = 0.7f))
                    Text("$0.00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("Buy") }
                        Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("Sell") }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Assets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "You don't own any crypto assets yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdraftProtectionScreen(onNavigateBack: () -> Unit) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Safe Spend",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Prevent declined transactions and overdraft fees by linking a backup account.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ListItem(
                headlineContent = { Text("Enable Protection") },
                supportingContent = { Text("Automatically cover shortfalls from savings.") },
                trailingContent = { Switch(checked = false, onCheckedChange = { /* TODO */ }) }
            )
            
            HorizontalDivider()
            
            ListItem(
                headlineContent = { Text("Overdraft Limit") },
                trailingContent = { Text("$500.00", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

// TaxReportsScreen moved to TaxReportsScreen.kt (real implementation)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowForecastScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Flow Forecast") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Future Spending Analysis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Forecast Chart Placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Predicted Balance (30 Days)", fontWeight = FontWeight.Bold)
                    Text("$4,320.00", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternationalTransferScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("International Transfer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Send Money Abroad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Destination Country") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = "USD",
                    onValueChange = {},
                    label = { Text("Currency") },
                    modifier = Modifier.width(80.dp),
                    readOnly = true
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Exchange Rate: 1 USD = 0.92 EUR", style = MaterialTheme.typography.bodySmall)
                    Text("Fee: $5.00", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalSignatureScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Signature") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign Documents Securely", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign Here", color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = { /* TODO */ }, modifier = Modifier.weight(1f)) { Text("Save Signature") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcPaymentScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contactless Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(150.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Hold Near Terminal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Make sure NFC is enabled in your device settings.", textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModeScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ListItem(
                headlineContent = { Text("Enable Offline Mode") },
                supportingContent = { Text("Access basic features without internet.") },
                trailingContent = { Switch(checked = true, onCheckedChange = { /* TODO */ }) }
            )
            Text(
                "In offline mode, you can still view your last known balance and recent transactions. Payments will be queued and sent when you're back online.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRefinancingScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Refinancing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Optimize Your Debt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Lower your interest rate or change your repayment term.")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active Loan", fontWeight = FontWeight.Bold)
                    Text("Balance: $5,200.00", style = MaterialTheme.typography.bodyLarge)
                    Text("Current APR: 12.5%", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("View Refinancing Offers")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreServicesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkTransfer: () -> Unit,
    onNavigateToSubAccounts: () -> Unit,
    onNavigateToCryptoWallet: () -> Unit,
    onNavigateToOverdraftProtection: () -> Unit,
    onNavigateToTaxReports: () -> Unit,
    onNavigateToCashFlowForecast: () -> Unit,
    onNavigateToInternationalTransfer: () -> Unit,
    onNavigateToDigitalSignature: () -> Unit,
    onNavigateToNfcPayment: () -> Unit,
    onNavigateToOfflineMode: () -> Unit,
    onNavigateToLoanRefinancing: () -> Unit,
    onNavigateToSpendingAnalytics: () -> Unit,
    onNavigateToBudgetManagement: () -> Unit,
    onNavigateToVirtualCards: () -> Unit,
    onNavigateToQRPayment: () -> Unit,
    onNavigateToATMLocator: () -> Unit,
    onNavigateToBeneficiaryManagement: () -> Unit,
    onNavigateToSplitBill: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onNavigateToLoanCalculator: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More Services") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val services = listOf(
            ServiceItem("Bulk Transfer", Icons.Default.Groups, Color(0xFF4CAF50), onNavigateToBulkTransfer),
            ServiceItem("Sub Accounts", Icons.Default.AccountTree, Color(0xFF2196F3), onNavigateToSubAccounts),
            ServiceItem("Crypto Wallet", Icons.Default.CurrencyBitcoin, Color(0xFFFF9800), onNavigateToCryptoWallet),
            ServiceItem("Overdraft", Icons.Default.Security, Color(0xFFF44336), onNavigateToOverdraftProtection),
            ServiceItem("Tax Reports", Icons.Default.Assessment, Color(0xFF607D8B), onNavigateToTaxReports),
            ServiceItem("Cash Flow", Icons.Default.TrendingUp, Color(0xFF9C27B0), onNavigateToCashFlowForecast),
            ServiceItem("International", Icons.Default.Public, Color(0xFF03A9F4), onNavigateToInternationalTransfer),
            ServiceItem("Digital Sign", Icons.Default.Draw, Color(0xFF795548), onNavigateToDigitalSignature),
            ServiceItem("NFC Payment", Icons.Default.Contactless, Color(0xFFE91E63), onNavigateToNfcPayment),
            ServiceItem("Offline Mode", Icons.Default.CloudOff, Color(0xFF9E9E9E), onNavigateToOfflineMode),
            ServiceItem("Loan Refinance", Icons.Default.HistoryEdu, Color(0xFF673AB7), onNavigateToLoanRefinancing),
            ServiceItem("Analytics", Icons.Default.PieChart, Color(0xFF00BCD4), onNavigateToSpendingAnalytics),
            ServiceItem("Budget", Icons.Default.AccountBalanceWallet, Color(0xFFFF5722), onNavigateToBudgetManagement),
            ServiceItem("Virtual Cards", Icons.Default.CreditCard, Color(0xFF3F51B5), onNavigateToVirtualCards),
            ServiceItem("QR Payment", Icons.Default.QrCode, Color(0xFF009688), onNavigateToQRPayment),
            ServiceItem("ATM Locator", Icons.Default.LocationOn, Color(0xFFFFC107), onNavigateToATMLocator),
            ServiceItem("Beneficiaries", Icons.Default.PersonAdd, Color(0xFF8BC34A), onNavigateToBeneficiaryManagement),
            ServiceItem("Split Bill", Icons.Default.ReceiptLong, Color(0xFFCDDC39), onNavigateToSplitBill),
            ServiceItem("Referral", Icons.Default.Share, Color(0xFFFF4081), onNavigateToReferral),
            ServiceItem("Loan Calc", Icons.Default.Calculate, Color(0xFF5C6BC0), onNavigateToLoanCalculator)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(services) { service ->
                ServiceCard(service)
            }
        }
    }
}

data class ServiceItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun ServiceCard(service: ServiceItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { service.onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(service.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = service.name,
                tint = service.color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = service.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
