package com.app.switcher5g.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.switcher5g.ui.theme.ExpressiveShapeHolder
import com.app.switcher5g.ui.theme.PetalMaterialShapes

/*
 * Petal grouped containment, ported from Petal Browser's `PetalContainment.kt`:
 * rows are stacked with a 3dp gap; the first row has large top corners, the last has large
 * bottom corners and everything in between has small 6dp corners, so a group reads as one
 * connected container that is still individually tappable. Icons sit in Material 3 Expressive
 * shaped badges.
 */

enum class PetalGroupPosition { SINGLE, TOP, MIDDLE, BOTTOM }

fun petalGroupShape(position: PetalGroupPosition): RoundedCornerShape = when (position) {
    PetalGroupPosition.SINGLE -> RoundedCornerShape(28.dp)
    PetalGroupPosition.TOP -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    PetalGroupPosition.MIDDLE -> RoundedCornerShape(6.dp)
    PetalGroupPosition.BOTTOM -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
}

fun petalGroupPositionFor(index: Int, count: Int): PetalGroupPosition = when {
    count <= 1 -> PetalGroupPosition.SINGLE
    index == 0 -> PetalGroupPosition.TOP
    index == count - 1 -> PetalGroupPosition.BOTTOM
    else -> PetalGroupPosition.MIDDLE
}

/** Surface colour for group rows: white-ish on light backgrounds, raised tone on dark ones. */
@Composable
fun petalGroupColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() > 0.5f) scheme.surfaceContainerLow else scheme.surfaceContainerHigh
}

@Composable
private fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "petalGroupPressScale",
    )
    return scale
}

/** Collects the rows of a [PetalGroup]; each row receives the corner position it must render. */
class PetalGroupScope {
    internal val rows = mutableListOf<@Composable (PetalGroupPosition) -> Unit>()

    fun row(content: @Composable (PetalGroupPosition) -> Unit) {
        rows += content
    }
}

/** A vertical group of connected rows. Add rows with [PetalGroupScope.row]. */
@Composable
fun PetalGroup(
    modifier: Modifier = Modifier,
    content: PetalGroupScope.() -> Unit,
) {
    val scope = PetalGroupScope().apply(content)
    val count = scope.rows.size
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        scope.rows.forEachIndexed { index, row -> row(petalGroupPositionFor(index, count)) }
    }
}

/** Section title above a [PetalGroup] (optionally with a short description). */
@Composable
fun PetalSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Section = label + group, the standard way every screen lays out its containment. */
@Composable
fun PetalSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: PetalGroupScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PetalSectionLabel(text = title, description = description)
        PetalGroup(content = content)
    }
}

/** Colour pairs for icon badges. */
enum class PetalBadgeVariant { PRIMARY, SECONDARY, TERTIARY, SURFACE_TONAL, ERROR }

@Composable
fun PetalBadgeVariant.colors(): Pair<Color, Color> = when (this) {
    PetalBadgeVariant.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    PetalBadgeVariant.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    PetalBadgeVariant.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    PetalBadgeVariant.SURFACE_TONAL -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    PetalBadgeVariant.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

/** Icon inside a Material 3 Expressive shaped container (cookie, sunny, clover, ...). */
@Composable
fun PetalShapedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    shape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie6Sided,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    val (container, tint) = variant.colors()
    Box(
        modifier = modifier
            .size(size)
            .clip(shape.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Shaped container for arbitrary content (for example a loading indicator in place of an icon). */
@Composable
fun PetalShapedContainer(
    modifier: Modifier = Modifier,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** The raw rounded surface every group row is built on. Clickable rows squash on press. */
@Composable
fun PetalGroupItem(
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    color: Color = petalGroupColor(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = petalGroupShape(position)
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    } else {
        val source = remember { MutableInteractionSource() }
        val scale = rememberPressScale(source)
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = shape,
            color = color,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            interactionSource = source,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/** Icon badge + title/subtitle + optional trailing content (chevron by default when clickable). */
@Composable
fun PetalGroupRow(
    icon: ImageVector,
    title: String,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie6Sided,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
    trailing: (@Composable () -> Unit)? = null,
) {
    PetalGroupItem(
        position = position,
        modifier = modifier.alpha(if (enabled) 1f else 0.38f),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetalShapedIcon(icon = icon, shape = badgeShape, variant = variant)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A row whose whole surface toggles a switch. */
@Composable
fun PetalGroupToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie6Sided,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
) {
    PetalGroupRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        position = position,
        modifier = modifier,
        enabled = enabled,
        badgeShape = badgeShape,
        variant = variant,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        },
    )
}

/**
 * A row with the badge + title header on top and an arbitrary control below it
 * (toggle button groups, sliders, buttons, status text).
 */
@Composable
fun PetalGroupControlRow(
    icon: ImageVector,
    title: String,
    position: PetalGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie6Sided,
    variant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
    headerTrailing: (@Composable () -> Unit)? = null,
    control: @Composable ColumnScope.() -> Unit,
) {
    PetalGroupItem(position = position, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PetalShapedIcon(icon = icon, shape = badgeShape, variant = variant, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (headerTrailing != null) {
                    Spacer(Modifier.width(8.dp))
                    headerTrailing()
                }
            }
            control()
        }
    }
}
