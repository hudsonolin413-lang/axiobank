package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dals.project.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") }
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
            // Market Overview Section
            item {
                Text(
                    text = "Market Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MarketOverviewCard()
            }

            // Featured Opportunities Section
            item {
                Text(
                    text = "Featured Opportunities",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(getFeaturedOpportunities()) { opportunity ->
                OpportunityCard(opportunity = opportunity)
            }

            // Educational Content Section
            item {
                Text(
                    text = "Learn & Grow",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(getEducationalContent()) { content ->
                EducationalCard(content = content)
            }
        }
    }
}

@Composable
private fun MarketOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Digital Lending Market",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MarketStatItem(
                    label = "Total Value Locked",
                    value = "$2.8B",
                    change = "+5.2%"
                )
                MarketStatItem(
                    label = "Average APY",
                    value = "8.4%",
                    change = "+0.3%"
                )
                MarketStatItem(
                    label = "Active Loans",
                    value = "12.4K",
                    change = "+2.1%"
                )
            }
        }
    }
}

@Composable
private fun MarketStatItem(
    label: String,
    value: String,
    change: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            color = if (change.startsWith("+"))
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = change,
                style = MaterialTheme.typography.labelSmall,
                color = if (change.startsWith("+"))
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OpportunityCard(opportunity: LoanOpportunity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opportunity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = opportunity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${opportunity.apy}% APY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: $${opportunity.minAmount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Term: ${opportunity.termMonths} months",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = opportunity.riskLevel,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (opportunity.riskLevel) {
                        "Low Risk" -> MaterialTheme.colorScheme.primary
                        "Medium Risk" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun EducationalCard(content: EducationalContent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = content.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = "${content.readTime} min read",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Data classes for mock data
data class LoanOpportunity(
    val title: String,
    val description: String,
    val apy: Double,
    val minAmount: Int,
    val termMonths: Int,
    val riskLevel: String
)

data class EducationalContent(
    val title: String,
    val description: String,
    val category: String,
    val readTime: Int
)

// Mock data functions
private fun getFeaturedOpportunities(): List<LoanOpportunity> {
    return listOf(
        LoanOpportunity(
            title = "Stable Yield Pool",
            description = "Low-risk lending pool backed by stablecoins with consistent returns",
            apy = 6.5,
            minAmount = 1000,
            termMonths = 12,
            riskLevel = "Low Risk"
        ),
        LoanOpportunity(
            title = "Growth Lending",
            description = "Higher yield opportunities for growth-oriented investors",
            apy = 12.3,
            minAmount = 5000,
            termMonths = 18,
            riskLevel = "Medium Risk"
        ),
        LoanOpportunity(
            title = "Flash Loans",
            description = "Ultra-short term lending for arbitrage opportunities",
            apy = 25.0,
            minAmount = 10000,
            termMonths = 1,
            riskLevel = "High Risk"
        )
    )
}

private fun getEducationalContent(): List<EducationalContent> {
    return listOf(
        EducationalContent(
            title = "Understanding Digital Lending",
            description = "Learn the basics of decentralized finance and peer-to-peer lending",
            category = "Basics",
            readTime = 5
        ),
        EducationalContent(
            title = "Risk Management in Crypto Loans",
            description = "Essential strategies for managing risk in cryptocurrency lending",
            category = "Risk Management",
            readTime = 8
        ),
        EducationalContent(
            title = "Yield Farming Explained",
            description = "Maximize your returns with advanced yield farming techniques",
            category = "Advanced",
            readTime = 12
        )
    )
}