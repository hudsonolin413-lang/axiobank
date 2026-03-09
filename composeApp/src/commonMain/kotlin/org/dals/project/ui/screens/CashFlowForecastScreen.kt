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
import org.dals.project.viewmodel.CashFlowForecastViewModel
import org.dals.project.repository.CashFlowAnalysisDto
import org.dals.project.repository.CashFlowForecastDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowForecastScreen(
    customerId: String,
    accountId: String,
    viewModel: CashFlowForecastViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId, accountId) {
        viewModel.loadAnalysis(accountId, customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Flow Forecast") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateForecast(customerId, accountId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                uiState.analysis?.let { analysis ->
                    CashFlowOverviewCard(analysis)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Projections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.forecasts) { forecast ->
                            ForecastItem(forecast)
                        }
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No forecast data available")
                            Button(onClick = { viewModel.generateForecast(customerId, accountId) }) {
                                Text("Generate Forecast")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowOverviewCard(analysis: CashFlowAnalysisDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("90-Day Outlook", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Risk Level", style = MaterialTheme.typography.bodySmall)
                    Text(
                        analysis.riskLevel,
                        style = MaterialTheme.typography.titleLarge,
                        color = when(analysis.riskLevel) {
                            "LOW" -> Color.Green
                            "MEDIUM" -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("90d Projection", style = MaterialTheme.typography.bodySmall)
                    Text("$${analysis.projectedBalanceIn90Days}", style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Insights", fontWeight = FontWeight.Bold)
            analysis.insights.forEach { insight ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Yellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(insight, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ForecastItem(forecast: CashFlowForecastDto) {
    ListItem(
        headlineContent = { Text(forecast.forecastDate) },
        supportingContent = { Text("Confidence: ${(forecast.confidence * 100).toInt()}%") },
        trailingContent = { 
            Column(horizontalAlignment = Alignment.End) {
                Text("$${forecast.predictedBalance}", fontWeight = FontWeight.Bold)
                Text(
                    if (forecast.predictedIncome > forecast.predictedExpenses) "+$${forecast.predictedIncome - forecast.predictedExpenses}" 
                    else "-$${forecast.predictedExpenses - forecast.predictedIncome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (forecast.predictedIncome > forecast.predictedExpenses) Color.Green else Color.Red
                )
            }
        }
    )
}
