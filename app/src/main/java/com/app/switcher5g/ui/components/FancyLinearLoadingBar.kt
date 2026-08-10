package com.app.switcher5g.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Expressive M3 Linear Loading Bar Component.
 * Supports both Determinate (progress 0.0 .. 1.0) and Indeterminate (flowing shimmer wave) modes.
 */
@Composable
fun FancyLinearLoadingBar(
    modifier: Modifier = Modifier,
    progress: Float? = null, // null for indeterminate, 0.0..1.0 for determinate
    barHeight: Dp = 8.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = false,
    label: String? = null,
) {
    val isIndeterminate = progress == null
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "LinearProgressAnimation",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "LinearShimmerTransition")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ShimmerOffset",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label != null || (showPercentage && !isIndeterminate)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showPercentage && !isIndeterminate) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = primaryColor,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(CircleShape)
                .background(trackColor),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val cornerRadius = CornerRadius(height / 2f, height / 2f)

                if (isIndeterminate) {
                    // Indeterminate flowing wave
                    val barWidth = width * 0.4f
                    val left = shimmerOffset * width
                    val right = left + barWidth

                    val brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.1f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.9f),
                            primaryColor.copy(alpha = 0.1f),
                        ),
                        startX = left,
                        endX = right,
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left.coerceIn(-barWidth, width), 0f),
                        size = Size(barWidth, height),
                        cornerRadius = cornerRadius,
                    )
                } else {
                    // Determinate fill bar with glowing head
                    val fillWidth = width * animatedProgress

                    if (fillWidth > 0f) {
                        val brush = Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.7f),
                                primaryColor,
                            ),
                        )

                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(0f, 0f),
                            size = Size(fillWidth, height),
                            cornerRadius = cornerRadius,
                        )

                        // Shimmer accent effect over active progress fill
                        val shimmerWidth = fillWidth * 0.5f
                        val shimmerLeft = (shimmerOffset * fillWidth) - shimmerWidth
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                            startX = shimmerLeft.coerceAtLeast(0f),
                            endX = (shimmerLeft + shimmerWidth).coerceAtMost(fillWidth),
                        )

                        drawRoundRect(
                            brush = shimmerBrush,
                            topLeft = Offset(0f, 0f),
                            size = Size(fillWidth, height),
                            cornerRadius = cornerRadius,
                        )
                    }
                }
            }
        }
    }
}
