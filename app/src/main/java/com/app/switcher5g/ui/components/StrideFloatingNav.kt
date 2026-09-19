@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
)

val switcherDestinations = listOf(
    NavDestination(route = "home", label = "Switcher", icon = Icons.Rounded.CellTower),
    NavDestination(route = "settings", label = "Settings", icon = Icons.Rounded.Settings),
    NavDestination(route = "about", label = "About", icon = Icons.Rounded.Info),
)

/**
 * Material 3 Expressive Floating Bottom Navigation Bar ported from Petal Browser.
 * Features:
 * - HorizontalFloatingToolbar with surfaceContainer styling
 * - Expressive pill expansion spring physics with low-bouncy overshoot
 * - Tactile press scaling (bouncy touch feedback)
 * - Borderless containment styling
 */
@Composable
fun StrideFloatingNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toolbarColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
        toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        toolbarContentColor = MaterialTheme.colorScheme.onSurface,
    )

    HorizontalFloatingToolbar(
        expanded = true,
        colors = toolbarColors,
        modifier = modifier
            .wrapContentWidth()
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            )
            .clip(CircleShape),
    ) {
        switcherDestinations.forEachIndexed { index, dest ->
            val isSelected = currentRoute == dest.route
            FloatingNavTabItem(
                selected = isSelected,
                label = dest.label,
                index = index,
                icon = { selected, tint ->
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                        label = "icon_scale_$index",
                    )
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = tint,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
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

    // Bouncy touch feedback press scale from Petal
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
