package com.app.switcher5g.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.network.NetworkMode
import kotlin.math.roundToInt

/**
 * Material 3 Fancy Segmented Slide Bar with spring-animated sliding pill thumb
 * for switching between network modes (NR_ONLY, NR_LTE, LTE_ONLY).
 */
@Composable
fun FancyModeSlideBar(
    selectedMode: NetworkMode,
    onModeSelected: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = remember { NetworkMode.entries }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    var widthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val segmentWidthPx = widthPx / modes.size
    val targetOffsetPx = selectedIndex * segmentWidthPx

    val animatedOffsetPx by animateFloatAsState(
        targetValue = targetOffsetPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 300f,
        ),
        label = "slidingPillOffset",
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        label = "thumbScale",
    )

    val density = LocalDensity.current

    fun selectFromX(x: Float) {
        val index = (x / segmentWidthPx).toInt().coerceIn(0, modes.size - 1)
        onModeSelected(modes[index])
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .onGloballyPositioned { widthPx = it.size.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isDragging = true
                            selectFromX(it.x)
                            tryAwaitRelease()
                            isDragging = false
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    ) { change, _ ->
                        selectFromX(change.position.x)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Spring-animated sliding pill thumb
            if (widthPx > 1f) {
                val pillWidthDp = with(density) { (widthPx / modes.size).toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = animatedOffsetPx.roundToInt(), y = 0) }
                        .width(pillWidthDp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            // Segment Icons & Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                modes.forEachIndexed { index, mode ->
                    val isSelected = index == selectedIndex
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                NetworkMode.NR_ONLY -> Icons.Default.Speed
                                NetworkMode.NR_LTE -> Icons.Default.CellTower
                                NetworkMode.LTE_ONLY -> Icons.Default.SignalCellular4Bar
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (mode) {
                                NetworkMode.NR_ONLY -> "5G SA"
                                NetworkMode.NR_LTE -> "5G NSA"
                                NetworkMode.LTE_ONLY -> "4G LTE"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
