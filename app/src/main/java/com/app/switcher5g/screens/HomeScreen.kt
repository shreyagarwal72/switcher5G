package com.app.switcher5g.screens

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
import com.app.switcher5g.ui.components.*
import com.app.switcher5g.util.AppPreferences
import com.app.switcher5g.util.MarkdownUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                fadeIn(tween(180)) + slideInHorizontally(tween(240)) { it / 8 } +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(240))
            },
            exitTransition = {
                fadeOut(tween(140)) + slideOutHorizontally(tween(240)) { -it / 8 }
            },
            popEnterTransition = {
                fadeIn(tween(180)) + slideInHorizontally(tween(240)) { -it / 8 }
            },
            popExitTransition = {
                fadeOut(tween(140)) + slideOutHorizontally(tween(240)) { it / 8 }
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
    var availableSubIds by remember { mutableStateOf<List<Int>>(listOf(1)) }
    var selectedSubId by remember { mutableStateOf<Int?>(null) }
    var statusText by remember { mutableStateOf("Ready to switch network mode.") }
    var isSwitching by remember { mutableStateOf(false) }
    var isScanningSims by remember { mutableStateOf(false) }
    var shizukuReady by remember { mutableStateOf(ShizukuHelper.hasPermission()) }
    var useWheelPicker by remember { mutableStateOf(prefs.useWheelPicker) }

    val formattedDate = remember {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
        } catch (_: Throwable) {
            "5G Network Controller"
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.autoScanSims && ShizukuHelper.hasPermission()) {
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

    // Modal Expressive loading overlay during network mode switch via Shizuku
    NetworkModeSwitchLoadingOverlay(
        isSwitching = isSwitching,
        modeName = selectedMode.label(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Linear Loading Bar during active operations
        if (isSwitching || isScanningSims) {
            FancyLinearLoadingBar(
                progress = null,
                label = if (isSwitching) "Executing Shizuku IPC switch…" else "Scanning active SIM cards…",
                modifier = Modifier.entrance(0),
            )
        }

        // Stride-style App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(0)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Switcher 5G",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Shizuku Connection Status Pill (matching Stride streak badge style)
            Surface(
                shape = CircleShape,
                color = if (shizukuReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.shadow(4.dp, CircleShape),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (shizukuReady) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (shizukuReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = if (shizukuReady) "Shizuku Ready" else "Disconnected",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (shizukuReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Stride Hero Card Banner
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
                                Icons.Rounded.CellTower,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "Privileged Band Control",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                ),
                            )
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

        // Interactive Shizuku Activation & ADB Command Card
        ShizukuActivationCard(
            modifier = Modifier.entrance(2),
            onStatusChanged = { shizukuReady = it },
        )

        // Active SIM Card Selector Section
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
                    text = "Selected Sub ID: ${selectedSubId ?: "Auto-Detect"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // SIM Slot Choice Chips with Mobile Wrapping Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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

        // Network Mode Selector Section
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
                            onCheckedChange = {
                                useWheelPicker = it
                                prefs.useWheelPicker = it
                            },
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

                // Description for current mode
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
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
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
    NetworkMode.NR_ONLY -> "Pure 5G Standalone. Forces 5G NR band locking."
    NetworkMode.NR_LTE -> "5G NSA with LTE anchor. Most compatible mode."
    NetworkMode.LTE_ONLY -> "Locks to 4G LTE. Preserves battery life."
}
