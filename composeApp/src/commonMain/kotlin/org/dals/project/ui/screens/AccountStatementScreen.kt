package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.utils.DateRangePeriod
import org.dals.project.utils.StatementFormat
import org.dals.project.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountStatementScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by transactionViewModel.uiState.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(DateRangePeriod.THIS_MONTH) }
    var selectedFormat by remember { mutableStateOf(StatementFormat.TXT) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var customEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Clear any previous statement when entering the screen
        transactionViewModel.clearStatement()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Statement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.generatedStatement != null) {
                        IconButton(
                            onClick = {
                                // In a real app, this would trigger a download
                                // For now, we'll just show a message
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                    }
                    IconButton(
                        onClick = {
                            transactionViewModel.generateStatementForPeriod(selectedPeriod, selectedFormat)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Generate Statement")
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
            // Controls Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Statement Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Period Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Period:")
                        OutlinedButton(
                            onClick = { showPeriodDialog = true }
                        ) {
                            Text(getPeriodDisplayName(selectedPeriod))
                        }
                    }

                    // Format Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Format:")
                        OutlinedButton(
                            onClick = { showFormatDialog = true }
                        ) {
                            Text(selectedFormat.name)
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            transactionViewModel.generateStatementForPeriod(selectedPeriod, selectedFormat)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Generate Statement")
                        }
                    }

                    // Download and Email Button
                    OutlinedButton(
                        onClick = {
                            showEmailDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Email Statement")
                    }
                }
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Success Message
            uiState.successMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Generated Statement
            uiState.generatedStatement?.let { statement ->
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Statement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            uiState.statementFileName?.let { fileName ->
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SelectionContainer {
                            Text(
                                text = statement,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }

    // Period Selection Dialog
    if (showPeriodDialog) {
        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            title = { Text("Select Period") },
            text = {
                LazyColumn {
                    items(DateRangePeriod.values()) { period ->
                        TextButton(
                            onClick = {
                                selectedPeriod = period
                                showPeriodDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = getPeriodDisplayName(period),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPeriodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Format Selection Dialog
    if (showFormatDialog) {
        AlertDialog(
            onDismissRequest = { showFormatDialog = false },
            title = { Text("Select Format") },
            text = {
                Column {
                    StatementFormat.values().forEach { format ->
                        TextButton(
                            onClick = {
                                selectedFormat = format
                                showFormatDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${format.name} - ${getFormatDescription(format)}",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Email Input Dialog
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Send Statement to Email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the email address where you'd like to receive the statement. Leave blank to use your account email.")
                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("Recipient Email") },
                        placeholder = { Text("example@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.downloadAndEmailStatement(
                            period = selectedPeriod,
                            email = if (customEmail.isBlank()) null else customEmail
                        )
                        showEmailDialog = false
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getPeriodDisplayName(period: DateRangePeriod): String {
    return when (period) {
        DateRangePeriod.TODAY -> "Today"
        DateRangePeriod.THIS_WEEK -> "This Week"
        DateRangePeriod.THIS_MONTH -> "This Month"
        DateRangePeriod.LAST_30_DAYS -> "Last 30 Days"
        DateRangePeriod.THIS_YEAR -> "This Year"
    }
}

private fun getFormatDescription(format: StatementFormat): String {
    return when (format) {
        StatementFormat.TXT -> "Text format for viewing"
        StatementFormat.CSV -> "CSV format for spreadsheets"
    }
}