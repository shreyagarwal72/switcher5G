package com.app.switcher5g.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Discrete Network Mode Slider wrapping [StrideSlider] with step labels above.
 */
@Composable
fun NetworkModeSlider(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val maxIndex = (options.size - 1).coerceAtLeast(1)
    val clampedSelected = selected.coerceIn(0, maxIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Step Labels Above StrideSlider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val isStepSelected = index == clampedSelected
                val textColor = if (isStepSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                val fontWeight = if (isStepSelected) FontWeight.ExtraBold else FontWeight.Medium

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = fontWeight,
                        fontSize = if (isStepSelected) 13.sp else 11.sp,
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (index != selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(index)
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }

        // Stride Slider Component
        StrideSlider(
            value = clampedSelected.toFloat(),
            onValueChange = { floatVal ->
                val step = floatVal.roundToInt().coerceIn(0, maxIndex)
                if (step != selected) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(step)
                }
            },
            valueRange = 0f..maxIndex.toFloat(),
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
