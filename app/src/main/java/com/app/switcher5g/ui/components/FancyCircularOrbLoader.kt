package com.app.switcher5g.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app-wide circular loader. It used to be a plain rotating arc; it now renders Petal's
 * circular wavy loader ([PetalCircularWavyProgressIndicator]) so every spinner in the app
 * (buttons, cards, dialogs, the switching overlay) shares the same Material 3 Expressive look.
 *
 * The name and [size] parameter are kept so existing call sites keep working.
 * [color] defaults to the surrounding content color, so the spinner is readable on filled
 * buttons (onPrimary), outlined/text buttons (primary) and plain surfaces alike; the track is
 * a faint tint of the same color. Stroke and wavelength scale with [size] so the small 16-24dp
 * button spinners keep a clean wave instead of a thick, lumpy ring.
 */
@Composable
fun FancyCircularOrbLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = Color.Unspecified,
) {
    val indicatorColor = if (color.isSpecified) color else LocalContentColor.current
    PetalCircularWavyProgressIndicator(
        modifier = modifier,
        color = indicatorColor,
        trackColor = indicatorColor.copy(alpha = 0.22f),
        size = size,
        strokeWidth = minOf(strokeWidth, size * 0.12f),
        wavelength = size * 0.41f,
    )
}
