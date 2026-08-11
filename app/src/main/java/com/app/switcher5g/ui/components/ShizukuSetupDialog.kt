package com.app.switcher5g.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.Manual5gSwitchHelper
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
                            hasPermission -> "✅ Shizuku active & authorized!"
                            isRootGranted -> "✅ Root shell (su) authorized!"
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
                text = "Service & Privilege Setup",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = 350f))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Root Access Card (First-class alternative for users who don't want Shizuku)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isRootGranted) MaterialTheme.colorScheme.primaryContainer
                    else if (isRootAvailable) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isRootGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isRootGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer
                                else if (isRootAvailable) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRootGranted) "Root Shell (su) Authorized"
                                    else if (isRootAvailable) "Root Access (su) Available"
                                    else "Root Mode (su)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (isRootAvailable) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (isRootGranted) "Root permission granted! No Shizuku daemon required."
                                    else if (isRootAvailable) "Don't want to use Shizuku? Grant root access to switch directly!"
                                    else "Unrooted device. Use Shizuku, Wireless Debugging, or RadioInfo below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isRootGranted) MaterialTheme.colorScheme.onPrimaryContainer
                                    else if (isRootAvailable) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (isRootAvailable && !isRootGranted) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val granted = RootHelper.requestRootAccess()
                                        isRootGranted = granted
                                        onStatusUpdated(hasPermission || isRootGranted)
                                        val msg = if (granted) "✅ Root access granted!" else "❌ Root permission denied in Superuser app."
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
                                Text("Grant Root (su) Access", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Shizuku Service Setup Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (hasPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (hasPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasPermission) "Shizuku Active & Authorized"
                                    else if (isAvailable) "Shizuku Running — Authorization Needed"
                                    else "Shizuku Service Not Running",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (hasPermission) "Ready for 1-tap network mode switching via Shizuku IPC!"
                                    else if (isAvailable) "Tap 'Request Shizuku Permission' to authorize Switcher 5G."
                                    else "Start Shizuku via Wireless Debugging or ADB command below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
                                        Text("Request Permission", style = MaterialTheme.typography.labelMedium)
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

                                OutlinedButton(
                                    onClick = { Manual5gSwitchHelper.openRadioInfo(context) },
                                    modifier = Modifier.bouncyClickable {},
                                ) {
                                    Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use System RadioInfo", style = MaterialTheme.typography.labelMedium)
                                }
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
                            "Option B: ADB Terminal Command Execution\n" +
                            "Connect phone to PC with USB Debugging enabled, then execute the command below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Command Selector Chips (FlowRow guarantees non-overflow text wrapping)
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

                // Command Snippet Box
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

                // REDESIGNED Troubleshoot / "No such file or directory" Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = 300f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Getting 'No such file or directory'?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }

                            TextButton(
                                onClick = { showFixGuide = !showFixGuide },
                                modifier = Modifier.bouncyClickable {},
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = if (showFixGuide) "Hide Steps" else "View Fix",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showFixGuide,
                            enter = fadeIn(tween(200)) + expandVertically(tween(250, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200, easing = FastOutSlowInEasing)),
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TroubleshootStepRow(
                                    number = "1",
                                    title = "Open Shizuku App First",
                                    description = "You MUST open the Shizuku app at least ONCE after installation so it extracts start.sh binary script to storage.",
                                )
                                TroubleshootStepRow(
                                    number = "2",
                                    title = "Omit 'adb shell' if inside shell",
                                    description = "If your PC terminal is already at the 'adb shell' prompt ($), select the 'Inside Shell ($)' chip above so the extra 'adb shell' command is omitted.",
                                )
                                TroubleshootStepRow(
                                    number = "3",
                                    title = "Don't want Shizuku? Use Root or System 5G",
                                    description = "Tap 'Grant Root Access' above if rooted, or tap 'Use System RadioInfo' for 0-command manual 5G mode switching!",
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun TroubleshootStepRow(
    number: String,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
            )
        }
    }
}
