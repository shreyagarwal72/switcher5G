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
import com.app.switcher5g.ui.components.IconSwitch
import kotlin.math.roundToInt
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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
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

                // Typography & Font Family StrideSlider
                val fontOptions = remember { AppFont.entries.map { it.name.replace("_", " ") } }
                val selectedFontIndex = AppFont.entries.indexOf(prefs.appFont).coerceAtLeast(0)
                SettingStrideSlider(
                    title = "Typography & Font Family",
                    currentLabel = fontOptions.getOrElse(selectedFontIndex) { "" },
                    options = fontOptions,
                    selectedIndex = selectedFontIndex,
                    onSelectIndex = { idx -> prefs.appFont = AppFont.entries[idx] },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Color Palette StrideSlider
                val paletteOptions = remember { AppPalettes.map { it.label } }
                val selectedPaletteIndex = AppPalettes.indexOfFirst { it.id == prefs.paletteId }.coerceAtLeast(0)
                SettingStrideSlider(
                    title = "Color Palette",
                    currentLabel = paletteOptions.getOrElse(selectedPaletteIndex) { "" },
                    options = paletteOptions,
                    selectedIndex = selectedPaletteIndex,
                    onSelectIndex = { idx -> prefs.paletteId = AppPalettes[idx].id },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Color Style StrideSlider
                val styleOptions = remember { ColorStyle.entries.map { it.name.replace("_", " ") } }
                val selectedStyleIndex = ColorStyle.entries.indexOf(prefs.colorStyle).coerceAtLeast(0)
                SettingStrideSlider(
                    title = "Color Style",
                    currentLabel = styleOptions.getOrElse(selectedStyleIndex) { "" },
                    options = styleOptions,
                    selectedIndex = selectedStyleIndex,
                    onSelectIndex = { idx -> prefs.colorStyle = ColorStyle.entries[idx] },
                )

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

                Text(
                    text = "Quick Settings Toggle Modes (2-Mode Tile):",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "First Mode:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NetworkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.toggleMode1 == mode,
                            onClick = { prefs.toggleMode1 = mode },
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

                Text(
                    text = "Second Mode:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NetworkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.toggleMode2 == mode,
                            onClick = { prefs.toggleMode2 = mode },
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

        // 3. Backup & Restore Settings Card
        var showRestoreDialog by remember { mutableStateOf(false) }
        var restoreJsonInput by remember { mutableStateOf("") }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Settings JSON", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste your exported settings JSON configuration below:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            placeholder = { Text("{ \"amoled\": true, ... }") },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = prefs.importFromJsonString(restoreJsonInput)
                            if (success) {
                                Toast.makeText(context, "✅ Settings restored successfully!", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "❌ Invalid JSON format", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(3)
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
                        Icons.Rounded.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Backup & Restore Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Export your app preferences as a JSON backup or restore a previously saved backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val json = prefs.exportToJsonString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Switcher5G Settings Backup", json))
                            Toast.makeText(context, "✅ Settings exported & copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).bouncyClickable {},
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup Settings")
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f).bouncyClickable {},
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Settings")
                    }
                }
            }
        }

        // 4. Execution Mode & Setup Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(4)
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
                        Icons.Rounded.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Execution Method & Privileges",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Select preferred 5G network mode switching engine:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ActivationMethod.entries.forEach { method ->
                        FilterChip(
                            selected = prefs.activationMethod == method,
                            onClick = { prefs.activationMethod = method },
                            label = {
                                Text(
                                    text = when (method) {
                                        ActivationMethod.AUTO -> "Auto-Detect (Recommended)"
                                        ActivationMethod.SHIZUKU -> "Shizuku IPC"
                                        ActivationMethod.ROOT -> "Root Shell (su)"
                                        ActivationMethod.DIRECT_ADB -> "Direct ADB"
                                        ActivationMethod.RADIO_INFO -> "System RadioInfo"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                OutlinedButton(
                    onClick = { showSetupDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable {},
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Shizuku & Root Setup Dialog")
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
        IconSwitch(
            checked = checked,
            icon = icon,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingStrideSlider(
    title: String,
    currentLabel: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxIndex = (options.size - 1).coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        com.app.switcher5g.ui.components.StrideSlider(
            value = selectedIndex.coerceIn(0, maxIndex).toFloat(),
            onValueChange = { floatVal ->
                val idx = floatVal.roundToInt().coerceIn(0, options.size - 1)
                if (idx != selectedIndex) {
                    onSelectIndex(idx)
                }
            },
            valueRange = 0f..maxIndex.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
