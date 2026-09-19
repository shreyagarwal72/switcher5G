package com.app.switcher5g.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Expressive Rippling Wavy Linear Progress Indicator ported from Petal Browser.
 * Renders a dynamic continuous sine wave progress line with gradient flow and ripple physics.
 */
@Composable
fun LinearRipplingWavyProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    label: String? = null,
    height: Dp = 4.5.dp,
    strokeWidth: Dp = 3.5.dp,
    waveAmplitude: Dp = 3.dp,
    waveWavelength: Dp = 24.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripplingWaveTransition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseAnimation",
    )

    val shimmerShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerAnimation",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val centerY = size.height / 2f
            val strokeWidthPx = strokeWidth.toPx()
            val amplitudePx = waveAmplitude.toPx()
            val wavelengthPx = waveWavelength.toPx()

            val effectiveProgress = (progress ?: 1f).coerceIn(0.04f, 1f)
            val activeWidth = width * effectiveProgress

            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                end = androidx.compose.ui.geometry.Offset(width, centerY),
                strokeWidth = strokeWidthPx / 1.5f,
            )

            val path = Path()
            var first = true

            var x = 0f
            while (x <= activeWidth) {
                val angle = (x / wavelengthPx) * 2f * Math.PI.toFloat() + phase
                val y = centerY + sin(angle).toFloat() * amplitudePx

                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += 2f
            }

            val gradientBrush = Brush.horizontalGradient(
                colors = listOf(activeColor, secondaryColor, activeColor),
                startX = activeWidth * (shimmerShift - 0.5f),
                endX = activeWidth * (shimmerShift + 0.5f),
            )

            drawPath(
                path = path,
                brush = gradientBrush,
                style = Stroke(width = strokeWidthPx),
            )
        }
    }
}
