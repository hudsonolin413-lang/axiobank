package org.dals.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.dals.project.navigation.AppNavGraph
import org.dals.project.repository.AuthRepository
import org.dals.project.repository.NotificationRepository
import org.dals.project.ui.theme.AppTheme
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.viewmodel.LoanViewModel
import org.dals.project.viewmodel.NotificationViewModel
import org.dals.project.viewmodel.TransactionViewModel
import org.dals.project.storage.PreferencesStorage
import org.dals.project.utils.InactivityManager
import org.dals.project.utils.InactivityTracker
import org.dals.project.utils.SnackbarManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(preferencesStorage: PreferencesStorage? = null) {
    AppTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Initialize global Snackbar manager
        LaunchedEffect(Unit) {
            SnackbarManager.initialize(snackbarHostState, coroutineScope)
        }

        val authRepository = remember { AuthRepository() }
        val notificationRepository = remember { NotificationRepository(authRepository) }
        val authViewModel = remember { AuthViewModel(authRepository) }
        val loanViewModel = remember { LoanViewModel(authRepository, notificationRepository) }
        val transactionViewModel = remember { TransactionViewModel(authRepository, notificationRepository) }
        val notificationViewModel = remember { NotificationViewModel(authRepository, notificationRepository) }
        val cardViewModel = remember { org.dals.project.viewmodel.CardViewModel(authRepository) }

        // Inactivity manager for auto-logout
        val inactivityManager = remember {
            InactivityManager(
                timeoutMillis = 20_000L, // 20 seconds
                onTimeout = {
//                    println(" Auto-logout due to inactivity")
                    authViewModel.lock()
                }
            ).also {
                authViewModel.setInactivityManager(it)
                transactionViewModel.setInactivityManager(it)
                loanViewModel.setInactivityManager(it)
            }
        }

        // Initialize storage if provided
        LaunchedEffect(preferencesStorage) {
            preferencesStorage?.let { storage ->
                authViewModel.setStorage(storage)
                // Try auto-login after storage is set
                authViewModel.tryAutoLogin()
            }
        }

        // Track if user is logged in to enable/disable inactivity tracking
        val currentUser by authRepository.currentUser.collectAsState()
        val isLoggedIn = currentUser != null

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                InactivityTracker(
                    inactivityManager = inactivityManager,
                    enabled = isLoggedIn
                ) {
                    AppNavGraph(
                        authViewModel = authViewModel,
                        loanViewModel = loanViewModel,
                        transactionViewModel = transactionViewModel,
                        notificationViewModel = notificationViewModel,
                        cardViewModel = cardViewModel,
                        inactivityManager = inactivityManager
                    )
                }
            }
        }
    }
}