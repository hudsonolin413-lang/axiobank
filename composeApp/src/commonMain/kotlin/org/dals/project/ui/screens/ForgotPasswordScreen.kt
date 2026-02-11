package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import decentralizedaccessloan.composeapp.generated.resources.AxioBank
import decentralizedaccessloan.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource
import org.dals.project.viewmodel.AuthViewModel

enum class ForgotPasswordStep {
    EMAIL_ENTRY,
    CODE_VERIFICATION,
    PASSWORD_RESET
}

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableStateOf(ForgotPasswordStep.EMAIL_ENTRY) }
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Back button and Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !authUiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Image(
                        painter = painterResource(Res.drawable.AxioBank),
                        contentDescription = "Axio Bank Logo",
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                }

                Text(
                    text = when (currentStep) {
                        ForgotPasswordStep.EMAIL_ENTRY -> "Forgot Password"
                        ForgotPasswordStep.CODE_VERIFICATION -> "Verify Code"
                        ForgotPasswordStep.PASSWORD_RESET -> "Reset Password"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (currentStep) {
                        ForgotPasswordStep.EMAIL_ENTRY -> "Enter your email address and we'll send you a 6-digit verification code"
                        ForgotPasswordStep.CODE_VERIFICATION -> "Enter the 6-digit code sent to $email"
                        ForgotPasswordStep.PASSWORD_RESET -> "Create a new password for your account"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Step content
                when (currentStep) {
                    ForgotPasswordStep.EMAIL_ENTRY -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                authViewModel.clearMessages()
                            },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            enabled = !authUiState.isLoading
                        )

                        Button(
                            onClick = {
                                authViewModel.sendPasswordResetCode(email) { success ->
                                    if (success) {
                                        currentStep = ForgotPasswordStep.CODE_VERIFICATION
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !authUiState.isLoading && email.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (authUiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Send Code",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    ForgotPasswordStep.CODE_VERIFICATION -> {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    verificationCode = it
                                    authViewModel.clearMessages()
                                }
                            },
                            label = { Text("Verification Code") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !authUiState.isLoading,
                            placeholder = { Text("000000") }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = ForgotPasswordStep.EMAIL_ENTRY
                                    verificationCode = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                enabled = !authUiState.isLoading,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    authViewModel.verifyPasswordResetCode(email, verificationCode) { success ->
                                        if (success) {
                                            currentStep = ForgotPasswordStep.PASSWORD_RESET
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                enabled = !authUiState.isLoading && verificationCode.length == 6,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (authUiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Verify")
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                authViewModel.sendPasswordResetCode(email) {}
                            },
                            enabled = !authUiState.isLoading
                        ) {
                            Text(
                                text = "Resend Code",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    ForgotPasswordStep.PASSWORD_RESET -> {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                authViewModel.clearMessages()
                            },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !authUiState.isLoading,
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                authViewModel.clearMessages()
                            },
                            label = { Text("Confirm Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !authUiState.isLoading,
                            trailingIcon = {
                                IconButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                            Text(
                                text = "Passwords do not match",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (newPassword == confirmPassword) {
                                    authViewModel.resetPassword(email, verificationCode, newPassword) { success ->
                                        if (success) {
                                            // Automatically log in the user after successful password reset
                                            authViewModel.login(email, newPassword, true)
                                            onSuccess()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !authUiState.isLoading &&
                                     newPassword.isNotEmpty() &&
                                     confirmPassword.isNotEmpty() &&
                                     newPassword == confirmPassword,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (authUiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Reset Password",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                // Error/Success Messages
                authUiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                authUiState.successMessage?.let { success ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = success,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
