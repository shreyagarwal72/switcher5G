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
import androidx.compose.material.icons.filled.*
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
import com.app.switcher5g.ui.components.ShizukuActivationCard
import com.app.switcher5g.ui.components.bouncyClickable
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.util.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var defaultMode by remember { mutableStateOf(prefs.defaultNetworkMode) }
    var autoScanSims by remember { mutableStateOf(prefs.autoScanSims) }
    var useWheelPicker by remember { mutableStateOf(prefs.useWheelPicker) }
    var autoCheckUpdates by remember { mutableStateOf(prefs.autoCheckUpdates) }
    var useDynamicTheme by remember { mutableStateOf(prefs.useDynamicTheme) }
    var enableAnimations by remember { mutableStateOf(prefs.enableAnimations) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = "Preferences & Options",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Configure app defaults, UI controls, and automation triggers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 1. Preferred Network Mode Defaults Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(1)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp),
                )
                .shadow(4.dp, RoundedCornerShape(24.dp)),
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
                        Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Network Defaults",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Set default network mode preselected on launch:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NetworkMode.entries.forEach { mode ->
                        val selected = defaultMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = {
                                defaultMode = mode
                                prefs.defaultNetworkMode = mode
                                Toast.makeText(context, "Default mode set to ${mode.name}", Toast.LENGTH_SHORT).show()
                            },
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Toggle Auto-scan SIMs
                SettingToggleRow(
                    title = "Auto-scan SIMs on Launch",
                    subtitle = "Automatically detect active Dual-SIM subscriptions when app opens",
                    icon = Icons.Default.SimCard,
                    checked = autoScanSims,
                    onCheckedChange = {
                        autoScanSims = it
                        prefs.autoScanSims = it
                    },
                )
            }
        }

        // 2. UI & Appearance Settings
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp),
                ),
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
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "Appearance & Interface",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                SettingToggleRow(
                    title = "Material You Dynamic Colors",
                    subtitle = "Adapt theme and app icon dynamically to device system wallpaper",
                    icon = Icons.Default.ColorLens,
                    checked = useDynamicTheme,
                    onCheckedChange = {
                        useDynamicTheme = it
                        prefs.useDynamicTheme = it
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Wheel Scroller Picker UI",
                    subtitle = "Use 3D wheel scroller instead of slide bar for mode selection",
                    icon = Icons.Default.UnfoldMore,
                    checked = useWheelPicker,
                    onCheckedChange = {
                        useWheelPicker = it
                        prefs.useWheelPicker = it
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Auto-Check for Updates",
                    subtitle = "Automatically check GitHub releases for app updates on startup",
                    icon = Icons.Default.SystemUpdate,
                    checked = autoCheckUpdates,
                    onCheckedChange = {
                        autoCheckUpdates = it
                        prefs.autoCheckUpdates = it
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                SettingToggleRow(
                    title = "Micro-Animations",
                    subtitle = "Enable bouncy spring interactions and entrance motion transitions",
                    icon = Icons.Default.Animation,
                    checked = enableAnimations,
                    onCheckedChange = {
                        enableAnimations = it
                        prefs.enableAnimations = it
                    },
                )
            }
        }

        // 3. Interactive Shizuku Activation & ADB Command Card
        ShizukuActivationCard(
            modifier = Modifier.entrance(3),
        )

        // 4. Automation & CLI Triggers Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(4)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp),
                ),
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
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = "Automation & ADB Command Triggers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Trigger network mode switching programmatically via Termux, Tasker, Automate, or ADB commands:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Deep Link Command
                val deepLinkCmd = "adb shell am start -a android.intent.action.VIEW -d \"switcher5g://switch?mode=NR_ONLY\""
                CommandSnippetBox(
                    label = "Deep Link Intent (ADB / Tasker)",
                    command = deepLinkCmd,
                    context = context,
                )

                // Broadcast Receiver Command
                val broadcastCmd = "adb shell am broadcast -a com.app.switcher5g.SET_NETWORK_MODE --es mode NR_ONLY"
                CommandSnippetBox(
                    label = "Privileged Broadcast Intent",
                    command = broadcastCmd,
                    context = context,
                )
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

@Composable
private fun CommandSnippetBox(
    label: String,
    command: String,
    context: Context,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Command", command))
                        Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy command",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
