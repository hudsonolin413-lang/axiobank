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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.Transaction
import org.dals.project.model.TransactionCategory
import org.dals.project.model.TransactionType
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.ui.components.PieChart3D
import org.dals.project.ui.components.PieChartData

data class BudgetCategory(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val budgetAmount: Double,
    val spentAmount: Double,
    val color: Color,
    val category: TransactionCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    
    // Calculate actual spending from transactions
    val spendingByCategory = remember(transactionUiState.transactions) {
        calculateSpendingByCategory(transactionUiState.transactions)
    }
    
    // Budget definitions with actual spending - Using Material Icons with color theme
    var budgets by remember(spendingByCategory) {
        mutableStateOf(listOf(
            BudgetCategory("1", "Food & Dining", Icons.Filled.Restaurant, 500.0, spendingByCategory[TransactionCategory.FOOD] ?: 0.0, Color(0xFFF44336), TransactionCategory.FOOD), // Red
            BudgetCategory("2", "Shopping", Icons.Filled.ShoppingCart, 300.0, spendingByCategory[TransactionCategory.SHOPPING] ?: 0.0, Color(0xFFFF5722), TransactionCategory.SHOPPING), // Deep Orange/Red
            BudgetCategory("3", "Transportation", Icons.Filled.DirectionsCar, 200.0, spendingByCategory[TransactionCategory.TRANSPORT] ?: 0.0, Color(0xFF03A9F4), TransactionCategory.TRANSPORT), // Light Blue
            BudgetCategory("4", "Bills & Utilities", Icons.Filled.Receipt, 400.0, spendingByCategory[TransactionCategory.BILLS] ?: 0.0 + (spendingByCategory[TransactionCategory.UTILITIES] ?: 0.0), Color(0xFFFFA000), TransactionCategory.BILLS), // Amber/Yellow
            BudgetCategory("5", "Transfers", Icons.Filled.SwapHoriz, 1000.0, spendingByCategory[TransactionCategory.TRANSFER] ?: 0.0, Color(0xFFFFC107), TransactionCategory.TRANSFER), // Yellow
            BudgetCategory("6", "Other", Icons.Filled.Category, 200.0, spendingByCategory[TransactionCategory.OTHER] ?: 0.0, Color(0xFF9E9E9E), TransactionCategory.OTHER) // Grey
        ))
    }
    
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetCategory?>(null) }

    val totalBudget = budgets.sumOf { it.budgetAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val remainingBudget = totalBudget - totalSpent

    // Refresh data on load
    LaunchedEffect(Unit) {
        transactionViewModel.refreshAllData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { transactionViewModel.refreshAllData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showAddBudgetDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Budget")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (transactionUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Monthly Budget Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BudgetSummaryItem(
                                    label = "Total Budget",
                                    amount = totalBudget,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                BudgetSummaryItem(
                                    label = "Spent",
                                    amount = totalSpent,
                                    color = MaterialTheme.colorScheme.error
                                )
                                BudgetSummaryItem(
                                    label = "Remaining",
                                    amount = remainingBudget,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Overall progress bar
                            val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = if (overallProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${(overallProgress * 100).toInt()}% of total budget used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // 3D Pie Chart - Budget Overview
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Budget vs Spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // 3D Pie Chart showing spending
                            PieChart3D(
                                data = budgets.filter { it.spentAmount > 0 }.map { budget ->
                                    PieChartData(
                                        label = budget.name,
                                        value = budget.spentAmount.toFloat(),
                                        color = budget.color
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                chartSize = 300.dp,
                                depth3D = 35f,
                                showLegend = true,
                                animate = true
                            )
                        }
                    }
                }

                // Budget Categories
                item {
                    Text(
                        text = "Budget Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(budgets) { budget ->
                    BudgetCategoryCard(
                        budget = budget,
                        onEdit = { editingBudget = budget }
                    )
                }

                // Tips Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Budget Tips",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val overBudgetCategories = budgets.filter { it.spentAmount > it.budgetAmount }
                            val nearBudgetCategories = budgets.filter { it.spentAmount > it.budgetAmount * 0.8 && it.spentAmount <= it.budgetAmount }
                            
                            if (overBudgetCategories.isNotEmpty()) {
                                TipItem(
                                    icon = Icons.Filled.Warning,
                                    iconColor = Color(0xFFF44336), // Red
                                    text = "${overBudgetCategories.size} categories are over budget"
                                )
                            }
                            if (nearBudgetCategories.isNotEmpty()) {
                                TipItem(
                                    icon = Icons.Filled.BarChart,
                                    iconColor = Color(0xFFFFA000), // Yellow/Amber
                                    text = "${nearBudgetCategories.size} categories are near their limit"
                                )
                            }
                            if (totalSpent < totalBudget * 0.5) {
                                TipItem(
                                    icon = Icons.Filled.CheckCircle,
                                    iconColor = Color(0xFF4CAF50), // Green
                                    text = "Great job! You're well under budget this month"
                                )
                            }
                            TipItem(
                                icon = Icons.Filled.Lightbulb,
                                iconColor = Color(0xFFFFC107), // Yellow
                                text = "Review your spending weekly to stay on track"
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Budget Dialog
    editingBudget?.let { budget ->
        EditBudgetDialog(
            budget = budget,
            onDismiss = { editingBudget = null },
            onSave = { newAmount ->
                budgets = budgets.map { 
                    if (it.id == budget.id) it.copy(budgetAmount = newAmount) else it 
                }
                editingBudget = null
            }
        )
    }

    // Add Budget Dialog
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onAdd = { name, amount, icon, color, category ->
                budgets = budgets + BudgetCategory(
                    id = (budgets.size + 1).toString(),
                    name = name,
                    icon = icon,
                    budgetAmount = amount,
                    spentAmount = spendingByCategory[category] ?: 0.0,
                    color = color,
                    category = category
                )
                showAddBudgetDialog = false
            }
        )
    }
}

private fun calculateSpendingByCategory(transactions: List<Transaction>): Map<TransactionCategory, Double> {
    val outgoingTypes = listOf(TransactionType.SEND, TransactionType.BILL_PAYMENT, TransactionType.WITHDRAWAL, TransactionType.RENT_PAYMENT, TransactionType.LOAN_PAYMENT)
    return transactions
        .filter { it.type in outgoingTypes }
        .groupBy { it.category }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
}

@Composable
private fun BudgetSummaryItem(
    label: String,
    amount: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetCategoryCard(
    budget: BudgetCategory,
    onEdit: () -> Unit
) {
    val progress = if (budget.budgetAmount > 0) (budget.spentAmount / budget.budgetAmount).toFloat().coerceIn(0f, 1.5f) else 0f
    val isOverBudget = budget.spentAmount > budget.budgetAmount
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(budget.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = budget.icon,
                            contentDescription = null,
                            tint = budget.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$${String.format("%.2f", budget.spentAmount)} of $${String.format("%.2f", budget.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverBudget) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Over",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else budget.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt().coerceAtMost(100)}% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOverBudget) 
                        "-$${String.format("%.2f", budget.spentAmount - budget.budgetAmount)} over" 
                    else 
                        "$${String.format("%.2f", budget.budgetAmount - budget.spentAmount)} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun TipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun EditBudgetDialog(
    budget: BudgetCategory,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amount by remember { mutableStateOf(budget.budgetAmount.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${budget.name} Budget") },
        text = {
            Column {
                Text(
                    text = "Current spending: $${String.format("%.2f", budget.spentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Amount") },
                    leadingIcon = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    amount.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, category: TransactionCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionCategory.OTHER) }

    val categoryOptions = listOf(
        Triple(TransactionCategory.FOOD, Icons.Filled.Restaurant, "Food"),
        Triple(TransactionCategory.SHOPPING, Icons.Filled.ShoppingCart, "Shopping"),
        Triple(TransactionCategory.TRANSPORT, Icons.Filled.DirectionsCar, "Transport"),
        Triple(TransactionCategory.BILLS, Icons.Filled.Receipt, "Bills"),
        Triple(TransactionCategory.OTHER, Icons.Filled.Category, "Other")
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Amount") },
                    leadingIcon = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { (category, icon, label) ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(label)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val option = categoryOptions.find { it.first == selectedCategory }
                    onAdd(
                        name.ifBlank { option?.third ?: "Other" },
                        amountValue,
                        option?.second ?: Icons.Filled.Category,
                        Color(0xFF9E9E9E),
                        selectedCategory
                    )
                },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
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
