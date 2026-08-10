package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
 * Floating bottom navigation bar modeled on StrideFloatingNav from Stride.
 */
@Composable
fun StrideFloatingNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 12.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            switcherDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val pop = remember { Animatable(1f) }

                LaunchedEffect(selected) {
                    if (selected) {
                        pop.snapTo(0.65f)
                        pop.animateTo(
                            1f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .bouncyClickable(scaleDown = 0.88f) { onNavigate(dest.route) }
                        .clip(CircleShape)
                        .animateContentSize(),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (selected) 16.dp else 10.dp,
                            vertical = 10.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = pop.value
                                    scaleY = pop.value
                                },
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut(),
                        ) {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
