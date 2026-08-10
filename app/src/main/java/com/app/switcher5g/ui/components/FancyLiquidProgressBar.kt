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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Horizontal progress indicator with a Primary->Tertiary gradient fill and a
 * continuously oscillating wave at the fill's leading edge.
 *
 * @param progress 0f..1f
 */
@Composable
fun FancyLiquidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp),
    ) {
        val fillWidth = size.width * progress.coerceIn(0f, 1f)
        val waveAmplitude = 4.dp.toPx()
        val waveLength = 40.dp.toPx()

        // background track
        drawRoundRect(
            color = track,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )

        if (fillWidth > 0f) {
            val path = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, waveAmplitude)
                var x = 0f
                while (x <= fillWidth) {
                    val y = waveAmplitude + waveAmplitude * sin((x / waveLength) + phase)
                    lineTo(x, y.coerceIn(0f, size.height))
                    x += 4f
                }
                lineTo(fillWidth, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(primary, tertiary),
                    startX = 0f,
                    endX = fillWidth.coerceAtLeast(1f),
                ),
            )
        }
    }
}
