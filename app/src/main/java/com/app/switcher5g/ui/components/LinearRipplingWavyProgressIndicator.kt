package com.app.switcher5g.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expressive Rippling Wavy Linear Progress Indicator ported from Petal Browser.
 * Now directly delegates to AndroidX Material 3 Expressive [LinearWavyProgressIndicator]
 * exactly as done in Petal's website loading bar (`PetalFancyWebLoadingBar`).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinearRipplingWavyProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    label: String? = null,
    height: Dp = 4.5.dp,
    strokeWidth: Dp = 3.5.dp,
    waveAmplitude: Dp = 3.dp,
    waveWavelength: Dp = 24.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
) {
    if (progress != null) {
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0.05f, 1f) },
            modifier = modifier,
            color = activeColor,
            trackColor = trackColor,
        )
    } else {
        LinearWavyProgressIndicator(
            modifier = modifier,
            color = activeColor,
            trackColor = trackColor,
        )
    }
}
