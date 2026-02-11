package org.dals.project

import androidx.compose.ui.window.ComposeUIViewController
import org.dals.project.storage.createPreferencesStorage

fun MainViewController() = ComposeUIViewController {
    App(preferencesStorage = createPreferencesStorage())
}