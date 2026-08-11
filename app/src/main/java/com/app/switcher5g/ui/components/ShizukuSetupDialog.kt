package com.app.switcher5g.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShizukuSetupDialog(
    onDismissRequest: () -> Unit,
    onStatusUpdated: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(ShizukuHelper.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuHelper.hasPermission()) }

    var isRootAvailable by remember { mutableStateOf(RootHelper.isRootAvailable()) }
    var isRootGranted by remember { mutableStateOf(false) }

    var isInsideShell by remember { mutableStateOf(false) }
    var selectedPathIndex by remember { mutableIntStateOf(0) }
    var showFixGuide by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isRootAvailable) {
            isRootGranted = RootHelper.isRootGranted()
        }
    }

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
            onStatusUpdated(permission || isRootGranted)
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
                    scope.launch {
                        isChecking = true
                        isAvailable = ShizukuHelper.isAvailable()
                        hasPermission = ShizukuHelper.hasPermission()
                        isRootAvailable = RootHelper.isRootAvailable()
                        if (isRootAvailable) {
                            isRootGranted = RootHelper.isRootGranted()
                        }
                        onStatusUpdated(hasPermission || isRootGranted)
                        isChecking = false
                        val statusMsg = when {
                            hasPermission -> "✅ Shizuku active & authorized"
                            isRootGranted -> "✅ Root shell (su) authorized"
                            isAvailable -> "⚠️ Shizuku running. Authorization needed."
                            else -> "❌ Shizuku not running."
                        }
                        Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.bouncyClickable {},
            ) {
                if (isChecking) {
                    FancyCircularOrbLoader(size = 16.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Check Status", style = MaterialTheme.typography.labelMedium)
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = if (hasPermission || isRootGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = "Service Setup & Privileges",
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
                // Root Shell Facility Section
                if (isRootAvailable) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRootGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isRootGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRootGranted) "Root Access (su) Granted" else "Root Binary (su) Detected",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    Text(
                                        text = if (isRootGranted) "Root privilege active. 1-tap network switching enabled!" else "Device is rooted. Grant su access to switch network mode directly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }

                            if (!isRootGranted) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val granted = RootHelper.requestRootAccess()
                                            isRootGranted = granted
                                            onStatusUpdated(hasPermission || isRootGranted)
                                            val msg = if (granted) "✅ Root access granted!" else "❌ Root access denied by Superuser manager."
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bouncyClickable {},
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                ) {
                                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Grant Root (su) Permission", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                // Shizuku Service Setup
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            tint = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = if (hasPermission) "Shizuku Active & Authorized"
                                else if (isAvailable) "Shizuku Running — Authorization Needed"
                                else "Shizuku Service Not Running",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (hasPermission) "Ready to switch network modes via Shizuku IPC!"
                                else if (isAvailable) "Tap 'Request Shizuku Permission' to authorize Switcher 5G."
                                else "Start Shizuku via Wireless Debugging or ADB command.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (!hasPermission) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (isAvailable) {
                            Button(
                                onClick = { ShizukuHelper.requestPermission() },
                                modifier = Modifier.bouncyClickable {},
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request Shizuku Permission", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            Button(
                                onClick = { ShizukuHelper.launchShizukuApp(context) },
                                modifier = Modifier.bouncyClickable {},
                            ) {
                                Icon(Icons.Rounded.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Shizuku App", style = MaterialTheme.typography.labelMedium)
                            }
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
                            "Option B: ADB Terminal Execution\n" +
                            "Connect phone to PC with USB Debugging enabled, then run the command below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Command Selector Chips (FlowRow guarantees text wrapping and no clipping)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = !isInsideShell,
                        onClick = { isInsideShell = false },
                        label = { Text("From PC (adb shell)", style = MaterialTheme.typography.labelSmall) },
                    )
                    FilterChip(
                        selected = isInsideShell,
                        onClick = { isInsideShell = true },
                        label = { Text("Inside Shell ($)", style = MaterialTheme.typography.labelSmall) },
                    )
                    FilterChip(
                        selected = selectedPathIndex == 0,
                        onClick = { selectedPathIndex = 0 },
                        label = { Text("/sdcard path", style = MaterialTheme.typography.labelSmall) },
                    )
                    FilterChip(
                        selected = selectedPathIndex == 1,
                        onClick = { selectedPathIndex = 1 },
                        label = { Text("/data/user path", style = MaterialTheme.typography.labelSmall) },
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
                                text = "ADB Start Command",
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
                                            "2. If you are already inside 'adb shell' prompt ($), select 'Inside Shell ($)' chip above so 'adb shell' prefix is omitted.\n" +
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
