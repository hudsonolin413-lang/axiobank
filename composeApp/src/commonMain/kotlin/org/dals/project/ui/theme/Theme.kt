package org.dals.project.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppLightColorScheme = lightColorScheme(
    // Primary colors - Using dark/black instead of blue
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    // Secondary colors - Using grey
    secondary = GreySecondary,
    onSecondary = GreyOnSecondary,
    secondaryContainer = GreySecondaryContainer,
    onSecondaryContainer = GreyOnSecondaryContainer,

    // Tertiary colors - Using green
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,

    // Error colors - Using red
    error = RedError,
    onError = RedOnError,
    errorContainer = RedErrorContainer,
    onErrorContainer = RedOnErrorContainer,

    // Background colors - Pure white
    background = WhiteBackground,
    onBackground = WhiteOnBackground,

    // Surface colors - White and light grey
    surface = WhiteSurface,
    onSurface = WhiteOnSurface,
    surfaceVariant = WhiteSurfaceVariant,
    onSurfaceVariant = WhiteOnSurfaceVariant,
    surfaceContainer = GreySurfaceContainer,
    surfaceContainerHigh = GreySurfaceContainerHigh,
    surfaceContainerHighest = GreySurfaceContainerHighest,

    // Outline colors
    outline = GreyOutline,
    outlineVariant = GreyOutlineVariant,

    // Other system colors
    surfaceTint = DarkPrimary,
    inverseSurface = DarkPrimary,
    inverseOnSurface = WhiteSurface,
    inversePrimary = WhiteSurface,
    scrim = BlackText.copy(alpha = 0.32f)
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        content = content
    )
}