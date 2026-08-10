package com.app.switcher5g.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Switch with the feature's icon riding inside the thumb, modeled from Stride.
 */
@Composable
fun IconSwitch(
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        modifier = modifier.scale(0.9f),
    )
}
