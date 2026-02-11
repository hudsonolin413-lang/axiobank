package org.dals.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.dals.project.storage.PreferencesStorageProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PreferencesStorageProvider.initialize(this)

        setContent {
            App(preferencesStorage = PreferencesStorageProvider.get())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}