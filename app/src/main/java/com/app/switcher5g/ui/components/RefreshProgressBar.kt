package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Top refresh progress indicator wrapping Petal's [LinearRipplingWavyProgressIndicator]
 * and [ExpressivePullToRefreshWaterRipple].
 */
@Composable
fun RefreshProgressBar(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Refreshing network state…",
    progress: Float? = null,
) {
    AnimatedVisibility(
        visible = isRefreshing,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            LinearRipplingWavyProgressIndicator(
                progress = progress,
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
