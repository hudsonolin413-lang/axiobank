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
import org.dals.project.viewmodel.LoanRefinancingViewModel
import org.dals.project.repository.RefinancingAnalysisDto
import org.dals.project.repository.CreateRefinancingApplicationRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanRefinancingScreen(
    customerId: String,
    viewModel: LoanRefinancingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var loanId by remember { mutableStateOf("") }
    var proposedRate by remember { mutableStateOf("") }
    var proposedTerm by remember { mutableStateOf("") }

    LaunchedEffect(customerId) {
        viewModel.loadApplications(customerId)
    }

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
                Text("Analyze New Refinancing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = loanId, onValueChange = { loanId = it }, label = { Text("Loan ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = proposedRate, onValueChange = { proposedRate = it }, label = { Text("Proposed Rate (%)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = proposedTerm, onValueChange = { proposedTerm = it }, label = { Text("Proposed Term (Months)") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { viewModel.analyze(loanId, proposedRate.toDoubleOrNull() ?: 0.0, proposedTerm.toIntOrNull() ?: 0) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Analyze Savings")
                }

                uiState.analysis?.let { analysis ->
                    RefinancingAnalysisCard(analysis) {
                        viewModel.apply(
                            CreateRefinancingApplicationRequest(
                                customerId = customerId,
                                loanId = loanId,
                                proposedRate = proposedRate.toDoubleOrNull() ?: 0.0,
                                proposedTerm = proposedTerm.toIntOrNull() ?: 0,
                                closingCosts = 0.0,
                                monthlySavings = analysis.monthlySavings,
                                totalInterestSavings = analysis.totalInterestSavings
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("My Applications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.applications) { app ->
                        ListItem(
                            headlineContent = { Text("Loan: ${app.loanId}") },
                            supportingContent = { Text("Status: ${app.status} • Savings: $${app.monthlySavings}/mo") },
                            trailingContent = { Text(app.createdAt, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RefinancingAnalysisCard(analysis: RefinancingAnalysisDto, onApply: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = if (analysis.recommended) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (analysis.recommended) "Recommended!" else "Not Recommended", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Monthly Savings: $${analysis.monthlySavings}")
            Text("Total Interest Savings: $${analysis.totalInterestSavings}")
            Text("Break-even: ${analysis.breakEvenMonths} months")
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text("Apply for Refinancing")
            }
        }
    }
}
