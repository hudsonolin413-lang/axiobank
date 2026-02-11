package org.dals.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.CardRequest
import org.dals.project.model.CardType
import org.dals.project.model.CardBrand
import org.dals.project.model.BillingAddress
import org.dals.project.viewmodel.CardViewModel
import org.dals.project.viewmodel.AuthViewModel
import org.jetbrains.compose.resources.painterResource
import decentralizedaccessloan.composeapp.generated.resources.Res
import decentralizedaccessloan.composeapp.generated.resources.AxioBank
import kotlin.reflect.KProperty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    cardViewModel: CardViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onCardAdded: () -> Unit
) {
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = authUiState.currentUser

    var cardHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var selectedCardType by remember { mutableStateOf(CardType.CREDIT) }
    var detectedCardBrand by remember { mutableStateOf<CardBrand?>(null) }

    // Detect card brand based on card number
    LaunchedEffect(cardNumber) {
        detectedCardBrand = detectCardBrand(cardNumber)
    }

    // Show success message
    LaunchedEffect(cardUiState.successMessage) {
        if (cardUiState.successMessage != null) {
            onCardAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Card") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Preview
                CardPreview(
                    cardHolderName = cardHolderName.ifEmpty { "CARD HOLDER NAME" },
                    cardNumber = formatCardNumber(cardNumber.ifEmpty { "•••• •••• •••• ••••" }),
                    expiryMonth = expiryMonth.ifEmpty { "MM" },
                    expiryYear = expiryYear.ifEmpty { "YY" },
                    cardType = selectedCardType,
                    cardBrand = detectedCardBrand
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card Type Selection
                Text(
                    text = "Card Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilterChip(
                        selected = selectedCardType == CardType.CREDIT,
                        onClick = { selectedCardType = CardType.CREDIT },
                        label = { Text("Credit Card") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCardType == CardType.DEBIT,
                        onClick = { selectedCardType = CardType.DEBIT },
                        label = { Text("Debit Card") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Card Holder Name
                OutlinedTextField(
                    value = cardHolderName,
                    onValueChange = { cardHolderName = it.uppercase() },
                    label = { Text("Card Holder Name") },
                    placeholder = { Text("JOHN DOE") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.CreditCard, "Card Holder")
                    }
                )

                // Card Number
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { value: String ->
                        if (value.length <= 16 && value.all { char: Char -> char.isDigit() }) {
                            cardNumber = value
                        }
                    },
                    label = { Text("Card Number") },
                    placeholder = { Text("1234 5678 9012 3456") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CardNumberVisualTransformation(),
                    trailingIcon = {
                        detectedCardBrand?.let { brand ->
                            when (brand) {
                                CardBrand.VISA -> Text(
                                    text = "VISA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                CardBrand.MASTERCARD -> Text(
                                    text = "MASTERCARD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                else -> {}
                            }
                        }
                    },
                    isError = cardNumber.length >= 6 && detectedCardBrand != CardBrand.VISA && detectedCardBrand != CardBrand.MASTERCARD,
                    supportingText = {
                        if (cardNumber.length >= 6 && detectedCardBrand != CardBrand.VISA && detectedCardBrand != CardBrand.MASTERCARD) {
                            Text(
                                text = "Only Visa and Mastercard are accepted",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                // Expiry Date and CVV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Expiry Month
                    OutlinedTextField(
                        value = expiryMonth,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { char -> char.isDigit() }) {
                                val month = value.toIntOrNull()
                                if (month == null || month <= 12) {
                                    expiryMonth = value
                                }
                            }
                        },
                        label = { Text("MM") },
                        placeholder = { Text("01") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Expiry Year
                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = { value ->
                            if (value.length <= 4 && value.all { char -> char.isDigit() }) {
                                expiryYear = value
                            }
                        },
                        label = { Text("YYYY") },
                        placeholder = { Text("2026") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // CVV
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { value ->
                            if (value.length <= 4 && value.all { char -> char.isDigit() }) {
                                cvv = value
                            }
                        },
                        label = { Text("CVV") },
                        placeholder = { Text("123") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }

                // Card Nickname (Optional)
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Card Nickname (Optional)") },
                    placeholder = { Text("Personal Card") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Error Message
                if (cardUiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = cardUiState.errorMessage!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Card Button
                Button(
                    onClick = {
                        if (validateCardInput(cardHolderName, cardNumber, expiryMonth, expiryYear, cvv, detectedCardBrand)) {
                            currentUser?.let { user ->
                                val cardRequest = CardRequest(
                                    cardNumber = cardNumber,
                                    cardHolderName = cardHolderName,
                                    expiryMonth = expiryMonth.toInt(),
                                    expiryYear = expiryYear.toInt(),
                                    cvv = cvv,
                                    cardType = selectedCardType,
                                    nickname = nickname.ifEmpty { null },
                                    billingAddress = null
                                )
                                cardViewModel.addCard(cardRequest)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !cardUiState.isAddingCard &&
                              (detectedCardBrand == CardBrand.VISA || detectedCardBrand == CardBrand.MASTERCARD)
                ) {
                    if (cardUiState.isAddingCard) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add Card", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Security Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Your card is secure",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "We use bank-level encryption to protect your card information. Your full card number is never stored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardPreview(
    cardHolderName: String,
    cardNumber: String,
    expiryMonth: String,
    expiryYear: String,
    cardType: CardType,
    cardBrand: CardBrand?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (cardBrand) {
                CardBrand.VISA -> Color(0xFF1A1F71) // Visa blue
                CardBrand.MASTERCARD -> Color(0xFFEB001B) // Mastercard red
                else -> if (cardType == CardType.CREDIT) {
                    Color(0xFF1E3A8A) // Blue for credit
                } else {
                    Color(0xFF059669) // Green for debit
                }
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row with AxioBank logo and Card Brand logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AxioBank Logo
                    Image(
                        painter = painterResource(Res.drawable.AxioBank),
                        contentDescription = "AxioBank Logo",
                        modifier = Modifier.height(24.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Card Brand Logo
                    cardBrand?.let { brand ->
                        when (brand) {
                            CardBrand.VISA -> {
                                Text(
                                    text = "VISA",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            CardBrand.MASTERCARD -> {
                                Text(
                                    text = "mastercard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {
                                Text(
                                    text = if (cardType == CardType.CREDIT) "CREDIT" else "DEBIT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } ?: Text(
                        text = if (cardType == CardType.CREDIT) "CREDIT" else "DEBIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Card Number
                Text(
                    text = cardNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                // Card Holder and Expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "CARD HOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = cardHolderName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "EXPIRES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$expiryMonth/$expiryYear",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatCardNumber(number: String): String {
    val cleaned = number.replace(" ", "")
    return cleaned.chunked(4).joinToString(" ")
}

private fun detectCardBrand(cardNumber: String): CardBrand? {
    if (cardNumber.isEmpty()) return null

    // Visa: starts with 4
    if (cardNumber.startsWith("4")) {
        return CardBrand.VISA
    }

    // Mastercard: starts with 51-55 or 2221-2720
    if (cardNumber.startsWith("5")) {
        val firstTwo = cardNumber.take(2).toIntOrNull()
        if (firstTwo != null && firstTwo in 51..55) {
            return CardBrand.MASTERCARD
        }
    }
    if (cardNumber.length >= 4) {
        val firstFour = cardNumber.take(4).toIntOrNull()
        if (firstFour != null && firstFour in 2221..2720) {
            return CardBrand.MASTERCARD
        }
    }

    return CardBrand.UNKNOWN
}

private fun validateCardInput(
    cardHolderName: String,
    cardNumber: String,
    expiryMonth: String,
    expiryYear: String,
    cvv: String,
    cardBrand: CardBrand?
): Boolean {
    return cardHolderName.isNotEmpty() &&
            cardNumber.length >= 13 &&
            expiryMonth.length == 2 &&
            expiryYear.length == 4 &&
            cvv.length >= 3 &&
            (cardBrand == CardBrand.VISA || cardBrand == CardBrand.MASTERCARD)
}

// Custom visual transformation for card number formatting
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(16)
        val formatted = trimmed.chunked(4).joinToString(" ")
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                if (offset <= 12) return offset + 2
                return offset + 3
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                return offset - 3
            }
        }
        
        return TransformedText(
            AnnotatedString(formatted),
            offsetMapping
        )
    }
}
