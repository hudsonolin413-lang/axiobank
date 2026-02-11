package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dals.project.model.*
import org.dals.project.utils.CurrencyUtils
import org.dals.project.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transaction: Transaction,
    currentCurrency: String = "USD",
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Share transaction */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                    IconButton(onClick = { /* TODO: Download receipt */ }) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Receipt"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Transaction Card
            TransactionSummaryCard(
                transaction = transaction,
                currentCurrency = currentCurrency
            )

            // Transaction Details
            TransactionDetailsCard(
                transaction = transaction,
                currentCurrency = currentCurrency
            )

            // Recipient Information (if applicable)
            if (transaction.recipientName != null || transaction.recipientAccount != null) {
                RecipientInfoCard(transaction = transaction)
            }

            // Additional Information
            AdditionalInfoCard(transaction = transaction)

            // Transaction Status Timeline
            TransactionStatusCard(transaction = transaction)
        }
    }
}

@Composable
private fun TransactionSummaryCard(
    transaction: Transaction,
    currentCurrency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (transaction.status) {
                TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                TransactionStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainer
                TransactionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                TransactionStatus.REVERSED -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transaction Icon
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.SEND -> Icons.AutoMirrored.Outlined.CallMade
                            TransactionType.RECEIVE -> Icons.AutoMirrored.Outlined.CallReceived
                            TransactionType.BILL_PAYMENT -> Icons.Outlined.Receipt
                            TransactionType.RENT_PAYMENT -> Icons.Outlined.Home
                            TransactionType.LOAN_PAYMENT -> Icons.Outlined.AccountBalance
                            TransactionType.INVESTMENT -> Icons.AutoMirrored.Outlined.TrendingUp
                            TransactionType.WITHDRAWAL -> Icons.Outlined.AccountBalanceWallet
                            TransactionType.DEPOSIT -> Icons.Outlined.Savings
                        },
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount
            val convertedAmount = CurrencyUtils.convertFromUSD(transaction.amount, currentCurrency)
            Text(
                text = when (transaction.type) {
                    TransactionType.SEND, TransactionType.BILL_PAYMENT,
                    TransactionType.WITHDRAWAL, TransactionType.INVESTMENT,
                    TransactionType.RENT_PAYMENT, TransactionType.LOAN_PAYMENT ->
                        "-${CurrencyUtils.formatAmount(convertedAmount, currentCurrency)}"

                    TransactionType.RECEIVE, TransactionType.DEPOSIT ->
                        "+${CurrencyUtils.formatAmount(convertedAmount, currentCurrency)}"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    TransactionType.SEND, TransactionType.BILL_PAYMENT,
                    TransactionType.WITHDRAWAL, TransactionType.INVESTMENT,
                    TransactionType.RENT_PAYMENT, TransactionType.LOAN_PAYMENT ->
                        MaterialTheme.colorScheme.error

                    TransactionType.RECEIVE, TransactionType.DEPOSIT ->
                        MaterialTheme.colorScheme.tertiary
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Type
            Text(
                text = transaction.type.name.replace('_', ' '),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Badge
            Surface(
                color = when (transaction.status) {
                    TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                    TransactionStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHighest
                    TransactionStatus.FAILED -> MaterialTheme.colorScheme.error
                    TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                    TransactionStatus.REVERSED -> MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = transaction.status.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.status) {
                        TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiary
                        TransactionStatus.PENDING -> MaterialTheme.colorScheme.onSurface
                        TransactionStatus.FAILED -> MaterialTheme.colorScheme.onError
                        TransactionStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        TransactionStatus.REVERSED -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailsCard(
    transaction: Transaction,
    currentCurrency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DetailRow(
                icon = Icons.Outlined.Tag,
                label = "Transaction ID",
                value = transaction.reference ?: transaction.id
            )

            DetailRow(
                icon = Icons.Outlined.Description,
                label = "Description",
                value = transaction.description
            )

            DetailRow(
                icon = Icons.Outlined.Category,
                label = "Category",
                value = transaction.category.name.replace('_', ' ')
            )

            if (transaction.fee > 0) {
                val convertedFee = CurrencyUtils.convertFromUSD(transaction.fee, currentCurrency)
                DetailRow(
                    icon = Icons.Outlined.Receipt,
                    label = "Transaction Fee",
                    value = CurrencyUtils.formatAmount(convertedFee, currentCurrency)
                )
            }

            transaction.reference?.let { ref ->
                DetailRow(
                    icon = Icons.Outlined.Numbers,
                    label = "Reference",
                    value = ref
                )
            }

            DetailRow(
                icon = Icons.Outlined.Schedule,
                label = "Date & Time",
                value = formatDateTime(transaction.timestamp)
            )

            DetailRow(
                icon = Icons.Outlined.CurrencyExchange,
                label = "Currency",
                value = transaction.currency,
                showDivider = false
            )
        }
    }
}

@Composable
private fun RecipientInfoCard(
    transaction: Transaction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = if (transaction.type == TransactionType.SEND) "Recipient Information" else "Sender Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            transaction.recipientName?.let { name ->
                DetailRow(
                    icon = Icons.Outlined.Person,
                    label = "Name",
                    value = name
                )
            }

            transaction.recipientAccount?.let { account ->
                DetailRow(
                    icon = Icons.Outlined.AccountBalance,
                    label = "Account",
                    value = account,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun AdditionalInfoCard(
    transaction: Transaction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Security & Compliance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DetailRow(
                icon = Icons.Outlined.Security,
                label = "Secure Transaction",
                value = "✓ Encrypted & Protected"
            )

            DetailRow(
                icon = Icons.Outlined.Gavel,
                label = "Compliance",
                value = "✓ Regulation Compliant"
            )

            DetailRow(
                icon = Icons.Outlined.VerifiedUser,
                label = "Authentication",
                value = "✓ Verified & Authorized",
                showDivider = false
            )
        }
    }
}

@Composable
private fun TransactionStatusCard(
    transaction: Transaction
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Transaction Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Timeline based on current status
            when (transaction.status) {
                TransactionStatus.COMPLETED -> {
                    TimelineItem(
                        icon = Icons.Outlined.Check,
                        title = "Transaction Initiated",
                        subtitle = formatDateTime(transaction.timestamp),
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.Sync,
                        title = "Processing",
                        subtitle = "Transaction processed successfully",
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.CheckCircle,
                        title = "Completed",
                        subtitle = "Transaction completed successfully",
                        isCompleted = true,
                        isLast = true
                    )
                }

                TransactionStatus.PENDING -> {
                    TimelineItem(
                        icon = Icons.Outlined.Check,
                        title = "Transaction Initiated",
                        subtitle = formatDateTime(transaction.timestamp),
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.Sync,
                        title = "Processing",
                        subtitle = "Transaction is being processed",
                        isCompleted = false,
                        isLast = true
                    )
                }

                TransactionStatus.FAILED -> {
                    TimelineItem(
                        icon = Icons.Outlined.Check,
                        title = "Transaction Initiated",
                        subtitle = formatDateTime(transaction.timestamp),
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.Error,
                        title = "Failed",
                        subtitle = "Transaction failed to process",
                        isCompleted = false,
                        isFailed = true,
                        isLast = true
                    )
                }

                TransactionStatus.CANCELLED -> {
                    TimelineItem(
                        icon = Icons.Outlined.Check,
                        title = "Transaction Initiated",
                        subtitle = formatDateTime(transaction.timestamp),
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.Cancel,
                        title = "Cancelled",
                        subtitle = "Transaction was cancelled",
                        isCompleted = false,
                        isFailed = true,
                        isLast = true
                    )
                }

                TransactionStatus.REVERSED -> {
                    TimelineItem(
                        icon = Icons.Outlined.Check,
                        title = "Transaction Initiated",
                        subtitle = formatDateTime(transaction.timestamp),
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.CheckCircle,
                        title = "Completed",
                        subtitle = "Transaction was initially completed",
                        isCompleted = true
                    )
                    TimelineItem(
                        icon = Icons.Outlined.History,
                        title = "Reversed",
                        subtitle = "Transaction was reversed by customer care",
                        isCompleted = true,
                        isFailed = false,
                        isLast = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 32.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun TimelineItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isFailed: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = when {
                    isFailed -> MaterialTheme.colorScheme.errorContainer
                    isCompleted -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            isFailed -> MaterialTheme.colorScheme.onErrorContainer
                            isCompleted -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .padding(vertical = 4.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = if (isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 16.dp else 0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatDateTime(timestamp: String): String {
    val dateTime = DateTimeUtils.parseDateTime(timestamp)
    return if (dateTime != null) {
        DateTimeUtils.formatForDisplay(dateTime)
    } else {
        // Fallback to simple formatting
        try {
            val parts = timestamp.split("T")
            val date = parts[0]
            val time = parts.getOrNull(1)?.take(8) ?: ""
            "$date at $time"
        } catch (e: Exception) {
            timestamp
        }
    }
}