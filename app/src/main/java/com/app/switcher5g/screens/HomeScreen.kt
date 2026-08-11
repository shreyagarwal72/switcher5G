package com.app.switcher5g.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.ui.components.*
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                        slideInHorizontally(tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.15f).toInt() } +
                        scaleIn(initialScale = 0.94f, animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (-it * 0.15f).toInt() } +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                        slideInHorizontally(tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (-it * 0.15f).toInt() } +
                        scaleIn(initialScale = 0.94f, animationSpec = tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                        slideOutHorizontally(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.15f).toInt() } +
                        scaleOut(targetScale = 0.94f, animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            },
        ) {
            composable("home") {
                HomeScreenContent(prefs = prefs)
            }
            composable("settings") {
                SettingsScreen(prefs = prefs)
            }
            composable("about") {
                AboutScreen()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreenContent(prefs: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { NetworkModeManager(context.applicationContext) }

    var selectedMode by remember { mutableStateOf(prefs.defaultNetworkMode) }
    var availableSubIds by remember { mutableStateOf<List<Int>>(listOf(1, 2)) }
    var selectedSubId by remember { mutableStateOf<Int?>(null) }
    var statusText by remember { mutableStateOf("Ready to switch network mode.") }
    var isSwitching by remember { mutableStateOf(false) }
    var isScanningSims by remember { mutableStateOf(false) }
    var shizukuReady by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var rootReady by remember { mutableStateOf(false) }
    val isPrivileged = shizukuReady || rootReady
    var showSetupDialog by remember { mutableStateOf(false) }
    var showManual5gDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.app.switcher5g.update.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (RootHelper.isRootAvailable()) {
            rootReady = RootHelper.isRootGranted()
        }
        if (prefs.autoScanSims && (shizukuReady || rootReady)) {
            isScanningSims = true
            val ids = manager.getAvailableSubIds()
            availableSubIds = ids
            if (ids.isNotEmpty()) selectedSubId = ids[0]
            isScanningSims = false
        }
        if (prefs.autoCheckUpdates) {
            val currentVer = com.app.switcher5g.util.AppInfo.getAppVersionName(context)
            val info = com.app.switcher5g.update.UpdateManager.checkForUpdates(currentVer)
            if (info.hasUpdate) {
                updateInfo = info
                showUpdateDialog = true
            }
        }
    }

    DisposableEffect(Unit) {
        val unregisterShizuku = ShizukuHelper.registerListeners { _, permission ->
            shizukuReady = permission
        }
        onDispose {
            unregisterShizuku()
            manager.unbind()
        }
    }

    if (showSetupDialog) {
        ShizukuSetupDialog(
            onDismissRequest = { showSetupDialog = false },
            onStatusUpdated = {
                shizukuReady = ShizukuHelper.hasPermission()
                scope.launch {
                    if (RootHelper.isRootAvailable()) {
                        rootReady = RootHelper.isRootGranted()
                    }
                }
            },
        )
    }

    if (showManual5gDialog) {
        Manual5gFirstTimeDialog(
            onDismissRequest = { showManual5gDialog = false },
            onDontShowAgain = { dontShow -> if (dontShow) prefs.hasSeenManual5gDialog = true },
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        com.app.switcher5g.ui.components.UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            onDismissRequest = { showUpdateDialog = false },
        )
    }

    NetworkModeSwitchLoadingOverlay(
        isSwitching = isSwitching,
        modeName = selectedMode.label(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RefreshProgressBar(
            isRefreshing = isSwitching || isScanningSims,
            label = if (isSwitching) "Executing network mode switch…" else "Scanning active SIM cards…",
            modifier = Modifier.entrance(0),
        )

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .entrance(0),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Switcher 5G",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Setup / Status Pill Badge
            val badgeLabel = when {
                shizukuReady -> "Shizuku Active"
                rootReady -> "Root Active"
                else -> "Setup / ADB"
            }
            Surface(
                shape = CircleShape,
                color = if (isPrivileged) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .bouncyClickable(scaleDown = 0.92f) { showSetupDialog = true }
                    .shadow(4.dp, CircleShape),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isPrivileged) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isPrivileged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isPrivileged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        // ONE-TIME Setup Banner (Appears only 1 time on Home Screen until dismissed)
        if (!prefs.hasDismissedSetupCard && !isPrivileged) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(1)
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Column {
                            Text(
                                text = "Setup Shizuku or ADB",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "Optional for 1-tap in-app switching, or use Root/RadioInfo standalone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showSetupDialog = true }) {
                            Text("Setup")
                        }
                        IconButton(
                            onClick = { prefs.hasDismissedSetupCard = true },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Dismiss Banner",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Live Cellular Signal Telemetry Card
        CellularSignalMonitorCard(
            subId = selectedSubId,
            modifier = Modifier.entrance(2),
        )

        // Target SIM Subscription Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(3)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
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
                            Icons.Rounded.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "SIM Subscription",
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
                                    val ids = manager.getAvailableSubIds()
                                    availableSubIds = ids
                                    if (ids.isNotEmpty() && selectedSubId == null) selectedSubId = ids[0]
                                    isScanningSims = false
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Scan SIMs")
                        }
                    }
                }

                Text(
                    text = "Active Subscription ID: ${selectedSubId ?: "Auto-Detect"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // SIM Slot Choice Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InputChip(
                        selected = selectedSubId == null,
                        onClick = { selectedSubId = null },
                        label = { Text("Auto-Detect SIM") },
                    )
                    availableSubIds.forEach { subId ->
                        InputChip(
                            selected = selectedSubId == subId,
                            onClick = { selectedSubId = subId },
                            label = { Text("SIM Slot (Sub $subId)") },
                        )
                    }
                }
            }
        }

        // Preferred Network Mode Custom Slider Section
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
                Text(
                    text = "Preferred Network Mode",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val networkOptions = remember { listOf("2G", "3G", "4G LTE", "5G NSA", "5G SA") }
                val currentOptionIndex = when (selectedMode) {
                    NetworkMode.LTE_ONLY -> 2
                    NetworkMode.NR_LTE -> 3
                    NetworkMode.NR_ONLY -> 4
                }

                NetworkModeSlider(
                    options = networkOptions,
                    selected = currentOptionIndex,
                    onSelect = { index ->
                        selectedMode = when (index) {
                            0, 1, 2 -> NetworkMode.LTE_ONLY
                            3 -> NetworkMode.NR_LTE
                            4 -> NetworkMode.NR_ONLY
                            else -> NetworkMode.NR_LTE
                        }
                    },
                    enabled = !isSwitching,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Standalone Apply Button (Works with Shizuku, Root, or RadioInfo Testing Menu!)
        Button(
            enabled = !isSwitching,
            onClick = {
                isSwitching = true
                statusText = "Applying network mode switch…"
                scope.launch {
                    val result = manager.switchTo(selectedMode, selectedSubId, prefs.activationMethod)
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
                .entrance(5)
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
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                }
                Text(
                    text = if (isSwitching) "Applying Network Mode…" else "Apply Network Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        // Manual 5G System Switcher Button Card (OpenAppsLabs/5G)
        OutlinedButton(
            onClick = {
                if (!prefs.hasSeenManual5gDialog) {
                    showManual5gDialog = true
                } else {
                    com.app.switcher5g.network.Manual5gSwitchHelper.openRadioInfo(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .entrance(5)
                .bouncyClickable {},
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Rounded.NetworkCheck, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Manual 5G Switch (System RadioInfo)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
        }

        // Status Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(6),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (statusText.startsWith("✅")) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
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
    NetworkMode.NR_ONLY -> "Forces pure 5G Standalone mode."
    NetworkMode.NR_LTE -> "5G NSA with LTE anchor band."
    NetworkMode.LTE_ONLY -> "Locks to 4G LTE mode."
}
