package org.dals.project.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.repository.*
import org.dals.project.viewmodel.BeneficiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryManagementScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    viewModel: BeneficiaryViewModel = viewModel { BeneficiaryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(customerId) {
        viewModel.loadBeneficiaries(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beneficiaries") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "Add Beneficiary")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Beneficiary")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.searchBeneficiaries(customerId, it) },
                onClearSearch = { viewModel.clearSearchQuery() }
            )

            // Filter Chips
            FilterChips(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.filterByType(it) }
            )

            // Favorites Section
            if (uiState.favorites.isNotEmpty() && uiState.searchQuery.isBlank()) {
                FavoritesSection(
                    favorites = uiState.favorites,
                    onBeneficiaryClick = { /* Navigate to transfer */ },
                    onToggleFavorite = { viewModel.toggleFavorite(customerId, it) }
                )
            }

            // Beneficiaries List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error!!,
                            onRetry = { viewModel.loadBeneficiaries(customerId) },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                    viewModel.getFilteredBeneficiaries().isEmpty() -> {
                        EmptyState(onAddClick = { viewModel.showAddDialog() })
                    }
                    else -> {
                        BeneficiariesList(
                            beneficiaries = viewModel.getFilteredBeneficiaries(),
                            onBeneficiaryClick = { /* Navigate to transfer */ },
                            onToggleFavorite = { viewModel.toggleFavorite(customerId, it) },
                            onEdit = { viewModel.startEdit(it) },
                            onDelete = { viewModel.deleteBeneficiary(customerId, it) }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (uiState.showAddDialog) {
            AddBeneficiaryDialog(
                customerId = customerId,
                viewModel = viewModel,
                onDismiss = { viewModel.hideAddDialog() }
            )
        }

        if (uiState.editingBeneficiary != null) {
            EditBeneficiaryDialog(
                customerId = customerId,
                beneficiary = uiState.editingBeneficiary!!,
                viewModel = viewModel,
                onDismiss = { viewModel.cancelEdit() }
            )
        }

        // Success Snackbar
        if (uiState.successMessage != null) {
            LaunchedEffect(uiState.successMessage) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccess()
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search beneficiaries...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == "ALL",
            onClick = { onFilterSelected("ALL") },
            label = { Text("All") },
            leadingIcon = if (selectedFilter == "ALL") {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == "BANK",
            onClick = { onFilterSelected("BANK") },
            label = { Text("Bank") },
            leadingIcon = if (selectedFilter == "BANK") {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == "MOBILE",
            onClick = { onFilterSelected("MOBILE") },
            label = { Text("Mobile") },
            leadingIcon = if (selectedFilter == "MOBILE") {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = selectedFilter == "INTERNAL",
            onClick = { onFilterSelected("INTERNAL") },
            label = { Text("Internal") },
            leadingIcon = if (selectedFilter == "INTERNAL") {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun FavoritesSection(
    favorites: List<BeneficiaryRecord>,
    onBeneficiaryClick: (BeneficiaryRecord) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            favorites.forEach { beneficiary ->
                FavoriteCard(
                    beneficiary = beneficiary,
                    onClick = { onBeneficiaryClick(beneficiary) },
                    onToggleFavorite = { onToggleFavorite(beneficiary.id) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
    }
}

@Composable
private fun FavoriteCard(
    beneficiary: BeneficiaryRecord,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = beneficiary.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clickable(onClick = onToggleFavorite),
                    tint = Color(0xFFFFC107)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = beneficiary.nickname ?: beneficiary.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BeneficiariesList(
    beneficiaries: List<BeneficiaryRecord>,
    onBeneficiaryClick: (BeneficiaryRecord) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEdit: (BeneficiaryRecord) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(beneficiaries) { beneficiary ->
            BeneficiaryCard(
                beneficiary = beneficiary,
                onClick = { onBeneficiaryClick(beneficiary) },
                onToggleFavorite = { onToggleFavorite(beneficiary.id) },
                onEdit = { onEdit(beneficiary) },
                onDelete = { onDelete(beneficiary.id) }
            )
        }
    }
}

@Composable
private fun BeneficiaryCard(
    beneficiary: BeneficiaryRecord,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (beneficiary.type) {
                            "BANK" -> MaterialTheme.colorScheme.primaryContainer
                            "MOBILE" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = beneficiary.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = when (beneficiary.type) {
                        "BANK" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "MOBILE" -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = beneficiary.nickname ?: beneficiary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (beneficiary.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = when (beneficiary.type) {
                        "BANK" -> "${beneficiary.bankName} - ${beneficiary.accountNumber}"
                        "MOBILE" -> beneficiary.phoneNumber ?: beneficiary.accountNumber ?: ""
                        else -> beneficiary.accountNumber ?: ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Chip(text = beneficiary.type, color = MaterialTheme.colorScheme.surfaceVariant)
                    Text(
                        text = "• ${beneficiary.transferCount} transfers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (beneficiary.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (beneficiary.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (beneficiary.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PersonAddAlt1,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Beneficiaries Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add beneficiaries for quick and easy transfers",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Beneficiary")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun AddBeneficiaryDialog(
    customerId: String,
    viewModel: BeneficiaryViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BANK") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Beneficiary") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Beneficiary Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == "BANK",
                            onClick = { type = "BANK" },
                            label = { Text("Bank") }
                        )
                        FilterChip(
                            selected = type == "MOBILE",
                            onClick = { type = "MOBILE" },
                            label = { Text("Mobile") }
                        )
                        FilterChip(
                            selected = type == "INTERNAL",
                            onClick = { type = "INTERNAL" },
                            label = { Text("Internal") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text(if (type == "MOBILE") "Phone Number *" else "Account Number *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (type == "BANK") {
                    item {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (type != "MOBILE") {
                    item {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.createBeneficiary(
                        customerId,
                        CreateBeneficiaryRequest(
                            customerId = customerId,
                            name = name,
                            nickname = nickname.ifBlank { null },
                            accountNumber = accountNumber.ifBlank { null },
                            bankName = bankName.ifBlank { null },
                            phoneNumber = phoneNumber.ifBlank { null },
                            email = email.ifBlank { null },
                            type = type
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = name.isNotBlank() && accountNumber.isNotBlank()
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

@Composable
private fun EditBeneficiaryDialog(
    customerId: String,
    beneficiary: BeneficiaryRecord,
    viewModel: BeneficiaryViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(beneficiary.name) }
    var nickname by remember { mutableStateOf(beneficiary.nickname ?: "") }
    var accountNumber by remember { mutableStateOf(beneficiary.accountNumber ?: "") }
    var bankName by remember { mutableStateOf(beneficiary.bankName ?: "") }
    var phoneNumber by remember { mutableStateOf(beneficiary.phoneNumber ?: "") }
    var email by remember { mutableStateOf(beneficiary.email ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Beneficiary") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Nickname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (beneficiary.type == "BANK") {
                    item {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateBeneficiary(
                        customerId,
                        beneficiary.id,
                        UpdateBeneficiaryRequest(
                            name = if (name != beneficiary.name) name else null,
                            nickname = if (nickname != beneficiary.nickname) nickname else null,
                            accountNumber = if (accountNumber != beneficiary.accountNumber) accountNumber else null,
                            bankName = if (bankName != beneficiary.bankName) bankName else null,
                            phoneNumber = if (phoneNumber != beneficiary.phoneNumber) phoneNumber else null,
                            email = if (email != beneficiary.email) email else null
                        ),
                        onSuccess = onDismiss
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
