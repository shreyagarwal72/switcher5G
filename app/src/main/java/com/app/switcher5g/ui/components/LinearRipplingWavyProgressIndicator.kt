package com.app.switcher5g.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Expressive Rippling Wavy Linear Progress Indicator.
 * Modeled after Flutter's rippling_wavy_progress linear component.
 * Renders a dynamic, continuous sine wave progress line with gradient flow and ripple physics.
 */
@Composable
fun LinearRipplingWavyProgressIndicator(
    progress: Float? = null, // null for indeterminate rippling wave animation
    modifier: Modifier = Modifier,
    label: String? = null,
    height: Dp = 24.dp,
    strokeWidth: Dp = 3.5.dp,
    waveAmplitude: Dp = 4.dp,
    waveWavelength: Dp = 28.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripplingWaveTransition")

    // Continuous rippling phase animation
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseAnimation",
    )

    // Indeterminate shimmer translation
    val shimmerShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerAnimation",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = trackColor.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (!label.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (progress != null) "${(progress * 100).toInt()}%" else "Working…",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                        color = activeColor,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(height / 2)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val centerY = size.height / 2f
                    val strokeWidthPx = strokeWidth.toPx()
                    val amplitudePx = waveAmplitude.toPx()
                    val wavelengthPx = waveWavelength.toPx()

                    val effectiveProgress = (progress ?: 1f).coerceIn(0.05f, 1f)
                    val activeWidth = width * effectiveProgress

                    // 1. Background Track Line
                    drawLine(
                        color = trackColor,
                        start = androidx.compose.ui.geometry.Offset(0f, centerY),
                        end = androidx.compose.ui.geometry.Offset(width, centerY),
                        strokeWidth = strokeWidthPx / 1.5f,
                    )

                    // 2. Rippling Sine Wave Path
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
    }
}
