package com.app.switcher5g.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.TelephonyManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.ui.components.PetalBadgeVariant
import com.app.switcher5g.ui.components.PetalButton
import com.app.switcher5g.ui.components.PetalButtonStyle
import com.app.switcher5g.ui.components.PetalContainedLoadingIndicator
import com.app.switcher5g.ui.components.PetalGroupItem
import com.app.switcher5g.ui.components.PetalGroupPosition
import com.app.switcher5g.ui.components.PetalGroupRow
import com.app.switcher5g.ui.components.PetalScreenScaffold
import com.app.switcher5g.ui.components.PetalSection
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.ui.components.rememberPetalHeaderScrollBehavior
import com.app.switcher5g.ui.theme.PetalMaterialShapes
import com.app.switcher5g.ui.components.PetalShapedContainer
import com.app.switcher5g.update.UpdateInfo
import com.app.switcher5g.update.UpdateManager
import com.app.switcher5g.util.MarkdownUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val appVersion = remember(context) { com.app.switcher5g.util.AppInfo.getAppVersionName(context) }
    val deviceModel = remember { "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}" }
    val androidVersion = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
    val telephonyManager = remember { context.getSystemService(TelephonyManager::class.java) }
    val networkOperatorName = remember { telephonyManager?.networkOperatorName.orEmpty().ifBlank { "Cellular Radio" } }

    fun openLink(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    val scrollBehavior = rememberPetalHeaderScrollBehavior()

    PetalScreenScaffold(
        title = "About",
        scrollBehavior = scrollBehavior,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        // ---- Hero --------------------------------------------------------------------------
        PetalGroupItem(
            position = PetalGroupPosition.SINGLE,
            modifier = Modifier.entrance(0),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PetalShapedContainer(
                    shape = PetalMaterialShapes.Cookie12Sided.toShape(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 96.dp,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CellTower,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Switcher 5G",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "5G Standalone (SA), 5G Non-Standalone (NSA), and 4G LTE network mode switcher for Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ---- Updates -----------------------------------------------------------------------
        PetalSection(
            title = "Updates",
            modifier = Modifier.entrance(1),
        ) {
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for updates",
                    subtitle = when {
                        isCheckingUpdate -> "Checking for updates…"
                        updateInfo == null -> "You are on v$appVersion"
                        else -> "Last checked just now"
                    },
                    position = position,
                    badgeShape = PetalMaterialShapes.Sunny,
                    trailing = {
                        if (isCheckingUpdate) {
                            PetalContainedLoadingIndicator(size = 44.dp)
                        } else {
                            PetalButton(
                                onClick = {
                                    scope.launch {
                                        isCheckingUpdate = true
                                        val info = UpdateManager.checkForUpdates(currentVersion = appVersion)
                                        updateInfo = info
                                        isCheckingUpdate = false
                                    }
                                },
                                style = PetalButtonStyle.TONAL,
                                height = 40.dp,
                            ) {
                                Text("Check")
                            }
                        }
                    },
                )
            }

            val info = updateInfo
            if (info != null) {
                row { position ->
                    PetalGroupItem(
                        position = position,
                        color = if (info.hasUpdate) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            com.app.switcher5g.ui.components.petalGroupColor()
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val onColor = if (info.hasUpdate) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = if (info.hasUpdate) {
                                    "Update available: v${info.latestVersion}"
                                } else {
                                    "App is up to date (v${info.latestVersion})"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = onColor,
                            )

                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = MarkdownUtils.parseMarkdown(info.releaseNotes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (info.hasUpdate) onColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (info.hasUpdate && info.isAvailable) {
                                if (isDownloadingUpdate) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        PetalContainedLoadingIndicator(
                                            size = 48.dp,
                                            progress = { downloadProgress },
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Downloading update…",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = onColor,
                                            )
                                            Text(
                                                text = "${(downloadProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = onColor,
                                            )
                                        }
                                    }
                                } else {
                                    PetalButton(
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
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Download & install v${info.latestVersion}")
                                    }
                                }
                            } else if (!info.hasUpdate) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "You are running the latest release.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- System info -------------------------------------------------------------------
        val shizukuActive = remember { ShizukuHelper.hasPermission() }
        PetalSection(
            title = "System",
            modifier = Modifier.entrance(2),
        ) {
            row { position ->
                InfoRow(Icons.Rounded.PermDeviceInformation, "Device", deviceModel, position, PetalMaterialShapes.Cookie6Sided)
            }
            row { position ->
                InfoRow(Icons.Rounded.Android, "Android", androidVersion, position, PetalMaterialShapes.Clover4Leaf, PetalBadgeVariant.SECONDARY)
            }
            row { position ->
                InfoRow(Icons.Rounded.SimCard, "Carrier", networkOperatorName, position, PetalMaterialShapes.Pentagon, PetalBadgeVariant.TERTIARY)
            }
            row { position ->
                InfoRow(
                    icon = Icons.Rounded.Security,
                    label = "Shizuku",
                    value = if (shizukuActive) "Active & authorized" else "Disconnected",
                    position = position,
                    shape = PetalMaterialShapes.Gem,
                    variant = if (shizukuActive) PetalBadgeVariant.PRIMARY else PetalBadgeVariant.SURFACE_TONAL,
                )
            }
        }

        // ---- Community & links -------------------------------------------------------------
        PetalSection(
            title = "Community & source",
            modifier = Modifier.entrance(3),
        ) {
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Send,
                    title = "Telegram",
                    subtitle = "@championworkspace: support, updates and feature requests",
                    position = position,
                    onClick = { openLink("https://t.me/championworkspace") },
                    badgeShape = PetalMaterialShapes.Flower,
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Code,
                    title = "GitHub",
                    subtitle = "Source code and releases",
                    position = position,
                    onClick = { openLink("https://github.com/shreyagarwal72/switcher5G") },
                    badgeShape = PetalMaterialShapes.Burst,
                    variant = PetalBadgeVariant.SECONDARY,
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Public,
                    title = "Shizuku",
                    subtitle = "Get Shizuku for in-app switching",
                    position = position,
                    onClick = { openLink("https://shizuku.rikka.app") },
                    badgeShape = PetalMaterialShapes.SoftBurst,
                    variant = PetalBadgeVariant.TERTIARY,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    position: PetalGroupPosition,
    shape: com.app.switcher5g.ui.theme.ExpressiveShapeHolder,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
) {
    PetalGroupRow(
        icon = icon,
        title = label,
        position = position,
        badgeShape = shape,
        variant = variant,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
    )
}
