package com.app.switcher5g.ui.predictive

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Predictive back + depth blur for Switcher 5G, ported from Petal Browser's
 * `PetalPredictiveJunction` / `PetalScreenWrapper` / `PetalTransitions`.
 *
 * Switcher is a root screen (Home) with tab-like screens (Settings, About) that sit on top of it,
 * exactly like Petal's pushed screens. Home therefore stays composed underneath, and during a back
 * gesture the real Home surface is what you see behind the departing screen, blurred and dimmed,
 * clearing as the gesture completes.
 */

// Material 3 "emphasized" easing - cubic-bezier(0.2, 0, 0, 1.0).
val PetalM3EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// Pop exit slide easing (cubic), as in Petal.
private val PetalPopExitSlideEasing = Easing { f -> f * f * f }

const val PETAL_TRANSITION_DURATION = 350
private const val POP_EXIT_MAX_SCALE_DELTA = 0.15f
private const val MAX_DEPTH_BLUR_DP = 24f
private const val CORNER_RADIUS_DP = 32f

/** Live state of an in-flight predictive back gesture. */
data class PredictiveBackState(
    val isActive: Boolean = false,
    val progress: Float = 0f,
    val swipeEdge: Int = BackEventCompat.EDGE_LEFT,
) {
    companion object {
        val Idle = PredictiveBackState()
    }
}

/** Lets any descendant react to the gesture progress live (for example to fade chrome). */
val LocalPredictiveBackState = compositionLocalOf { PredictiveBackState.Idle }

/**
 * Hosts [root] with [overlay] screens stacked above it.
 *
 * @param route          current route. Equal to [rootRoute] when only the root is shown.
 * @param routeOrder     ordering used to pick the slide direction when swapping overlay screens.
 * @param onBack         pop to the root (invoked on back press and on a committed gesture).
 * @param predictiveEnabled live gesture preview on/off (off = instant standard back).
 * @param blurEnabled    depth blur on the screen behind (off = stronger dim instead).
 */
@Composable
fun PetalScreenHost(
    route: String,
    rootRoute: String,
    routeOrder: List<String>,
    onBack: () -> Unit,
    predictiveEnabled: Boolean,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
    root: @Composable () -> Unit,
    overlay: @Composable (String) -> Unit,
) {
    val overlayVisible = route != rootRoute

    val progressAnim = remember { Animatable(0f) }
    var backState by remember { mutableStateOf(PredictiveBackState.Idle) }

    // True from the moment a committed gesture pops the screen until the new state has been
    // applied, so the normal pop transition and the "behind" animation do not play a second time
    // on top of the gesture that already animated the departure.
    var gestureCommitted by remember { mutableStateOf(false) }

    // 0f = root fully in front, 1f = root fully behind an overlay (blurred, dimmed, pushed back).
    val behindAnim = remember { Animatable(if (overlayVisible) 1f else 0f) }
    LaunchedEffect(route) {
        val target = if (overlayVisible) 1f else 0f
        if (gestureCommitted) {
            behindAnim.snapTo(target)
            gestureCommitted = false
        } else if (behindAnim.targetValue != target) {
            behindAnim.animateTo(target, tween(PETAL_TRANSITION_DURATION, easing = PetalM3EmphasizedEasing))
        }
    }
    // Safety net: never leave the committed flag stuck if the route did not actually change.
    LaunchedEffect(gestureCommitted) {
        if (gestureCommitted) {
            delay(600)
            gestureCommitted = false
        }
    }

    if (overlayVisible) {
        if (predictiveEnabled) {
            PredictiveBackHandler(enabled = true) { progressFlow ->
                try {
                    progressFlow.collect { event ->
                        progressAnim.snapTo(event.progress)
                        backState = PredictiveBackState(
                            isActive = true,
                            progress = event.progress,
                            swipeEdge = event.swipeEdge,
                        )
                    }
                    // Gesture committed: finish the departure, then actually pop.
                    progressAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy,
                        ),
                    ) {
                        backState = backState.copy(isActive = true, progress = value)
                    }
                    backState = backState.copy(isActive = true, progress = 1f)
                    gestureCommitted = true
                    try {
                        onBack()
                    } finally {
                        progressAnim.snapTo(0f)
                        backState = PredictiveBackState.Idle
                    }
                } catch (e: CancellationException) {
                    // Gesture cancelled: spring the screen back into place.
                    withContext(NonCancellable) {
                        progressAnim.snapTo(backState.progress)
                        progressAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMedium,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            ),
                        ) {
                            backState = backState.copy(isActive = true, progress = value)
                        }
                        backState = PredictiveBackState.Idle
                    }
                }
            }
        } else {
            BackHandler(enabled = true, onBack = onBack)
        }
    }

    // Read live (draw / layer phase) so a running gesture never recomposes the whole host.
    // While the gesture runs, "behind" follows the gesture; otherwise it follows the animation.
    // After a committed gesture the root is already fully in front for the very first frame.
    val behindProvider: () -> Float = {
        val st = backState
        when {
            st.isActive -> 1f - PetalM3EmphasizedEasing.transform(st.progress.coerceIn(0f, 1f))
            gestureCommitted && !overlayVisible -> 0f
            else -> behindAnim.value
        }.coerceIn(0f, 1f)
    }

    CompositionLocalProvider(LocalPredictiveBackState provides backState) {
        Box(modifier = modifier.fillMaxSize()) {
            // ---- Root (Home) surface, behind the overlay ------------------------------------
            PetalRootLayer(
                behind = behindProvider,
                backState = { backState },
                overlayVisible = overlayVisible,
                blurEnabled = blurEnabled,
                content = root,
            )

            // ---- Overlay screens (Settings / About) ------------------------------------------
            AnimatedContent(
                targetState = route,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val st = backState
                        if (st.isActive) {
                            val p = st.progress.coerceIn(0f, 1f)
                            val eased = PetalM3EmphasizedEasing.transform(p)
                            val slideEased = PetalPopExitSlideEasing.transform(p)
                            val edgeFactor = if (st.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                            val scale = 1f - POP_EXIT_MAX_SCALE_DELTA * eased
                            scaleX = scale
                            scaleY = scale
                            translationX = size.width * edgeFactor * slideEased
                            compositingStrategy = CompositingStrategy.Offscreen
                            val corner = CORNER_RADIUS_DP * eased
                            if (corner > 0.5f) {
                                shape = RoundedCornerShape(corner.dp)
                                clip = true
                                shadowElevation = (16f * eased).dp.toPx()
                            }
                        }
                    },
                transitionSpec = {
                    val from = routeOrder.indexOf(initialState)
                    val to = routeOrder.indexOf(targetState)
                    when {
                        gestureCommitted -> EnterTransition.None togetherWith ExitTransition.None
                        initialState == rootRoute ->
                            petalPushEnter() togetherWith ExitTransition.None
                        targetState == rootRoute ->
                            EnterTransition.None togetherWith petalPopExit()
                        to > from ->
                            petalAxisEnter(forward = true) togetherWith petalAxisExit(forward = true)
                        else ->
                            petalAxisEnter(forward = false) togetherWith petalAxisExit(forward = false)
                    }
                },
                label = "PetalOverlayHost",
            ) { target ->
                if (target == rootRoute) {
                    Box(Modifier.fillMaxSize())
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            // Overlays are opaque: never let touches fall through to Home.
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        overlay(target)
                    }
                }
            }
        }
    }
}

/**
 * The root screen layer. Isolated in its own composable so that the per-frame blur radius, which
 * has to be read at composition time, only recomposes this small scope during transitions and
 * gestures.
 */
@Composable
private fun PetalRootLayer(
    behind: () -> Float,
    backState: () -> PredictiveBackState,
    overlayVisible: Boolean,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val b = behind()
    val gestureActive = backState().isActive
    // No point blurring while fully hidden under an opaque overlay.
    val blurRadius = if (blurEnabled && (gestureActive || (b > 0.01f && b < 0.995f))) {
        (MAX_DEPTH_BLUR_DP * b).dp
    } else {
        0.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .graphicsLayer {
                val amount = behind()
                val st = backState()
                val settled = amount >= 0.995f && !st.isActive
                val edgeFactor = if (st.swipeEdge == BackEventCompat.EDGE_RIGHT) 1f else -1f
                val scale = 1f - 0.06f * amount
                scaleX = scale
                scaleY = scale
                translationX = if (st.isActive) size.width * edgeFactor * (1f / 3f) * amount else 0f
                // Fully covered by an opaque overlay: skip drawing it entirely.
                alpha = if (settled) 0f else 1f
                val corner = CORNER_RADIUS_DP * amount
                if (corner > 0.5f) {
                    shape = RoundedCornerShape(corner.dp)
                    clip = true
                } else {
                    clip = false
                }
                compositingStrategy =
                    if (amount > 0.001f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .then(if (overlayVisible) Modifier.clearAndSetSemantics { } else Modifier),
    ) {
        content()

        // Depth dim on top of the root while it sits behind an overlay.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (if (blurEnabled) 0.40f else 0.75f) * behind() }
                .background(Color.Black),
        )
    }
}

// ---- Transitions (Petal's aospSharedAxis family) ---------------------------------------------

private fun petalPushEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = tween(PETAL_TRANSITION_DURATION, easing = PetalM3EmphasizedEasing),
    ) + fadeIn(animationSpec = tween(PETAL_TRANSITION_DURATION, easing = PetalM3EmphasizedEasing))

private fun petalPopExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(PETAL_TRANSITION_DURATION, easing = PetalPopExitSlideEasing),
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(PETAL_TRANSITION_DURATION, easing = PetalM3EmphasizedEasing),
    )

private fun petalAxisEnter(forward: Boolean): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { if (forward) it else -it },
        animationSpec = tween(400, easing = PetalM3EmphasizedEasing),
    ) + fadeIn(animationSpec = tween(400, easing = PetalM3EmphasizedEasing))

private fun petalAxisExit(forward: Boolean): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { if (forward) -it else it },
        animationSpec = tween(400, easing = PetalM3EmphasizedEasing),
    ) + fadeOut(animationSpec = tween(400, easing = PetalM3EmphasizedEasing))
