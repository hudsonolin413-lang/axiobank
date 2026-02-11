package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.Currency
import org.dals.project.utils.SettingsManager
import org.dals.project.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    onNavigateBack: () -> Unit
) {
    val settingsRepository = SettingsManager.settingsRepository
    val appSettings by settingsRepository.appSettings.collectAsStateWithLifecycle()

    var selectedCurrency by remember { mutableStateOf(appSettings.currency) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Update selectedCurrency when appSettings changes
    LaunchedEffect(appSettings.currency) {
        selectedCurrency = appSettings.currency
    }

    val currencies = CurrencyUtils.getSupportedCurrencies()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency Settings") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp).padding(end = 8.dp)
                            )
                            Text(
                                text = "Choose Currency",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select your preferred currency for transactions and display",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        // Current selection indicator
                        if (selectedCurrency != appSettings.currency) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Current: ${settingsRepository.getCurrentCurrencyName()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        // Exchange rate info
                        if (selectedCurrency != "USD" && selectedCurrency != appSettings.currency) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val rate = CurrencyUtils.getExchangeRate("USD", selectedCurrency)
                            Text(
                                text = "1 USD = ${CurrencyUtils.formatAmount(rate, selectedCurrency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            items(currencies) { currency ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCurrency = currency.code }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currency.flag,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currency.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${currency.code} (${currency.symbol})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Show exchange rate for non-USD currencies
                            if (currency.code != "USD") {
                                val rate = CurrencyUtils.getExchangeRate("USD", currency.code)
                                Text(
                                    text = "1 USD = ${CurrencyUtils.formatAmount(rate, currency.code)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (selectedCurrency == currency.code) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedCurrency != appSettings.currency) {
                            showSaveDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCurrency.isNotEmpty()
                ) {
                    Text(
                        text = if (selectedCurrency != appSettings.currency) {
                            "Save Currency Preference"
                        } else {
                            "Back to Settings"
                        }
                    )
                }
            }
        }
    }

    // Save confirmation dialog
    if (showSaveDialog) {
        val selectedCurrencyObj = currencies.find { it.code == selectedCurrency }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Currency Preference") },
            text = {
                Column {
                    Text("Change currency to:")
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedCurrencyObj?.let { currency ->
                        Text(
                            text = "${currency.flag} ${currency.name} (${currency.code})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will convert and display all amounts throughout the app in ${selectedCurrencyObj?.name ?: selectedCurrency}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsRepository.updateCurrency(selectedCurrency)
                        showSaveDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        selectedCurrency = appSettings.currency // Reset selection
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}