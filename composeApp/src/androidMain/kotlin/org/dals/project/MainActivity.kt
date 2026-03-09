package org.dals.project

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import org.dals.project.storage.PreferencesStorageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("AxioBank", "MainActivity onCreate started")

            // Configure status bar and navigation bar
            enableEdgeToEdge()
            setupStatusBar()

            super.onCreate(savedInstanceState)

            Log.d("AxioBank", "Initializing PreferencesStorage...")
            PreferencesStorageProvider.initialize(this)
            Log.d("AxioBank", "PreferencesStorage initialized")

            Log.d("AxioBank", "Setting up Compose content...")
            setContent {
                App(preferencesStorage = PreferencesStorageProvider.get())
            }

            Log.d("AxioBank", "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("AxioBank", "Fatal error in MainActivity.onCreate", e)
            Log.e("AxioBank", "Error details: ${e.stackTraceToString()}")
            // Re-throw to show system error dialog
            throw RuntimeException("Failed to initialize MainActivity: ${e.message}", e)
        }
    }

    private fun setupStatusBar() {
        // Make status bar transparent and show content behind it
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set status bar icons to dark color (visible on white background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}