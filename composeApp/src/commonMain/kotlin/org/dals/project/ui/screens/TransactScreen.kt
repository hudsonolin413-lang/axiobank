package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import decentralizedaccessloan.composeapp.generated.resources.*
import org.dals.project.model.*
import org.dals.project.ui.components.Country
import org.dals.project.ui.components.PhoneNumberInput
import org.dals.project.utils.getPaymentMethodName
import org.dals.project.utils.getPaymentMethodDescription
import org.dals.project.viewmodel.TransactionUiState
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.ui.components.TransactionConfirmationDialog

enum class TransactTab {
    SEND, RECEIVE, BILLS, INVEST, WITHDRAW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillTypeDropdown(
    selectedType: BillType,
    onTypeSelected: (BillType) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = showDropdown,
        onExpandedChange = { showDropdown = it }
    ) {
        OutlinedTextField(
            value = selectedType.name.replace('_', ' '),
            onValueChange = { },
            readOnly = true,
            label = { Text("Bill Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            BillType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace('_', ' ')) },
                    onClick = {
                        onTypeSelected(type)
                        showDropdown = false
                    }
                )
            }
        }
    }
}

enum class TransferType {
    ACCOUNT_TRANSFER, MOBILE_MONEY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToKYC: () -> Unit = {}
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(TransactTab.SEND) }
    var hasSubmittedKYC by remember { mutableStateOf(true) } // Will be checked in LaunchedEffect

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transact") }
            )
        }
    ) { paddingValues ->
        if (transactionUiState.isConfirming) {
            TransactionConfirmationDialog(
                amount = transactionUiState.confirmationAmount,
                recipient = transactionUiState.confirmationRecipient,
                onConfirm = { password ->
                    transactionViewModel.confirmTransaction(password)
                },
                onDismiss = {
                    transactionViewModel.cancelTransaction()
                },
                isLoading = transactionUiState.isLoading,
                errorMessage = transactionUiState.confirmationError
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                TransactTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    TransactTab.SEND -> "Send Money"
                                    TransactTab.RECEIVE -> "Receive"
                                    TransactTab.BILLS -> "Pay Bills"
                                    TransactTab.INVEST -> "Invest"
                                    TransactTab.WITHDRAW -> "Withdraw"
                                }
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                TransactTab.SEND -> SendMoneyTab(transactionViewModel, transactionUiState)
                TransactTab.RECEIVE -> ReceiveMoneyTab(transactionViewModel, transactionUiState)
                TransactTab.BILLS -> PayBillsTab(transactionViewModel, transactionUiState) { bill ->
                    transactionViewModel.initiateTransaction(
                        amount = bill.amount,
                        recipient = bill.providerName
                    ) {
                        transactionViewModel.payBill(bill.id)
                    }
                }
                TransactTab.INVEST -> InvestTab(transactionViewModel, transactionUiState)
                TransactTab.WITHDRAW -> WithdrawTab(transactionViewModel, transactionUiState)
            }
        }
    }
}

@Composable
private fun SendMoneyTab(
    viewModel: TransactionViewModel,
    uiState: TransactionUiState
) {
    var transferType by remember { mutableStateOf(TransferType.ACCOUNT_TRANSFER) }
    var recipientAccount by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showPaymentMethodScreen by remember { mutableStateOf(false) }

    if (showPaymentMethodScreen) {
        PaymentMethodScreen(
            onPaymentMethodSelected = { paymentMethod ->
                selectedPaymentMethod = paymentMethod
                showPaymentMethodScreen = false
            },
            onNavigateBack = {
                showPaymentMethodScreen = false
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Send Money",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Transfer Type Selection
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Transfer Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                onClick = {
                                    transferType = TransferType.ACCOUNT_TRANSFER
                                    // Clear mobile-specific fields
                                    mobileNumber = ""
                                    selectedCountry = null
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Account Transfer")
                                    }
                                },
                                selected = transferType == TransferType.ACCOUNT_TRANSFER,
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                onClick = {
                                    transferType = TransferType.MOBILE_MONEY
                                    // Clear account-specific fields
                                    recipientAccount = ""
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mobile Money")
                                    }
                                },
                                selected = transferType == TransferType.MOBILE_MONEY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Transfer type specific fields
                        when (transferType) {
                            TransferType.ACCOUNT_TRANSFER -> {
                                OutlinedTextField(
                                    value = recipientAccount,
                                    onValueChange = {
                                        recipientAccount = it
                                        viewModel.clearMessages()
                                    },
                                    label = { Text("Account Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = { Text("Enter recipient's bank account number") },
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                                    }
                                )
                            }

                            TransferType.MOBILE_MONEY -> {
                                Text(
                                    text = "Recipient Mobile Number",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                PhoneNumberInput(
                                    phoneNumber = mobileNumber,
                                    onPhoneNumberChange = {
                                        mobileNumber = it
                                        viewModel.clearMessages()
                                    },
                                    selectedCountry = selectedCountry,
                                    onCountrySelected = { country ->
                                        selectedCountry = country
                                        viewModel.clearMessages()
                                    },
                                    enabled = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Select Recipient's Mobile Money Provider",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedButton(
                                    onClick = { showPaymentMethodScreen = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = selectedPaymentMethod?.let { getPaymentMethodName(it) }
                                                    ?: "Choose provider (M-Pesa, Airtel, etc.)",
                                                fontWeight = FontWeight.Medium
                                            )
                                            selectedPaymentMethod?.let { method ->
                                                Text(
                                                    text = getPaymentMethodDescription(method),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        selectedPaymentMethod?.let { method ->
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        color = when (method) {
                                                            PaymentMethod.MPESA -> Color(0xFF00C853)
                                                            PaymentMethod.AIRTEL -> Color(0xFFE53935)
                                                            PaymentMethod.CREDIT_CARD -> Color(0xFF1976D2)
                                                            PaymentMethod.DEBIT_CARD -> Color(0xFF7B1FA2)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(
                                                        when (method) {
                                                            PaymentMethod.MPESA -> Res.drawable.mpesa
                                                            PaymentMethod.AIRTEL -> Res.drawable.airtel_money
                                                            PaymentMethod.CREDIT_CARD -> Res.drawable.AxioBank
                                                            PaymentMethod.DEBIT_CARD -> Res.drawable.AxioBank
                                                        }
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } ?: Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null)
                            },
                            supportingText = { Text("Money will be sent from your main wallet") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.Notes, contentDescription = null)
                            },
                            supportingText = { Text("Add a note for this transfer") }
                        )
                    }
                }
            }

            // Messages
            item {
                MessageCards(uiState)
            }

            item {
                val isFormValid = when (transferType) {
                    TransferType.ACCOUNT_TRANSFER -> {
                        recipientAccount.isNotEmpty() &&
                        amount.isNotEmpty()
                    }
                    TransferType.MOBILE_MONEY -> {
                        mobileNumber.isNotEmpty() &&
                        selectedCountry != null &&
                        amount.isNotEmpty() &&
                        selectedPaymentMethod != null
                    }
                }

                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0

                        viewModel.initiateTransaction(
                            amount = amountDouble,
                            recipient = if (transferType == TransferType.ACCOUNT_TRANSFER) recipientAccount else mobileNumber
                        ) {
                            when (transferType) {
                                TransferType.ACCOUNT_TRANSFER -> {
                                    viewModel.sendMoneyToAccount(
                                        recipientName = "Account Holder",
                                        recipientAccount = recipientAccount,
                                        amount = amountDouble,
                                        description = description.ifEmpty { "Account transfer to $recipientAccount" }
                                    )
                                }
                                TransferType.MOBILE_MONEY -> {
                                    val fullPhoneNumber = if (selectedCountry != null) {
                                        "${selectedCountry!!.dialCode}$mobileNumber"
                                    } else {
                                        mobileNumber
                                    }

                                    selectedPaymentMethod?.let { paymentMethod ->
                                        viewModel.sendMoneyToMobile(
                                            recipientName = "Mobile User",
                                            mobileNumber = fullPhoneNumber,
                                            amount = amountDouble,
                                            description = description.ifEmpty { "Mobile money transfer via ${getPaymentMethodName(paymentMethod)}" },
                                            paymentMethod = paymentMethod,
                                            country = selectedCountry?.name ?: "Unknown"
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = !uiState.isLoading && isFormValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (transferType) {
                            TransferType.ACCOUNT_TRANSFER -> "Send to Account"
                            TransferType.MOBILE_MONEY -> "Send via Mobile Money"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiveMoneyTab(
    viewModel: TransactionViewModel,
    uiState: TransactionUiState
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Receive Money",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Expected Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Messages
        item {
            MessageCards(uiState)
        }

        item {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    viewModel.receiveMoney(amountDouble, description)
                },
                enabled = !uiState.isLoading && amount.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generate Receive Request")
            }
        }
    }
}

enum class PaymentMode {
    PAYBILL, BUY_GOODS
}

@Composable
private fun PayBillsTab(
    viewModel: TransactionViewModel,
    uiState: TransactionUiState,
    onPayBill: (BillPayment) -> Unit
) {
    var paybillNumber by remember { mutableStateOf("") }
    var tillNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accountReference by remember { mutableStateOf("") }
    var selectedBillType by remember { mutableStateOf(BillType.ELECTRICITY) }
    var showBillTypeDropdown by remember { mutableStateOf(false) }
    var showManualPayment by remember { mutableStateOf(false) }
    var paymentMode by remember { mutableStateOf(PaymentMode.PAYBILL) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pay Bills",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Toggle between saved bills and manual payment
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Payment Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { showManualPayment = false },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Saved Bills")
                                }
                            },
                            selected = !showManualPayment,
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            onClick = { showManualPayment = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enter Details")
                                }
                            },
                            selected = showManualPayment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (showManualPayment) {
            // Manual Bill Payment Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Enter Bill Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Bill Type Dropdown
                        BillTypeDropdown(
                            selectedType = selectedBillType,
                            onTypeSelected = { selectedBillType = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = paybillNumber,
                            onValueChange = {
                                paybillNumber = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Paybill Number") },
                            placeholder = { Text("Enter paybill or business number") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Enter the paybill number for the service provider") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("e.g., Account number, reference") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Add account number or reference if required") }
                        )
                    }
                }
            }

            // Messages
            item {
                MessageCards(uiState)
            }

            item {
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        viewModel.initiateTransaction(
                            amount = amountDouble,
                            recipient = paybillNumber
                        ) {
                            viewModel.payManualBill(
                                paybillNumber = paybillNumber,
                                amount = amountDouble,
                                billType = selectedBillType,
                                description = description.ifEmpty { "Payment to $paybillNumber" }
                            )
                        }
                    },
                    enabled = !uiState.isLoading && paybillNumber.isNotEmpty() && amount.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Pay Bill")
                }
            }
        } else {
            // Saved Bills Section
            item {
                MessageCards(uiState)
            }

            if (uiState.billPayments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Saved Bills",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You don't have any saved bills. Use 'Enter Details' to pay bills manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { showManualPayment = true }
                            ) {
                                Text("Enter Bill Details")
                            }
                        }
                    }
                }
            } else {
                items(uiState.billPayments) { bill ->
                    BillPaymentCard(
                        bill = bill,
                        onPayBill = { onPayBill(bill) },
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestTab(
    viewModel: TransactionViewModel,
    uiState: TransactionUiState
) {
    var selectedType by remember { mutableStateOf(InvestmentType.STOCKS) }
    var investmentName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Invest",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Current Investments
        if (uiState.investments.isNotEmpty()) {
            item {
                Text(
                    text = "Your Investments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.investments) { investment ->
                InvestmentCard(investment = investment)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Make New Investment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Investment Type Dropdown
                    InvestmentTypeDropdown(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = investmentName,
                        onValueChange = {
                            investmentName = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Investment Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Messages
        item {
            MessageCards(uiState)
        }

        item {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    viewModel.initiateTransaction(
                        amount = amountDouble,
                        recipient = investmentName
                    ) {
                        viewModel.makeInvestment(selectedType, investmentName, amountDouble)
                    }
                },
                enabled = !uiState.isLoading && investmentName.isNotEmpty() && amount.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Make Investment")
            }
        }
    }
}

@Composable
private fun WithdrawTab(
    viewModel: TransactionViewModel,
    uiState: TransactionUiState
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showPaymentMethodScreen by remember { mutableStateOf(false) }

    if (showPaymentMethodScreen) {
        PaymentMethodScreen(
            onPaymentMethodSelected = { paymentMethod ->
                selectedPaymentMethod = paymentMethod
                showPaymentMethodScreen = false
            },
            onNavigateBack = {
                showPaymentMethodScreen = false
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Withdraw Money",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                uiState.walletBalance?.let { balance ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Available Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${balance.availableBalance}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showPaymentMethodScreen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedPaymentMethod?.let { getPaymentMethodName(it) }
                                            ?: "Select Payment Method",
                                        fontWeight = FontWeight.Medium
                                    )
                                    selectedPaymentMethod?.let { method ->
                                        Text(
                                            text = getPaymentMethodDescription(method),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                selectedPaymentMethod?.let { method ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = when (method) {
                                                    PaymentMethod.MPESA -> Color(0xFF00C853)
                                                    PaymentMethod.AIRTEL -> Color(0xFFE53935)
                                                    PaymentMethod.CREDIT_CARD -> Color(0xFF1976D2)
                                                    PaymentMethod.DEBIT_CARD -> Color(0xFF7B1FA2)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(
                                                when (method) {
                                                    PaymentMethod.MPESA -> Res.drawable.mpesa
                                                    PaymentMethod.AIRTEL -> Res.drawable.airtel_money
                                                    PaymentMethod.CREDIT_CARD -> Res.drawable.AxioBank
                                                    PaymentMethod.DEBIT_CARD -> Res.drawable.AxioBank
                                                }
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } ?: Text("→")
                            }
                        }

                        // Show phone number field only when M-Pesa is selected
                        if (selectedPaymentMethod == PaymentMethod.MPESA) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    viewModel.clearMessages()
                                },
                                label = { Text("M-Pesa Phone Number") },
                                placeholder = { Text("254712345678") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    Text("Enter the phone number to receive money", style = MaterialTheme.typography.bodySmall)
                                }
                            )
                        }
                    }
                }
            }

            // Messages
            item {
                MessageCards(uiState)
            }

            item {
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        viewModel.initiateTransaction(
                            amount = amountDouble,
                            recipient = if (selectedPaymentMethod == PaymentMethod.MPESA) phoneNumber else getPaymentMethodName(selectedPaymentMethod!!)
                        ) {
                            selectedPaymentMethod?.let { paymentMethod ->
                                viewModel.withdraw(amountDouble, description, paymentMethod, phoneNumber.ifBlank { null })
                            }
                        }
                    },
                    enabled = !uiState.isLoading && amount.isNotEmpty() && selectedPaymentMethod != null &&
                            (selectedPaymentMethod != PaymentMethod.MPESA || phoneNumber.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Withdraw")
                }
            }
        }
    }
}

@Composable
private fun BillPaymentCard(
    bill: BillPayment,
    onPayBill: () -> Unit,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = bill.billType.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Due: ${bill.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${bill.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (bill.status == PaymentStatus.SCHEDULED) {
                        Button(
                            onClick = onPayBill,
                            enabled = !isLoading,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Pay Now")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = bill.status.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentCard(investment: Investment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = investment.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    investment.symbol?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${investment.currentValue}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (investment.returnPercentage >= 0) "+" else ""}${investment.returnPercentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (investment.returnPercentage >= 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCards(uiState: TransactionUiState) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvestmentTypeDropdown(
    selectedType: InvestmentType,
    onTypeSelected: (InvestmentType) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = showDropdown,
        onExpandedChange = { showDropdown = it }
    ) {
        OutlinedTextField(
            value = selectedType.name.replace('_', ' '),
            onValueChange = { },
            readOnly = true,
            label = { Text("Investment Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false }
        ) {
            InvestmentType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace('_', ' ')) },
                    onClick = {
                        onTypeSelected(type)
                        showDropdown = false
                    }
                )
            }
        }
    }
}