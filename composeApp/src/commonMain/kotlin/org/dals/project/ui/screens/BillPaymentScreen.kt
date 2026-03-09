package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.repository.BillVendor
import org.dals.project.repository.PayBillRequest
import org.dals.project.repository.SavedBiller
import org.dals.project.viewmodel.BillPaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPaymentScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: BillPaymentViewModel = viewModel { BillPaymentViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedVendor by remember { mutableStateOf<BillVendor?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadSavedBillers(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pay Bills") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "Payment History")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search vendors...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { viewModel.filterByCategory(null) },
                            label = { Text("All") }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.filterByCategory(category) },
                            label = { Text(category.replace("_", " ")) }
                        )
                    }
                }
            }

            // Saved billers section
            if (uiState.savedBillers.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Billers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.savedBillers) { biller ->
                    SavedBillerCard(
                        biller = biller,
                        onClick = {
                            // Find the vendor and show payment dialog
                            val vendor = uiState.vendors.find { it.id == biller.vendorId }
                            if (vendor != null) {
                                selectedVendor = vendor
                                showPaymentDialog = true
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // All vendors section
            item {
                Text(
                    text = "All Billers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isLoading) {
                items(5) {
                    VendorCardSkeleton()
                }
            } else {
                items(viewModel.getFilteredVendors()) { vendor ->
                    VendorCard(
                        vendor = vendor,
                        onClick = {
                            selectedVendor = vendor
                            showPaymentDialog = true
                        }
                    )
                }
            }

            if (!uiState.isLoading && viewModel.getFilteredVendors().isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No billers found",
                        message = "Try adjusting your search or category filter"
                    )
                }
            }
        }
    }

    // Payment dialog
    if (showPaymentDialog && selectedVendor != null) {
        PaymentDialog(
            vendor = selectedVendor!!,
            userId = userId,
            viewModel = viewModel,
            onDismiss = {
                showPaymentDialog = false
                selectedVendor = null
            }
        )
    }

    // Error snackbar
    if (uiState.error != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.error!!)
        }
    }

    // Success snackbar
    if (uiState.successMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            action = {
                TextButton(onClick = { viewModel.clearSuccess() }) {
                    Text("OK")
                }
            }
        ) {
            Text(uiState.successMessage!!)
        }
    }
}

@Composable
private fun VendorCard(vendor: BillVendor, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (vendor.category) {
                        "ELECTRICITY" -> Icons.Default.Lightbulb
                        "WATER" -> Icons.Default.WaterDrop
                        "INTERNET", "CABLE_TV" -> Icons.Default.Wifi
                        "MOBILE_AIRTIME" -> Icons.Default.Phone
                        "INSURANCE" -> Icons.Default.Shield
                        "EDUCATION" -> Icons.Default.School
                        "GOVERNMENT" -> Icons.Default.AccountBalance
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vendor.vendorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vendor.category.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (vendor.description != null) {
                    Text(
                        text = vendor.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SavedBillerCard(biller: SavedBiller, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = biller.nickname ?: biller.vendorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = biller.accountNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            OutlinedButton(onClick = onClick) {
                Text("Pay")
            }
        }
    }
}

@Composable
private fun VendorCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    vendor: BillVendor,
    userId: String,
    viewModel: BillPaymentViewModel,
    onDismiss: () -> Unit
) {
    var accountNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saveBiller by remember { mutableStateOf(false) }
    var billerNickname by remember { mutableStateOf("") }

    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val processingFee = viewModel.calculateProcessingFee(vendor, amountDouble)
    val totalAmount = amountDouble + processingFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${vendor.vendorName}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text(vendor.accountNumberLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            Text("Min: ${vendor.minAmount}, Max: ${vendor.maxAmount}")
                        },
                        singleLine = true
                    )
                }

                if (amountDouble > 0) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                                    Text("$${String.format("%.2f", amountDouble)}", fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Fee:", style = MaterialTheme.typography.bodyMedium)
                                    Text("$${String.format("%.2f", processingFee)}", fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("$${String.format("%.2f", totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saveBiller,
                            onCheckedChange = { saveBiller = it }
                        )
                        Text("Save this biller for future payments")
                    }
                }

                if (saveBiller) {
                    item {
                        OutlinedTextField(
                            value = billerNickname,
                            onValueChange = { billerNickname = it },
                            label = { Text("Nickname (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val request = PayBillRequest(
                        userId = userId,
                        vendorId = vendor.id,
                        accountNumber = accountNumber,
                        amount = amountDouble,
                        description = description.ifBlank { null },
                        saveBiller = saveBiller,
                        billerNickname = billerNickname.ifBlank { null }
                    )
                    viewModel.payBill(request) {
                        onDismiss()
                    }
                },
                enabled = accountNumber.isNotBlank() && amountDouble >= vendor.minAmount && amountDouble <= vendor.maxAmount
            ) {
                Text("Pay $${String.format("%.2f", totalAmount)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
