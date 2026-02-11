package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.LoanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApplicationScreen(
    viewModel: LoanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf(LoanPurpose.PERSONAL) }
    var termInMonths by remember { mutableStateOf("12") }
    var description by remember { mutableStateOf("") }
    var collateralType by remember { mutableStateOf(CollateralType.NONE) }
    var collateralValue by remember { mutableStateOf("") }
    var employerName by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var employmentType by remember { mutableStateOf(EmploymentType.FULL_TIME) }
    var yearsOfExperience by remember { mutableStateOf("") }
    var monthlyIncome by remember { mutableStateOf("") }
    var existingDebts by remember { mutableStateOf("") }

    var showPurposeDropdown by remember { mutableStateOf(false) }
    var showCollateralDropdown by remember { mutableStateOf(false) }
    var showEmploymentDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (!uiState.successMessage.isNullOrEmpty()) {
            onNavigateToDashboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("Loan Application")
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
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
            item {
                Text(
                    text = "Apply for a Loan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fill out the information below to submit your loan application",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Loan Details Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Loan Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Loan Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Purpose Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showPurposeDropdown,
                            onExpandedChange = { showPurposeDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = purpose.name.replace('_', ' '),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Loan Purpose") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPurposeDropdown) },
                                modifier = Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showPurposeDropdown,
                                onDismissRequest = { showPurposeDropdown = false }
                            ) {
                                LoanPurpose.values().forEach { purposeOption ->
                                    DropdownMenuItem(
                                        text = { Text(purposeOption.name.replace('_', ' ')) },
                                        onClick = {
                                            purpose = purposeOption
                                            showPurposeDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = termInMonths,
                            onValueChange = { termInMonths = it },
                            label = { Text("Term (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Collateral Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Collateral Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Collateral Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showCollateralDropdown,
                            onExpandedChange = { showCollateralDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = collateralType.name.replace('_', ' '),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Collateral Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCollateralDropdown) },
                                modifier = Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showCollateralDropdown,
                                onDismissRequest = { showCollateralDropdown = false }
                            ) {
                                CollateralType.values().forEach { collateralOption ->
                                    DropdownMenuItem(
                                        text = { Text(collateralOption.name.replace('_', ' ')) },
                                        onClick = {
                                            collateralType = collateralOption
                                            showCollateralDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        if (collateralType != CollateralType.NONE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = collateralValue,
                                onValueChange = { collateralValue = it },
                                label = { Text("Collateral Value ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Employment Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Employment Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = employerName,
                            onValueChange = { employerName = it },
                            label = { Text("Employer Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Employment Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showEmploymentDropdown,
                            onExpandedChange = { showEmploymentDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = employmentType.name.replace('_', ' '),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Employment Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEmploymentDropdown) },
                                modifier = Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                ).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = showEmploymentDropdown,
                                onDismissRequest = { showEmploymentDropdown = false }
                            ) {
                                EmploymentType.values().forEach { employmentOption ->
                                    DropdownMenuItem(
                                        text = { Text(employmentOption.name.replace('_', ' ')) },
                                        onClick = {
                                            employmentType = employmentOption
                                            showEmploymentDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = yearsOfExperience,
                            onValueChange = { yearsOfExperience = it },
                            label = { Text("Years of Experience") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Financial Information Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Financial Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = monthlyIncome,
                            onValueChange = { monthlyIncome = it },
                            label = { Text("Monthly Income ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = existingDebts,
                            onValueChange = { existingDebts = it },
                            label = { Text("Existing Debts ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
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

            // Submit Button
            item {
                Button(
                    onClick = {
                        viewModel.clearMessages()

                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        val termInt = termInMonths.toIntOrNull() ?: 12
                        val collateralValueDouble = collateralValue.toDoubleOrNull() ?: 0.0
                        val yearsInt = yearsOfExperience.toIntOrNull() ?: 0
                        val monthlyIncomeDouble = monthlyIncome.toDoubleOrNull() ?: 0.0
                        val existingDebtsDouble = existingDebts.toDoubleOrNull() ?: 0.0

                        val employmentInfo = EmploymentInfo(
                            employerName = employerName,
                            jobTitle = jobTitle,
                            employmentType = employmentType,
                            yearsOfExperience = yearsInt
                        )

                        viewModel.submitLoanApplication(
                            amount = amountDouble,
                            purpose = purpose,
                            termInMonths = termInt,
                            description = description,
                            collateralType = collateralType,
                            collateralValue = collateralValueDouble,
                            employmentInfo = employmentInfo,
                            monthlyIncome = monthlyIncomeDouble,
                            existingDebts = existingDebtsDouble
                        )
                    },
                    enabled = !uiState.isLoading && amount.isNotEmpty() && description.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Submit Application")
                }
            }
        }
    }
}