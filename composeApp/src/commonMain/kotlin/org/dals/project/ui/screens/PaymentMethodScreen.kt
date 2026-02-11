package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import decentralizedaccessloan.composeapp.generated.resources.*
import org.dals.project.model.PaymentMethod
import org.dals.project.utils.getPaymentMethodName
import org.dals.project.utils.getPaymentMethodDescription
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.ui.components.PhoneNumberInput
import org.dals.project.ui.components.Country
import org.dals.project.ui.components.countries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Payment Method") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
            Text(
                text = "Choose how you want to send money",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Select your preferred mobile money service",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mpesa Option
            PaymentMethodCard(
                title = getPaymentMethodName(PaymentMethod.MPESA),
                subtitle = getPaymentMethodDescription(PaymentMethod.MPESA),
                image = painterResource(Res.drawable.mpesa),
                backgroundColor = Color(0xFF00C853), // M-Pesa green
                textColor = Color.White,
                onClick = { onPaymentMethodSelected(PaymentMethod.MPESA) }
            )

            // Airtel Option
            PaymentMethodCard(
                title = getPaymentMethodName(PaymentMethod.AIRTEL),
                subtitle = getPaymentMethodDescription(PaymentMethod.AIRTEL),
                image = painterResource(Res.drawable.airtel_money),
                backgroundColor = Color(0xFFE53935), // Airtel red
                textColor = Color.White,
                onClick = { onPaymentMethodSelected(PaymentMethod.AIRTEL) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToManageCards: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    // Mock payment methods - in a real app, this would come from the ViewModel
    val paymentMethods = remember {
        mutableStateListOf(
            SavedPaymentMethod("M-PESA", "254712345678", PaymentMethod.MPESA, true),
            SavedPaymentMethod("Airtel Money", "254798765432", PaymentMethod.AIRTEL, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMethodDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Payment Method")
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
            // Header
            item {
                PaymentMethodsHeader()
            }

            // Cards Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToManageCards() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Payment Cards",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Manage your credit & debit cards",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Payment Methods List
            item {
                Text(
                    text = "Mobile Money",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (paymentMethods.isEmpty()) {
                item {
                    EmptyPaymentMethodsCard(
                        onAddMethod = { showAddMethodDialog = true }
                    )
                }
            } else {
                items(paymentMethods) { method ->
                    SavedPaymentMethodCard(
                        method = method,
                        onEdit = { 
                            // TODO: Open edit dialog
                        },
                        onDelete = { 
                            showDeleteDialog = method.id
                        },
                        onSetDefault = {
                            val index = paymentMethods.indexOf(method)
                            paymentMethods.forEachIndexed { i, pm ->
                                paymentMethods[i] = pm.copy(isDefault = i == index)
                            }
                        }
                    )
                }
            }

            // Add Method Button
            item {
                OutlinedButton(
                    onClick = { showAddMethodDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Payment Method")
                }
            }
        }
    }

    // Add Payment Method Dialog
    if (showAddMethodDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddMethodDialog = false },
            onAdd = { name, number, type ->
                paymentMethods.add(
                    SavedPaymentMethod(
                        name = name,
                        number = number,
                        type = type,
                        isDefault = paymentMethods.isEmpty()
                    )
                )
                showAddMethodDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { methodId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Payment Method") },
            text = { Text("Are you sure you want to delete this payment method?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val indexToRemove = paymentMethods.indexOfFirst { it.id == methodId }
                        if (indexToRemove >= 0) {
                            paymentMethods.removeAt(indexToRemove)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodsHeader() {
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
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Payment Methods",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Manage your saved payment methods for quick transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SavedPaymentMethodCard(
    method: SavedPaymentMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (method.isDefault) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (method.isDefault) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method Icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = when (method.type) {
                                PaymentMethod.MPESA -> Color(0xFF00C853)
                                PaymentMethod.AIRTEL -> Color(0xFFE53935)
                                PaymentMethod.CREDIT_CARD -> Color(0xFF1976D2)
                                PaymentMethod.DEBIT_CARD -> Color(0xFF7B1FA2)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            when (method.type) {
                                PaymentMethod.MPESA -> Res.drawable.mpesa
                                PaymentMethod.AIRTEL -> Res.drawable.airtel_money
                                PaymentMethod.CREDIT_CARD -> Res.drawable.AxioBank // Using AxioBank logo for cards as placeholder
                                PaymentMethod.DEBIT_CARD -> Res.drawable.AxioBank
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Payment Method Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = method.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (method.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Default",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = "****${method.number.takeLast(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!method.isDefault) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Default")
                }
            }
        }
    }
}

@Composable
private fun EmptyPaymentMethodsCard(
    onAddMethod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Payment Methods",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Add a payment method to make transactions easier",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAddMethod) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Payment Method")
            }
        }
    }
}

@Composable
private fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, PaymentMethod) -> Unit
) {
    var selectedType by remember { mutableStateOf(PaymentMethod.MPESA) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var methodName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment Method") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Method Type Selection
                Text(
                    text = "Select Payment Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = PaymentMethod.MPESA },
                        label = { Text("M-PESA") },
                        selected = selectedType == PaymentMethod.MPESA
                    )
                    FilterChip(
                        onClick = { selectedType = PaymentMethod.AIRTEL },
                        label = { Text("Airtel Money") },
                        selected = selectedType == PaymentMethod.AIRTEL
                    )
                }

                // Method Name
                OutlinedTextField(
                    value = methodName,
                    onValueChange = { methodName = it },
                    label = { Text("Method Name") },
                    placeholder = { Text("e.g., Personal M-PESA") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Number with Country Picker
                PhoneNumberInput(
                    phoneNumber = phoneNumber,
                    onPhoneNumberChange = { phoneNumber = it },
                    selectedCountry = selectedCountry,
                    onCountrySelected = { country ->
                        selectedCountry = country
                    },
                    enabled = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (methodName.isNotEmpty() && phoneNumber.isNotEmpty() && selectedCountry != null) {
                        val fullPhoneNumber = "${selectedCountry!!.dialCode}$phoneNumber"
                        onAdd(methodName, fullPhoneNumber, selectedType)
                    }
                },
                enabled = methodName.isNotEmpty() && phoneNumber.isNotEmpty() && selectedCountry != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Data class for saved payment methods
data class SavedPaymentMethod(
    val name: String,
    val number: String,
    val type: PaymentMethod,
    val isDefault: Boolean,
    val id: String = "${type.name}_${number.hashCode()}"
)

@Composable
private fun PaymentMethodCard(
    title: String,
    subtitle: String,
    image: androidx.compose.ui.graphics.painter.Painter? = null,
    icon: String = "",
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon/Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        painter = image,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.displaySmall,
                        color = textColor
                    )
                }
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Arrow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}