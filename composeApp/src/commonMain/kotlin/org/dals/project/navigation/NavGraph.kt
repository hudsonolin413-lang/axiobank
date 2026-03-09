package org.dals.project.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.AuthState
import org.dals.project.model.KycStatus
import org.dals.project.ui.screens.*
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.LoanViewModel
import org.dals.project.viewmodel.NotificationViewModel
import org.dals.project.viewmodel.TransactionViewModel

enum class Screen {
    WELCOME, LOGIN, REGISTER, FORGOT_PASSWORD, KYC_REQUIRED, MAIN_APP, DASHBOARD, LOAN_APPLICATION, LOAN_DETAILS, PROFILE, LOAN_HISTORY
}

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel,
    loanViewModel: LoanViewModel,
    transactionViewModel: TransactionViewModel,
    notificationViewModel: NotificationViewModel,
    cardViewModel: org.dals.project.viewmodel.CardViewModel,
    referralViewModel: org.dals.project.viewmodel.ReferralViewModel,
    offlineViewModel: org.dals.project.viewmodel.OfflineViewModel,
    settingsRepository: org.dals.project.repository.SettingsRepository,
    inactivityManager: org.dals.project.utils.InactivityManager? = null
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(Screen.WELCOME) }
    var selectedLoanId by remember { mutableStateOf("") }

    // On start, if already logged in, go to main app or KYC
    LaunchedEffect(Unit) {
        // Wait a bit to allow tryAutoLogin to potentially change the state
        kotlinx.coroutines.delay(500)
        if (authViewModel.isLoggedIn()) {
            val user = authUiState.currentUser
            if (user != null && user.kycStatus == KycStatus.VERIFIED) {
                currentScreen = Screen.MAIN_APP
            } else if (user != null) {
                currentScreen = Screen.KYC_REQUIRED
            }
        }
    }

    // Handle authentication state changes
    LaunchedEffect(authUiState.authState, authUiState.currentUser) {
        println("🔄 NavGraph: AuthState = ${authUiState.authState}, User = ${authUiState.currentUser?.username}, Current Screen = $currentScreen")
        println("🔄 NavGraph: User KYC Status = ${authUiState.currentUser?.kycStatus}")

        when (authUiState.authState) {
            AuthState.LOGGED_OUT -> {
                // When logged out, stay on the current screen if it's a guest screen
                // Otherwise navigate to Login
                println("🚪 NavGraph: AuthState is LOGGED_OUT, currentScreen = $currentScreen")
                if (currentScreen != Screen.WELCOME && currentScreen != Screen.REGISTER && currentScreen != Screen.FORGOT_PASSWORD && currentScreen != Screen.LOGIN) {
                    currentScreen = Screen.LOGIN
                }
            }
            AuthState.LOCKED -> {
                // When locked, go to Login screen (it will show the "Welcome [User]" view)
                println("🔒 NavGraph: Navigating to LOGIN screen (locked)")
                currentScreen = Screen.LOGIN
            }
            AuthState.LOGGED_IN -> {
                val user = authUiState.currentUser
                println("🔑 NavGraph: LOGGED_IN - User is ${if (user != null) "NOT NULL" else "NULL"}")
                if (user != null) {
                    println("🔑 NavGraph: User details - Username: ${user.username}, KYC: ${user.kycStatus}")
                    if (user.kycStatus == KycStatus.VERIFIED) {
                        println("✅ NavGraph: Navigating to MAIN_APP (verified)")
                        currentScreen = Screen.MAIN_APP
                    } else {
                        println("⚠️ NavGraph: Navigating to KYC_REQUIRED (not verified) - KYC Status is ${user.kycStatus}")
                        println("⚠️ NavGraph: About to set currentScreen to KYC_REQUIRED")
                        currentScreen = Screen.KYC_REQUIRED
                        println("⚠️ NavGraph: Successfully set currentScreen to KYC_REQUIRED")
                    }
                }
            }
            AuthState.LOADING -> {
                println("⏳ NavGraph: Loading, keeping current screen = $currentScreen")
                /* Keep current screen */
            }
        }
    }

    println("📱 NavGraph: Rendering screen = $currentScreen")

    when (currentScreen) {
        Screen.WELCOME -> {
            println("📱 NavGraph: Rendering WELCOME screen")
            WelcomeScreen(
                onNavigateToLogin = {
                    currentScreen = Screen.LOGIN
                },
                onNavigateToRegister = {
                    currentScreen = Screen.REGISTER
                }
            )
        }

        Screen.LOGIN -> {
            println("📱 NavGraph: Rendering LOGIN screen")
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    currentScreen = Screen.REGISTER
                },
                onNavigateToForgotPassword = {
                    currentScreen = Screen.FORGOT_PASSWORD
                },
                onLoginSuccess = {
                    println("✅ NavGraph: Login success callback triggered")
                    val user = authUiState.currentUser
                    println("✅ NavGraph: User after login - ${user?.username}, KYC: ${user?.kycStatus}")
                    if (user != null && user.kycStatus == KycStatus.VERIFIED) {
                        println("✅ NavGraph: Navigating to MAIN_APP from login callback")
                        currentScreen = Screen.MAIN_APP
                    } else {
                        println("✅ NavGraph: Navigating to KYC_REQUIRED from login callback")
                        currentScreen = Screen.KYC_REQUIRED
                    }
                    println("✅ NavGraph: Login success callback completed")
                }
            )
        }

        Screen.REGISTER -> {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    currentScreen = Screen.LOGIN
                },
                onRegisterSuccess = {
                    // After successful registration, redirect to KYC screen
                    currentScreen = Screen.KYC_REQUIRED
                }
            )
        }

        Screen.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    currentScreen = Screen.LOGIN
                },
                onSuccess = {
                    val user = authUiState.currentUser
                    if (user != null && user.kycStatus == KycStatus.VERIFIED) {
                        currentScreen = Screen.MAIN_APP
                    } else if (user != null) {
                        currentScreen = Screen.KYC_REQUIRED
                    } else {
                        currentScreen = Screen.LOGIN
                    }
                }
            )
        }

        Screen.KYC_REQUIRED -> {
            println("📱 NavGraph: Rendering KYC_REQUIRED screen - STARTING")
            KYCRequiredScreen(
                authViewModel = authViewModel,
                onNavigateToMainApp = {
                    currentScreen = Screen.MAIN_APP
                }
            )
            println("📱 NavGraph: KYC_REQUIRED screen composable returned")
        }

        Screen.MAIN_APP -> {
            MainAppScreen(
                authViewModel = authViewModel,
                loanViewModel = loanViewModel,
                transactionViewModel = transactionViewModel,
                notificationViewModel = notificationViewModel,
                cardViewModel = cardViewModel,
                referralViewModel = referralViewModel,
                offlineViewModel = offlineViewModel,
                settingsRepository = settingsRepository,
                inactivityManager = inactivityManager,
                onNavigateToKYCRequired = {
                    currentScreen = Screen.KYC_REQUIRED
                }
            )
        }

        // These screens are now handled within the MainAppScreen
        Screen.DASHBOARD,
        Screen.LOAN_APPLICATION,
        Screen.LOAN_DETAILS,
        Screen.PROFILE,
        Screen.LOAN_HISTORY -> {
            // Fallback to main app if somehow we end up here
            MainAppScreen(
                authViewModel = authViewModel,
                loanViewModel = loanViewModel,
                transactionViewModel = transactionViewModel,
                notificationViewModel = notificationViewModel,
                cardViewModel = cardViewModel,
                referralViewModel = referralViewModel,
                offlineViewModel = offlineViewModel,
                settingsRepository = settingsRepository,
                inactivityManager = inactivityManager,
                onNavigateToKYCRequired = {
                    currentScreen = Screen.KYC_REQUIRED
                }
            )
        }
    }
}

// Keep the old LoanNavGraph for use within the MainAppScreen
@Composable
fun LoanNavGraph(
    viewModel: LoanViewModel,
    authViewModel: AuthViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var selectedLoanId by remember { mutableStateOf("") }

    when (currentScreen) {
        Screen.DASHBOARD -> {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToLoanApplication = {
                    currentScreen = Screen.LOAN_APPLICATION
                },
                onNavigateToLoanDetails = { loanId ->
                    selectedLoanId = loanId
                    currentScreen = Screen.LOAN_DETAILS
                },
                onNavigateToProfile = {
                    currentScreen = Screen.PROFILE
                },
                onNavigateToHistory = {
                    currentScreen = Screen.LOAN_HISTORY
                }
            )
        }

        Screen.LOAN_APPLICATION -> {
            LoanApplicationScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.DASHBOARD
                },
                onNavigateToDashboard = {
                    currentScreen = Screen.DASHBOARD
                }
            )
        }

        Screen.LOAN_DETAILS -> {
            LoanDetailsScreen(
                loanId = selectedLoanId,
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.DASHBOARD
                }
            )
        }

        Screen.PROFILE -> {
            ProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    currentScreen = Screen.DASHBOARD
                }
            )
        }

        Screen.LOAN_HISTORY -> {
            LoanHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.DASHBOARD
                },
                onNavigateToLoanDetails = { loanId ->
                    selectedLoanId = loanId
                    currentScreen = Screen.LOAN_DETAILS
                }
            )
        }

        else -> {
            // Fallback to dashboard
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToLoanApplication = {
                    currentScreen = Screen.LOAN_APPLICATION
                },
                onNavigateToLoanDetails = { loanId ->
                    selectedLoanId = loanId
                    currentScreen = Screen.LOAN_DETAILS
                },
                onNavigateToProfile = {
                    currentScreen = Screen.PROFILE
                },
                onNavigateToHistory = {
                    currentScreen = Screen.LOAN_HISTORY
                }
            )
        }
    }
}