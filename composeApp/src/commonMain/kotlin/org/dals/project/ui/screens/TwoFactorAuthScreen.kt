package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TwoFactorMethod {
    SMS, EMAIL, AUTHENTICATOR_APP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorAuthScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit = {}
) {
    var selectedMethod by remember { mutableStateOf<TwoFactorMethod?>(null) }
    var verificationCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCountdown by remember { mutableStateOf(0) }
    
    val scope = rememberCoroutineScope()

    // Countdown timer for resend
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000)
            resendCountdown--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Two-Factor Authentication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Secure Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Add an extra layer of security by enabling two-factor authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isCodeSent) {
                // Method Selection
                Text(
                    text = "Choose verification method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // SMS Option
                TwoFactorMethodCard(
                    title = "SMS Verification",
                    description = "Receive codes via text message",
                    icon = Icons.Filled.Sms,
                    isSelected = selectedMethod == TwoFactorMethod.SMS,
                    onClick = { selectedMethod = TwoFactorMethod.SMS }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email Option
                TwoFactorMethodCard(
                    title = "Email Verification",
                    description = "Receive codes via email",
                    icon = Icons.Filled.Email,
                    isSelected = selectedMethod == TwoFactorMethod.EMAIL,
                    onClick = { selectedMethod = TwoFactorMethod.EMAIL }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Authenticator App Option
                TwoFactorMethodCard(
                    title = "Authenticator App",
                    description = "Use Google Authenticator or similar",
                    icon = Icons.Filled.PhoneAndroid,
                    isSelected = selectedMethod == TwoFactorMethod.AUTHENTICATOR_APP,
                    onClick = { selectedMethod = TwoFactorMethod.AUTHENTICATOR_APP }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Input field based on selected method
                when (selectedMethod) {
                    TwoFactorMethod.SMS -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1 (555) 123-4567") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TwoFactorMethod.EMAIL -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("your@email.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TwoFactorMethod.AUTHENTICATOR_APP -> {
                        // Show QR code placeholder for authenticator app
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scan this QR code with your authenticator app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Manual key: ABCD-EFGH-IJKL-MNOP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    null -> { /* No method selected */ }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Send Code Button
                Button(
                    onClick = {
                        scope.launch {
                            isVerifying = true
                            delay(1500) // Simulate API call
                            isCodeSent = true
                            resendCountdown = 60
                            isVerifying = false
                        }
                    },
                    enabled = selectedMethod != null && !isVerifying && when (selectedMethod) {
                        TwoFactorMethod.SMS -> phoneNumber.isNotBlank()
                        TwoFactorMethod.EMAIL -> email.isNotBlank()
                        TwoFactorMethod.AUTHENTICATOR_APP -> true
                        null -> false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (selectedMethod == TwoFactorMethod.AUTHENTICATOR_APP) "Continue" else "Send Code")
                    }
                }
            } else {
                // Verification Code Entry
                Text(
                    text = "Enter Verification Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when (selectedMethod) {
                        TwoFactorMethod.SMS -> "We sent a code to $phoneNumber"
                        TwoFactorMethod.EMAIL -> "We sent a code to $email"
                        TwoFactorMethod.AUTHENTICATOR_APP -> "Enter the code from your authenticator app"
                        null -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Code Input
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6) verificationCode = it },
                    label = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resend Code
                if (selectedMethod != TwoFactorMethod.AUTHENTICATOR_APP) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                resendCountdown = 60
                                // Simulate resend
                            }
                        },
                        enabled = resendCountdown == 0
                    ) {
                        Text(
                            if (resendCountdown > 0) "Resend code in ${resendCountdown}s" else "Resend Code"
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Verify Button
                Button(
                    onClick = {
                        scope.launch {
                            isVerifying = true
                            errorMessage = null
                            delay(1500) // Simulate verification
                            if (verificationCode == "123456") { // Demo: accept 123456
                                onSetupComplete()
                            } else {
                                errorMessage = "Invalid code. Please try again."
                            }
                            isVerifying = false
                        }
                    },
                    enabled = verificationCode.length == 6 && !isVerifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Verify & Enable 2FA")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Back to method selection
                TextButton(
                    onClick = {
                        isCodeSent = false
                        verificationCode = ""
                        errorMessage = null
                    }
                ) {
                    Text("Choose different method")
                }
            }
        }
    }
}

@Composable
private fun TwoFactorMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
