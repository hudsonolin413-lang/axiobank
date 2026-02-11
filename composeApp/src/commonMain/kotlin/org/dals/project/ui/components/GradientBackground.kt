package org.dals.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import org.dals.project.ui.theme.GoldishWhiteTop
import org.dals.project.ui.theme.GoldishWhiteMid
import org.dals.project.ui.theme.PureWhiteBottom

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GoldishWhiteTop,    // Light golden cream at top
                        GoldishWhiteMid,    // Slightly more golden in middle  
                        PureWhiteBottom     // Pure white at bottom
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        content()
    }
}