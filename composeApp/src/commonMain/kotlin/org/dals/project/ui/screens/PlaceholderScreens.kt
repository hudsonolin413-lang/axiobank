package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import org.dals.project.viewmodel.AuthViewModel

// SplitBillScreen moved to SplitBillScreen.kt
// ATMLocatorScreen moved to ATMLocatorScreen.kt
// CryptoWalletScreen moved to CryptoWalletScreen.kt
// OverdraftProtectionScreen moved to OverdraftProtectionScreen.kt
// CashFlowForecastScreen moved to CashFlowForecastScreen.kt
// InternationalTransferScreen moved to InternationalTransferScreen.kt
// LoanRefinancingScreen moved to LoanRefinancingScreen.kt
// DigitalSignatureScreen moved to DigitalSignatureScreen.kt
// NfcPaymentScreen moved to NfcPaymentScreen.kt

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
                .padding(16.dp)
        ) {
            Text("Trusted Devices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text("Current Device: Pixel 7") },
                supportingContent = { Text("Active now • Nairobi, Kenya") },
                trailingContent = { Text("Active", color = Color.Green) }
            )
            
            HorizontalDivider()
            
            ListItem(
                headlineContent = { Text("iPhone 13") },
                supportingContent = { Text("Last active: Oct 12, 2023") },
                trailingContent = { TextButton(onClick = { /* TODO */ }) { Text("Remove") } }
            )
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
fun MoreServicesScreen(
    authViewModel: AuthViewModel,
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
