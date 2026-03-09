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
import org.dals.project.viewmodel.InternationalTransferViewModel
import org.dals.project.repository.CreateInternationalTransferRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternationalTransferScreen(
    customerId: String,
    accountId: String,
    viewModel: InternationalTransferViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var amount by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientBank by remember { mutableStateOf("") }
    var recipientCountry by remember { mutableStateOf("USA") }
    var toCurrency by remember { mutableStateOf("USD") }

    LaunchedEffect(customerId) {
        viewModel.loadTransfers(customerId)
    }

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
                Text("New Transfer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = recipientName, onValueChange = { recipientName = it }, label = { Text("Recipient Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = recipientBank, onValueChange = { recipientBank = it }, label = { Text("Recipient Bank") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { viewModel.getQuote(amount.toDoubleOrNull() ?: 0.0, "USD", toCurrency, recipientCountry) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Get Quote")
                }

                uiState.quote?.let { quote ->
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Quote Details", fontWeight = FontWeight.Bold)
                            Text("Exchange Rate: ${quote.exchangeRate}")
                            Text("Fee: $${quote.transferFee}")
                            Text("Total Cost: $${quote.totalCost}")
                            Text("Est. Delivery: ${quote.estimatedDeliveryDays} days")
                            
                            Button(
                                onClick = {
                                    viewModel.createTransfer(
                                        CreateInternationalTransferRequest(
                                            customerId = customerId,
                                            accountId = accountId,
                                            recipientName = recipientName,
                                            recipientBank = recipientBank,
                                            recipientCountry = recipientCountry,
                                            amount = amount.toDoubleOrNull() ?: 0.0,
                                            currency = toCurrency,
                                            purpose = "Personal Transfer"
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                            ) {
                                Text("Confirm Transfer")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Transfer History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.transfers) { transfer ->
                        ListItem(
                            headlineContent = { Text(transfer.recipientName) },
                            supportingContent = { Text("${transfer.amount} ${transfer.currency} • ${transfer.status}") },
                            trailingContent = { Text(transfer.createdAt, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }
    }
}
