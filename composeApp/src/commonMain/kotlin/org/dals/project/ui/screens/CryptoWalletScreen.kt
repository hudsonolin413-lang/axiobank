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
import org.dals.project.viewmodel.CryptoWalletViewModel
import org.dals.project.repository.BuyCryptoRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoWalletScreen(
    customerId: String,
    accountId: String,
    viewModel: CryptoWalletViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSymbol by remember { mutableStateOf("BTC") }
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(customerId) {
        viewModel.loadPrices()
        viewModel.loadWallets(customerId)
    }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Market Prices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(uiState.prices) { price ->
                        ListItem(
                            headlineContent = { Text("${price.name} (${price.symbol})") },
                            supportingContent = { Text("$${price.price}") },
                            trailingContent = { 
                                Text(
                                    "${price.change24h}%", 
                                    color = if (price.change24h >= 0) Color.Green else Color.Red
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("My Portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.wallets) { wallet ->
                        ListItem(
                            headlineContent = { Text("${wallet.balance} ${wallet.symbol}") },
                            supportingContent = { Text("$${wallet.fiatEquivalent}") },
                            trailingContent = { Text(wallet.address.take(6) + "...", style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quick Buy", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (USD)") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                viewModel.buy(BuyCryptoRequest(customerId, accountId, selectedSymbol, amount.toDoubleOrNull() ?: 0.0))
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Buy $selectedSymbol")
                        }
                    }
                }
            }
        }
    }
}
