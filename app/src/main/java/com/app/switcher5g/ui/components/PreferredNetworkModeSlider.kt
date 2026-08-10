package com.app.switcher5g.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CellularGood
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.NetworkMode
import kotlin.math.roundToInt

/**
 * Specification details for discrete Network Modes displayed on the slider.
 */
data class NetworkModeSpec(
    val mode: NetworkMode,
    val shortLabel: String,
    val subtitle: String,
    val fullTitle: String,
    val badge: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val description: String,
)

/**
 * Specs helper for Network Modes.
 */
fun getNetworkModeSpec(mode: NetworkMode): NetworkModeSpec = when (mode) {
    NetworkMode.NR_ONLY -> NetworkModeSpec(
        mode = NetworkMode.NR_ONLY,
        shortLabel = "5G SA",
        subtitle = "Standalone",
        fullTitle = "NR Only (5G Standalone)",
        badge = "⚡ Ultra Latency",
        icon = Icons.Rounded.Bolt,
        primaryColor = Color(0xFFA033FF),
        secondaryColor = Color(0xFF00E5FF),
        description = "Locks cellular modem strictly to 5G Standalone core. Maximizes bandwidth and minimizes latency.",
    )
    NetworkMode.NR_LTE -> NetworkModeSpec(
        mode = NetworkMode.NR_LTE,
        shortLabel = "5G NSA",
        subtitle = "Hybrid 5G+4G",
        fullTitle = "NR / LTE (5G Non-Standalone)",
        badge = "🌐 Auto Hybrid",
        icon = Icons.Rounded.CellularGood,
        primaryColor = Color(0xFF00E676),
        secondaryColor = Color(0xFF00B0FF),
        description = "Uses 5G data radio with 4G LTE anchor control channel. Recommended default mode.",
    )
    NetworkMode.LTE_ONLY -> NetworkModeSpec(
        mode = NetworkMode.LTE_ONLY,
        shortLabel = "4G LTE",
        subtitle = "Power Saver",
        fullTitle = "LTE Only (4G)",
        badge = "🔋 Battery Saver",
        icon = Icons.Rounded.SignalCellular4Bar,
        primaryColor = Color(0xFF2979FF),
        secondaryColor = Color(0xFF40C4FF),
        description = "Disables 5G search to conserve battery and eliminate network switching delays.",
    )
}

/**
 * Custom Expressive Jetpack Compose Slider for Preferred Network Mode switching.
 *
 * Specifications & Features:
 * - Discrete Stepped Snapping for Network Modes (NR_ONLY, NR_LTE, LTE_ONLY)
 * - M3-Expressive Squishy Track & Floating Spring Thumb Physics
 * - Haptic Feedback on Step Threshold Triggers
 * - Dynamic Mode Colors & Gradient Trails per Network Mode
 * - Interactive Step Anchors & Tooltip Badges
 * - Full Accessibility & Accessibility Semantics support
 *
 * @param selectedMode Currently active [NetworkMode]
 * @param onModeSelected Callback fired when a new [NetworkMode] is selected
 * @param modifier Custom layout modifier
 * @param enabled Controls interactive state
 * @param modes List of available discrete modes (defaults to [NetworkMode.entries])
 */
@Composable
fun PreferredNetworkModeSlider(
    selectedMode: NetworkMode,
    onModeSelected: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    modes: List<NetworkMode> = NetworkMode.entries,
) {
    val haptic = LocalHapticFeedback.current
    val totalSteps = modes.size.coerceAtLeast(1)
    val maxIndex = (totalSteps - 1).coerceAtLeast(0)

    val targetSelectedIndex = remember(selectedMode, modes) {
        modes.indexOf(selectedMode).coerceIn(0, maxIndex)
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember(targetSelectedIndex) { mutableFloatStateOf(targetSelectedIndex.toFloat()) }
    var lastHapticIndex by remember { mutableIntStateOf(targetSelectedIndex) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val activeSpec = getNetworkModeSpec(modes[targetSelectedIndex])

    // Animated spring physics for smooth track gliding when not dragging
    val animatedFraction by animateFloatAsState(
        targetValue = if (isDragging) dragProgressFraction else targetSelectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sliderFractionAnimation",
    )

    val currentNormalizedProgress = (animatedFraction / maxIndex.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    // Dynamic Track & Thumb Squishy Height Animations
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 28.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "trackHeightAnim",
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "thumbScaleAnim",
    )

    // Dynamic Accent Colors based on active network mode
    val activePrimaryColor by animateColorAsState(
        targetValue = if (enabled) activeSpec.primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        animationSpec = tween(300),
        label = "primaryColorAnim",
    )

    val activeSecondaryColor by animateColorAsState(
        targetValue = if (enabled) activeSpec.secondaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        animationSpec = tween(300),
        label = "secondaryColorAnim",
    )

    fun updateIndexFromX(xPx: Float) {
        if (!enabled) return
        val clampedX = xPx.coerceIn(0f, widthPx)
        val rawFraction = (clampedX / widthPx.coerceAtLeast(1f)) * maxIndex
        dragProgressFraction = rawFraction.coerceIn(0f, maxIndex.toFloat())

        val nearestIndex = dragProgressFraction.roundToInt().coerceIn(0, maxIndex)
        if (nearestIndex != lastHapticIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticIndex = nearestIndex
        }
    }

    fun finalizeSnap() {
        if (!enabled) return
        isDragging = false
        val finalIndex = dragProgressFraction.roundToInt().coerceIn(0, maxIndex)
        dragProgressFraction = finalIndex.toFloat()
        if (modes[finalIndex] != selectedMode) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onModeSelected(modes[finalIndex])
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                stateDescription = activeSpec.fullTitle
            },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- 1. Top Mode Details Badge Header ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(activePrimaryColor, activeSecondaryColor),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = activeSpec.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = activeSpec.fullTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = activeSpec.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = activePrimaryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, activePrimaryColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = activeSpec.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = activePrimaryColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        // --- 2. Interactive Expressive Slider Track ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .onGloballyPositioned { widthPx = it.size.width.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            updateIndexFromX(offset.x)
                            tryAwaitRelease()
                            finalizeSnap()
                        },
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            updateIndexFromX(offset.x)
                        },
                        onDragEnd = { finalizeSnap() },
                        onDragCancel = { finalizeSnap() },
                    ) { change, _ ->
                        updateIndexFromX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Slider Track Background & Gradient Fill
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight),
            ) {
                val trackCornerRadius = CornerRadius(trackHeight.toPx() / 2f)
                val trackWidth = size.width
                val trackH = size.height

                // Inactive track base
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.15f),
                    size = Size(trackWidth, trackH),
                    cornerRadius = trackCornerRadius,
                )

                // Active glowing gradient track fill
                val fillWidth = trackWidth * currentNormalizedProgress
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(activePrimaryColor.copy(alpha = 0.8f), activePrimaryColor),
                            endX = fillWidth.coerceAtLeast(1f),
                        ),
                        size = Size(fillWidth, trackH),
                        cornerRadius = trackCornerRadius,
                    )
                }

                // Step Tick Anchors
                if (totalSteps > 1) {
                    val stepSpacing = trackWidth / (totalSteps - 1)
                    for (i in 0 until totalSteps) {
                        val tickX = stepSpacing * i
                        val isPassed = i.toFloat() <= animatedFraction + 0.1f
                        val tickColor = if (isPassed) Color.White.copy(alpha = 0.9f) else activePrimaryColor.copy(alpha = 0.4f)
                        val tickRadius = if (i == targetSelectedIndex) 5.dp.toPx() else 3.5.dp.toPx()

                        drawCircle(
                            color = tickColor,
                            radius = tickRadius,
                            center = Offset(tickX.coerceIn(12.dp.toPx(), trackWidth - 12.dp.toPx()), trackH / 2f),
                        )
                    }
                }
            }

            // Animated Floating Thumb Capsule
            val thumbOffsetPx = currentNormalizedProgress * widthPx
            val thumbWidth = 48.dp
            val thumbHeight = 36.dp

            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = (thumbOffsetPx - (thumbWidth.toPx() / 2f)).roundToInt(),
                            y = 0,
                        )
                    }
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                    }
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = activePrimaryColor)
                    .size(width = thumbWidth, height = thumbHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(activePrimaryColor, activeSecondaryColor),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = activeSpec.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // --- 3. Step Mode Anchor Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            modes.forEachIndexed { index, mode ->
                val spec = getNetworkModeSpec(mode)
                val isSelected = index == targetSelectedIndex

                val stepColor by animateColorAsState(
                    targetValue = if (isSelected) spec.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    animationSpec = tween(200),
                    label = "stepColorAnim",
                )

                val stepWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) {
                            if (mode != selectedMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onModeSelected(mode)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = spec.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = stepWeight,
                            fontSize = 13.sp,
                        ),
                        color = stepColor,
                    )
                    Text(
                        text = spec.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // --- 4. Description Footer ---
        Text(
            text = activeSpec.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )
    }
}
