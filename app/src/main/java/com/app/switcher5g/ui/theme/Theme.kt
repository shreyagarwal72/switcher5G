package com.app.switcher5g.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    tertiary = Tertiary,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceVariant = SurfaceVariant,
    background = Background,
)

@Composable
fun Switcher5GTheme(
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
