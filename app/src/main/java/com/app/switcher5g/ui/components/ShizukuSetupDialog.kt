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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuSetupDialog(
    onDismissRequest: () -> Unit,
    onStatusUpdated: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(ShizukuHelper.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val commands = remember {
        listOf(
            "Standard ADB" to ShizukuHelper.ADB_START_COMMAND_SDCARD,
            "User 0 ADB" to ShizukuHelper.ADB_START_COMMAND_USER,
            "Root Terminal" to "su -c sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh",
        )
    }

    val activeCommand = commands[selectedTab].second

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
                        else if (isAvailable) "⚠️ Shizuku service running. Permission required."
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
                tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "Shizuku & ADB Setup",
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
                // Status Badge Row
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
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = when {
                                hasPermission -> "Shizuku Service Active & Authorized"
                                isAvailable -> "Shizuku Running (Permission Required)"
                                else -> "Shizuku Service Disconnected"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (isAvailable && !hasPermission) {
                    Button(
                        onClick = {
                            ShizukuHelper.requestPermission()
                            hasPermission = ShizukuHelper.hasPermission()
                            onStatusUpdated(hasPermission)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable {},
                    ) {
                        Text("Grant Shizuku Permission")
                    }
                }

                // Launch Shizuku App Button
                OutlinedButton(
                    onClick = {
                        val launched = ShizukuHelper.launchShizukuApp(context)
                        if (!launched) ShizukuHelper.openPlayStore(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable {},
                ) {
                    Icon(Icons.Rounded.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Shizuku App")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // ADB Section
                Text(
                    text = "ADB Activation Commands",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )

                // Command Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    commands.forEachIndexed { idx, pair ->
                        FilterChip(
                            selected = idx == selectedTab,
                            onClick = { selectedTab = idx },
                            label = { Text(pair.first, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                // Command Box with One-tap Copy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            Text(
                                text = "ADB Shell Start Command",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", activeCommand))
                                    Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy command", modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = activeCommand,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Troubleshooting Note for "No such file or directory"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Note: If ADB returns 'no such file or directory', launch the Shizuku app once to unpack its starter script, or use Wireless Debugging pairing inside Shizuku.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}
