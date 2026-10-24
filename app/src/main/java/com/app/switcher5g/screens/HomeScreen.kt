package com.app.switcher5g.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.switcher5g.network.Manual5gSwitchHelper
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.ui.components.*
import com.app.switcher5g.ui.predictive.PetalScreenHost
import com.app.switcher5g.ui.theme.PetalMaterialShapes
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    var route by rememberSaveable { mutableStateOf("home") }
    val routeOrder = remember { listOf("home", "settings", "about") }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PetalScreenHost(
            route = route,
            rootRoute = "home",
            routeOrder = routeOrder,
            onBack = { route = "home" },
            predictiveEnabled = prefs.predictiveBack,
            blurEnabled = prefs.depthBlur,
            root = { HomeScreenContent(prefs = prefs) },
            overlay = { target ->
                when (target) {
                    "settings" -> SettingsScreen(prefs = prefs, onNavigateBack = { route = "home" })
                    "about" -> AboutScreen(onNavigateBack = { route = "home" })
                }
            },
        )

        StrideFloatingNav(
            currentRoute = route,
            onNavigate = { target -> route = target },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        if (prefs.checkForUpdatesOnStart) {
            val appVer = com.app.switcher5g.util.AppInfo.getAppVersionName(context)
            val info = com.app.switcher5g.update.UpdateManager.checkForUpdates(appVer)
            if (info != null && info.hasUpdate) {
                updateInfo = info
                showUpdateDialog = true
            }
        }
    }

    fun applyMode(mode: NetworkMode) {
        selectedMode = mode
        isSwitching = true
        scope.launch {
            val effectiveMethod = when (prefs.activationMethod) {
                ActivationMethod.AUTO -> when {
                    shizukuReady -> ActivationMethod.SHIZUKU
                    rootReady -> ActivationMethod.ROOT
                    else -> ActivationMethod.MANUAL
                }
                else -> prefs.activationMethod
            }

            val result = when (effectiveMethod) {
                ActivationMethod.SHIZUKU, ActivationMethod.AUTO ->
                    manager.switchModeWithShizuku(mode, selectedSubId)
                ActivationMethod.ROOT ->
                    manager.switchModeWithRoot(mode, selectedSubId)
                ActivationMethod.MANUAL -> {
                    if (!prefs.hasSeenManual5gDialog) {
                        showManual5gDialog = true
                    } else {
                        Manual5gSwitchHelper.openRadioInfo(context)
                    }
                    SwitchResult.Success("Opening system network settings…")
                }
            }

            statusText = when (result) {
                is SwitchResult.Success -> "✅ ${result.message}"
                is SwitchResult.Error -> "❌ ${result.error}"
            }
            isSwitching = false
        }
    }

    if (isSwitching) {
        SwitchingDialog(modeName = selectedMode.label())
    }

    if (showSetupDialog) {
        ShizukuSetupDialog(
            onDismiss = {
                showSetupDialog = false
                shizukuReady = ShizukuHelper.hasPermission()
                rootReady = RootHelper.isRootGranted()
            },
        )
    }

    if (showManual5gDialog) {
        Manual5gFirstTimeDialog(
            onDismiss = { showManual5gDialog = false },
            onContinue = {
                showManual5gDialog = false
                prefs.hasSeenManual5gDialog = true
                Manual5gSwitchHelper.openRadioInfo(context)
            },
        )
    }

    if (showUpdateDialog && updateInfo != null) {
        UpdateAvailableDialog(
            updateInfo = updateInfo!!,
            onDismiss = { showUpdateDialog = false },
        )
    }

    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    val scrollState = rememberScrollState()

    ExpressivePullToRefreshBox(
        isRefreshing = isScanningSims,
        onRefresh = {
            isScanningSims = true
            shizukuReady = ShizukuHelper.hasPermission()
            rootReady = RootHelper.isRootGranted()
            scope.launch {
                val ids = manager.getAvailableSubIds()
                availableSubIds = ids
                if (ids.isNotEmpty() && (selectedSubId == null || selectedSubId !in ids)) {
                    selectedSubId = ids[0]
                }
                isScanningSims = false
            }
        },
        scrollState = scrollState,
        modifier = Modifier.fillMaxSize(),
    ) {
        PetalScreenScaffold(
            title = "Switcher 5G",
            scrollBehavior = scrollBehavior,
            scrollState = scrollState,
        ) {
            // 1. Privileged Authorization Status Banner (if not connected)
            if (!isPrivileged) {
                PetalGroupItem(
                    position = PetalGroupPosition.SINGLE,
                    modifier = Modifier.entrance(0),
                    onClick = { showSetupDialog = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        PetalShapedIcon(
                            icon = Icons.Rounded.Security,
                            shape = PetalMaterialShapes.Burst,
                            variant = PetalBadgeVariant.SECONDARY,
                            size = 44.dp,
                            iconSize = 22.dp,
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Privileged Service Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Connect Shizuku or grant Root access for seamless in-app switching without opening system menus.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Setup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 2. Real-time Cellular Signal & Telemetry Monitor
            CellularSignalMonitorCard(
                subId = selectedSubId,
                modifier = Modifier.entrance(1),
            )

            // 3. Network Control Center (SIM Selection & Expressive Mode Switcher)
            PetalSection(
                title = "Network Controls",
                modifier = Modifier.entrance(2),
            ) {
                // SIM Selection Row
                row { position ->
                    PetalGroupControlRow(
                        icon = Icons.Rounded.SimCard,
                        title = "Active Subscription",
                        subtitle = if (availableSubIds.size > 1) "Select which SIM card to configure" else "Primary SIM Slot",
                        position = position,
                        badgeShape = PetalMaterialShapes.Cookie6Sided,
                        variant = PetalBadgeVariant.PRIMARY,
                    ) {
                        val simOptions = remember(availableSubIds) {
                            if (availableSubIds.isNotEmpty()) {
                                availableSubIds.map { subId ->
                                    PetalButtonGroupItem(value = subId as Int?, label = "SIM $subId")
                                }
                            } else {
                                listOf(
                                    PetalButtonGroupItem(value = 1 as Int?, label = "SIM 1"),
                                    PetalButtonGroupItem(value = 2 as Int?, label = "SIM 2"),
                                )
                            }
                        }

                        PetalChoiceButtonGroup(
                            options = simOptions,
                            selected = selectedSubId ?: availableSubIds.firstOrNull() ?: 1,
                            onSelect = { selectedSubId = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Consolidated Network Mode Selector
                row { position ->
                    PetalGroupControlRow(
                        icon = Icons.Rounded.CellTower,
                        title = "Preferred Network Mode",
                        subtitle = selectedMode.description(),
                        position = position,
                        badgeShape = PetalMaterialShapes.Flower,
                        variant = PetalBadgeVariant.TERTIARY,
                    ) {
                        PetalChoiceButtonGroup(
                            options = listOf(
                                PetalButtonGroupItem(value = NetworkMode.NR_ONLY, label = "5G SA"),
                                PetalButtonGroupItem(value = NetworkMode.NR_LTE, label = "5G NSA"),
                                PetalButtonGroupItem(value = NetworkMode.LTE_ONLY, label = "4G LTE"),
                            ),
                            selected = selectedMode,
                            onSelect = { newMode -> selectedMode = newMode },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 4. Primary Action Deck
            Column(
                modifier = Modifier.fillMaxWidth().entrance(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Big Apply Button
                PetalButton(
                    onClick = { applyMode(selectedMode) },
                    enabled = !isSwitching,
                    style = PetalButtonStyle.FILLED,
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSwitching) {
                        PetalLoadingIndicator(size = 22.dp)
                    } else {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isSwitching) "Applying network mode…" else "Apply ${selectedMode.label()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Quick Hardware / Radio Diagnostics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PetalButton(
                        onClick = {
                            if (!prefs.hasSeenManual5gDialog) showManual5gDialog = true
                            else Manual5gSwitchHelper.openRadioInfo(context)
                        },
                        style = PetalButtonStyle.TONAL,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Radio Info", fontWeight = FontWeight.SemiBold)
                    }
                    PetalButton(
                        onClick = { Manual5gSwitchHelper.openBandSelection(context) },
                        style = PetalButtonStyle.TONAL,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.SettingsCell, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lock Bands", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 5. Switch Feedback / Status Card
            PetalGroupItem(
                position = PetalGroupPosition.SINGLE,
                modifier = Modifier.entrance(4),
            ) {
                val ok = statusText.startsWith("✅")
                val isErr = statusText.startsWith("❌")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PetalShapedIcon(
                        icon = when {
                            ok -> Icons.Rounded.CheckCircle
                            isErr -> Icons.Rounded.ErrorOutline
                            else -> Icons.Rounded.Info
                        },
                        shape = PetalMaterialShapes.Cookie4Sided,
                        variant = when {
                            ok -> PetalBadgeVariant.PRIMARY
                            isErr -> PetalBadgeVariant.SECONDARY
                            else -> PetalBadgeVariant.TERTIARY
                        },
                        size = 38.dp,
                        iconSize = 18.dp,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Modal shown while the privileged switch runs; blocks input behind it. */
@Composable
private fun SwitchingDialog(modeName: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PetalContainedLoadingIndicator(size = 72.dp)
                Text(
                    "Applying Network Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Switching radio to $modeName…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun NetworkMode.label(): String = when (this) {
    NetworkMode.NR_ONLY -> "5G SA (NR Only)"
    NetworkMode.NR_LTE -> "5G NSA (NR / LTE)"
    NetworkMode.LTE_ONLY -> "4G LTE Only"
}

private fun NetworkMode.description(): String = when (this) {
    NetworkMode.NR_ONLY -> "Forces pure 5G Standalone mode."
    NetworkMode.NR_LTE -> "5G Non-Standalone with 4G LTE anchor."
    NetworkMode.LTE_ONLY -> "Locks connection strictly to 4G LTE."
}
