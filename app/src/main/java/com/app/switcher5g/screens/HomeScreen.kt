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
import com.app.switcher5g.network.NetworkMode
import com.app.switcher5g.network.NetworkModeManager
import com.app.switcher5g.network.RootHelper
import com.app.switcher5g.network.ShizukuHelper
import com.app.switcher5g.network.SwitchResult
import com.app.switcher5g.ui.components.*
import com.app.switcher5g.util.ActivationMethod
import com.app.switcher5g.util.AppPreferences
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.switcher5g.ui.predictive.PetalScreenHost
import com.app.switcher5g.ui.theme.PetalMaterialShapes

@Composable
fun MainScreen(
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    var route by rememberSaveable { mutableStateOf("home") }
    val routeOrder = remember { listOf("home", "settings", "about") }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Home is the root; Settings and About stack above it with Petal's predictive back and
        // depth blur (the real Home surface shows blurred behind the departing screen).
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

    if (isSwitching) {
        SwitchingDialog(modeName = selectedMode.label())
    }

    val scrollState = rememberScrollState()
    val scrollBehavior = rememberPetalHeaderScrollBehavior()

    fun scanSims() {
        scope.launch {
            isScanningSims = true
            val ids = manager.getAvailableSubIds()
            availableSubIds = ids
            if (ids.isNotEmpty() && selectedSubId == null) selectedSubId = ids[0]
            isScanningSims = false
        }
    }

    ExpressivePullToRefreshBox(
        isRefreshing = isScanningSims,
        canPull = { scrollState.value == 0 && scrollBehavior.state.collapsedFraction < 0.01f },
        onRefresh = { scanSims() },
        modifier = Modifier.fillMaxSize(),
    ) {
        PetalScreenScaffold(
            title = "Switcher 5G",
            scrollBehavior = scrollBehavior,
            scrollState = scrollState,
            actions = {
                PetalLoadingBadge(visible = isSwitching || isScanningSims, size = 36.dp)
                val badgeLabel = when {
                    shizukuReady -> "Shizuku"
                    rootReady -> "Root"
                    else -> "Setup"
                }
                Surface(
                    onClick = { showSetupDialog = true },
                    shape = CircleShape,
                    color = if (isPrivileged) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val tint = if (isPrivileged) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer
                        Icon(
                            imageVector = if (isPrivileged) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = tint,
                        )
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                        )
                    }
                }
            },
        ) {
            // One-time setup banner
            if (!prefs.hasDismissedSetupCard && !isPrivileged) {
                PetalGroupItem(
                    position = PetalGroupPosition.SINGLE,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.entrance(0),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PetalShapedIcon(
                            icon = Icons.Rounded.Security,
                            shape = PetalMaterialShapes.Cookie6Sided,
                            variant = PetalBadgeVariant.TERTIARY,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Set up Shizuku or ADB",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "Optional for 1-tap in-app switching, or use Root / RadioInfo standalone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PetalButton(
                                onClick = { showSetupDialog = true },
                                style = PetalButtonStyle.FILLED,
                                height = 36.dp,
                            ) { Text("Setup") }
                            IconButton(onClick = { prefs.hasDismissedSetupCard = true }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Dismiss banner",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Live telemetry
            CellularSignalMonitorCard(
                subId = selectedSubId,
                modifier = Modifier.entrance(1),
            )

            // SIM selection
            PetalSection(title = "SIM subscription", modifier = Modifier.entrance(2)) {
                row { position ->
                    PetalGroupControlRow(
                        icon = Icons.Rounded.SimCard,
                        title = "Target SIM",
                        subtitle = "Active subscription: ${selectedSubId ?: "Auto-detect"}",
                        position = position,
                        badgeShape = PetalMaterialShapes.Pentagon,
                        variant = PetalBadgeVariant.TERTIARY,
                        headerTrailing = {
                            if (isScanningSims) {
                                PetalContainedLoadingIndicator(size = 40.dp)
                            } else {
                                FilledTonalIconButton(onClick = { scanSims() }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Scan SIMs")
                                }
                            }
                        },
                    ) {
                        PetalChoiceButtonGroup(
                            options = buildList {
                                add(PetalButtonGroupItem<Int?>(null, "Auto"))
                                availableSubIds.forEach { add(PetalButtonGroupItem<Int?>(it, "SIM $it")) }
                            },
                            selected = selectedSubId,
                            onSelect = { selectedSubId = it },
                        )
                    }
                }
            }

            // Network mode
            PetalSection(title = "Preferred network mode", modifier = Modifier.entrance(3)) {
                row { position ->
                    PetalGroupControlRow(
                        icon = Icons.Rounded.NetworkCheck,
                        title = selectedMode.label(),
                        subtitle = selectedMode.description(),
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                    ) {
                        PetalChoiceButtonGroup(
                            options = listOf(
                                PetalButtonGroupItem(NetworkMode.LTE_ONLY, "4G LTE"),
                                PetalButtonGroupItem(NetworkMode.NR_LTE, "5G NSA"),
                                PetalButtonGroupItem(NetworkMode.NR_ONLY, "5G SA"),
                            ),
                            selected = selectedMode,
                            onSelect = { selectedMode = it },
                            enabled = !isSwitching,
                            minHeight = 52.dp,
                        )
                    }
                }
            }

            // Apply
            PetalButton(
                enabled = !isSwitching,
                onClick = {
                    isSwitching = true
                    statusText = "Applying network mode switch…"
                    scope.launch {
                        val res = manager.switchTo(selectedMode, overrideSubId = selectedSubId, method = prefs.activationMethod)
                        statusText = when (res) {
                            is SwitchResult.Success -> {
                                prefs.currentActiveMode = selectedMode
                                "✅ ${res.message}"
                            }
                            is SwitchResult.Failure -> "⚠️ ${res.reason}"
                        }
                        isSwitching = false
                    }
                },
                height = 56.dp,
                modifier = Modifier.fillMaxWidth().entrance(4),
            ) {
                if (isSwitching) {
                    PetalLoadingIndicator(size = 24.dp)
                } else {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isSwitching) "Applying network mode…" else "Apply network mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Manual tools
            Row(
                modifier = Modifier.fillMaxWidth().entrance(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PetalButton(
                    onClick = {
                        if (!prefs.hasSeenManual5gDialog) showManual5gDialog = true
                        else com.app.switcher5g.network.Manual5gSwitchHelper.openRadioInfo(context)
                    },
                    style = PetalButtonStyle.TONAL,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Radio Info")
                }
                PetalButton(
                    onClick = { com.app.switcher5g.network.Manual5gSwitchHelper.openBandSelection(context) },
                    style = PetalButtonStyle.TONAL,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.SettingsCell, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lock Bands")
                }
            }

            // Status
            PetalGroupItem(
                position = PetalGroupPosition.SINGLE,
                modifier = Modifier.entrance(6),
            ) {
                val ok = statusText.startsWith("✅")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PetalShapedIcon(
                        icon = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                        shape = PetalMaterialShapes.Cookie4Sided,
                        variant = if (ok) PetalBadgeVariant.PRIMARY else PetalBadgeVariant.TERTIARY,
                        size = 40.dp,
                        iconSize = 20.dp,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PetalContainedLoadingIndicator(size = 80.dp)
                Text(
                    "Applying network mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Switching to $modeName…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
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
