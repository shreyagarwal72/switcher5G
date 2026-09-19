@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.app.switcher5g.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    /** Extra icon rotation (degrees) applied with a spring while the tab is selected. */
    val selectedRotation: Float = 0f,
)

val switcherDestinations = listOf(
    NavDestination(route = "home", label = "Switcher", icon = Icons.Rounded.CellTower),
    NavDestination(route = "settings", label = "Settings", icon = Icons.Rounded.Settings, selectedRotation = 90f),
    NavDestination(route = "about", label = "About", icon = Icons.Rounded.Info),
)

/**
 * Floating bottom navigation bar, ported 1:1 from Petal Browser's `PetalBottomNavBar`
 * (floating style):
 *
 * - Material 3 Expressive [HorizontalFloatingToolbar] on a `surfaceContainer` vibrant container
 * - soft primary-tinted shadow plus Petal's faint gradient outline
 * - selected item grows into a `primaryContainer` pill and reveals its label with a low-bouncy spring
 * - bouncy press-scale feedback, icon scale pop (and rotation where set) on selection
 *
 * The bar handles the navigation-bar inset and bottom margin itself, exactly like Petal, so
 * callers only need to align it to the bottom of their container.
 */
@Composable
fun StrideFloatingNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Material 3 Expressive Floating Toolbar with styled surfaceContainer for proper theme presentation
        val toolbarColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .wrapContentWidth()
                .height(64.dp)
                .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                .border(
                    0.75.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        ),
                    ),
                    CircleShape,
                )
                .clip(CircleShape),
            colors = toolbarColors,
        ) {
            switcherDestinations.forEachIndexed { index, dest ->
                FloatingNavTabItem(
                    selected = currentRoute == dest.route,
                    label = dest.label,
                    index = index,
                    icon = { isSelected, tint ->
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isSelected) dest.selectedRotation else 0f,
                            animationSpec = spring(dampingRatio = 0.68f, stiffness = 450f),
                            label = "nav_rotation_$index",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                            label = "nav_scale_$index",
                        )
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = tint,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = rotationAngle
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                    },
                    onClick = { onNavigate(dest.route) },
                )
            }
        }
    }
}

@Composable
private fun FloatingNavTabItem(
    selected: Boolean,
    label: String,
    index: Int,
    icon: @Composable (isSelected: Boolean, tint: Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy touch feedback press scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press_scale_$index",
    )

    val labelWidth by animateDpAsState(
        targetValue = if (selected) 72.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 360f,
        ),
        label = "nav_label_$index",
    )

    val activeContainerColor = MaterialTheme.colorScheme.primaryContainer
    val activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentContentColor by animateColorAsState(
        targetValue = if (selected) activeContentColor else inactiveContentColor,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "nav_color_$index",
    )

    val currentBgColor by animateColorAsState(
        targetValue = if (selected) activeContainerColor else Color.Transparent,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "nav_bg_$index",
    )

    Surface(
        shape = CircleShape,
        color = currentBgColor,
        modifier = modifier
            .height(48.dp)
            .width(48.dp + labelWidth)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = if (selected) 8.dp else 0.dp)
                .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon(selected, currentContentColor)
            }

            if (labelWidth > 4.dp) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    ),
                    color = currentContentColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
