package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Top refresh progress bar wrapping [LinearRipplingWavyProgressIndicator].
 * Positioned right above main content list during pull-to-refresh and initial loads.
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
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth(),
    ) {
        LinearRipplingWavyProgressIndicator(
            progress = progress,
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
    }
}
