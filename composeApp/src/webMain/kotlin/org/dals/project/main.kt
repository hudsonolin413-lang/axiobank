package org.dals.project

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.dals.project.storage.createPreferencesStorage

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Mount the Compose app into the explicit container on the landing page
    ComposeViewport("app-root") {
        App(preferencesStorage = createPreferencesStorage())
    }
}