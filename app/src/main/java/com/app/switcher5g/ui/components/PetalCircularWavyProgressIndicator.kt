package com.app.switcher5g.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Expressive Circular Wavy Progress Indicator, ported 1:1 from Petal Browser
 * (`com.petal.browser.ui.components.PetalCircularWavyProgressIndicator`).
 *
 * Wraps the official Material 3 Expressive `CircularWavyProgressIndicator`:
 * smooth determinate progress when [progress] is given, or continuously rotating
 * wavy ripples when it is null (indeterminate).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PetalCircularWavyProgressIndicator(
    progress: (() -> Float)? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    size: Dp = 44.dp,
    strokeWidth: Dp = 4.dp,
    wavelength: Dp = 18.dp,
    @Suppress("UNUSED_PARAMETER") waveAmplitude: Dp = 2.5.dp,
) {
    val density = LocalDensity.current
    val customStroke = remember(density, strokeWidth) {
        with(density) {
            Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (progress != null) {
            // Determinate Wavy Circular Progress
            CircularWavyProgressIndicator(
                progress = progress,
                color = color,
                trackColor = trackColor,
                stroke = customStroke,
                trackStroke = customStroke,
                wavelength = wavelength,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Indeterminate Wavy Circular Progress
            CircularWavyProgressIndicator(
                color = color,
                trackColor = trackColor,
                stroke = customStroke,
                trackStroke = customStroke,
                wavelength = wavelength,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
