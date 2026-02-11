package org.dals.project.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class MpesaTransaction(
    val id: String,
    val checkoutRequestId: String,
    val merchantRequestId: String? = null,
    val phoneNumber: String,
    val accountNumber: String,
    val amount: Double,
    val status: String,
    val mpesaReceiptNumber: String? = null,
    val description: String? = null,
    val createdAt: String
)

@Serializable
data class MpesaTransactionsResponse(
    val success: Boolean,
    val message: String,
    val data: List<MpesaTransaction>
)

@Composable
fun MpesaTransactionHistoryDialog(
    phoneNumber: String,
    onDismiss: () -> Unit,
    onReverseTransaction: (String) -> Unit,
    httpClient: HttpClient,
    baseUrl: String
) {
    var transactions by remember { mutableStateOf<List<MpesaTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(phoneNumber) {
        scope.launch {
            try {
                val response = httpClient.get("$baseUrl/mpesa/transactions") {
                    parameter("phoneNumber", phoneNumber)
                    contentType(ContentType.Application.Json)
                }.body<MpesaTransactionsResponse>()

                if (response.success) {
                    transactions = response.data
                } else {
                    errorMessage = response.message
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load transactions: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "M-Pesa Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Phone: $phoneNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    transactions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions) { transaction ->
                                MpesaTransactionCard(
                                    transaction = transaction,
                                    onReverse = { onReverseTransaction(transaction.checkoutRequestId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MpesaTransactionCard(
    transaction: MpesaTransaction,
    onReverse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (transaction.status) {
                "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
                "REVERSED" -> MaterialTheme.colorScheme.errorContainer
                "PENDING" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$${"%.2f".format(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = transaction.accountNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusChip(status = transaction.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transaction.mpesaReceiptNumber != null) {
                Text(
                    text = "Receipt: ${transaction.mpesaReceiptNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "ID: ${transaction.checkoutRequestId.take(20)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = transaction.createdAt.take(19).replace('T', ' '),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (transaction.status == "COMPLETED") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onReverse,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reverse Transaction")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, contentColor, icon) = when (status) {
        "COMPLETED" -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.Default.CheckCircle
        )
        "REVERSED" -> Triple(
            Color(0xFFF44336),
            Color.White,
            Icons.Default.Undo
        )
        "PENDING" -> Triple(
            Color(0xFFFF9800),
            Color.White,
            Icons.Default.Schedule
        )
        else -> Triple(
            Color(0xFF9E9E9E),
            Color.White,
            Icons.Default.Info
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
