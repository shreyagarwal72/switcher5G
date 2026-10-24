package com.app.switcher5g.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.R
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.ui.components.*
import com.app.switcher5g.ui.theme.ExpressiveShapeHolder
import com.app.switcher5g.ui.theme.PetalMaterialShapes
import com.app.switcher5g.ui.theme.toShape
import com.app.switcher5g.update.UpdateInfo
import com.app.switcher5g.update.UpdateManager
import com.app.switcher5g.util.MarkdownUtils
import kotlinx.coroutines.launch

private const val SITE_BASE_URL = "https://switcher-5g.vercel.app"

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
        }.onFailure {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollBehavior = rememberPetalHeaderScrollBehavior()

    PetalScreenScaffold(
        title = "About",
        scrollBehavior = scrollBehavior,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    ) {
        // 1. Developer Hero Section (Adopted from Petal Browser)
        DeveloperHeroSection(
            versionName = appVersion,
            onOpenUrl = { url -> openLink(url) },
            modifier = Modifier.entrance(0),
        )

        // 2. Official Website & Documentation (Interactive FAQ, Architecture, etc.)
        PetalSection(
            title = "Documentation & Web",
            modifier = Modifier.entrance(1),
        ) {
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Quiz,
                    title = "Frequently Asked Questions",
                    subtitle = "Troubleshooting, carrier compatibility & usage guides",
                    position = position,
                    onClick = { openLink("$SITE_BASE_URL/faq") },
                    badgeShape = PetalMaterialShapes.Clover4Leaf,
                    variant = PetalBadgeVariant.PRIMARY,
                    trailing = {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Architecture,
                    title = "System Architecture",
                    subtitle = "Shizuku IPC binder & Samsung Knox bypass mechanics",
                    position = position,
                    onClick = { openLink("$SITE_BASE_URL/architecture") },
                    badgeShape = PetalMaterialShapes.Burst,
                    variant = PetalBadgeVariant.SECONDARY,
                    trailing = {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.DesignServices,
                    title = "Material 3 Expressive Design",
                    subtitle = "Design foundations, shapes, and tokens",
                    position = position,
                    onClick = { openLink("$SITE_BASE_URL/design") },
                    badgeShape = PetalMaterialShapes.Flower,
                    variant = PetalBadgeVariant.TERTIARY,
                    trailing = {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Download,
                    title = "Download Releases",
                    subtitle = "Official APK mirrors, verify checksums & SHA-256",
                    position = position,
                    onClick = { openLink("$SITE_BASE_URL/download") },
                    badgeShape = PetalMaterialShapes.Cookie6Sided,
                    variant = PetalBadgeVariant.SURFACE_TONAL,
                    trailing = {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }

        // 3. Updates & Release Channel
        PetalSection(
            title = "Software Updates",
            modifier = Modifier.entrance(2),
        ) {
            row { position ->
                PetalGroupControlRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Switcher 5G v$appVersion",
                    subtitle = when {
                        isCheckingUpdate -> "Checking GitHub releases…"
                        isDownloadingUpdate -> "Downloading ${(downloadProgress * 100).toInt()}%…"
                        updateInfo == null -> "Latest production build installed"
                        updateInfo!!.hasUpdate -> "Update available: v${updateInfo!!.latestVersion}"
                        else -> "App is up to date (v${updateInfo!!.latestVersion})"
                    },
                    position = position,
                    badgeShape = PetalMaterialShapes.Gem,
                    variant = if (updateInfo?.hasUpdate == true) PetalBadgeVariant.SECONDARY else PetalBadgeVariant.PRIMARY,
                ) {
                    val info = updateInfo
                    if (info?.releaseNotes?.isNotBlank() == true) {
                        Text(
                            text = MarkdownUtils.parseMarkdown(info.releaseNotes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isDownloadingUpdate) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PetalContainedLoadingIndicator(
                                size = 44.dp,
                                progress = { downloadProgress },
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Downloading update…",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else if (info?.hasUpdate == true && info.isAvailable) {
                        Spacer(Modifier.height(4.dp))
                        PetalButton(
                            onClick = {
                                scope.launch {
                                    isDownloadingUpdate = true
                                    downloadProgress = 0f
                                    UpdateManager.downloadAndInstallApk(
                                        context = context,
                                        apkUrl = info.apkUrl,
                                        onProgress = { p -> downloadProgress = p },
                                    )
                                    isDownloadingUpdate = false
                                }
                            },
                            style = PetalButtonStyle.FILLED,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download & Install v${info.latestVersion}")
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                        PetalButton(
                            onClick = {
                                isCheckingUpdate = true
                                scope.launch {
                                    val checked = UpdateManager.checkForUpdates(currentVersion = appVersion)
                                    isCheckingUpdate = false
                                    updateInfo = checked
                                    if (!checked.hasUpdate) {
                                        Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            style = PetalButtonStyle.TONAL,
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isCheckingUpdate) {
                                PetalLoadingIndicator(size = 18.dp)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (isCheckingUpdate) "Checking…" else "Check for Updates")
                        }
                    }
                }
            }
        }

        // 4. System Diagnostics & Telemetry
        val shizukuActive = remember { ShizukuHelper.hasPermission() }
        PetalSection(
            title = "Device Telemetry",
            modifier = Modifier.entrance(3),
        ) {
            row { position ->
                InfoRow(Icons.Rounded.PermDeviceInformation, "Device Model", deviceModel, position, PetalMaterialShapes.Cookie6Sided)
            }
            row { position ->
                InfoRow(Icons.Rounded.Android, "Platform", androidVersion, position, PetalMaterialShapes.Clover4Leaf, PetalBadgeVariant.SECONDARY)
            }
            row { position ->
                InfoRow(Icons.Rounded.SimCard, "Active Carrier", networkOperatorName, position, PetalMaterialShapes.Pentagon, PetalBadgeVariant.TERTIARY)
            }
            row { position ->
                InfoRow(
                    icon = Icons.Rounded.Security,
                    label = "Shizuku Service",
                    value = if (shizukuActive) "Active & Authorized" else "Disconnected",
                    position = position,
                    shape = PetalMaterialShapes.Gem,
                    variant = if (shizukuActive) PetalBadgeVariant.PRIMARY else PetalBadgeVariant.SURFACE_TONAL,
                )
            }
        }

        // 5. Community & Open Source
        PetalSection(
            title = "Community & Source",
            modifier = Modifier.entrance(4),
        ) {
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Send,
                    title = "Telegram Community",
                    subtitle = "@championworkspace: Support, news & feature requests",
                    position = position,
                    onClick = { openLink("https://t.me/championworkspace") },
                    badgeShape = PetalMaterialShapes.Flower,
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Code,
                    title = "GitHub Repository",
                    subtitle = "shreyagarwal72/switcher5G (Open source, MIT)",
                    position = position,
                    onClick = { openLink("https://github.com/shreyagarwal72/switcher5G") },
                    badgeShape = PetalMaterialShapes.Burst,
                    variant = PetalBadgeVariant.SECONDARY,
                )
            }
            row { position ->
                PetalGroupRow(
                    icon = Icons.Rounded.Public,
                    title = "Shizuku Framework",
                    subtitle = "Get Shizuku for rootless privileged switching",
                    position = position,
                    onClick = { openLink("https://shizuku.rikka.app") },
                    badgeShape = PetalMaterialShapes.SoftBurst,
                    variant = PetalBadgeVariant.TERTIARY,
                )
            }
        }
    }
}

/**
 * Developer Hero Section with 360° Infinite Cookie Rotation avatar,
 * Vanshu Agarwal bio, and 3-pill segmented contact deck.
 */
@Composable
private fun DeveloperHeroSection(
    versionName: String,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "avatar_cookie_rotation")
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cookie_rotation",
    )

    PetalGroupItem(
        position = PetalGroupPosition.SINGLE,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Animated Rotating Cookie 12-Sided Avatar
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer { rotationZ = rotationDegrees }
                    .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        Toast.makeText(context, "Vanshu Agarwal • @shreyagarwal72", Toast.LENGTH_SHORT).show()
                        onOpenUrl("https://github.com/shreyagarwal72")
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_developer),
                    contentDescription = "Developer Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(136.dp)
                        .graphicsLayer { rotationZ = -rotationDegrees },
                )
            }

            // Developer Title & Subtext
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Switcher 5G v$versionName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Crafted by Vanshu Agarwal\nwith ❤️ from 🇮🇳",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            // Segmented 3-Pill Contact Deck (Website, Email, Telegram)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                // Official Website Pill
                val webShape = RoundedCornerShape(
                    topStart = 20.dp,
                    bottomStart = 20.dp,
                    topEnd = 6.dp,
                    bottomEnd = 6.dp,
                )
                Button(
                    onClick = { onOpenUrl(SITE_BASE_URL) },
                    shape = webShape,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Website",
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Contact Email Pill
                val emailShape = RoundedCornerShape(6.dp)
                Button(
                    onClick = {
                        val mailUri = Uri.parse("mailto:hereyourchampion@gmail.com")
                        val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Hello from Switcher 5G")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No email client installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = emailShape,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mail,
                        contentDescription = "Contact Email",
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Telegram Pill
                val tgShape = RoundedCornerShape(
                    topStart = 6.dp,
                    bottomStart = 6.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                )
                Button(
                    onClick = { onOpenUrl("https://t.me/hereyourchampion") },
                    shape = tgShape,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Telegram",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    position: PetalGroupPosition,
    shape: ExpressiveShapeHolder,
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
