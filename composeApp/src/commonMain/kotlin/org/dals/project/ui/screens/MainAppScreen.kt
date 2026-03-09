package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dals.project.ui.components.PieChart3D
import org.dals.project.ui.components.PieChartData
import org.dals.project.ui.screens.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import org.dals.project.model.Transaction
import org.dals.project.navigation.LoanNavGraph
import org.dals.project.ui.components.AppNavigationDrawer
import org.dals.project.ui.components.GradientBackground
import org.dals.project.utils.SettingsManager
import org.dals.project.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dals.project.viewmodel.LoanViewModel
import org.dals.project.viewmodel.NotificationViewModel
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.viewmodel.NfcPaymentViewModel
import org.dals.project.repository.*
import org.dals.project.viewmodel.*
import org.dals.project.API_BASE_URL
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

enum class BottomNavItem(val title: String) {
    HOME("Home"),
    TRANSACT("Transact"),
    LOANS("Loans"),
    PROFILE("Profile")
}

enum class DrawerScreen {
    MAIN_APP, DASHBOARD, TRANSACTIONS, LOANS, SAVINGS, NOTIFICATIONS, SETTINGS, HELP, ABOUT,
    ACCOUNT_DETAILS, STATEMENT, KYC_VERIFICATION, PROFILE_INFORMATION, SECURITY_SETTINGS, PAYMENT_METHODS,
    LANGUAGE_SETTINGS, CURRENCY_SETTINGS, TRANSACTION_LIMITS, AUTOPAY_SETTINGS, LOCATION_SETTINGS,
    DATETIME_SETTINGS, TRANSACTION_DETAILS, MANAGE_CARDS, ADD_CARD, CARD_TRANSACTIONS,
    // New screens
    TWO_FACTOR_AUTH, VIRTUAL_CARDS, SPENDING_ANALYTICS, TRANSACTION_DISPUTES, LOAN_CALCULATOR,
    BUDGET_MANAGEMENT, REFERRAL, SPLIT_BILL, ATM_LOCATOR, BENEFICIARY_MANAGEMENT, DEVICE_MANAGEMENT,
    QR_PAYMENT,
    // Additional new screens
    BULK_TRANSFER, SUB_ACCOUNTS, CRYPTO_WALLET, OVERDRAFT_PROTECTION, TAX_REPORTS,
    CASH_FLOW_FORECAST, INTERNATIONAL_TRANSFER, DIGITAL_SIGNATURE, NFC_PAYMENT, OFFLINE_MODE,
    LOAN_REFINANCING, MORE_SERVICES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    authViewModel: AuthViewModel,
    loanViewModel: LoanViewModel,
    transactionViewModel: TransactionViewModel,
    notificationViewModel: NotificationViewModel,
    cardViewModel: org.dals.project.viewmodel.CardViewModel,
    referralViewModel: org.dals.project.viewmodel.ReferralViewModel,
    offlineViewModel: org.dals.project.viewmodel.OfflineViewModel,
    settingsRepository: org.dals.project.repository.SettingsRepository,
    inactivityManager: org.dals.project.utils.InactivityManager? = null,
    onNavigateToKYCRequired: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    val httpClient = remember {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    val overdraftProtectionViewModel: OverdraftProtectionViewModel = viewModel {
        OverdraftProtectionViewModel(OverdraftProtectionRepository(httpClient, API_BASE_URL))
    }
    val cashFlowForecastViewModel: CashFlowForecastViewModel = viewModel {
        CashFlowForecastViewModel(CashFlowForecastRepository(httpClient, API_BASE_URL))
    }
    val cashFlowViewModel: CashFlowViewModel = viewModel {
        CashFlowViewModel(CashFlowRepository(transactionViewModel.repository), transactionViewModel.repository)
    }
    val loanRefinancingViewModel: LoanRefinancingViewModel = viewModel {
        LoanRefinancingViewModel(LoanRefinancingRepository(httpClient, API_BASE_URL))
    }
    val internationalTransferViewModel: InternationalTransferViewModel = viewModel {
        InternationalTransferViewModel(InternationalTransferRepository(httpClient, API_BASE_URL))
    }
    val cryptoWalletViewModel: CryptoWalletViewModel = viewModel {
        CryptoWalletViewModel(CryptoWalletRepository(httpClient, API_BASE_URL))
    }
    val atmLocatorViewModel: AtmLocatorViewModel = viewModel {
        AtmLocatorViewModel(AtmLocatorRepository(httpClient, API_BASE_URL))
    }
    val budgetManagementViewModel: BudgetManagementViewModel = viewModel {
        BudgetManagementViewModel(BudgetManagementRepository(httpClient, API_BASE_URL))
    }
    val virtualCardViewModel: VirtualCardViewModel = viewModel {
        VirtualCardViewModel(VirtualCardRepository(httpClient, API_BASE_URL))
    }

    var selectedTab by remember { mutableStateOf(BottomNavItem.HOME) }
    var currentDrawerScreen by remember { mutableStateOf(DrawerScreen.MAIN_APP) }
    var loanScreenKey by remember { mutableStateOf(0) }
    var transactScreenKey by remember { mutableStateOf(0) }
    var profileScreenKey by remember { mutableStateOf(0) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var hasCheckedKYC by remember { mutableStateOf(false) }
    var hasSubmittedKYC by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Check if user has submitted KYC documents
    // Reset hasCheckedKYC when KYC status changes to allow re-checking
    LaunchedEffect(authUiState.currentUser?.kycStatus) {
        hasCheckedKYC = false
    }

    LaunchedEffect(authUiState.currentUser?.id, authUiState.currentUser?.kycStatus, authUiState.authState) {
        if (!hasCheckedKYC && authUiState.currentUser != null && authUiState.authState == org.dals.project.model.AuthState.LOGGED_IN) {
            val user = authUiState.currentUser!!

            println("🔍 MainAppScreen: Checking KYC - User ID: ${user.id}, KYC Status: ${user.kycStatus}")

            // If user is already verified, no need to check documents or redirect
            if (user.kycStatus == org.dals.project.model.KycStatus.VERIFIED) {
                println("✅ MainAppScreen: User is VERIFIED, skipping KYC check")
                hasCheckedKYC = true
                hasSubmittedKYC = true
                return@LaunchedEffect
            }

            println("⚠️ MainAppScreen: User KYC status is ${user.kycStatus}, checking documents...")

            val customerId = user.id
            authViewModel.getKYCDocuments(
                customerId = customerId,
                onSuccess = { documents ->
                    hasSubmittedKYC = documents.isNotEmpty()
                    hasCheckedKYC = true

                    // If no KYC documents submitted, redirect to KYC required screen
                    if (documents.isEmpty()) {
                        onNavigateToKYCRequired()
                    }
                },
                onError = { error ->
                    println("Failed to check KYC status: $error")
                    hasCheckedKYC = true
                }
            )
        }
    }

    GradientBackground {
        ModalNavigationDrawer(
            drawerState = drawerState,
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        inactivityManager?.resetTimer()
                    }
                }
            },
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AppNavigationDrawer(
                        currentUser = authUiState.currentUser,
                        onNavigateToSettings = {
                            currentDrawerScreen = DrawerScreen.SETTINGS
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToNotifications = {
                            currentDrawerScreen = DrawerScreen.NOTIFICATIONS
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToHelp = {
                            currentDrawerScreen = DrawerScreen.HELP
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToAbout = {
                            currentDrawerScreen = DrawerScreen.ABOUT
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToDashboard = {
                            currentDrawerScreen = DrawerScreen.DASHBOARD
                            selectedTab = BottomNavItem.HOME
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToTransactions = {
                            currentDrawerScreen = DrawerScreen.TRANSACTIONS
                            selectedTab = BottomNavItem.TRANSACT
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToLoans = {
                            currentDrawerScreen = DrawerScreen.LOANS
                            selectedTab = BottomNavItem.LOANS
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToInvestments = {
                            currentDrawerScreen = DrawerScreen.SAVINGS
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToKYC = {
                            currentDrawerScreen = DrawerScreen.KYC_VERIFICATION
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToSpendingAnalytics = {
                            currentDrawerScreen = DrawerScreen.SPENDING_ANALYTICS
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToBudgetManagement = {
                            currentDrawerScreen = DrawerScreen.BUDGET_MANAGEMENT
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToReferral = {
                            currentDrawerScreen = DrawerScreen.REFERRAL
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToSplitBill = {
                            currentDrawerScreen = DrawerScreen.SPLIT_BILL
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToATMLocator = {
                            currentDrawerScreen = DrawerScreen.ATM_LOCATOR
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToBeneficiaries = {
                            currentDrawerScreen = DrawerScreen.BENEFICIARY_MANAGEMENT
                            scope.launch { drawerState.close() }
                        },
                        onNavigateToLoanCalculator = {
                            currentDrawerScreen = DrawerScreen.LOAN_CALCULATOR
                            scope.launch { drawerState.close() }
                        },
                        onLogout = { 
                            authViewModel.logout()
                            // No need to manually navigate, NavGraph handles AuthState.LOGGED_OUT
                        },
                        onCloseDrawer = {
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { }, // Empty title as requested
                        navigationIcon = {
                            // Menu Icon
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        },
                        actions = {
                            // QR Code Scanner Icon
                            IconButton(
                                onClick = {
                                    currentDrawerScreen = DrawerScreen.QR_PAYMENT
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "QR Payment"
                                )
                            }
                            
                            // Notification Icon with badge
                            Box {
                                IconButton(
                                    onClick = {
                                        // Navigate directly to Notifications screen
                                        currentDrawerScreen = DrawerScreen.NOTIFICATIONS
                                        scope.launch { drawerState.close() }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = "Notifications"
                                    )
                                }

                                // Notification Badge
                                if (notificationUiState.unreadCount > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                                    ) {
                                        Text(
                                            text = if (notificationUiState.unreadCount > 99) "99+" else notificationUiState.unreadCount.toString(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFE5E4E0)
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        BottomNavItem.values().forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = when (item) {
                                            BottomNavItem.HOME -> Icons.Filled.Home
                                            BottomNavItem.TRANSACT -> Icons.Outlined.SwapHoriz
                                            BottomNavItem.LOANS -> Icons.Outlined.AccountBalance
                                            BottomNavItem.PROFILE -> Icons.Filled.Person
                                        },
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = when (item) {
                                            BottomNavItem.HOME -> "Home"
                                            BottomNavItem.PROFILE -> "Profile"
                                            else -> item.title
                                        },
                                        fontWeight = if (selectedTab == item) FontWeight.Bold else FontWeight.Normal,
                                        color = when (item) {
                                            BottomNavItem.HOME -> MaterialTheme.colorScheme.onSurface
                                            BottomNavItem.PROFILE -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                },
                                selected = selectedTab == item && currentDrawerScreen == DrawerScreen.MAIN_APP,
                                onClick = { 
                                    if (selectedTab == item) {
                                        // If already on this tab, just ensure drawer screen is reset
                                        currentDrawerScreen = DrawerScreen.MAIN_APP
                                        // Also reset the internal state of the tab's screen
                                        when (item) {
                                            BottomNavItem.TRANSACT -> transactScreenKey++
                                            BottomNavItem.LOANS -> loanScreenKey++
                                            BottomNavItem.PROFILE -> profileScreenKey++
                                            BottomNavItem.HOME -> { /* Home is usually single-level or handles itself */ }
                                        }
                                    } else {
                                        selectedTab = item
                                        currentDrawerScreen = DrawerScreen.MAIN_APP
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                        }
                    }
                },
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Handle drawer navigation first, then fall back to bottom navigation
                    when (currentDrawerScreen) {
                        DrawerScreen.MAIN_APP -> {
                            // Use bottom navigation when in main app mode
                            when (selectedTab) {
                                BottomNavItem.HOME -> {
                                    HomeScreen(
                                        authViewModel = authViewModel,
                                        transactionViewModel = transactionViewModel,
                                        onNavigateToTransact = { selectedTab = BottomNavItem.TRANSACT },
                                        onNavigateToAccountDetails = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS },
                                        onNavigateToStatement = { currentDrawerScreen = DrawerScreen.STATEMENT },
                                        onTransactionClick = { transaction ->
                                            selectedTransaction = transaction
                                            currentDrawerScreen = DrawerScreen.TRANSACTION_DETAILS
                                        },
                                        onNavigateToCards = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS },
                                        onNavigateToBankAccounts = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS },
                                        onNavigateToMoreServices = { currentDrawerScreen = DrawerScreen.MORE_SERVICES },
                                        onNavigateToCrypto = { currentDrawerScreen = DrawerScreen.CRYPTO_WALLET }
                                    )
                                }

                                BottomNavItem.TRANSACT -> {
                                    key(transactScreenKey) {
                                        TransactScreen(
                                            transactionViewModel = transactionViewModel,
                                            onNavigateToKYC = { currentDrawerScreen = DrawerScreen.KYC_VERIFICATION }
                                        )
                                    }
                                }

                                BottomNavItem.LOANS -> {
                                    key(loanScreenKey) {
                                        LoanNavGraph(
                                            viewModel = loanViewModel,
                                            authViewModel = authViewModel
                                        )
                                    }
                                }

                                BottomNavItem.PROFILE -> {
                                    key(profileScreenKey) {
                                        ProfileScreen(
                                            authViewModel = authViewModel,
                                            onNavigateBack = { selectedTab = BottomNavItem.HOME },
                                            onNavigateToEditProfile = {
                                                currentDrawerScreen = DrawerScreen.PROFILE_INFORMATION
                                            },
                                            onNavigateToKYC = { currentDrawerScreen = DrawerScreen.KYC_VERIFICATION }
                                        )
                                    }
                                }
                            }
                        }

                        DrawerScreen.DASHBOARD -> {
                            HomeScreen(
                                authViewModel = authViewModel,
                                transactionViewModel = transactionViewModel,
                                onNavigateToTransact = {
                                    selectedTab = BottomNavItem.TRANSACT
                                    currentDrawerScreen = DrawerScreen.MAIN_APP
                                },
                                onNavigateToAccountDetails = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS },
                                onNavigateToStatement = { currentDrawerScreen = DrawerScreen.STATEMENT },
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                onTransactionClick = { transaction ->
                                    selectedTransaction = transaction
                                    currentDrawerScreen = DrawerScreen.TRANSACTION_DETAILS
                                },
                                onNavigateToCards = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS },
                                onNavigateToBankAccounts = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS },
                                onNavigateToMoreServices = { currentDrawerScreen = DrawerScreen.MORE_SERVICES },
                                onNavigateToCrypto = { currentDrawerScreen = DrawerScreen.CRYPTO_WALLET }
                            )
                        }

                        DrawerScreen.TRANSACTIONS -> {
                            key(transactScreenKey) {
                                TransactScreen(
                                    transactionViewModel = transactionViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                    onNavigateToKYC = { currentDrawerScreen = DrawerScreen.KYC_VERIFICATION }
                                )
                            }
                        }

                        DrawerScreen.LOANS -> {
                            key(loanScreenKey) {
                                LoanNavGraph(
                                    viewModel = loanViewModel,
                                    authViewModel = authViewModel
                                )
                            }
                        }

                        DrawerScreen.SAVINGS -> {
                            SavingsScreen(
                                authViewModel = authViewModel,
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.NOTIFICATIONS -> {
                            NotificationScreen(
                                notificationViewModel = notificationViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.SETTINGS -> {
                            SettingsScreen(
                                authViewModel = authViewModel,
                                notificationViewModel = notificationViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                onNavigateToProfile = { currentDrawerScreen = DrawerScreen.PROFILE_INFORMATION },
                                onNavigateToKYC = { currentDrawerScreen = DrawerScreen.KYC_VERIFICATION },
                                onNavigateToSecurity = { currentDrawerScreen = DrawerScreen.SECURITY_SETTINGS },
                                onNavigateToPaymentMethods = { currentDrawerScreen = DrawerScreen.PAYMENT_METHODS },
                                onNavigateToHelp = { currentDrawerScreen = DrawerScreen.HELP },
                                onNavigateToAbout = { currentDrawerScreen = DrawerScreen.ABOUT },
                                onNavigateToLanguage = { currentDrawerScreen = DrawerScreen.LANGUAGE_SETTINGS },
                                onNavigateToCurrency = { currentDrawerScreen = DrawerScreen.CURRENCY_SETTINGS },
                                onNavigateToTransactionLimits = { currentDrawerScreen = DrawerScreen.TRANSACTION_LIMITS },
                                onNavigateToAutoPaySettings = { currentDrawerScreen = DrawerScreen.AUTOPAY_SETTINGS },
                                onNavigateToLocationSettings = { currentDrawerScreen = DrawerScreen.LOCATION_SETTINGS },
                                onNavigateToDateTimeSettings = { currentDrawerScreen = DrawerScreen.DATETIME_SETTINGS }
                            )
                        }

                        DrawerScreen.HELP -> {
                            HelpScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.ABOUT -> {
                            AboutScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.ACCOUNT_DETAILS -> {
                            AccountDetailsScreen(
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.STATEMENT -> {
                            StatementScreen(
                                authViewModel = authViewModel,
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.KYC_VERIFICATION -> {
                            KYCVerificationScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = {
                                // Check if we came from profile or settings and navigate accordingly
                                    if (selectedTab == BottomNavItem.PROFILE) {
                                        currentDrawerScreen = DrawerScreen.MAIN_APP
                                    } else {
                                        currentDrawerScreen = DrawerScreen.SETTINGS
                                    }
                                }
                            )
                        }

                        DrawerScreen.PROFILE_INFORMATION -> {
                            ProfileInformationScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = {
                                    // Check if we came from profile or settings and navigate accordingly
                                    if (selectedTab == BottomNavItem.PROFILE) {
                                        currentDrawerScreen = DrawerScreen.MAIN_APP
                                    } else {
                                        currentDrawerScreen = DrawerScreen.SETTINGS
                                    }
                                }
                            )
                        }

                        DrawerScreen.SECURITY_SETTINGS -> {
                            SecuritySettingsScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS },
                                onNavigateToTwoFactorAuth = { currentDrawerScreen = DrawerScreen.TWO_FACTOR_AUTH },
                                onNavigateToDeviceManagement = { currentDrawerScreen = DrawerScreen.DEVICE_MANAGEMENT }
                            )
                        }

                        DrawerScreen.PAYMENT_METHODS -> {
                            PaymentMethodsScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS },
                                onNavigateToManageCards = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS }
                            )
                        }

                        DrawerScreen.MANAGE_CARDS -> {
                            ManageCardsScreen(
                                cardViewModel = cardViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.PAYMENT_METHODS },
                                onAddCard = { currentDrawerScreen = DrawerScreen.ADD_CARD },
                                onUseCard = { currentDrawerScreen = DrawerScreen.CARD_TRANSACTIONS }
                            )
                        }

                        DrawerScreen.ADD_CARD -> {
                            AddCardScreen(
                                cardViewModel = cardViewModel,
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS },
                                onCardAdded = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS }
                            )
                        }

                        DrawerScreen.CARD_TRANSACTIONS -> {
                            CardTransactionsScreen(
                                cardViewModel = cardViewModel,
                                authViewModel = authViewModel,
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS }
                            )
                        }

                        DrawerScreen.LANGUAGE_SETTINGS -> {
                            LanguageSettingsScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.CURRENCY_SETTINGS -> {
                            CurrencySettingsScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.TRANSACTION_LIMITS -> {
                            TransactionLimitsScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.AUTOPAY_SETTINGS -> {
                            AutoPaySettingsScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.LOCATION_SETTINGS -> {
                            // TODO: Create LocationSettingsScreen
                            SettingsScreen(
                                authViewModel = authViewModel,
                                notificationViewModel = notificationViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.DATETIME_SETTINGS -> {
                            // TODO: Create DateTimeSettingsScreen  
                            SettingsScreen(
                                authViewModel = authViewModel,
                                notificationViewModel = notificationViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.TRANSACTION_DETAILS -> {
                            selectedTransaction?.let { transaction ->
                                TransactionDetailsScreen(
                                    transaction = transaction,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            } ?: run {
                                // Fallback to main app if no transaction selected
                                currentDrawerScreen = DrawerScreen.MAIN_APP
                            }
                        }

                        // New screens
                        DrawerScreen.TWO_FACTOR_AUTH -> {
                            TwoFactorAuthScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SECURITY_SETTINGS },
                                onSetupComplete = { currentDrawerScreen = DrawerScreen.SECURITY_SETTINGS }
                            )
                        }

                        DrawerScreen.VIRTUAL_CARDS -> {
                            authUiState.currentUser?.let { user ->
                                VirtualCardsScreen(
                                    cardViewModel = cardViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MANAGE_CARDS }
                                )
                            }
                        }

                        DrawerScreen.SPENDING_ANALYTICS -> {
                            SpendingAnalyticsScreen(
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.TRANSACTION_DISPUTES -> {
                            TransactionDisputeScreen(
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.LOAN_CALCULATOR -> {
                            LoanCalculatorScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                onApplyForLoan = { _, _, _ -> 
                                    selectedTab = BottomNavItem.LOANS
                                    currentDrawerScreen = DrawerScreen.MAIN_APP
                                }
                            )
                        }

                        DrawerScreen.BUDGET_MANAGEMENT -> {
                            BudgetManagementScreen(
                                transactionViewModel = transactionViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.REFERRAL -> {
                            ReferralScreen(
                                authViewModel = authViewModel,
                                referralViewModel = referralViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.SPLIT_BILL -> {
                            org.dals.project.ui.screens.SplitBillScreenNew(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.ATM_LOCATOR -> {
                            org.dals.project.ui.screens.ATMLocatorScreen(
                                viewModel = atmLocatorViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.BENEFICIARY_MANAGEMENT -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.BeneficiaryManagementScreen(
                                    customerId = user.id,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.DEVICE_MANAGEMENT -> {
                            DeviceManagementScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SECURITY_SETTINGS }
                            )
                        }

                        DrawerScreen.QR_PAYMENT -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.QRPaymentScreen(
                                    customerId = user.id,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        // Additional new screens
                        DrawerScreen.BULK_TRANSFER -> {
                            authUiState.currentUser?.let { user ->
                                BulkTransferScreen(
                                    customerId = user.id,
                                    accountId = user.id, // Using user ID as account ID
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.SUB_ACCOUNTS -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.SubAccountsScreen(
                                    customerId = user.id,
                                    parentAccountId = user.id, // Using user ID as parent account ID
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.CRYPTO_WALLET -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.CryptoWalletScreen(
                                    customerId = user.id,
                                    accountId = user.id,
                                    viewModel = cryptoWalletViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.OVERDRAFT_PROTECTION -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.OverdraftProtectionScreen(
                                    customerId = user.id,
                                    accountId = user.id,
                                    viewModel = overdraftProtectionViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.TAX_REPORTS -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.TaxReportsScreen(
                                    userId = user.id,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.CASH_FLOW_FORECAST -> {
                            CashFlowManagementScreen(
                                viewModel = cashFlowViewModel,
                                onBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                            )
                        }

                        DrawerScreen.INTERNATIONAL_TRANSFER -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.InternationalTransferScreen(
                                    customerId = user.id,
                                    accountId = user.id,
                                    viewModel = internationalTransferViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.DIGITAL_SIGNATURE -> {
                            authUiState.currentUser?.let { user ->
                                DigitalSignatureScreen(
                                    customerId = user.id,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.NFC_PAYMENT -> {
                            authUiState.currentUser?.let { user ->
                                val nfcPaymentViewModel: NfcPaymentViewModel = viewModel {
                                    NfcPaymentViewModel(NfcPaymentRepository(httpClient, API_BASE_URL))
                                }
                                NfcPaymentScreen(
                                    customerId = user.id,
                                    accountId = user.id,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                    viewModel = nfcPaymentViewModel
                                )
                            }
                        }

                        DrawerScreen.OFFLINE_MODE -> {
                            OfflineModeScreen(
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
                            )
                        }

                        DrawerScreen.LOAN_REFINANCING -> {
                            authUiState.currentUser?.let { user ->
                                org.dals.project.ui.screens.LoanRefinancingScreen(
                                    customerId = user.id,
                                    viewModel = loanRefinancingViewModel,
                                    onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP }
                                )
                            }
                        }

                        DrawerScreen.MORE_SERVICES -> {
                            MoreServicesScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.MAIN_APP },
                                onNavigateToBulkTransfer = { currentDrawerScreen = DrawerScreen.BULK_TRANSFER },
                                onNavigateToSubAccounts = { currentDrawerScreen = DrawerScreen.SUB_ACCOUNTS },
                                onNavigateToCryptoWallet = { currentDrawerScreen = DrawerScreen.CRYPTO_WALLET },
                                onNavigateToOverdraftProtection = { currentDrawerScreen = DrawerScreen.OVERDRAFT_PROTECTION },
                                onNavigateToTaxReports = { currentDrawerScreen = DrawerScreen.TAX_REPORTS },
                                onNavigateToCashFlowForecast = { currentDrawerScreen = DrawerScreen.CASH_FLOW_FORECAST },
                                onNavigateToInternationalTransfer = { currentDrawerScreen = DrawerScreen.INTERNATIONAL_TRANSFER },
                                onNavigateToDigitalSignature = { currentDrawerScreen = DrawerScreen.DIGITAL_SIGNATURE },
                                onNavigateToNfcPayment = { currentDrawerScreen = DrawerScreen.NFC_PAYMENT },
                                onNavigateToOfflineMode = { currentDrawerScreen = DrawerScreen.OFFLINE_MODE },
                                onNavigateToLoanRefinancing = { currentDrawerScreen = DrawerScreen.LOAN_REFINANCING },
                                onNavigateToSpendingAnalytics = { currentDrawerScreen = DrawerScreen.SPENDING_ANALYTICS },
                                onNavigateToBudgetManagement = { currentDrawerScreen = DrawerScreen.BUDGET_MANAGEMENT },
                                onNavigateToVirtualCards = { currentDrawerScreen = DrawerScreen.VIRTUAL_CARDS },
                                onNavigateToQRPayment = { currentDrawerScreen = DrawerScreen.QR_PAYMENT },
                                onNavigateToATMLocator = { currentDrawerScreen = DrawerScreen.ATM_LOCATOR },
                                onNavigateToBeneficiaryManagement = { currentDrawerScreen = DrawerScreen.BENEFICIARY_MANAGEMENT },
                                onNavigateToSplitBill = { currentDrawerScreen = DrawerScreen.SPLIT_BILL },
                                onNavigateToReferral = { currentDrawerScreen = DrawerScreen.REFERRAL },
                                onNavigateToLoanCalculator = { currentDrawerScreen = DrawerScreen.LOAN_CALCULATOR }
                            )
                        }

                    }
                }
            }
        }
    }
}