package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToKYC: () -> Unit = {}
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Empty title in the center
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    // Profile title on the right side
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        authUiState.currentUser?.let { user ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Header Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Credit Score Badge
                            Surface(
                                color = when {
                                    user.creditScore >= 750 -> MaterialTheme.colorScheme.primaryContainer
                                    user.creditScore >= 650 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "Credit Score: ${user.creditScore}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Personal Information Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            ProfileDetailRow("Full Name", user.fullName)
                            ProfileDetailRow("Customer Number", user.customerNumber)
                            ProfileDetailRow("Email", user.email)
                            ProfileDetailRow("Phone", user.phoneNumber)
                            if (user.accountNumber != null) {
                                ProfileDetailRow("Account Number", user.accountNumber)
                            }
                            ProfileDetailRow("Member Since", user.joinedDate)
                        }
                    }
                }

                // Wallet Information Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Wallet Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            ProfileDetailRow(
                                "Wallet Address",
                                "${user.walletAddress.take(10)}...${user.walletAddress.takeLast(8)}"
                            )
                        }
                    }
                }

                // Verification Status Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Verification Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KYC Status",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    color = when (user.kycStatus) {
                                        KycStatus.VERIFIED -> MaterialTheme.colorScheme.primaryContainer
                                        KycStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                                        KycStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = user.kycStatus.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            // Show KYC status message
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (user.kycStatus) {
                                    KycStatus.VERIFIED -> "Your identity has been verified"
                                    KycStatus.PENDING -> "Identity verification is in progress"
                                    KycStatus.REJECTED -> "Identity verification needs attention"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (user.kycStatus) {
                                    KycStatus.VERIFIED -> MaterialTheme.colorScheme.primary
                                    KycStatus.PENDING -> MaterialTheme.colorScheme.secondary
                                    KycStatus.REJECTED -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }

                // Financial Summary Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Financial Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            ProfileDetailRow("Total Borrowed", "$${formatCurrency(user.totalBorrowed)}")
                            ProfileDetailRow("Total Repaid", "$${formatCurrency(user.totalRepaid)}")
                            ProfileDetailRow("Active Loans", user.activeLoans.toString())
                            ProfileDetailRow("Defaulted Loans", user.defaultedLoans.toString())

                            Spacer(modifier = Modifier.height(12.dp))

                            // Repayment Progress
                            if (user.totalBorrowed > 0) {
                                val repaymentProgress = (user.totalRepaid / user.totalBorrowed).toFloat()

                                Text(
                                    text = "Overall Repayment Progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                LinearProgressIndicator(
                                    progress = { repaymentProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Text(
                                    text = "${(repaymentProgress * 100).toInt()}% repaid overall",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Actions Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Account Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedButton(
                                onClick = onNavigateToEditProfile,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Profile")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (user.kycStatus != KycStatus.VERIFIED) {
                                Button(
                                    onClick = onNavigateToKYC,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = when (user.kycStatus) {
                                            KycStatus.PENDING -> "View KYC Status"
                                            KycStatus.REJECTED -> "Complete KYC Verification"
                                            else -> "Complete KYC Verification"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No user profile found")
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to logout?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will:",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("• Sign you out of your account")
                    Text("• Clear all saved login credentials")
                    Text("• Disable auto-login")
                    Text("• Require manual sign-in next time")

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can always sign back in with your username and password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    }
                ) {
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return amount.toString()
}