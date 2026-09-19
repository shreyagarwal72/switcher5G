package com.app.switcher5g.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ExpressivePullToRefreshBox ported from Petal Browser architecture.
 *
 * Utilizes [NestedScrollConnection] to seamlessly intercept downward scroll/drag gestures
 * on vertically scrollable child content (e.g. `Column(Modifier.verticalScroll(...))`)
 * without gesture cancellation or conflicts.
 *
 * Displays Petal's exact [RefreshBarLoadingIndicator] containing the Material 3 Expressive
 * [ZenithContainedLoadingIndicator] pill.
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
    val maxDragPx = with(density) { 96.dp.toPx() }
    val triggerThresholdPx = maxDragPx * 0.65f

    val pullOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // When refresh finishes, animate offset back to 0
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && !isDragging) {
            pullOffset.animateTo(0f, tween(300, easing = LinearEasing))
        }
    }

    val pullFraction = (pullOffset.value / triggerThresholdPx).coerceIn(0f, 1.25f)

    val nestedScrollConnection = remember(isRefreshing, canPull, triggerThresholdPx, maxDragPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If user is currently dragging upwards while pullOffset > 0, consume available dy
                if (available.y < 0f && pullOffset.value > 0f) {
                    val consumedY = available.y * 0.5f
                    val newOffset = (pullOffset.value + consumedY).coerceAtLeast(0f)
                    scope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (isRefreshing || !canPull()) return Offset.Zero

                // When scrolled to top (or list cannot scroll up anymore), available.y > 0 means downward drag
                if (available.y > 0f) {
                    isDragging = true
                    val dragDelta = available.y * 0.48f // Elastic drag resistance
                    val newOffset = (pullOffset.value + dragDelta).coerceIn(0f, maxDragPx * 1.3f)
                    scope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isDragging || pullOffset.value > 0f) {
                    isDragging = false
                    if (pullOffset.value >= triggerThresholdPx && !isRefreshing) {
                        scope.launch {
                            pullOffset.animateTo(
                                triggerThresholdPx * 0.8f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            )
                        }
                        onRefresh()
                    } else {
                        scope.launch {
                            pullOffset.animateTo(0f, tween(250))
                        }
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullOffset.value > 0f && !isRefreshing) {
                    isDragging = false
                    if (pullOffset.value >= triggerThresholdPx) {
                        pullOffset.animateTo(
                            triggerThresholdPx * 0.8f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        )
                        onRefresh()
                    } else {
                        pullOffset.animateTo(0f, tween(250))
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        // Main content with subtle downward spring translation during pull
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (pullOffset.value * 0.42f).roundToInt()) },
            content = content,
        )

        // Petal Contained Loading Indicator Bar positioned at the top
        RefreshBarLoadingIndicator(
            isRefreshing = isRefreshing,
            pullProgress = if (isRefreshing) 1.0f else pullFraction,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(500f),
        )
    }
}
