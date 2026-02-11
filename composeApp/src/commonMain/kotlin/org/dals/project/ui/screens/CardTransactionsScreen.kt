package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.Card
import org.dals.project.viewmodel.CardViewModel
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTransactionsScreen(
    cardViewModel: CardViewModel,
    authViewModel: AuthViewModel,
    transactionViewModel: TransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val transactionUiState by transactionViewModel.uiState.collectAsStateWithLifecycle()

    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var showTransactionTypeDialog by remember { mutableStateOf(false) }
    var selectedTransactionType by remember { mutableStateOf<CardTransactionType?>(null) }

    LaunchedEffect(Unit) {
        cardViewModel.loadCards()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Use Your Card",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Shop online, pay bills, transfer money, and more",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Balance Display
            if (transactionUiState.walletBalance != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available Balance",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${transactionUiState.walletBalance!!.currency} ${String.format("%.2f", transactionUiState.walletBalance!!.availableBalance)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (transactionUiState.walletBalance!!.pendingAmount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pending Amount",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${transactionUiState.walletBalance!!.currency} ${String.format("%.2f", transactionUiState.walletBalance!!.pendingAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Select a Card",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (cardUiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (cardUiState.cards.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Cards Added",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add a card to start making transactions",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    cardUiState.cards.forEach { card ->
                        CardItem(
                            card = card,
                            isSelected = selectedCard?.id == card.id,
                            onSelect = { selectedCard = card }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Linked Account Balance (shown when card is selected)
            if (selectedCard != null && selectedCard!!.linkedAccountBalance != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Linked Account",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedCard!!.linkedAccountNumber ?: "Unknown Account",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Available: $${String.format("%.2f", selectedCard!!.linkedAccountBalance)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Icon(
                                Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Transaction Types
            if (selectedCard != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "What would you like to do?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TransactionTypeCard(
                            icon = Icons.Default.ShoppingCart,
                            title = "Online\nShopping",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTransactionType = CardTransactionType.ONLINE_PAYMENT
                                showTransactionTypeDialog = true
                            }
                        )
                        TransactionTypeCard(
                            icon = Icons.Default.CreditCard,
                            title = "POS\nPayment",
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTransactionType = CardTransactionType.POS_TRANSACTION
                                showTransactionTypeDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TransactionTypeCard(
                            icon = Icons.Default.Receipt,
                            title = "Pay\nBills",
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTransactionType = CardTransactionType.BILL_PAYMENT
                                showTransactionTypeDialog = true
                            }
                        )
                        TransactionTypeCard(
                            icon = Icons.Default.Send,
                            title = "Transfer\nMoney",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTransactionType = CardTransactionType.CARD_TRANSFER
                                showTransactionTypeDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TransactionTypeCard(
                        icon = Icons.Default.LocalAtm,
                        title = "ATM Withdrawal",
                        color = Color(0xFFE91E63),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selectedTransactionType = CardTransactionType.ATM_WITHDRAWAL
                            showTransactionTypeDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Transaction Dialog
        if (showTransactionTypeDialog && selectedCard != null && selectedTransactionType != null) {
            TransactionDialog(
                transactionType = selectedTransactionType!!,
                card = selectedCard!!,
                cardViewModel = cardViewModel,
                onDismiss = {
                    showTransactionTypeDialog = false
                    selectedTransactionType = null
                }
            )
        }
    }
}

@Composable
fun CardItem(
    card: Card,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        when (card.cardBrand.name) {
                            "VISA" -> Color(0xFF1A1F71)
                            "MASTERCARD" -> Color(0xFFEB001B)
                            "AMERICAN_EXPRESS" -> Color(0xFF006FCF)
                            else -> Color.Gray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.cardBrand.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "**** **** **** ${card.lastFourDigits}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${card.cardHolderName} • ${card.cardType.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionTypeCard(
    icon: ImageVector,
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

enum class CardTransactionType {
    ONLINE_PAYMENT,
    POS_TRANSACTION,
    BILL_PAYMENT,
    CARD_TRANSFER,
    ATM_WITHDRAWAL
}

@Composable
fun TransactionDialog(
    transactionType: CardTransactionType,
    card: Card,
    cardViewModel: CardViewModel,
    onDismiss: () -> Unit
) {
    when (transactionType) {
        CardTransactionType.ONLINE_PAYMENT -> OnlinePaymentDialog(card, cardViewModel, onDismiss)
        CardTransactionType.POS_TRANSACTION -> POSTransactionDialog(card, cardViewModel, onDismiss)
        CardTransactionType.BILL_PAYMENT -> BillPaymentDialog(card, cardViewModel, onDismiss)
        CardTransactionType.CARD_TRANSFER -> CardTransferDialog(card, cardViewModel, onDismiss)
        CardTransactionType.ATM_WITHDRAWAL -> ATMWithdrawalDialog(card, cardViewModel, onDismiss)
    }
}
