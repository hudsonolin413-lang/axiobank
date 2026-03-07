package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalculatorScreen(
    onNavigateBack: () -> Unit,
    onApplyForLoan: (amount: Double, tenure: Int, rate: Double) -> Unit = { _, _, _ -> }
) {
    var loanAmount by remember { mutableStateOf("10000") }
    var interestRate by remember { mutableStateOf("12") }
    var tenureMonths by remember { mutableStateOf("12") }
    
    val amount = loanAmount.toDoubleOrNull() ?: 0.0
    val rate = interestRate.toDoubleOrNull() ?: 0.0
    val months = tenureMonths.toIntOrNull() ?: 1
    
    // EMI Calculation: EMI = P × r × (1 + r)^n / ((1 + r)^n - 1)
    val monthlyRate = rate / 12 / 100
    val emi = if (monthlyRate > 0 && months > 0 && amount > 0) {
        amount * monthlyRate * (1 + monthlyRate).pow(months) / ((1 + monthlyRate).pow(months) - 1)
    } else if (months > 0 && amount > 0) {
        amount / months
    } else 0.0
    
    val totalPayment = emi * months
    val totalInterest = totalPayment - amount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Calculator") },
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
            // Calculator Icon
            item {
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
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "EMI Calculator",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Calculate your monthly payments before applying",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Input Fields
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Loan Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = loanAmount,
                            onValueChange = { loanAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Loan Amount") },
                            leadingIcon = { Text("$", style = MaterialTheme.typography.bodyLarge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Loan Amount Slider
                        Column {
                            Text(
                                text = "Amount: $${loanAmount.toIntOrNull() ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = (loanAmount.toFloatOrNull() ?: 0f).coerceIn(1000f, 100000f),
                                onValueChange = { loanAmount = it.toInt().toString() },
                                valueRange = 1000f..100000f,
                                steps = 98
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$1,000", style = MaterialTheme.typography.labelSmall)
                                Text("$100,000", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedTextField(
                            value = interestRate,
                            onValueChange = { interestRate = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Interest Rate (% per year)") },
                            trailingIcon = { Text("%", style = MaterialTheme.typography.bodyLarge) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tenureMonths,
                            onValueChange = { tenureMonths = it.filter { c -> c.isDigit() } },
                            label = { Text("Loan Tenure (months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Tenure Slider
                        Column {
                            Text(
                                text = "Tenure: $months months (${months / 12} years ${months % 12} months)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = (tenureMonths.toFloatOrNull() ?: 12f).coerceIn(1f, 84f),
                                onValueChange = { tenureMonths = it.toInt().toString() },
                                valueRange = 1f..84f,
                                steps = 82
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1 month", style = MaterialTheme.typography.labelSmall)
                                Text("84 months", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Results
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Calculation Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // EMI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Monthly EMI",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = "$${String.format("%.2f", emi)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))

                        // Total Interest
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Interest",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$${String.format("%.2f", totalInterest)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Total Payment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Payment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$${String.format("%.2f", totalPayment)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Principal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Principal Amount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$${String.format("%.2f", amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Payment Breakdown Visual
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val principalPercentage = if (totalPayment > 0) (amount / totalPayment).toFloat() else 0f
                        val interestPercentage = if (totalPayment > 0) (totalInterest / totalPayment).toFloat() else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Principal",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(
                                    progress = { principalPercentage },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(principalPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Interest",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(
                                    progress = { interestPercentage },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "${(interestPercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Apply Button
            item {
                Button(
                    onClick = { onApplyForLoan(amount, months, rate) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount > 0 && months > 0
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply for This Loan")
                }
            }

            // Disclaimer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This is an estimate only. Actual EMI may vary based on processing fees, insurance, and other charges. Terms and conditions apply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
