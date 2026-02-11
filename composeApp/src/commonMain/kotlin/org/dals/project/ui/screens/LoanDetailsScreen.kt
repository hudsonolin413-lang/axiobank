package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.LoanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailsScreen(
    loanId: String,
    viewModel: LoanViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loan = viewModel.getLoanById(loanId)

    var paymentAmount by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan Details") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (loan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loan not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Loan Overview Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Loan #${loan.id.take(8)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Loan Amount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${formatCurrency(loan.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Remaining Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${formatCurrency(loan.remainingBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress bar
                            val progress = 1f - (loan.remainingBalance / loan.amount).toFloat()
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Text(
                                text = "${(progress * 100).toInt()}% repaid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Loan Details Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Loan Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LoanDetailRow("Interest Rate", "${loan.interestRate}%")
                            LoanDetailRow("Term", "${loan.termInMonths} months")
                            LoanDetailRow("Monthly Payment", "$${formatCurrency(loan.monthlyPayment)}")
                            LoanDetailRow("Status", loan.status.name)
                            LoanDetailRow("Created Date", loan.createdDate)
                            LoanDetailRow("Due Date", loan.dueDate)
                            loan.lastPaymentDate?.let {
                                LoanDetailRow("Last Payment", it)
                            }
                            loan.smartContractAddress?.let {
                                LoanDetailRow("Contract Address", it.take(10) + "...")
                            }
                        }
                    }
                }

                // Collateral Information
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Collateral Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LoanDetailRow("Type", loan.collateral.type.name.replace('_', ' '))
                            LoanDetailRow("Description", loan.collateral.description)
                            LoanDetailRow("Value", "$${formatCurrency(loan.collateral.value)}")
                        }
                    }
                }

                // Payment Actions
                if (loan.status == LoanStatus.ACTIVE) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Make Payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            paymentAmount = loan.monthlyPayment.toString()
                                            showPaymentDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Monthly Payment")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            paymentAmount = ""
                                            showPaymentDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Custom Amount")
                                    }
                                }
                            }
                        }
                    }
                }

                // Error/Success Messages
                item {
                    uiState.errorMessage?.let { error ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    uiState.successMessage?.let { success ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                text = success,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog
    if (showPaymentDialog && loan != null) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Make Payment") },
            text = {
                Column {
                    Text(
                        text = "Enter payment amount:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Show remaining balance
                    Text(
                        text = "Remaining Balance: $${formatCurrency(loan.remainingBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Enter custom amount or use button below")
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pay Full Amount button
                    OutlinedButton(
                        onClick = {
                            paymentAmount = loan.remainingBalance.toString()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pay Full Amount ($${formatCurrency(loan.remainingBalance)})")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.makePayment(loanId, amount)
                            showPaymentDialog = false
                            paymentAmount = ""
                        }
                    },
                    enabled = paymentAmount.toDoubleOrNull()?.let { it > 0 } == true
                ) {
                    Text("Submit Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LoanDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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

private fun formatCurrency(amount: Double): String {
    return amount.toString()
}