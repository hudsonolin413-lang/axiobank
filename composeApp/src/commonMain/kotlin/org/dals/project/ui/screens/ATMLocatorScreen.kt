package org.dals.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dals.project.viewmodel.AtmLocatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ATMLocatorScreen(
    viewModel: AtmLocatorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Default to a known location or ask for permission
        viewModel.findNearby(-1.286389, 36.817223) // Nairobi coordinates
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ATM Locator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by City or Zip") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.search(searchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Nearby ATMs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.atms) { atm ->
                        ListItem(
                            headlineContent = { Text(atm.name) },
                            supportingContent = { Text("${atm.address}, ${atm.city}") },
                            trailingContent = { 
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(atm.status, color = if (atm.status == "OPERATIONAL") androidx.compose.ui.graphics.Color.Green else androidx.compose.ui.graphics.Color.Red)
                                    atm.distance?.let { Text("${(it * 10).toInt() / 10.0} mi", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        )
                    }
                    if (uiState.atms.isEmpty()) {
                        item {
                            Text("No ATMs found in this area", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
