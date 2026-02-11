package org.dals.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.dals.project.storage.createPreferencesStorage

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Axio Bank",
    ) {
        App(preferencesStorage = createPreferencesStorage())
    }
}