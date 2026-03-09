package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.model.*
import org.dals.project.repository.CashFlowRepository
import org.dals.project.viewmodel.CashFlowViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowManagementScreen(
    viewModel: CashFlowViewModel = viewModel(),
    onBack: () -> Unit
) {
    val cashFlowSummary by viewModel.cashFlowSummary.collectAsState()
    val monthlyTrends by viewModel.monthlyTrends.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val periodComparison by viewModel.periodComparison.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Flow Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
            // Period Selector
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            // Loading Indicator
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Summary Card
            if (cashFlowSummary != null) {
                item {
                    CashFlowSummaryCard(cashFlowSummary!!)
                }
            }

            // Period Comparison
            if (periodComparison != null) {
                item {
                    PeriodComparisonCard(periodComparison!!)
                }
            }

            // Insights
            if (insights.isNotEmpty()) {
                item {
                    Text(
                        "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(insights) { insight ->
                    InsightCard(insight)
                }
            }

            // Category Breakdown
            if (cashFlowSummary != null && cashFlowSummary!!.categories.isNotEmpty()) {
                item {
                    Text(
                        "Category Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(cashFlowSummary!!.categories) { category ->
                    CategoryFlowCard(category)
                }
            }

            // Monthly Trends
            if (monthlyTrends.isNotEmpty()) {
                item {
                    Text(
                        "Monthly Trends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(monthlyTrends) { trend ->
                    MonthlyTrendCard(trend)
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: CashFlowRepository.AnalysisPeriod,
    onPeriodSelected: (CashFlowRepository.AnalysisPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Analysis Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CashFlowRepository.AnalysisPeriod.values().forEach { period ->
                    if (period != CashFlowRepository.AnalysisPeriod.ALL_TIME) {
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                            label = { Text(period.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowSummaryCard(summary: CashFlowSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Cash Flow Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                summary.period,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Income
            SummaryRow(
                label = "Total Income",
                amount = summary.totalIncome,
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expenses
            SummaryRow(
                label = "Total Expenses",
                amount = summary.totalExpenses,
                icon = Icons.Default.TrendingDown,
                color = Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            // Net Cash Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (summary.netCashFlow >= 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (summary.netCashFlow >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Net Cash Flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    "$${String.format("%.2f", abs(summary.netCashFlow))}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.netCashFlow >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PeriodComparisonCard(comparison: PeriodComparison) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Period Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Current Period
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Current Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$${String.format("%.2f", comparison.currentPeriod.netFlow)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (comparison.currentPeriod.netFlow >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                    Text(
                        "${comparison.currentPeriod.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }

                // Trend Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = when (comparison.trend) {
                            TrendDirection.UP -> Icons.Default.TrendingUp
                            TrendDirection.DOWN -> Icons.Default.TrendingDown
                            TrendDirection.STABLE -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when (comparison.trend) {
                            TrendDirection.UP -> Color(0xFF4CAF50)
                            TrendDirection.DOWN -> Color(0xFFFF5252)
                            TrendDirection.STABLE -> Color.Gray
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "${if (comparison.percentageChange >= 0) "+" else ""}${String.format("%.1f", comparison.percentageChange)}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (comparison.trend) {
                            TrendDirection.UP -> Color(0xFF4CAF50)
                            TrendDirection.DOWN -> Color(0xFFFF5252)
                            TrendDirection.STABLE -> Color.Gray
                        }
                    )
                }

                // Previous Period
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Previous Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$${String.format("%.2f", comparison.previousPeriod.netFlow)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    )
                    Text(
                        "${comparison.previousPeriod.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(insight: CashFlowInsight) {
    val backgroundColor = when (insight.severity) {
        InsightSeverity.POSITIVE -> Color(0xFFE8F5E9)
        InsightSeverity.INFO -> Color(0xFFE3F2FD)
        InsightSeverity.WARNING -> Color(0xFFFFF3E0)
        InsightSeverity.CRITICAL -> Color(0xFFFFEBEE)
    }

    val iconColor = when (insight.severity) {
        InsightSeverity.POSITIVE -> Color(0xFF4CAF50)
        InsightSeverity.INFO -> Color(0xFF2196F3)
        InsightSeverity.WARNING -> Color(0xFFFF9800)
        InsightSeverity.CRITICAL -> Color(0xFFFF5252)
    }

    val icon = when (insight.type) {
        InsightType.POSITIVE_TREND -> Icons.Default.TrendingUp
        InsightType.BUDGET_WARNING -> Icons.Default.Warning
        InsightType.SAVING_OPPORTUNITY -> Icons.Default.Savings
        InsightType.SPENDING_PATTERN -> Icons.Default.BarChart
        InsightType.INCOME_TREND -> Icons.Default.ShowChart
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategoryFlowCard(category: CategoryFlow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(category.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            category.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${category.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                    "$${String.format("%.2f", abs(category.netFlow))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (category.netFlow >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
            }

            if (category.inflow > 0 || category.outflow > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (category.inflow > 0) {
                        Text(
                            "In: $${String.format("%.2f", category.inflow)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (category.outflow > 0) {
                        Text(
                            "Out: $${String.format("%.2f", category.outflow)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendCard(trend: MonthlyTrend) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                trend.month,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "$${String.format("%.0f", trend.income)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "$${String.format("%.0f", trend.expenses)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF5252)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Net",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "$${String.format("%.0f", abs(trend.netFlow))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (trend.netFlow >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
private fun getCategoryIcon(category: TransactionCategory): ImageVector {
    return when (category) {
        TransactionCategory.TRANSFER -> Icons.Default.SwapHoriz
        TransactionCategory.BILLS -> Icons.Default.Receipt
        TransactionCategory.RENT -> Icons.Default.Home
        TransactionCategory.LOANS -> Icons.Default.AccountBalance
        TransactionCategory.INVESTMENT -> Icons.Default.TrendingUp
        TransactionCategory.SHOPPING -> Icons.Default.ShoppingCart
        TransactionCategory.FOOD -> Icons.Default.Restaurant
        TransactionCategory.TRANSPORT -> Icons.Default.DirectionsCar
        TransactionCategory.UTILITIES -> Icons.Default.ElectricBolt
        TransactionCategory.OTHER -> Icons.Default.MoreHoriz
    }
}
