package com.app.switcher5g.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Material 3 Expressive Wheel Loading Indicator component.
 * Displays a multi-ring spinning wheel loader with breathing pulse center.
 */
@Composable
fun FancyExpressiveWheelLoader(
    modifier: Modifier = Modifier,
    size: Int = 48,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pulse(from = 0.85f, to = 1.15f, durationMs = 1200)
                .clip(CircleShape)
                .border(3.dp, color.copy(alpha = 0.35f), CircleShape),
        )

        // Inner circular orb
        FancyCircularOrbLoader(size = (size * 0.75f).dp)
    }
}

/**
 * Modal overlay that blocks user input while NetworkModeUserService is running via Shizuku.
 */
@Composable
fun NetworkModeSwitchLoadingOverlay(
    isSwitching: Boolean,
    modeName: String,
    onDismissRequest: () -> Unit = {},
) {
    if (isSwitching) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(28.dp),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    FancyExpressiveWheelLoader(size = 56, color = MaterialTheme.colorScheme.primary)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Applying Network Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Switching to $modeName via Shizuku shell IPC…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "⚡ Privileged Execution Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
