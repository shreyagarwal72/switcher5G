package com.app.switcher5g.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.ui.components.FancyCircularOrbLoader
import com.app.switcher5g.ui.components.FancyExpressiveWheelLoader
import com.app.switcher5g.ui.components.FancyLiquidProgressBar
import com.app.switcher5g.ui.components.FancyModeSlideBar
import com.app.switcher5g.ui.components.FancyPulseLoader
import com.app.switcher5g.ui.components.FancyWheelScroller
import com.app.switcher5g.ui.components.NetworkModeSwitchLoadingOverlay
import com.app.switcher5g.ui.components.StrideFloatingNav
import com.app.switcher5g.ui.components.bouncyClickable
import com.app.switcher5g.ui.components.entrance
import com.app.switcher5g.update.UpdateInfo
import com.app.switcher5g.update.UpdateManager
import com.app.switcher5g.util.AppLogger
import com.app.switcher5g.util.MarkdownUtils
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(150)) + slideInVertically(tween(210)) { it / 16 } +
                        scaleIn(initialScale = 0.98f, animationSpec = tween(210))
            },
            exitTransition = { fadeOut(tween(80)) },
        ) {
            composable("home") {
                HomeScreenContent()
            }
            composable("logs") {
                DevLogsScreen()
            }
        }

        StrideFloatingNav(
            currentRoute = currentRoute,
            onNavigate = { route -> navController.navigateSingleTop(route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun HomeScreenContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { NetworkModeManager(context.applicationContext) }

    var selectedMode by remember { mutableStateOf(NetworkMode.NR_LTE) }
    var availableSubIds by remember { mutableStateOf<List<Int>>(listOf(1)) }
    var selectedSubId by remember { mutableStateOf<Int?>(null) }
    var statusText by remember { mutableStateOf("Ready to switch network mode.") }
    var isSwitching by remember { mutableStateOf(false) }
    var isScanningSims by remember { mutableStateOf(false) }
    var shizukuReady by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var useWheelPicker by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        AppLogger.i("HomeScreen", "Initial SIM scan triggered")
        if (ShizukuHelper.hasPermission()) {
            isScanningSims = true
            val ids = manager.getAvailableSubIds()
            availableSubIds = ids
            if (ids.isNotEmpty()) selectedSubId = ids[0]
            isScanningSims = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { manager.unbind() }
    }

    // Modal M3 Expressive loading overlay during network mode switch via Shizuku
    NetworkModeSwitchLoadingOverlay(
        isSwitching = isSwitching,
        modeName = selectedMode.label(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header Card (Stride-style staggered entrance card)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(0)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(24.dp),
                )
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "5G Switcher",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                ),
                            )
                        }

                        // Shizuku connection badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (shizukuReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (shizukuReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (shizukuReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    text = if (shizukuReady) "Shizuku Ready" else "No Permission",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (shizukuReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    Text(
                        text = MarkdownUtils.parseMarkdown("Switch between **5G SA (NR)**, **5G NSA (NR/LTE)**, and **4G (LTE)** instantly using privileged Shizuku shell IPC."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Shizuku Permission Card
        if (!shizukuReady) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(1)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FancyPulseLoader(size = 28.dp, color = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Shizuku Permission Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = "Ensure Shizuku app is running (via Wireless Debugging ADB or Root), then authorize Switcher 5G.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            AppLogger.i("HomeScreen", "Requesting Shizuku permission")
                            ShizukuHelper.requestPermission()
                            shizukuReady = ShizukuHelper.hasPermission()
                        },
                        modifier = Modifier.bouncyClickable {},
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Grant Shizuku Permission")
                    }
                }
            }
        }

        // GitHub Auto Update Checker Facility
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "GitHub Auto Update Checker",
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
                                    val info = UpdateManager.checkForUpdates(currentVersion = "1.0.0")
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

                updateInfo?.let { info ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (info.hasUpdate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                    text = if (info.hasUpdate) "🚀 Update Available: v${info.latestVersion}" else "✅ Switcher 5G is up to date (v${info.latestVersion})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = MarkdownUtils.parseMarkdown(info.releaseNotes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (info.hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (info.isAvailable) {
                                if (isDownloadingUpdate) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FancyLiquidProgressBar(progress = downloadProgress, modifier = Modifier.fillMaxWidth())
                                        Text(
                                            text = "Downloading APK: ${(downloadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
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
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (info.hasUpdate) "Download & Install Update (v${info.latestVersion})" else "Re-download & Install Latest Release APK",
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

        // Active SIM Card Selector Section
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(3)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            Icons.Default.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "Target SIM Subscription",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    if (isScanningSims) {
                        FancyCircularOrbLoader(size = 24.dp)
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isScanningSims = true
                                    AppLogger.i("HomeScreen", "Scanning SIM subscriptions...")
                                    val ids = manager.getAvailableSubIds()
                                    availableSubIds = ids
                                    if (ids.isNotEmpty() && selectedSubId == null) selectedSubId = ids[0]
                                    isScanningSims = false
                                }
                            },
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan SIMs")
                        }
                    }
                }

                Text(
                    text = "Selected Sub ID: ${selectedSubId ?: "Auto-Detect"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // SIM Slot Choice Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InputChip(
                        selected = selectedSubId == null,
                        onClick = { selectedSubId = null },
                        label = { Text("Auto-Detect Active SIM") },
                    )
                    availableSubIds.forEach { subId ->
                        InputChip(
                            selected = selectedSubId == subId,
                            onClick = { selectedSubId = subId },
                            label = { Text("SIM (Sub ID $subId)") },
                        )
                    }
                }
            }
        }

        // Network Mode Selector Section (Material 3 Fancy Slide Bar + Wheel Scroller Toggle)
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(4)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Preferred Network Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (useWheelPicker) "Wheel" else "Slide Bar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = useWheelPicker,
                            onCheckedChange = { useWheelPicker = it },
                            modifier = Modifier.scale(0.8f),
                        )
                    }
                }

                if (useWheelPicker) {
                    val modes = NetworkMode.entries
                    val labels = remember { modes.map { it.label() } }
                    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

                    FancyWheelScroller(
                        items = labels,
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { index ->
                            if (index in modes.indices) {
                                selectedMode = modes[index]
                            }
                        },
                    )
                } else {
                    FancyModeSlideBar(
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                    )
                }

                // Description for current centered/selected mode
                Text(
                    text = selectedMode.description(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }

        // Apply Button with bouncyClickable spring animation
        Button(
            enabled = !isSwitching,
            onClick = {
                isSwitching = true
                statusText = "Connecting to Shizuku IPC service…"
                AppLogger.i("HomeScreen", "Apply clicked for mode: $selectedMode (subId=$selectedSubId)")
                scope.launch {
                    val result = manager.switchTo(selectedMode, selectedSubId)
                    statusText = when (result) {
                        is SwitchResult.Success -> "✅ ${result.message}"
                        is SwitchResult.Failure -> "⚠️ ${result.reason}"
                    }
                    isSwitching = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .bouncyClickable(scaleDown = 0.94f) {},
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSwitching) {
                    FancyCircularOrbLoader(size = 20.dp)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                }
                Text(
                    text = if (isSwitching) "Applying Network Mode…" else "Apply Network Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        // Status Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(5),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (statusText.startsWith("✅")) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (statusText.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                )
            }
        }
    }
}

private fun NetworkMode.label(): String = when (this) {
    NetworkMode.NR_ONLY -> "NR Only (5G SA)"
    NetworkMode.NR_LTE -> "NR / LTE (5G NSA)"
    NetworkMode.LTE_ONLY -> "LTE Only (4G)"
}

private fun NetworkMode.description(): String = when (this) {
    NetworkMode.NR_ONLY -> "Pure 5G Standalone. Forces 5G NR band locking."
    NetworkMode.NR_LTE -> "5G NSA with LTE anchor. Most compatible mode."
    NetworkMode.LTE_ONLY -> "Locks to 4G LTE. Preserves battery life."
}
