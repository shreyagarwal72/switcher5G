package com.app.switcher5g.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShizukuSetupDialog(
    onDismissRequest: () -> Unit,
    onStatusUpdated: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(ShizukuHelper.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var isInsideShell by remember { mutableStateOf(false) }
    var selectedPathIndex by remember { mutableIntStateOf(0) }
    var showFixGuide by remember { mutableStateOf(false) }

    val activeShizukuCommand = when {
        isInsideShell && selectedPathIndex == 0 -> ShizukuHelper.SHELL_SDCARD
        isInsideShell && selectedPathIndex == 1 -> ShizukuHelper.SHELL_USER
        !isInsideShell && selectedPathIndex == 0 -> ShizukuHelper.ADB_PC_SDCARD
        else -> ShizukuHelper.ADB_PC_USER
    }

    DisposableEffect(Unit) {
        val unregister = ShizukuHelper.registerListeners { available, permission ->
            isAvailable = available
            hasPermission = permission
            onStatusUpdated(permission)
        }
        onDispose { unregister() }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.bouncyClickable {},
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    isChecking = true
                    isAvailable = ShizukuHelper.isAvailable()
                    hasPermission = ShizukuHelper.hasPermission()
                    onStatusUpdated(hasPermission)
                    isChecking = false
                    Toast.makeText(
                        context,
                        if (hasPermission) "✅ Shizuku active & authorized"
                        else if (isAvailable) "⚠️ Shizuku running. Permission required."
                        else "❌ Shizuku not running",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                modifier = Modifier.bouncyClickable {},
            ) {
                if (isChecking) {
                    FancyCircularOrbLoader(size = 16.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Check Status")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "Shizuku Service Setup",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Shizuku Service Setup
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Column {
                            Text(
                                text = if (hasPermission) "Shizuku Active & Authorized"
                                else if (isAvailable) "Shizuku Service Running — Authorization Needed"
                                else "Shizuku Service Not Running",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = if (hasPermission) "You are ready to switch network modes in 1 tap!"
                                else if (isAvailable) "Tap 'Request Shizuku Permission' to authorize Switcher 5G."
                                else "Start Shizuku via Wireless Debugging or PC terminal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                if (!hasPermission) {
                    if (isAvailable) {
                        Button(
                            onClick = { ShizukuHelper.requestPermission() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Shizuku Permission")
                        }
                    } else {
                        Button(
                            onClick = { ShizukuHelper.launchShizukuApp(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Shizuku App")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // How to Start Shizuku Section
                Text(
                    text = "How to Start Shizuku Service:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )

                Text(
                    text = "Option A: Wireless Debugging (Android 11+, Recommended)\n" +
                            "Open Shizuku app -> Tap 'Pairing' under Wireless Debugging -> Enter pairing code -> Tap 'Start'.\n\n" +
                            "Option B: PC Terminal Execution\n" +
                            "Connect phone to PC with USB Debugging enabled, then run the command below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Command Path Toggle Buttons
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isInsideShell,
                        onClick = { isInsideShell = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text("From PC (adb shell)", style = MaterialTheme.typography.labelSmall)
                    }
                    SegmentedButton(
                        selected = isInsideShell,
                        onClick = { isInsideShell = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("Inside Shell ($)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // SDCard vs User Path Toggle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedPathIndex == 0,
                        onClick = { selectedPathIndex = 0 },
                        label = { Text("Standard Path (/sdcard)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedPathIndex == 1,
                        onClick = { selectedPathIndex = 1 },
                        label = { Text("Internal Path (/data/user)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Command Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Start Command",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Shizuku Command", activeShizukuCommand))
                                    Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Text(
                            text = activeShizukuCommand,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Troubleshoot / Fix Guide Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Getting 'No such file or directory'?",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            TextButton(
                                onClick = { showFixGuide = !showFixGuide },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    text = if (showFixGuide) "Hide" else "Fix Steps",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        AnimatedVisibility(visible = showFixGuide) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "1. You MUST open the Shizuku app at least ONCE after installing so it extracts start.sh to storage.\n" +
                                            "2. If you are already inside 'adb shell' prompt ($), select 'Inside adb shell ($)' tab above so 'adb shell' prefix is omitted.\n" +
                                            "3. Or use Wireless Debugging inside the Shizuku app (no commands needed!).",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
