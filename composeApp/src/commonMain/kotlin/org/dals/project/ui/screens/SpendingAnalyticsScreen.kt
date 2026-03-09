package org.dals.project.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.Transaction
import org.dals.project.model.TransactionCategory
import org.dals.project.model.TransactionType
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.ui.components.BarChart3D
import org.dals.project.ui.components.BarChartData

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val transactionCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingAnalyticsScreen(
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("This Week", "This Month", "Last 3 Months", "This Year")
    
    // Filter transactions based on selected period and calculate spending by category
    val categories = remember(transactionUiState.transactions, selectedPeriod) {
        calculateSpendingCategories(transactionUiState.transactions, selectedPeriod)
    }
    
    val totalSpending = categories.sumOf { it.amount }
    val totalTransactions = categories.sumOf { it.transactionCount }

    // Refresh data on load
    LaunchedEffect(Unit) {
        transactionViewModel.refreshAllData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { transactionViewModel.refreshAllData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
                // Period Selector
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        periods.forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size)
                            ) {
                                Text(period, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Total Spending Card
                item {
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
                            Text(
                                text = "Total Spending",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$${String.format("%.2f", totalSpending)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalTransactions transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (categories.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No spending data",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Your spending analytics will appear here once you make transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    // 3D Bar Chart - Spending by Category
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Spending by Category",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                // 3D Bar Chart
                                BarChart3D(
                                    data = categories.take(6).map { category ->
                                        BarChartData(
                                            label = category.name,
                                            value = category.amount.toFloat(),
                                            color = category.color,
                                            subLabel = "$${String.format("%.0f", category.amount)}"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    chartHeight = 320.dp,
                                    barWidth = 50f,
                                    depth3D = 20f,
                                    spacing = 30f,
                                    showValues = false,
                                    showLabels = true,
                                    animate = true,
                                    rotationAngle = 20f
                                )
                            }
                        }
                    }

                    // Category Details
                    item {
                        Text(
                            text = "By Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(categories) { category ->
                        CategorySpendingCard(category = category, totalSpending = totalSpending)
                    }

                    // Insights Card
                    item {
                        val insights = generateInsights(categories, totalSpending)
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
                                        text = "Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                insights.forEach { insight ->
                                    InsightItem(icon = insight.first, text = insight.second)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateSpendingCategories(transactions: List<Transaction>, period: String): List<SpendingCategory> {
    // Filter outgoing transactions (SEND, BILL_PAYMENT, etc.)
    val outgoingTypes = listOf(TransactionType.SEND, TransactionType.BILL_PAYMENT, TransactionType.WITHDRAWAL, TransactionType.RENT_PAYMENT, TransactionType.LOAN_PAYMENT)
    val outgoingTransactions = transactions.filter { it.type in outgoingTypes }
    
    if (outgoingTransactions.isEmpty()) return emptyList()
    
    // Group by category
    val categoryGroups = outgoingTransactions.groupBy { it.category }
    val totalSpending = outgoingTransactions.sumOf { it.amount }
    
    // Define colors and icons for each category using Material Icons
    val categoryInfo = mapOf(
        TransactionCategory.TRANSFER to Pair(Color(0xFFFFC107), Icons.Filled.SwapHoriz), // Yellow
        TransactionCategory.BILLS to Pair(Color(0xFFFFA000), Icons.Filled.Receipt), // Amber/Yellow
        TransactionCategory.RENT to Pair(Color(0xFF2196F3), Icons.Filled.Home), // Blue
        TransactionCategory.LOANS to Pair(Color(0xFF9C27B0), Icons.Filled.AccountBalance), // Purple
        TransactionCategory.INVESTMENT to Pair(Color(0xFF4CAF50), Icons.Filled.TrendingUp), // Green
        TransactionCategory.SHOPPING to Pair(Color(0xFFF44336), Icons.Filled.ShoppingCart), // Red
        TransactionCategory.FOOD to Pair(Color(0xFFFF5722), Icons.Filled.Restaurant), // Deep Orange/Red
        TransactionCategory.TRANSPORT to Pair(Color(0xFF03A9F4), Icons.Filled.DirectionsCar), // Light Blue
        TransactionCategory.UTILITIES to Pair(Color(0xFFFFEB3B), Icons.Filled.Power), // Yellow
        TransactionCategory.OTHER to Pair(Color(0xFF9E9E9E), Icons.Filled.Category) // Grey
    )
    
    return categoryGroups.map { (category, txns) ->
        val amount = txns.sumOf { it.amount }
        val info = categoryInfo[category] ?: Pair(Color(0xFF9E9E9E), Icons.Filled.Category)
        SpendingCategory(
            name = category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            amount = amount,
            percentage = if (totalSpending > 0) (amount / totalSpending).toFloat() else 0f,
            color = info.first,
            icon = info.second,
            transactionCount = txns.size
        )
    }.sortedByDescending { it.amount }
}

private fun generateInsights(categories: List<SpendingCategory>, totalSpending: Double): List<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>> {
    val insights = mutableListOf<Pair<androidx.compose.ui.graphics.vector.ImageVector, String>>()

    if (categories.isEmpty()) return insights

    // Top spending category
    val topCategory = categories.maxByOrNull { it.amount }
    topCategory?.let {
        insights.add(Icons.Filled.BarChart to "${it.name} is your highest spending category at ${(it.percentage * 100).toInt()}%")
    }

    // Number of categories
    insights.add(Icons.Filled.List to "You spent across ${categories.size} different categories")

    // Average transaction
    val totalTxns = categories.sumOf { it.transactionCount }
    if (totalTxns > 0) {
        val avgTxn = totalSpending / totalTxns
        insights.add(Icons.Filled.AttachMoney to "Average transaction amount: $${String.format("%.2f", avgTxn)}")
    }

    return insights
}

@Composable
private fun CategorySpendingCard(
    category: SpendingCategory,
    totalSpending: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${category.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { category.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = category.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", category.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(category.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
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
