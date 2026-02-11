package org.dals.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import org.dals.project.model.AuthState
import org.dals.project.utils.rememberBiometricManager
import org.dals.project.utils.SettingsManager

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle auth state changes - clear password on logout but keep username for quick re-login
    LaunchedEffect(authUiState.authState, authUiState.savedCredentials, authUiState.isRememberMeEnabled) {
        when (authUiState.authState) {
            AuthState.LOGGED_OUT -> {
                println("🧹 LoginScreen: User logged out - clearing password only")
                // Keep username for quick re-login, only clear password
                password = ""
                rememberMe = false
                // If lastUsername is available, pre-fill it
                authUiState.lastUsername?.let { lastUser ->
                    username = lastUser
                }
            }
            AuthState.LOCKED -> {
                // Auto-fill credentials when locked (not logged out)
                if (authUiState.isRememberMeEnabled) {
                    authUiState.savedCredentials.first?.let { savedUsername ->
                        username = savedUsername
                    }
                    authUiState.savedCredentials.second?.let { savedPassword ->
                        password = savedPassword
                    }
                    rememberMe = true
                }
            }
            else -> { /* No action needed */ }
        }
    }

    // Fill in last username on initial load or when it changes
    LaunchedEffect(authUiState.lastUsername) {
        authUiState.lastUsername?.let { lastUser ->
            if (username.isEmpty()) {
                username = lastUser
            }
        }
    }

    LaunchedEffect(authUiState.successMessage) {
        if (!authUiState.successMessage.isNullOrEmpty()) {
            onLoginSuccess()
            authViewModel.clearMessages() // Clear messages to avoid re-triggering
        }
    }

    val biometricManager = rememberBiometricManager()
    val appSettings by SettingsManager.settingsRepository.appSettings.collectAsStateWithLifecycle()

    // Only trigger biometric login if user is LOCKED, not LOGGED_OUT
    // This prevents auto-login immediately after logout
    LaunchedEffect(authUiState.authState) {
        println("🔒 LoginScreen: AuthState = ${authUiState.authState}, Biometric enabled = ${appSettings.isBiometricEnabled}, Available = ${biometricManager.isBiometricAvailable()}")
        if (authUiState.authState == AuthState.LOCKED &&
            appSettings.isBiometricEnabled &&
            biometricManager.isBiometricAvailable() &&
            authUiState.savedCredentials.first != null) {
            println("🔓 LoginScreen: Triggering biometric authentication")
            biometricManager.authenticate(
                title = "Unlock Axio Bank",
                subtitle = "Use your fingerprint to sign in",
                onSuccess = {
                    println("✅ LoginScreen: Biometric authentication successful, logging in...")
                    authViewModel.loginWithBiometrics()
                },
                onError = { error ->
                    println("❌ Biometric error: $error")
                }
            )
        }
    }

    val effectiveLastUsername = authUiState.lastUsername ?: authUiState.currentUser?.username

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
                // Logo and Title
                Image(
                    painter = painterResource(Res.drawable.AxioBank),
                    contentDescription = "Axio Bank Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 8.dp)
                )

                if (effectiveLastUsername != null) {
                    Text(
                        text = "Welcome back, $effectiveLastUsername",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Axio Bank",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = if (effectiveLastUsername != null) "Please enter your password to continue" else "Sign in to your account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Username/Email Field - always show when logged out or if no username is set
                if (authUiState.authState == AuthState.LOGGED_OUT || effectiveLastUsername == null) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            authViewModel.clearMessages()
                        },
                        label = { Text("Email or Username") },
                        placeholder = { Text("Enter your email or username") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        enabled = !authUiState.isLoading
                    )
                } else {
                    // For LOCKED state, hide username field (it's already filled)
                    // Update username state for login if it's currently empty but we have effectiveLastUsername
                    LaunchedEffect(effectiveLastUsername) {
                        if (username.isEmpty()) {
                            username = effectiveLastUsername
                        }
                    }
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        authViewModel.clearMessages()
                    },
                    label = { Text("Password") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            enabled = !authUiState.isLoading
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (authUiState.isRememberMeEnabled || effectiveLastUsername != null) {
                        TextButton(
                            onClick = {
                                if (authUiState.authState == AuthState.LOCKED) {
                                    authViewModel.logout()
                                } else {
                                    authViewModel.clearSavedCredentials()
                                }
                                username = ""
                                password = ""
                                rememberMe = false
                            },
                            enabled = !authUiState.isLoading
                        ) {
                        Text(
                            text = "Switch Account",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        }
                    }
                }

                // Forgot Password Link
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    enabled = !authUiState.isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Error Message
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

                Button(
                    onClick = {
                        println("🔐 LoginScreen: Manual login button clicked - Username: $username, Password filled: ${password.isNotEmpty()}")
                        authViewModel.login(username, password, rememberMe)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !authUiState.isLoading && username.isNotEmpty() && password.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (authUiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (biometricManager.isBiometricAvailable() && authUiState.savedCredentials.first != null) {
                    OutlinedButton(
                        onClick = {
                            biometricManager.authenticate(
                                title = "Unlock Axio Bank",
                                subtitle = "Use your fingerprint to sign in",
                                onSuccess = {
                                    authViewModel.loginWithBiometrics()
                                },
                                onError = { error ->
                                    println("Biometric error: $error")
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !authUiState.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with Fingerprint",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Register Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !authUiState.isLoading
                    ) {
                        Text(
                            text = "Sign Up",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}