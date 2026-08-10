package com.app.switcher5g.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuActivationCard(
    modifier: Modifier = Modifier,
    onStatusChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(ShizukuHelper.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var showGuide by remember { mutableStateOf(false) }
    var selectedCommandIndex by remember { mutableIntStateOf(0) }

    val commands = remember {
        listOf(
            "Standard ADB" to ShizukuHelper.ADB_START_COMMAND_SDCARD,
            "User Dir ADB" to ShizukuHelper.ADB_START_COMMAND_USER,
            "Local Shell" to ShizukuHelper.ADB_START_COMMAND_DIRECT,
        )
    }

    val currentCommand = commands[selectedCommandIndex].second

    val (statusText, badgeColor, textColor) = when {
        hasPermission -> Triple("ACTIVE & AUTHORIZED", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        isAvailable -> Triple("RUNNING (NEEDS PERMISSION)", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple("NOT RUNNING", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                1.dp,
                if (hasPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                RoundedCornerShape(24.dp),
            ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Shizuku Service Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor,
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = if (hasPermission)
                    "Shizuku service is connected with privileged shell permissions."
                else
                    "Shizuku service must be started via ADB command, Wireless Debugging, or Root to execute network mode switches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isAvailable && !hasPermission) {
                    Button(
                        onClick = {
                            ShizukuHelper.requestPermission()
                            hasPermission = ShizukuHelper.hasPermission()
                            onStatusChanged(hasPermission)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .bouncyClickable {},
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Grant Permission")
                    }
                }

                OutlinedButton(
                    onClick = {
                        isChecking = true
                        isAvailable = ShizukuHelper.isAvailable()
                        hasPermission = ShizukuHelper.hasPermission()
                        onStatusChanged(hasPermission)
                        isChecking = false
                        Toast.makeText(
                            context,
                            if (hasPermission) "✅ Shizuku connected & authorized!"
                            else if (isAvailable) "⚠️ Shizuku running. Permission required."
                            else "❌ Shizuku not running.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .bouncyClickable {},
                ) {
                    if (isChecking) {
                        FancyCircularOrbLoader(size = 18.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-check Status", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        val launched = ShizukuHelper.launchShizukuApp(context)
                        if (!launched) {
                            ShizukuHelper.openPlayStore(context)
                        }
                    },
                    modifier = Modifier.bouncyClickable {},
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Shizuku", style = MaterialTheme.typography.labelMedium)
                }
            }

            // ADB Command Activation Box (shown when Shizuku is not fully ready)
            if (!hasPermission) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Activate Shizuku via ADB Command",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }

                        TextButton(onClick = { showGuide = !showGuide }) {
                            Text(
                                text = if (showGuide) "Hide Guide" else "ADB Guide",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    // Command selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        commands.forEachIndexed { idx, pair ->
                            FilterChip(
                                selected = idx == selectedCommandIndex,
                                onClick = { selectedCommandIndex = idx },
                                label = { Text(pair.first, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    // Command snippet box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
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

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Shizuku Start Command", currentCommand))
                                        Toast.makeText(context, "Copied ADB start command to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .height(32.dp)
                                        .bouncyClickable {},
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Command", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Text(
                                text = currentCommand,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Collapsible Step-by-Step Wireless Debugging Guide
                    AnimatedVisibility(visible = showGuide) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "📱 How to activate without a PC (Wireless Debugging):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "1. Enable 'Developer options' in Android Settings -> About phone.\n" +
                                            "2. Turn on 'Wireless debugging' in Developer options.\n" +
                                            "3. Open Shizuku app -> tap 'Pairing' / 'Start via Wireless Debugging'.\n" +
                                            "4. Or run the copied ADB command using LADB, Termux, or PC terminal.\n" +
                                            "5. Tap 'Re-check Status' above once started!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
