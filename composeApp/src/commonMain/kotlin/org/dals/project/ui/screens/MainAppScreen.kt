package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import org.dals.project.viewmodel.LoanViewModel
import org.dals.project.viewmodel.NotificationViewModel
import org.dals.project.viewmodel.TransactionViewModel
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
    DATETIME_SETTINGS, TRANSACTION_DETAILS, MANAGE_CARDS, ADD_CARD, CARD_TRANSACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    authViewModel: AuthViewModel,
    loanViewModel: LoanViewModel,
    transactionViewModel: TransactionViewModel,
    notificationViewModel: NotificationViewModel,
    cardViewModel: org.dals.project.viewmodel.CardViewModel,
    inactivityManager: org.dals.project.utils.InactivityManager? = null,
    onNavigateToKYCRequired: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

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
                            // Notification Icon with badge
                            Box {
                                IconButton(
                                    onClick = {
                                        currentDrawerScreen = DrawerScreen.NOTIFICATIONS
                                        scope.launch {
                                            drawerState.open()
                                        }
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
                                        onNavigateToBankAccounts = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS }
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
                                onNavigateToBankAccounts = { currentDrawerScreen = DrawerScreen.ACCOUNT_DETAILS }
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
                                onNavigateBack = { currentDrawerScreen = DrawerScreen.SETTINGS }
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
                    }
                }
            }
        }
    }
}