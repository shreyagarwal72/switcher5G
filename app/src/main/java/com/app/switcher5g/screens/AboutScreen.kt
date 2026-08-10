package com.app.switcher5g.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.TelephonyManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.ui.components.FancyCircularOrbLoader
import com.app.switcher5g.ui.components.FancyLinearLoadingBar
import com.app.switcher5g.ui.components.bouncyClickable
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.update.UpdateInfo
import com.app.switcher5g.update.UpdateManager
import com.app.switcher5g.util.MarkdownUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val appVersion = "1.0.0"
    val deviceModel = remember { "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}" }
    val androidVersion = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val telephonyManager = remember { context.getSystemService(TelephonyManager::class.java) }
    val networkOperatorName = remember { telephonyManager?.networkOperatorName.orEmpty().ifBlank { "Cellular Radio" } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Stride-style Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(0)
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = "About Switcher 5G",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Version $appVersion • Material 3 Expressive",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // App Header Banner
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(1)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(28.dp),
                )
                .shadow(6.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                    )
                    .padding(20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // App Logo Ring
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CellTower,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Switcher 5G",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                            ),
                        )
                        Text(
                            text = "Version $appVersion • Material 3 Expressive",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Text(
                        text = "Instant 5G Standalone (SA), 5G Non-Standalone (NSA), and 4G LTE network mode switcher powered by privileged Shizuku IPC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // GitHub Auto Update Checker Card
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "App Update Checker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    if (isCheckingUpdate) {
                        FancyCircularOrbLoader(size = 22.dp)
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdate = true
                                    val info = UpdateManager.checkForUpdates(currentVersion = appVersion)
                                    updateInfo = info
                                    isCheckingUpdate = false
                                }
                            },
                            modifier = Modifier.bouncyClickable {},
                        ) {
                            Text("Check Updates")
                        }
                    }
                }

                if (isCheckingUpdate) {
                    FancyLinearLoadingBar(
                        progress = null,
                        label = "Querying GitHub Releases API…",
                    )
                }

                updateInfo?.let { info ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (info.hasUpdate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (info.hasUpdate) "🚀 Update Available: v${info.latestVersion}" else "✅ Switcher 5G is up to date (v${info.latestVersion})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            )

                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = MarkdownUtils.parseMarkdown(info.releaseNotes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (info.isAvailable) {
                                if (isDownloadingUpdate) {
                                    FancyLinearLoadingBar(
                                        progress = downloadProgress,
                                        showPercentage = true,
                                        label = "Downloading Update APK…",
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isDownloadingUpdate = true
                                                downloadProgress = 0f
                                                UpdateManager.downloadAndInstallApk(
                                                    context = context,
                                                    apkUrl = info.apkUrl,
                                                    onProgress = { downloadProgress = it },
                                                )
                                                isDownloadingUpdate = false
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bouncyClickable {},
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (info.hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (info.hasUpdate) "Download & Install Update (v${info.latestVersion})" else "Re-download Latest Release APK",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Device & Network System Diagnostics Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(3)
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
                        Icons.Rounded.PermDeviceInformation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "System & Hardware Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                InfoRow(label = "Device Hardware", value = deviceModel)
                InfoRow(label = "Android OS", value = androidVersion)
                InfoRow(label = "Carrier Radio", value = networkOperatorName)
                InfoRow(label = "Shizuku Shell Privileges", value = if (ShizukuHelper.hasPermission()) "Active & Authorized" else "Disconnected")
                InfoRow(label = "Material You Dynamic Theme", value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Supported (Android 12+)" else "Fallback Dark Theme")
            }
        }

        // Developer Page & Project Links Card
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Developer Page & Open Source",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }

                Text(
                    text = "Switcher 5G is an open-source utility designed to unlock hidden cellular radio band configurations without requiring full device root.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shreyagarwal72/switcher5G"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .bouncyClickable {},
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GitHub Repo", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .bouncyClickable {},
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shizuku Project", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
