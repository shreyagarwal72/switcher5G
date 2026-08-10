package com.app.switcher5g.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Modern fluid liquid progress bar with an oscillating sine wave leading edge
 * and glowing gradient fill. Supports fixed progress (0f..1f) or dynamic loading.
 */
@Composable
fun FancyLiquidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isIndeterminate: Boolean = false,
) {
    val infinite = rememberInfiniteTransition(label = "waveTransition")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val indeterminateOffset by infinite.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "indeterminateOffset",
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val effectiveProgress = if (isIndeterminate) indeterminateOffset else progress.coerceIn(0f, 1f)
        val fillWidth = size.width * effectiveProgress
        val waveAmplitude = 5.dp.toPx()
        val waveLength = 45.dp.toPx()

        // Background track pill
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(size.height / 2),
        )

        if (fillWidth > 0f) {
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, waveAmplitude)
                var x = 0f
                while (x <= fillWidth) {
                    val angle = (x / waveLength) + phase
                    val waveOffset = waveAmplitude * sin(angle.toDouble()).toFloat()
                    val y = waveAmplitude + waveOffset
                    lineTo(x, y.coerceIn(0f, size.height))
                    x += 4f
                }
                lineTo(fillWidth, size.height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(primary, tertiary, primary),
                    startX = 0f,
                    endX = fillWidth.coerceAtLeast(1f),
                ),
            )
        }
    }
}
