package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.model.*
import org.dals.project.viewmodel.AuthViewModel
import org.dals.project.ui.components.PhoneNumberInput
import org.dals.project.ui.components.Country
import org.dals.project.ui.components.countries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInformationScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }

    // Initialize fields with current user data
    LaunchedEffect(authUiState.currentUser) {
        authUiState.currentUser?.let { user ->
            fullName = user.fullName
            email = user.email

            // Parse phone number to extract country and number
            val userPhone = user.phoneNumber
            if (userPhone.isNotEmpty()) {
                // Try to find matching country by dial code
                val matchingCountry = countries.find { country ->
                    userPhone.startsWith(country.dialCode)
                }
                if (matchingCountry != null) {
                    selectedCountry = matchingCountry
                    phone = userPhone.removePrefix(matchingCountry.dialCode)
                } else {
                    phone = userPhone
                }
            }

            address = "" // User model doesn't have address field
            dateOfBirth = user.joinedDate // Using joinedDate as placeholder
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Information") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isEditing = !isEditing
                            if (!isEditing) {
                                // Save changes logic here
                                // authViewModel.updateProfile(...)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header
            item {
                ProfileHeaderCard(user = authUiState.currentUser)
            }

            // Basic Information Section
            item {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            enabled = isEditing,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            enabled = false, // Email typically can't be changed
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditing) {
                            PhoneNumberInput(
                                phoneNumber = phone,
                                onPhoneNumberChange = { phone = it },
                                selectedCountry = selectedCountry,
                                onCountrySelected = { country ->
                                    selectedCountry = country
                                },
                                enabled = isEditing
                            )
                        } else {
                            OutlinedTextField(
                                value = if (selectedCountry != null) {
                                    "${selectedCountry!!.dialCode}$phone"
                                } else {
                                    phone
                                },
                                onValueChange = { },
                                label = { Text("Phone Number") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            enabled = isEditing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Address Information Section
            item {
                Text(
                    text = "Address Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            enabled = isEditing,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Account Status Section
            item {
                Text(
                    text = "Account Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AccountStatusCard(user = authUiState.currentUser)
            }

            // Save Button (only visible when editing)
            if (isEditing) {
                item {
                    Button(
                        onClick = {
                            // Save changes logic
                            isEditing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(user: User?) {
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
            // Profile Avatar (placeholder)
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user?.fullName?.take(2)?.uppercase() ?: "??",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.fullName ?: "Unknown User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "@${user?.username ?: "unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Credit Score: ${user?.creditScore ?: 0}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AccountStatusCard(user: User?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Account Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active", // This would come from user data
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "✓ Verified",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Member since: ${formatJoinDate(user?.joinedDate ?: "")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Formats the join date from ISO format to readable format
 * Example: "2026-01-21T18:55:51.507538Z" -> "January 2026"
 */
private fun formatJoinDate(joinedDate: String): String {
    if (joinedDate.isEmpty()) return "Unknown"

    return try {
        // Parse ISO date format: 2026-01-21T18:55:51.507538Z
        val year = joinedDate.substring(0, 4)
        val month = joinedDate.substring(5, 7).toInt()

        val monthName = when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }

        "$monthName $year"
    } catch (e: Exception) {
        "Unknown"
    }
}