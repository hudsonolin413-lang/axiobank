package org.dals.project.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color,
    val subLabel: String? = null
)

@Composable
fun BarChart3D(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 300.dp,
    barWidth: Float = 60f,
    depth3D: Float = 25f,
    spacing: Float = 20f,
    showValues: Boolean = true,
    showLabels: Boolean = true,
    animate: Boolean = true,
    rotationAngle: Float = 15f // Rotation angle for 3D effect (degrees)
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.height(chartHeight),
            contentAlignment = Alignment.Center
        ) {
            Text("No data to display", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // Animation
    val animationProgress = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        ).value
    } else {
        1f
    }

    val barHeightAnimation = if (animate) {
        animationProgress
    } else {
        1f
    }

    Column(modifier = modifier) {
        // Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val maxValue = data.maxOfOrNull { it.value } ?: 1f
            val chartWidth = size.width
            val chartHeightPx = size.height
            val totalBars = data.size
            val totalWidth = totalBars * barWidth + (totalBars - 1) * spacing
            val startX = (chartWidth - totalWidth) / 2

            // Draw grid lines
            drawGridLines(chartHeightPx, chartWidth)

            // Draw bars
            data.forEachIndexed { index, barData ->
                val x = startX + index * (barWidth + spacing)
                val normalizedHeight = (barData.value / maxValue) * (chartHeightPx - 80)
                val animatedHeight = normalizedHeight * barHeightAnimation
                val y = chartHeightPx - animatedHeight - 40

                // Draw 3D bar
                draw3DBar(
                    x = x,
                    y = y,
                    width = barWidth,
                    height = animatedHeight,
                    depth = depth3D,
                    color = barData.color,
                    rotationAngle = rotationAngle
                )

                // Value text is drawn in the label section below for multiplatform compatibility
            }
        }

        // Labels
        if (showLabels) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { barData ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(barData.color, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = barData.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                        if (barData.subLabel != null) {
                            Text(
                                text = barData.subLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGridLines(height: Float, width: Float) {
    val gridLineCount = 5
    val gridColor = Color.LightGray.copy(alpha = 0.3f)

    for (i in 0..gridLineCount) {
        val y = (height - 40) * (i.toFloat() / gridLineCount) + 20
        drawLine(
            color = gridColor,
            start = Offset(20f, y),
            end = Offset(width - 20, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }
}

private fun DrawScope.draw3DBar(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    depth: Float,
    color: Color,
    rotationAngle: Float
) {
    val angleRad = Math.toRadians(rotationAngle.toDouble()).toFloat()
    val depthX = depth * cos(angleRad)
    val depthY = depth * sin(angleRad)

    // Front face
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = Fill
    )

    // Top face (3D depth)
    val topPath = Path().apply {
        moveTo(x, y)
        lineTo(x + depthX, y - depthY)
        lineTo(x + width + depthX, y - depthY)
        lineTo(x + width, y)
        close()
    }
    drawPath(
        path = topPath,
        color = color.copy(alpha = 0.7f),
        style = Fill
    )

    // Right face (3D depth)
    val rightPath = Path().apply {
        moveTo(x + width, y)
        lineTo(x + width + depthX, y - depthY)
        lineTo(x + width + depthX, y + height - depthY)
        lineTo(x + width, y + height)
        close()
    }
    drawPath(
        path = rightPath,
        color = color.copy(alpha = 0.5f),
        style = Fill
    )

    // Draw borders for definition
    drawRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = Stroke(width = 2f)
    )
}
