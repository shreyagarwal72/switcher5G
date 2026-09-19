package com.app.switcher5g.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// Gesture tuning taken 1:1 from Petal's PullToRefreshFrameLayout / BrowserActivity.initPullToRefresh.
private const val PULL_DISTANCE_DP = 80f    // damped drag distance that maps to 100% progress
private const val DRAG_DAMPING = 0.55f      // finger travel -> pull travel
private const val TRIGGER_THRESHOLD = 0.70f // progress needed on release to start a refresh
private const val TICK_PROGRESS = 0.75f     // haptic tick when the pull crosses this progress

// Petal keeps the spinner up for ~600ms on quick refreshes so it never flashes.
private const val MIN_REFRESH_DISPLAY_MS = 600L

@Stable
private class PullGesture {
    /** 0f..1f pull progress fed to the indicator (observed by composition). */
    var progress by mutableFloatStateOf(0f)

    /** True from release-past-threshold until the refresh has really finished (observed). */
    var committed by mutableStateOf(false)

    /** Raw finger travel in px past the top edge. Only read from gesture callbacks. */
    var rawPx = 0f

    /** Set when a second finger lands, so a pinch can never start or continue a pull. */
    var blocked = false

    var tickFired = false

    fun reset() {
        rawPx = 0f
        tickFired = false
        progress = 0f
    }
}

/**
 * Pull-to-refresh for scrollable Compose content, driven by Petal's refresh behaviour:
 *
 *  - the drag maths, thresholds and haptics of Petal's `PullToRefreshFrameLayout`
 *    (80dp pull distance, 0.55 damping, released past 70% starts a refresh, tick at 75%),
 *  - Petal's real `RefreshBarLoadingIndicator` (Material 3 Expressive contained loading
 *    indicator) as the visual, positioned under the status bar.
 *
 * Content is never translated or consumed except for the pull itself, so normal scrolling is
 * untouched. A pull only starts once the child has scrolled to the very top and [canPull]
 * allows it, and it can never fire twice for one gesture.
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
    val haptic = LocalHapticFeedback.current
    val pullDistancePx = with(density) { PULL_DISTANCE_DP.dp.toPx() }

    val currentIsRefreshing by rememberUpdatedState(isRefreshing)
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentCanPull by rememberUpdatedState(canPull)

    val gesture = remember { PullGesture() }

    val controller = remember(pullDistancePx, haptic) {
        PullController(
            gesture = gesture,
            pullDistancePx = pullDistancePx,
            haptic = haptic,
            isRefreshing = { currentIsRefreshing },
            canPull = { currentCanPull() },
            onRefresh = { currentOnRefresh() },
        )
    }

    // Keep the spinner pinned after release until the caller reports the refresh is over
    // (with a short minimum so instant refreshes don't flash), then hide it. This also covers
    // a refresh that finishes before the caller's isRefreshing ever became visible to us.
    LaunchedEffect(gesture.committed) {
        if (gesture.committed) {
            delay(MIN_REFRESH_DISPLAY_MS)
            snapshotFlow { currentIsRefreshing }.first { !it }
            gesture.committed = false
            gesture.reset()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Observes (never consumes) raw pointers on the Initial pass: detects the finger
            // lifting even if no fling callback arrives, and rejects multi-touch (pinch) like
            // Petal's PullToRefreshFrameLayout.
            .pointerInput(controller) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    controller.onGestureStart()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) controller.onMultiTouch()
                    } while (event.changes.any { it.pressed })
                    controller.onRelease()
                }
            }
            .nestedScroll(controller.connection),
    ) {
        content()

        RefreshBarLoadingIndicator(
            isRefreshing = isRefreshing || gesture.committed,
            pullProgress = if (isRefreshing || gesture.committed) 1f else gesture.progress,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .zIndex(500f),
        )
    }
}

private class PullController(
    private val gesture: PullGesture,
    private val pullDistancePx: Float,
    private val haptic: HapticFeedback,
    private val isRefreshing: () -> Boolean,
    private val canPull: () -> Boolean,
    private val onRefresh: () -> Unit,
) {
    private fun progressFor(rawPx: Float): Float = rawPx * DRAG_DAMPING / pullDistancePx

    private fun publish() {
        val progress = progressFor(gesture.rawPx).coerceIn(0f, 1f)
        gesture.progress = progress
        if (progress >= TICK_PROGRESS) {
            if (!gesture.tickFired) {
                gesture.tickFired = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            gesture.tickFired = false
        }
    }

    fun onGestureStart() {
        gesture.blocked = false
    }

    fun onMultiTouch() {
        gesture.blocked = true
        if (gesture.rawPx > 0f) gesture.reset()
    }

    /** Finger lifted: start a refresh once, or spring the indicator away. Idempotent. */
    fun onRelease(): Boolean {
        val raw = gesture.rawPx
        if (raw <= 0f) return false
        val triggered = progressFor(raw) >= TRIGGER_THRESHOLD && !isRefreshing() && !gesture.committed
        gesture.rawPx = 0f
        gesture.tickFired = false
        if (triggered) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            gesture.progress = 1f
            gesture.committed = true
            onRefresh()
        } else {
            gesture.progress = 0f
        }
        return true
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            val raw = gesture.rawPx
            // Finger travelling back up mid-pull: retract the pull first and only let the
            // content scroll once the indicator is fully gone. Consume exactly what we used.
            if (raw > 0f && available.y < 0f) {
                val used = minOf(raw, -available.y)
                gesture.rawPx = raw - used
                publish()
                return Offset(0f, -used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.UserInput || gesture.blocked) return Offset.Zero
            // Downward travel the child could not use means it is at the top.
            if (available.y > 0f && (gesture.rawPx > 0f || (!isRefreshing() && !gesture.committed && canPull()))) {
                gesture.rawPx += available.y
                publish()
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // Swallow the fling that ends a pull so it can't overscroll the content.
            return if (onRelease()) available else Velocity.Zero
        }
    }
}
