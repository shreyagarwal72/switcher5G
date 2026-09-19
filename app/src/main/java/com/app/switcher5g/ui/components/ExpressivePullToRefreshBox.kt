package com.app.switcher5g.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ExpressivePullToRefreshBox ported from Petal Browser architecture.
 * Provides touch-driven overscroll pull-to-refresh with the elastic M3 Expressive
 * [ExpressivePullToRefreshWaterRipple] water-ripple indicator.
 */
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    canPull: () -> Boolean = { true },
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxDragPx = with(density) { 80.dp.toPx() }
    val triggerThresholdPx = maxDragPx * 0.72f

    val pullOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // When refresh finishes, animate offset back to 0
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && !isDragging) {
            pullOffset.animateTo(0f, tween(300, easing = LinearEasing))
        }
    }

    val pullFraction = (pullOffset.value / maxDragPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRefreshing) {
                if (isRefreshing) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        if (pullOffset.value >= triggerThresholdPx) {
                            scope.launch {
                                pullOffset.animateTo(maxDragPx * 0.5f, tween(200))
                            }
                            onRefresh()
                        } else {
                            scope.launch {
                                pullOffset.animateTo(0f, tween(250))
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            pullOffset.animateTo(0f, tween(250))
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (!canPull() && pullOffset.value <= 0f) return@detectVerticalDragGestures
                        if (dragAmount > 0f || pullOffset.value > 0f) {
                            change.consume()
                            val newOffset = (pullOffset.value + dragAmount * 0.52f).coerceIn(0f, maxDragPx * 1.3f)
                            scope.launch {
                                pullOffset.snapTo(newOffset)
                            }
                        }
                    },
                )
            },
    ) {
        // Main content with subtle downward translation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (pullOffset.value * 0.45f).roundToInt()) },
            content = content,
        )

        // Elastic Water Ripple Indicator from Petal Browser positioned at the top
        if (pullOffset.value > 2f || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (pullOffset.value * 0.6f).roundToInt()) },
                contentAlignment = Alignment.Center,
            ) {
                ExpressivePullToRefreshWaterRipple(
                    isRefreshing = isRefreshing,
                    pullFraction = if (isRefreshing) 1f else pullFraction,
                )
            }
        }
    }
}
