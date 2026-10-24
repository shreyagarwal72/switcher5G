package com.app.switcher5g.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Option model for [PetalChoiceButtonGroup]. */
data class PetalButtonGroupItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

/**
 * Pick-one connected toggle button group, ported from Petal Browser's `PetalChoiceButtonGroup`.
 *
 * - A Row of Material 3 Expressive [ToggleButton]s spaced with [ButtonGroupDefaults.ConnectedSpaceBetween].
 * - Leading / middle / trailing connected shapes; the checked button morphs to a full pill with
 *   the stock expressive spring.
 * - Checked = primaryContainer / onPrimaryContainer, unchecked = surfaceContainerHighest.
 * - Light haptic tick on every selection, [Role.RadioButton] semantics.
 * - Crowded groups (more than four options, or long labels) scroll horizontally instead of
 *   truncating their labels.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> PetalChoiceButtonGroup(
    options: List<PetalButtonGroupItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 48.dp,
    scrollableIfCrowded: Boolean = true,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val shouldScroll = scrollableIfCrowded && (options.size > 4 || options.any { it.label.length > 12 })

    val unselectedContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val unselectedContent = MaterialTheme.colorScheme.onSurface
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val selectedContent = MaterialTheme.colorScheme.onPrimaryContainer

    val rowModifier = if (shouldScroll) {
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val count = options.size
        options.forEachIndexed { index, option ->
            val isChecked = option.value == selected

            val baseModifier = if (shouldScroll) {
                Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .semantics { role = Role.RadioButton }
            } else {
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = minHeight)
                    .semantics { role = Role.RadioButton }
            }

            val shapes = when {
                count == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            ToggleButton(
                checked = isChecked,
                onCheckedChange = {
                    if (!isChecked) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(option.value)
                    }
                },
                modifier = baseModifier,
                enabled = enabled && option.enabled,
                shapes = shapes,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = unselectedContainer,
                    contentColor = unselectedContent,
                    checkedContainerColor = selectedContainer,
                    checkedContentColor = selectedContent,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (option.icon != null) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (option.label.isNotEmpty()) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/** Index-based convenience overload for simple label (and optional icon) lists. */
@Composable
fun PetalChoiceButtonGroup(
    labels: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null,
    minHeight: Dp = 48.dp,
    scrollableIfCrowded: Boolean = true,
    enabled: Boolean = true,
) {
    val options = labels.mapIndexed { index, label ->
        PetalButtonGroupItem(value = index, label = label, icon = icons?.getOrNull(index))
    }
    PetalChoiceButtonGroup(
        options = options,
        selected = selectedIndex,
        onSelect = onSelectIndex,
        modifier = modifier,
        minHeight = minHeight,
        scrollableIfCrowded = scrollableIfCrowded,
        enabled = enabled,
    )
}
