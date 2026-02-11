package org.dals.project.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dals.project.utils.SettingsManager

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val settingsRepository = SettingsManager.settingsRepository
    val appSettings by settingsRepository.appSettings.collectAsStateWithLifecycle()

    var selectedLanguage by remember { mutableStateOf(appSettings.language) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Update selectedLanguage when appSettings changes
    LaunchedEffect(appSettings.language) {
        selectedLanguage = appSettings.language
    }

    val languages = listOf(
        Language("en", "English", "English", "🇺🇸"),
        Language("sw", "Swahili", "Kiswahili", "🇰🇪"),
        Language("fr", "French", "Français", "🇫🇷"),
        Language("es", "Spanish", "Español", "🇪🇸"),
        Language("ar", "Arabic", "العربية", "🇸🇦"),
        Language("pt", "Portuguese", "Português", "🇵🇹"),
        Language("zh", "Chinese", "中文", "🇨🇳"),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp).padding(end = 8.dp)
                            )
                            Text(
                                text = "Choose Language",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select your preferred language for the app interface",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        // Current selection indicator
                        if (selectedLanguage != appSettings.language) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Current: ${settingsRepository.getCurrentLanguageName()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            items(languages) { language ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguage = language.code }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.flag,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selectedLanguage == language.code) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedLanguage != appSettings.language) {
                            showSaveDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedLanguage.isNotEmpty()
                ) {
                    Text(
                        text = if (selectedLanguage != appSettings.language) {
                            "Save Language Preference"
                        } else {
                            "Back to Settings"
                        }
                    )
                }
            }
        }
    }
}