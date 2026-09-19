package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Zenith-style Material 3 Expressive contained loading indicator ported directly from Petal Browser.
 *
 * The container uses the theme primary color and the animated indicator uses onPrimary,
 * so it automatically follows light/dark and dynamic color schemes.
 */
@Composable
fun ZenithContainedLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ZenithContainedLoadingContainerColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onPrimary,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "ZenithContainedLoadingIndicatorColor",
    )

    Surface(
        modifier = modifier
            .requiredSize(40.dp)
            .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
            .semantics { stateDescription = "Loading..." },
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 6.dp,
        tonalElevation = 4.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.requiredSize(20.dp),
                color = indicatorColor,
                strokeWidth = 2.5.dp,
                trackColor = containerColor.copy(alpha = 0.2f),
            )
        }
    }
}

/**
 * RefreshBar pull-to-refresh loading indicator utilizing [ZenithContainedLoadingIndicator]
 * ported directly from Petal Browser (`com.petal.browser.compose.composable.RefreshBarLoadingIndicator`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshBarLoadingIndicator(
    isRefreshing: Boolean,
    onRefresh: () -> Unit = {},
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
            val offsetY = if (isRefreshing) 20.dp else if (!isVisible) 0.dp else ((pullProgress * 56.dp.value).coerceAtMost(70f)).dp
            val currentOpacity = if (isRefreshing) 1.0f else if (!isVisible) 0f else (pullProgress * 1.5f).coerceIn(0f, 1f)
            val targetScale = if (isRefreshing) 1.0f else if (!isVisible) 0f else (0.4f + (pullProgress * 0.6f)).coerceIn(0.4f, 1.0f)

            // Bouncy settle once the indicator commits to refreshing (target snaps to 1.0),
            // rather than animating every intermediate value while the user is still dragging -
            // that keeps the live pull feeling 1:1 with the finger, and only the final pop-in
            // overshoots and settles.
            val currentScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = if (isRefreshing) {
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                } else {
                    spring(stiffness = Spring.StiffnessHigh)
                },
                label = "RefreshBarIndicatorScale",
            )

            ZenithContainedLoadingIndicator(
                modifier = Modifier
                    .requiredSize(40.dp)
                    .graphicsLayer {
                        translationY = if (isVisible) offsetY.toPx() else 0f
                        alpha = if (isVisible) currentOpacity else 0f
                        scaleX = if (isVisible) currentScale else 0f
                        scaleY = if (isVisible) currentScale else 0f
                    },
            )
        }
    }
}
