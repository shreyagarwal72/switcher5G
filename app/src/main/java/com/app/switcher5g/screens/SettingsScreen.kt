package com.app.switcher5g.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.ui.components.ShizukuSetupDialog
import com.app.switcher5g.ui.components.bouncyClickable
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.ui.theme.AppPalettes
import com.app.switcher5g.ui.theme.ColorStyle
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppFont
import com.app.switcher5g.util.AppPreferences
import com.app.switcher5g.util.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSetupDialog by remember { mutableStateOf(false) }

    if (showSetupDialog) {
        ShizukuSetupDialog(onDismissRequest = { showSetupDialog = false })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp)
                .entrance(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // 1. Theme & Appearance
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(1)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(text = "Theme Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { prefs.themeMode = mode },
                            label = { Text(mode.name) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Pure AMOLED Black",
                    subtitle = "Use pure #000000 black background for AMOLED displays",
                    icon = Icons.Rounded.Contrast,
                    checked = prefs.amoled,
                    onCheckedChange = { prefs.amoled = it },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Text(text = "Typography & Font Family", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppFont.entries.forEach { font ->
                        FilterChip(
                            selected = prefs.appFont == font,
                            onClick = { prefs.appFont = font },
                            label = { Text(font.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Text(text = "Color Palette", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppPalettes.forEach { palette ->
                        FilterChip(
                            selected = prefs.paletteId == palette.id,
                            onClick = { prefs.paletteId = palette.id },
                            label = { Text(palette.label) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Text(text = "Color Style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ColorStyle.entries.forEach { style ->
                        FilterChip(
                            selected = prefs.colorStyle == style,
                            onClick = { prefs.colorStyle = style },
                            label = { Text(style.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Material You Dynamic Colors",
                    subtitle = "Adapt accent colors from wallpaper (Android 12+)",
                    icon = Icons.Rounded.ColorLens,
                    checked = prefs.useDynamicTheme,
                    onCheckedChange = { prefs.useDynamicTheme = it },
                )
            }
        }

        // 2. Network Defaults
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Network Defaults",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Default network mode preselected on launch:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NetworkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.defaultNetworkMode == mode,
                            onClick = { prefs.defaultNetworkMode = mode },
                            label = {
                                Text(
                                    text = when (mode) {
                                        NetworkMode.NR_ONLY -> "5G SA"
                                        NetworkMode.NR_LTE -> "5G NSA"
                                        NetworkMode.LTE_ONLY -> "4G LTE"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Auto-scan SIMs on Launch",
                    subtitle = "Automatically detect active Dual-SIM subscriptions on open",
                    icon = Icons.Rounded.SimCard,
                    checked = prefs.autoScanSims,
                    onCheckedChange = { prefs.autoScanSims = it },
                )
            }
        }

        // 3. Setup Dialog Trigger Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(3)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Shizuku & ADB Setup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Optional helper setup for Shizuku service, permissions, and direct ADB commands.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = { showSetupDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable {},
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Shizuku & ADB Setup Dialog")
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f),
        )
    }
}
