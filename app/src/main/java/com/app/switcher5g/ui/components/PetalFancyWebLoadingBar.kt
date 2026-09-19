package com.app.switcher5g.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Petal's exact website loading progress bar ported from Petal Browser
 * (com.petal.browser.ui.components.PetalFancyWebLoadingBar / PetalProgressBarBridge).
 *
 * Uses AndroidX Material 3 Expressive [LinearWavyProgressIndicator].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalFancyWebLoadingBar(
    progress: Float? = null,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
) {
    if (visible) {
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress.coerceIn(0.05f, 1f) },
                modifier = modifier.fillMaxWidth(),
                color = color,
                trackColor = trackColor,
            )
        } else {
            LinearWavyProgressIndicator(
                modifier = modifier.fillMaxWidth(),
                color = color,
                trackColor = trackColor,
            )
        }
    }
}
