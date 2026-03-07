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

data class BudgetCategory(
    val id: String,
    val name: String,
    val icon: String,
    val budgetAmount: Double,
    val spentAmount: Double,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    onNavigateBack: () -> Unit
) {
    var budgets by remember { mutableStateOf(listOf(
        BudgetCategory("1", "Food & Dining", "🍔", 500.0, 320.0, Color(0xFFFF6B6B)),
        BudgetCategory("2", "Shopping", "🛍️", 300.0, 280.0, Color(0xFF4ECDC4)),
        BudgetCategory("3", "Transportation", "🚗", 200.0, 150.0, Color(0xFF45B7D1)),
        BudgetCategory("4", "Entertainment", "🎬", 150.0, 80.0, Color(0xFF98D8C8)),
        BudgetCategory("5", "Bills & Utilities", "💡", 400.0, 380.0, Color(0xFFFFA07A)),
        BudgetCategory("6", "Healthcare", "💊", 100.0, 45.0, Color(0xFFDDA0DD))
    )) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetCategory?>(null) }

    val totalBudget = budgets.sumOf { it.budgetAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val remainingBudget = totalBudget - totalSpent

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
                    IconButton(onClick = { showAddBudgetDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Budget")
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

                        // Overall Progress
                        val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Overall Progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${(overallProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { overallProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = when {
                                    overallProgress > 0.9f -> MaterialTheme.colorScheme.error
                                    overallProgress > 0.7f -> Color(0xFFFFA000)
                                    else -> Color(0xFF4CAF50)
                                },
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Budget Categories Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddBudgetDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            // Budget Category Cards
            items(budgets) { budget ->
                BudgetCategoryCard(
                    budget = budget,
                    onEdit = { editingBudget = budget },
                    onDelete = { budgets = budgets.filter { it.id != budget.id } }
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
                        
                        BudgetTip("💡", "Try the 50/30/20 rule: 50% needs, 30% wants, 20% savings")
                        BudgetTip("📊", "Review your budgets weekly to stay on track")
                        BudgetTip("🎯", "Set realistic goals based on your income")
                    }
                }
            }
        }
    }

    // Add/Edit Budget Dialog
    if (showAddBudgetDialog || editingBudget != null) {
        BudgetDialog(
            budget = editingBudget,
            onDismiss = {
                showAddBudgetDialog = false
                editingBudget = null
            },
            onSave = { name, icon, amount ->
                if (editingBudget != null) {
                    budgets = budgets.map {
                        if (it.id == editingBudget!!.id) it.copy(name = name, icon = icon, budgetAmount = amount)
                        else it
                    }
                } else {
                    val newBudget = BudgetCategory(
                        id = (budgets.size + 1).toString(),
                        name = name,
                        icon = icon,
                        budgetAmount = amount,
                        spentAmount = 0.0,
                        color = listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1), Color(0xFF98D8C8)).random()
                    )
                    budgets = budgets + newBudget
                }
                showAddBudgetDialog = false
                editingBudget = null
            }
        )
    }
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = "$${String.format("%.0f", amount)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetCategoryCard(
    budget: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.budgetAmount > 0) (budget.spentAmount / budget.budgetAmount).toFloat() else 0f
    val isOverBudget = budget.spentAmount > budget.budgetAmount
    var showMenu by remember { mutableStateOf(false) }

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
                            .clip(RoundedCornerShape(10.dp))
                            .background(budget.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = budget.icon, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$${String.format("%.0f", budget.spentAmount)} of $${String.format("%.0f", budget.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options")
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
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    isOverBudget -> MaterialTheme.colorScheme.error
                    progress > 0.8f -> Color(0xFFFFA000)
                    else -> budget.color
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOverBudget) "Over by $${String.format("%.0f", budget.spentAmount - budget.budgetAmount)}"
                           else "$${String.format("%.0f", budget.budgetAmount - budget.spentAmount)} left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun BudgetTip(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun BudgetDialog(
    budget: BudgetCategory?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, amount: Double) -> Unit
) {
    var name by remember { mutableStateOf(budget?.name ?: "") }
    var icon by remember { mutableStateOf(budget?.icon ?: "💰") }
    var amount by remember { mutableStateOf(budget?.budgetAmount?.toString() ?: "") }

    val icons = listOf("🍔", "🛍️", "🚗", "🎬", "💡", "💊", "🏠", "✈️", "📚", "💰", "🎮", "👕")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget != null) "Edit Budget" else "Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Icon Selector
                Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.take(6).forEach { emoji ->
                        FilterChip(
                            selected = icon == emoji,
                            onClick = { icon = emoji },
                            label = { Text(emoji) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.drop(6).forEach { emoji ->
                        FilterChip(
                            selected = icon == emoji,
                            onClick = { icon = emoji },
                            label = { Text(emoji) }
                        )
                    }
                }

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
                onClick = { onSave(name, icon, amount.toDoubleOrNull() ?: 0.0) },
                enabled = name.isNotBlank() && amount.isNotBlank()
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
