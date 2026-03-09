package org.dals.project.ui.screens

/**
 * Platform-specific camera scanner
 * Opens native camera UI and scans for QR codes
 */
expect suspend fun openCameraScanner(onResult: (String?) -> Unit)
