package com.app.switcher5g.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.ui.components.PetalBadgeVariant
import com.app.switcher5g.ui.components.PetalButton
import com.app.switcher5g.ui.components.PetalButtonStyle
import com.app.switcher5g.ui.components.PetalButtonGroupItem
import com.app.switcher5g.ui.components.PetalChoiceButtonGroup
import com.app.switcher5g.ui.components.PetalGroupControlRow
import com.app.switcher5g.ui.components.PetalGroupRow
import com.app.switcher5g.ui.components.PetalGroupToggleRow
import com.app.switcher5g.ui.components.PetalScreenScaffold
import com.app.switcher5g.ui.components.PetalSection
import com.app.switcher5g.ui.components.ShizukuSetupDialog
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.ui.components.rememberPetalHeaderScrollBehavior
import com.app.switcher5g.ui.theme.AppPalettes
import com.app.switcher5g.ui.theme.ColorStyle
import com.app.switcher5g.ui.theme.PetalMaterialShapes
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppFont
import com.app.switcher5g.util.AppPreferences
import com.app.switcher5g.util.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSetupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    if (showSetupDialog) {
        ShizukuSetupDialog(onDismissRequest = { showSetupDialog = false })
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = {
                com.app.switcher5g.ui.components.PetalShapedIcon(
                    icon = Icons.Rounded.Restore,
                    shape = PetalMaterialShapes.Cookie7Sided,
                    size = 56.dp,
                    iconSize = 28.dp,
                )
            },
            title = {
                Text(
                    "Restore settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste your exported settings JSON below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("{ \"amoled\": true, ... }") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            },
            confirmButton = {
                PetalButton(
                    onClick = {
                        val success = prefs.importFromJsonString(restoreJsonInput)
                        if (success) {
                            Toast.makeText(context, "Settings restored", Toast.LENGTH_SHORT).show()
                            showRestoreDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                        }
                    },
                    height = 44.dp,
                ) { Text("Restore") }
            },
            dismissButton = {
                PetalButton(
                    onClick = { showRestoreDialog = false },
                    style = PetalButtonStyle.TEXT,
                    height = 44.dp,
                ) { Text("Cancel") }
            },
        )
    }

    val scrollBehavior = rememberPetalHeaderScrollBehavior()

    PetalScreenScaffold(
        title = "Settings",
        scrollBehavior = scrollBehavior,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        // ---- Appearance ----------------------------------------------------------------------
        PetalSection(title = "Appearance", modifier = Modifier.entrance(0)) {
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Brightness6,
                    title = "Theme",
                    position = position,
                    badgeShape = PetalMaterialShapes.Sunny,
                ) {
                    PetalChoiceButtonGroup(
                        options = listOf(
                            PetalButtonGroupItem(AppThemeMode.SYSTEM, "System", Icons.Rounded.SettingsBrightness),
                            PetalButtonGroupItem(AppThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
                            PetalButtonGroupItem(AppThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
                        ),
                        selected = prefs.themeMode,
                        onSelect = { prefs.themeMode = it },
                    )
                }
            }
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.Contrast,
                    title = "Pure AMOLED black",
                    subtitle = "True #000000 background in dark mode",
                    checked = prefs.amoled,
                    onCheckedChange = { prefs.amoled = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie4Sided,
                    variant = PetalBadgeVariant.SECONDARY,
                )
            }
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.ColorLens,
                    title = "Material You colors",
                    subtitle = "Adapt accents from your wallpaper (Android 12+)",
                    checked = prefs.useDynamicTheme,
                    onCheckedChange = { prefs.useDynamicTheme = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Flower,
                    variant = PetalBadgeVariant.TERTIARY,
                )
            }
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Palette,
                    title = "Color palette",
                    position = position,
                    badgeShape = PetalMaterialShapes.Clover4Leaf,
                    variant = PetalBadgeVariant.SECONDARY,
                ) {
                    PetalChoiceButtonGroup(
                        options = AppPalettes.map { PetalButtonGroupItem(it.id, it.label) },
                        selected = prefs.paletteId,
                        onSelect = { prefs.paletteId = it },
                    )
                }
            }
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Color style",
                    position = position,
                    badgeShape = PetalMaterialShapes.Burst,
                    variant = PetalBadgeVariant.TERTIARY,
                ) {
                    PetalChoiceButtonGroup(
                        options = ColorStyle.entries.map { PetalButtonGroupItem(it, it.name.pretty()) },
                        selected = prefs.colorStyle,
                        onSelect = { prefs.colorStyle = it },
                    )
                }
            }
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.TextFields,
                    title = "Typography",
                    position = position,
                    badgeShape = PetalMaterialShapes.Pentagon,
                ) {
                    PetalChoiceButtonGroup(
                        options = AppFont.entries.map { PetalButtonGroupItem(it, it.name.pretty()) },
                        selected = prefs.appFont,
                        onSelect = { prefs.appFont = it },
                    )
                }
            }
        }

        // ---- Motion & back gestures ------------------------------------------------------------
        PetalSection(
            title = "Motion & back gestures",
            description = "Petal's predictive back preview and depth blur",
            modifier = Modifier.entrance(1),
        ) {
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.Swipe,
                    title = "Predictive back animation",
                    subtitle = "Live preview of the previous screen while you swipe back",
                    checked = prefs.predictiveBack,
                    onCheckedChange = { prefs.predictiveBack = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Arrow,
                )
            }
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.BlurOn,
                    title = "Depth blur",
                    subtitle = "Blur and dim the screen behind during back gestures",
                    checked = prefs.depthBlur,
                    onCheckedChange = { prefs.depthBlur = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie9Sided,
                    variant = PetalBadgeVariant.SECONDARY,
                )
            }
        }

        // ---- Network defaults ------------------------------------------------------------------
        PetalSection(title = "Network defaults", modifier = Modifier.entrance(2)) {
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.NetworkCheck,
                    title = "Default mode on launch",
                    subtitle = "Preselected when the app opens",
                    position = position,
                    badgeShape = PetalMaterialShapes.Gem,
                ) {
                    ModeToggleGroup(selected = prefs.defaultNetworkMode, onSelect = { prefs.defaultNetworkMode = it })
                }
            }
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Tune,
                    title = "Quick tile: first mode",
                    subtitle = "2-mode Quick Settings toggle",
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie6Sided,
                    variant = PetalBadgeVariant.SECONDARY,
                ) {
                    ModeToggleGroup(selected = prefs.toggleMode1, onSelect = { prefs.toggleMode1 = it })
                }
            }
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Tune,
                    title = "Quick tile: second mode",
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie7Sided,
                    variant = PetalBadgeVariant.TERTIARY,
                ) {
                    ModeToggleGroup(selected = prefs.toggleMode2, onSelect = { prefs.toggleMode2 = it })
                }
            }
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.SimCard,
                    title = "Auto-scan SIMs on launch",
                    subtitle = "Detect active Dual-SIM subscriptions when the app opens",
                    checked = prefs.autoScanSims,
                    onCheckedChange = { prefs.autoScanSims = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Sunny,
                )
            }
            row { position ->
                PetalGroupToggleRow(
                    icon = Icons.Rounded.Update,
                    title = "Check for updates on launch",
                    checked = prefs.autoCheckUpdates,
                    onCheckedChange = { prefs.autoCheckUpdates = it },
                    position = position,
                    badgeShape = PetalMaterialShapes.Clover8Leaf,
                    variant = PetalBadgeVariant.SECONDARY,
                )
            }
        }

        // ---- Execution method ------------------------------------------------------------------
        PetalSection(title = "Execution method", modifier = Modifier.entrance(3)) {
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Security,
                    title = "Switching engine",
                    subtitle = "How network modes get applied",
                    position = position,
                    badgeShape = PetalMaterialShapes.Cookie12Sided,
                    variant = PetalBadgeVariant.TERTIARY,
                ) {
                    PetalChoiceButtonGroup(
                        options = ActivationMethod.entries.map {
                            PetalButtonGroupItem(
                                value = it,
                                label = when (it) {
                                    ActivationMethod.AUTO -> "Auto"
                                    ActivationMethod.SHIZUKU -> "Shizuku"
                                    ActivationMethod.ROOT -> "Root"
                                    ActivationMethod.DIRECT_ADB -> "Direct ADB"
                                    ActivationMethod.RADIO_INFO -> "RadioInfo"
                                },
                            )
                        },
                        selected = prefs.activationMethod,
                        onSelect = { prefs.activationMethod = it },
                    )
                }
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Terminal,
                    title = "Shizuku & root setup",
                    subtitle = "Authorize privileged switching",
                    position = position,
                    onClick = { showSetupDialog = true },
                    badgeShape = PetalMaterialShapes.Boom,
                )
            }
        }

        // ---- Backup & restore ------------------------------------------------------------------
        PetalSection(title = "Backup & restore", modifier = Modifier.entrance(4)) {
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.Backup,
                    title = "Settings backup",
                    subtitle = "Copy your preferences as JSON, or restore a saved backup",
                    position = position,
                    badgeShape = PetalMaterialShapes.Puffy,
                    variant = PetalBadgeVariant.SECONDARY,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PetalButton(
                            onClick = {
                                val json = prefs.exportToJsonString()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Switcher5G Settings Backup", json))
                                Toast.makeText(context, "Settings copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Backup")
                        }
                        PetalButton(
                            onClick = { showRestoreDialog = true },
                            style = PetalButtonStyle.TONAL,
                            modifier = Modifier.weight(1f),
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

/** Connected toggle group over the three network modes, shared by every mode picker. */
@Composable
private fun ModeToggleGroup(
    selected: NetworkMode,
    onSelect: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    PetalChoiceButtonGroup(
        options = NetworkMode.entries.map { mode ->
            PetalButtonGroupItem(
                value = mode,
                label = when (mode) {
                    NetworkMode.NR_ONLY -> "5G SA"
                    NetworkMode.NR_LTE -> "5G NSA"
                    NetworkMode.LTE_ONLY -> "4G LTE"
                },
            )
        },
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

private fun String.pretty(): String =
    lowercase().split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
