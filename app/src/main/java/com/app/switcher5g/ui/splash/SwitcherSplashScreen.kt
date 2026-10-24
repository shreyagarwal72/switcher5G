package com.app.switcher5g.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.switcher5g.ui.theme.PetalMaterialShapes
import com.app.switcher5g.ui.theme.toShape
import kotlinx.coroutines.delay
import kotlin.random.Random

object SwitcherSplashState {
    /**
     * In-memory process flag ensuring the splash screen plays once per app launch session.
     */
    @Volatile
    var hasShownThisSession: Boolean = false
}

private data class CellularGlyph(
    val iconIndex: Int,
    val xFraction: Float,
    val startFraction: Float,
    val speed: Float,
    val sizeDp: Int,
    val alpha: Float,
)

// Curated cellular, signal, connectivity & system glyphs
private val cellularGlyphIcons = listOf(
    Icons.Rounded.CellTower,
    Icons.Rounded.SignalCellularAlt,
    Icons.Rounded.Bolt,
    Icons.Rounded.SettingsCell,
    Icons.Rounded.NetworkCheck,
    Icons.Rounded.SimCard,
    Icons.Rounded.AutoAwesome,
    Icons.Rounded.Security,
    Icons.Rounded.SyncAlt,
    Icons.Rounded.Speed,
    Icons.Rounded.Sensors,
    Icons.Rounded.VpnLock,
    Icons.Rounded.Memory,
    Icons.Rounded.Tune,
)

@Composable
fun SwitcherSplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long = 1800L,
) {
    LaunchedEffect(Unit) {
        delay(durationMs)
        SwitcherSplashState.hasShownThisSession = true
        onSplashComplete()
    }

    SwitcherSplashContent(modifier = modifier)
}

@Composable
private fun CellularGlyphField(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    glyphCount: Int = 90,
    columns: Int = 12,
) {
    val painters = cellularGlyphIcons.map { rememberVectorPainter(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "cellular_glyphs_loop")
    val progressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glyphs_progress",
    )

    val glyphs = remember(glyphCount, columns) {
        val random = Random(42)
        List(glyphCount) { index ->
            val column = index % columns
            CellularGlyph(
                iconIndex = random.nextInt(cellularGlyphIcons.size),
                xFraction = ((column + 0.5f) / columns.toFloat()) + random.nextFloat() * 0.04f - 0.02f,
                startFraction = random.nextFloat() * 1.35f,
                speed = 0.30f + random.nextFloat() * 0.50f,
                sizeDp = 12 + random.nextInt(12),
                alpha = 0.08f + random.nextFloat() * 0.28f,
            )
        }
    }

    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val progress = progressState.value
        val widthPx = size.width
        val heightPx = size.height
        if (widthPx <= 0f || heightPx <= 0f) return@Canvas

        for (i in glyphs.indices) {
            val glyph = glyphs[i]
            val x = widthPx * glyph.xFraction
            val yProgress = (glyph.startFraction + progress * glyph.speed) % 1.35f
            val y = heightPx * yProgress - heightPx * 0.15f
            val sizePx = with(density) { glyph.sizeDp.dp.toPx() }
            val painter = painters[glyph.iconIndex]

            translate(left = x, top = y) {
                with(painter) {
                    draw(
                        size = Size(sizePx, sizePx),
                        alpha = glyph.alpha,
                        colorFilter = ColorFilter.tint(accentColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitcherSplashContent(
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    val backgroundColor = colorScheme.background
    val isDark = isSystemInDarkTheme() || backgroundColor == Color.Black || (colorScheme.surface.red + colorScheme.surface.green + colorScheme.surface.blue) < 1.5f

    // Subtle scale-up and spring entrance
    val contentScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splash_scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha",
    )

    // Breathing pulse for ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "switcher_ambient_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash_pulse",
    )

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "icon_cookie_rot",
    )

    // Dynamic Material You gradient
    val backgroundTop = if (isDark) backgroundColor else colorScheme.surface
    val backgroundMid = if (isDark) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.08f)
    val backgroundBottom = if (isDark) colorScheme.surfaceContainerLowest else colorScheme.surfaceContainerLow
    val textColor = if (isDark) Color.White else colorScheme.onBackground

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(backgroundTop, backgroundMid, backgroundBottom),
                ),
            ),
    ) {
        // Procedural cellular glyph particle field
        CellularGlyphField(
            modifier = Modifier.fillMaxSize(),
            accentColor = accentColor,
        )

        // Radial ambient backdrop glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.20f * pulseGlow else 0.12f * pulseGlow),
                            Color.Transparent,
                        ),
                        radius = 800f,
                    ),
                ),
        )

        // Center Content: App Icon + Single App Name (No Subtitle)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Rotating Cookie12Sided Expressive Shape containing App Icon
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer { rotationZ = iconRotation }
                    .clip(PetalMaterialShapes.Cookie12Sided.toShape())
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.graphicsLayer { rotationZ = -iconRotation },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CellTower,
                        contentDescription = "Switcher 5G Icon",
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(54.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Glowing Single Brand Title (No Subtitle, exact Petal design)
            Text(
                text = "Switcher 5G",
                color = textColor,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = accentColor.copy(alpha = if (isDark) 0.85f * pulseGlow else 0.40f * pulseGlow),
                        offset = Offset(0f, 4f),
                        blurRadius = 28f,
                    ),
                ),
            )
        }
    }
}
