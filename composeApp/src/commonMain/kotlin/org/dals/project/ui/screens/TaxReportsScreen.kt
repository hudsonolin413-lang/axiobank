package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.repository.TaxReportRecord
import org.dals.project.repository.TaxSummary
import org.dals.project.viewmodel.TaxReportViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxReportsScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaxReportViewModel = viewModel { TaxReportViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadInitialData(userId)
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Error will be shown in UI
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh(userId) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowGenerateDialog(true) }
            ) {
                Icon(Icons.Default.Add, "Generate Report")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.taxSummary == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Year selector
                    item {
                        YearSelector(
                            availableYears = uiState.availableYears,
                            selectedYear = uiState.selectedYear,
                            onYearSelected = { year ->
                                viewModel.loadReportsForYear(userId, year)
                            }
                        )
                    }

                    // Tax Summary Card
                    item {
                        uiState.taxSummary?.let { summary ->
                            TaxSummaryCard(summary = summary)
                        }
                    }

                    // Error message
                    if (uiState.error != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = uiState.error ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Reports section header
                    item {
                        Text(
                            text = "Generated Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reports list
                    if (uiState.reports.isEmpty()) {
                        item {
                            EmptyReportsState()
                        }
                    } else {
                        items(uiState.reports) { report ->
                            TaxReportCard(
                                report = report,
                                onDelete = { viewModel.deleteReport(userId, report.id) }
                            )
                        }
                    }
                }
            }

            // Generate Report Dialog
            if (uiState.showGenerateDialog) {
                GenerateReportDialog(
                    selectedYear = uiState.selectedYear ?: java.time.Year.now().value,
                    onDismiss = { viewModel.setShowGenerateDialog(false) },
                    onConfirm = { year, reportType ->
                        viewModel.generateReport(userId, year, reportType)
                    }
                )
            }
        }
    }
}

@Composable
private fun YearSelector(
    availableYears: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tax Year",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedYear?.toString() ?: "Select Year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, "Select Year")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableYears.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onYearSelected(year)
                                expanded = false
                            },
                            leadingIcon = {
                                if (year == selectedYear) {
                                    Icon(Icons.Default.Check, null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaxSummaryCard(summary: TaxSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tax Summary ${summary.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Divider()

            // Income and Expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Total Income",
                    value = "$${formatCurrency(summary.totalIncome)}",
                    valueColor = MaterialTheme.colorScheme.primary
                )
                SummaryItem(
                    label = "Total Expenses",
                    value = "$${formatCurrency(summary.totalExpenses)}",
                    valueColor = MaterialTheme.colorScheme.error
                )
            }

            // Net Income
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Net Income",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${formatCurrency(summary.netIncome)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Tax Details
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaxDetailRow("Interest Earned", summary.interestEarned)
                TaxDetailRow("Interest Paid", summary.interestPaid)
                TaxDetailRow("Capital Gains", summary.capitalGains)
                TaxDetailRow("Dividends", summary.dividends)
                TaxDetailRow("Tax Withheld", summary.taxWithheld)
            }

            Divider()

            // Estimated Tax
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Tax",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${formatCurrency(summary.estimatedTax)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Reports generated
            Text(
                text = "${summary.reportsGenerated} report(s) generated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun TaxDetailRow(label: String, value: String) {
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
            text = "$${formatCurrency(value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TaxReportCard(
    report: TaxReportRecord,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (report.reportType) {
                                "ANNUAL" -> Icons.Default.CalendarMonth
                                "QUARTERLY" -> Icons.Default.DateRange
                                "INTEREST_EARNED" -> Icons.Default.TrendingUp
                                "CAPITAL_GAINS" -> Icons.Default.ShowChart
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = formatReportType(report.reportType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generated: ${formatDate(report.generatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                Surface(
                    color = when (report.status) {
                        "GENERATED" -> MaterialTheme.colorScheme.primaryContainer
                        "PROCESSING" -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = report.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Financial details
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialRow("Total Income", report.totalIncome)
                FinancialRow("Total Expenses", report.totalExpenses)
                FinancialRow("Interest Earned", report.interestEarned)
                FinancialRow("Interest Paid", report.interestPaid)
                FinancialRow("Capital Gains", report.capitalGains)
                FinancialRow("Dividends", report.dividends)
                FinancialRow("Tax Withheld", report.taxWithheld)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Download functionality */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Report") },
            text = { Text("Are you sure you want to delete this tax report? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FinancialRow(label: String, value: String) {
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
            text = "$${formatCurrency(value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyReportsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Reports Generated",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generate your first tax report to see it here",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GenerateReportDialog(
    selectedYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var year by remember { mutableStateOf(selectedYear) }
    var reportType by remember { mutableStateOf("ANNUAL") }
    var showYearPicker by remember { mutableStateOf(false) }

    val reportTypes = listOf(
        "ANNUAL" to "Annual Report",
        "QUARTERLY" to "Quarterly Report",
        "INTEREST_EARNED" to "Interest Earned",
        "CAPITAL_GAINS" to "Capital Gains"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Tax Report") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year selector
                OutlinedButton(
                    onClick = { showYearPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Year: $year")
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }

                // Report type selector
                Text(
                    text = "Report Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reportTypes.forEach { (type, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = reportType == type,
                                onClick = { reportType = type }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(year, reportType) }
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Simple year picker dropdown
    if (showYearPicker) {
        val currentYear = java.time.Year.now().value
        val years = (currentYear - 10..currentYear).toList().reversed()

        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            title = { Text("Select Year") },
            text = {
                LazyColumn {
                    items(years) { y ->
                        TextButton(
                            onClick = {
                                year = y
                                showYearPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = y.toString(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun formatCurrency(value: String): String {
    return try {
        val amount = value.toDoubleOrNull() ?: 0.0
        String.format("%.2f", amount)
    } catch (e: Exception) {
        "0.00"
    }
}

private fun formatReportType(type: String): String {
    return when (type) {
        "ANNUAL" -> "Annual Report"
        "QUARTERLY" -> "Quarterly Report"
        "INTEREST_EARNED" -> "Interest Earned Report"
        "CAPITAL_GAINS" -> "Capital Gains Report"
        else -> type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter
            .ofPattern("MMM dd, yyyy")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString
    }
}
