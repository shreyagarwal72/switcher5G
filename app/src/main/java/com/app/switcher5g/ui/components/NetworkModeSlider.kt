package com.app.switcher5g.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 12-point scalloped "cookie" shape for the slider thumb.
 */
class ScallopedShape(
    private val points: Int = 12,
    private val innerRadiusRatio: Float = 0.82f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = min(size.width, size.height) / 2f
        val innerR = outerR * innerRadiusRatio

        val anglePerLobe = (2.0 * Math.PI / points).toFloat()

        for (i in 0 until points) {
            val startAngle = i * anglePerLobe - (Math.PI / 2).toFloat()
            val midAngle = startAngle + anglePerLobe / 2f
            val endAngle = startAngle + anglePerLobe

            val xOuter = cx + outerR * cos(startAngle)
            val yOuter = cy + outerR * sin(startAngle)

            val xInner = cx + innerR * cos(midAngle)
            val yInner = cy + innerR * sin(midAngle)

            val xEnd = cx + outerR * cos(endAngle)
            val yEnd = cy + outerR * sin(endAngle)

            if (i == 0) {
                path.moveTo(xOuter, yOuter)
            }

            path.quadraticTo(
                cx + outerR * cos(startAngle + anglePerLobe / 4f),
                cy + outerR * sin(startAngle + anglePerLobe / 4f),
                xInner,
                yInner,
            )
            path.quadraticTo(
                cx + outerR * cos(midAngle + anglePerLobe / 4f),
                cy + outerR * sin(midAngle + anglePerLobe / 4f),
                xEnd,
                yEnd,
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Reusable Discrete Stepped Slider for Telephony Preferred Network Mode selection.
 *
 * Specifications & Features:
 * - Pill-shaped track with rounded ends
 * - 12-point scalloped "cookie" shape thumb with rolling rotation physics
 *   Rotation = (distance traveled / thumb circumference) * 360°
 * - Accent color (MaterialTheme.colorScheme.primary) for fill track & cookie thumb
 * - Neutral gray (surfaceVariant) for unfilled track base
 * - Immediate 1:1 position tracking during drag (zero delay). Spring-style snap on release.
 * - Bold/highlighted label above selected step
 *
 * @param options List of step labels (e.g. listOf("2G", "3G", "4G LTE", "5G NSA", "5G SA"))
 * @param selected Currently active selected index in [options]
 * @param onSelect Callback when step index is selected
 */
@Composable
fun NetworkModeSlider(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val totalSteps = options.size.coerceAtLeast(1)
    val maxIndex = (totalSteps - 1).coerceAtLeast(1)
    val clampedSelected = selected.coerceIn(0, maxIndex)

    var isDragging by remember { mutableStateOf(false) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var lastHapticStep by remember { mutableIntStateOf(clampedSelected) }

    // Single dynamic accent color for fill track & scalloped thumb
    val accentColor = MaterialTheme.colorScheme.primary
    val trackBgColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbOnColor = MaterialTheme.colorScheme.onPrimary
    val selectedTextColor = MaterialTheme.colorScheme.primary
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Spring-style ease animation ONLY on release snap
    val snapAnimatedFraction by animateFloatAsState(
        targetValue = clampedSelected.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 350f,
        ),
        label = "snapTrackAnimation",
    )

    // Immediate 1:1 finger tracking during drag (zero animation delay)
    val currentStepFraction = if (isDragging) {
        (dragPx / trackWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f) * maxIndex
    } else {
        snapAnimatedFraction
    }

    val currentNormalizedProgress = (currentStepFraction / maxIndex.toFloat()).coerceIn(0f, 1f)

    // Rolling rotation angle: (distance / thumbCircumference) * 360°
    val thumbSize = 40.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val thumbCircumferencePx = (Math.PI * thumbSizePx).toFloat()
    val distanceTraveledPx = currentNormalizedProgress * trackWidthPx
    val rotationDegrees = (distanceTraveledPx / thumbCircumferencePx.coerceAtLeast(1f)) * 360f

    // Thumb scale: 1.1x on press/drag, back to 1.0x on release
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "thumbScaleAnim",
    )

    // Elevation increase while dragging
    val thumbElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "thumbElevationAnim",
    )

    fun updateDragX(xPx: Float) {
        if (!enabled) return
        dragPx = xPx.coerceIn(0f, trackWidthPx)
        val nearestStep = ((dragPx / trackWidthPx.coerceAtLeast(1f)) * maxIndex).roundToInt().coerceIn(0, maxIndex)
        if (nearestStep != lastHapticStep) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticStep = nearestStep
        }
    }

    fun releaseAndSnap() {
        if (!enabled) return
        isDragging = false
        val finalStep = ((dragPx / trackWidthPx.coerceAtLeast(1f)) * maxIndex).roundToInt().coerceIn(0, maxIndex)
        if (finalStep != selected) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(finalStep)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                stateDescription = options.getOrElse(clampedSelected) { "" }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // --- 1. Step Labels Above Slider (Bold & Highlighted when selected) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val isStepSelected = index == clampedSelected
                val textColor = if (isStepSelected) selectedTextColor else unselectedTextColor
                val fontWeight = if (isStepSelected) FontWeight.ExtraBold else FontWeight.Medium
                val fontSize = if (isStepSelected) 14.sp else 12.sp

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = fontWeight,
                        fontSize = fontSize,
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (index != selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(index)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        // --- 2. Track & Rolling Scalloped Cookie Thumb Component ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            updateDragX(offset.x)
                            tryAwaitRelease()
                            releaseAndSnap()
                        },
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            updateDragX(offset.x)
                        },
                        onDragEnd = { releaseAndSnap() },
                        onDragCancel = { releaseAndSnap() },
                    ) { change, _ ->
                        updateDragX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Pill-shaped Track: Neutral gray unfilled track & accent-colored fill track
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
            ) {
                val corner = CornerRadius(size.height / 2f)
                val fullW = size.width
                val h = size.height

                // Neutral gray unfilled track
                drawRoundRect(
                    color = trackBgColor,
                    size = Size(fullW, h),
                    cornerRadius = corner,
                )

                // Filled active track using single accent color
                val fillW = fullW * currentNormalizedProgress
                if (fillW > 0f) {
                    drawRoundRect(
                        color = accentColor,
                        size = Size(fillW, h),
                        cornerRadius = corner,
                    )
                }

                // Step Tick Indicators
                if (totalSteps > 1) {
                    val stepSpacing = fullW / (totalSteps - 1)
                    for (i in 0 until totalSteps) {
                        val tickX = stepSpacing * i
                        val isPassed = i <= currentStepFraction + 0.1f
                        val tickColor = if (isPassed) thumbOnColor else accentColor.copy(alpha = 0.5f)
                        val tickRadius = if (i == clampedSelected) 4.5.dp.toPx() else 3.dp.toPx()

                        drawCircle(
                            color = tickColor,
                            radius = tickRadius,
                            center = Offset(tickX.coerceIn(8.dp.toPx(), fullW - 8.dp.toPx()), h / 2f),
                        )
                    }
                }
            }

            // 12-point Scalloped "Cookie" Thumb with Rolling Rotation Physics
            val thumbOffsetPx = currentNormalizedProgress * trackWidthPx

            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            x = (thumbOffsetPx - (thumbSizePx / 2f)).roundToInt(),
                            y = 0,
                        )
                    }
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                        rotationZ = rotationDegrees // Visually rotates matching distance / circumference!
                    }
                    .shadow(
                        elevation = thumbElevation,
                        shape = ScallopedShape(points = 12),
                        spotColor = accentColor,
                    )
                    .size(thumbSize)
                    .clip(ScallopedShape(points = 12))
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                // Inner center dot accent
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(thumbOnColor),
                )
            }
        }
    }
}
