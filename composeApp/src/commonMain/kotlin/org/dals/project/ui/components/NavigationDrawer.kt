package org.dals.project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dals.project.model.User

data class NavigationDrawerItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun AppNavigationDrawer(
    currentUser: User?,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToKYC: () -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // User Profile Header
        item {
            currentUser?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Credit Score: ${user.creditScore}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Navigation Items
        item {
            Text(
                text = "Main Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        // Main navigation items
        val mainItems = listOf(
            NavigationDrawerItem(
                title = "Dashboard",
                subtitle = "Overview of your account",
                icon = Icons.Default.Dashboard,
                onClick = {
                    onNavigateToDashboard()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "Transactions",
                subtitle = "Send, receive, and manage money",
                icon = Icons.Outlined.SwapHoriz,
                onClick = {
                    onNavigateToTransactions()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "Loans",
                subtitle = "Apply and manage loans",
                icon = Icons.Outlined.AccountBalance,
                onClick = {
                    onNavigateToLoans()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "Locked Savings",
                subtitle = "Save and earn interest",
                icon = Icons.Default.Savings,
                onClick = {
                    onNavigateToInvestments()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "KYC Verification",
                subtitle = "Verify your identity",
                icon = Icons.Default.VerifiedUser,
                onClick = {
                    onNavigateToKYC()
                    onCloseDrawer()
                }
            )
        )

        items(mainItems.size) { index ->
            NavigationDrawerItemCard(mainItems[index])
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        item {
            Text(
                text = "Account & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        // Settings and account items
        val settingsItems = listOf(
            NavigationDrawerItem(
                title = "Settings",
                subtitle = "App preferences and configuration",
                icon = Icons.Default.Settings,
                onClick = {
                    onNavigateToSettings()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "Notifications",
                subtitle = "View all notifications",
                icon = Icons.Default.Notifications,
                onClick = {
                    onNavigateToNotifications()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "Help & Support",
                subtitle = "Get help and contact support",
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = {
                    onNavigateToHelp()
                    onCloseDrawer()
                }
            ),
            NavigationDrawerItem(
                title = "About",
                subtitle = "Learn about Axio Bank",
                icon = Icons.Default.Info,
                onClick = {
                    onNavigateToAbout()
                    onCloseDrawer()
                }
            )
        )

        items(settingsItems.size) { index ->
            NavigationDrawerItemCard(settingsItems[index])
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Logout Button
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLogout()
                        onCloseDrawer()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Sign out of your account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationDrawerItemCard(item: NavigationDrawerItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}