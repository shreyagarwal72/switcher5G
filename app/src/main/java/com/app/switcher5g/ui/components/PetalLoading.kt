package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Petal's Material 3 Expressive contained loading indicator (ported from Petal Browser's
 * `ContainedLoadingIndicator.kt`) and the single loading visual used across the whole app.
 *
 * The morphing shape sits on a rounded container that follows the theme (primary / onPrimary by
 * default), so it tracks light/dark, dynamic colour and the app palette automatically.
 *
 * - [progress] == null  -> indeterminate: the shape keeps morphing while work is in flight.
 * - [progress] != null  -> determinate: the shape morphs in step with the reported 0f..1f value.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    progress: (() -> Float)? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val animatedContainer by animateColorAsState(
        targetValue = containerColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PetalContainedLoadingContainer",
    )
    val animatedIndicator by animateColorAsState(
        targetValue = indicatorColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "PetalContainedLoadingIndicator",
    )
    val semanticsModifier = modifier
        .requiredSize(size)
        .semantics { stateDescription = "Loading" }

    if (progress != null) {
        ContainedLoadingIndicator(
            progress = progress,
            modifier = semanticsModifier,
            containerColor = animatedContainer,
            indicatorColor = animatedIndicator,
        )
    } else {
        ContainedLoadingIndicator(
            modifier = semanticsModifier,
            containerColor = animatedContainer,
            indicatorColor = animatedIndicator,
        )
    }
}

/**
 * Container-less variant of the same expressive loading indicator, for places that already sit on
 * a coloured surface (inside buttons, inside tinted badges). [color] defaults to the surrounding
 * content colour so it stays readable on filled, tonal and outlined buttons alike.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified,
) {
    val tint = if (color == Color.Unspecified) LocalContentColor.current else color
    LoadingIndicator(
        modifier = modifier
            .requiredSize(size)
            .semantics { stateDescription = "Loading" },
        color = tint,
    )
}

/**
 * Pops a [PetalContainedLoadingIndicator] in and out with a bouncy scale. Used in headers and
 * cards to signal background work (SIM scan, mode switch, update check).
 */
@Composable
fun PetalLoadingBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    progress: (() -> Float)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = 0.4f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = 0.4f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
        ) + fadeOut(),
    ) {
        PetalContainedLoadingIndicator(size = size, progress = progress)
    }
}

/**
 * Pull-to-refresh loading indicator (Petal's `RefreshBarLoadingIndicator`). Driven purely by
 * [isRefreshing] and [pullProgress] (0f..1f): it scales/fades in while the user pulls, then pins
 * and morphs while refreshing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RefreshBarLoadingIndicator(
    isRefreshing: Boolean,
    pullProgress: Float = 1.0f,
    modifier: Modifier = Modifier,
) {
    val isVisible = isRefreshing || pullProgress > 0.01f

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .zIndex(500f)
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val offsetY = if (isRefreshing) 20.dp else ((pullProgress * 56f).coerceAtMost(70f)).dp
            val currentOpacity = if (isRefreshing) 1.0f else (pullProgress * 1.5f).coerceIn(0f, 1f)
            val targetScale = if (isRefreshing) 1.0f else (0.4f + (pullProgress * 0.6f)).coerceIn(0.4f, 1.0f)

            // Bouncy settle once the indicator commits to refreshing (target snaps to 1.0), rather
            // than animating every intermediate value while the finger is still dragging - the
            // live pull stays 1:1 with the finger and only the final pop-in overshoots.
            val currentScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = if (isRefreshing) {
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                } else {
                    spring(stiffness = Spring.StiffnessHigh)
                },
                label = "RefreshBarIndicatorScale",
            )

            PetalContainedLoadingIndicator(
                size = 44.dp,
                modifier = Modifier.graphicsLayer {
                    translationY = offsetY.toPx()
                    alpha = currentOpacity
                    scaleX = currentScale
                    scaleY = currentScale
                },
            )
        }
    }
}
