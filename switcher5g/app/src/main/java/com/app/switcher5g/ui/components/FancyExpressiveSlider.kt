package com.app.switcher5g.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A slider with M3-Expressive-style "squishy" track and a floating value tooltip.
 * Reports discrete Int values in [min, max] via [onValueChange].
 */
@Composable
fun FancyExpressiveSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 100,
) {
    var isPressed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var progress by remember(value) { mutableFloatStateOf((value - min).toFloat() / (max - min)) }

    val trackHeight by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f),
        label = "trackHeight",
    )
    val tooltipScale by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f),
        label = "tooltipScale",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 180f),
        label = "thumbScale",
    )

    fun updateFromX(x: Float) {
        val clamped = (x / widthPx).coerceIn(0f, 1f)
        progress = clamped
        onValueChange((min + clamped * (max - min)).roundToInt())
    }

    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            // Floating tooltip, positioned along the track by `progress`
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = (progress * widthPx).roundToInt(),
                            y = 0,
                        )
                    }
                    .graphicsLayer {
                        scaleX = tooltipScale
                        scaleY = tooltipScale
                        alpha = tooltipScale.coerceIn(0f, 1f)
                    }
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = value.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // Track + thumb
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(top = 28.dp)
                    .onGloballyPositioned { widthPx = it.size.width.toFloat() }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                updateFromX(it.x)
                                tryAwaitRelease()
                                isPressed = false
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isPressed = true },
                            onDragEnd = { isPressed = false },
                            onDragCancel = { isPressed = false },
                        ) { change, _ ->
                            updateFromX(change.position.x)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                x = (progress * widthPx).roundToInt() - 11.dp.roundToPx(),
                                y = -5,
                            )
                        }
                        .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
        }
    }
}

