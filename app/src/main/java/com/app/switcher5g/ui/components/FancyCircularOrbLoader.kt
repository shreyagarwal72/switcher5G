package com.app.switcher5g.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glowing circular particle/orb loader ring with smooth rotation and color transitions.
 */
@Composable
fun FancyCircularOrbLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
) {
    val transition = rememberInfiniteTransition(label = "orbLoader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.size(size)) {
        val sweepAngle = 270f
        val strokePx = strokeWidth.toPx()

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    primary.copy(alpha = 0.1f),
                    primary,
                    tertiary,
                    primary.copy(alpha = 0.1f),
                ),
            ),
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}
