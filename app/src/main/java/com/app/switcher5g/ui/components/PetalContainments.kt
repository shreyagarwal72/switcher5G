package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Petal containment design system for Material 3 Expressive surfaces.
 * Standardizes borderless surfaces, grouped-corners, and expressive spacing.
 */
object PetalContainmentDefaults {
    val shape = RoundedCornerShape(24.dp)
    val groupedMiddleShape = RoundedCornerShape(8.dp)
    val contentPaddingHorizontal = 18.dp
    val contentPaddingVertical = 18.dp
    val contentSpacing = 14.dp
    val sectionSpacing = 16.dp
    val groupSpacing = 6.dp
    val standardColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val elevatedColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer
}

/**
 * Position-aware shape calculation helper for items inside vertical groups/lists.
 */
fun getGroupItemShape(
    index: Int,
    count: Int,
    topCorner: Dp = 24.dp,
    bottomCorner: Dp = 24.dp,
    middleCorner: Dp = 8.dp,
    singleCorner: Dp = 24.dp,
): RoundedCornerShape {
    return when {
        count <= 1 -> RoundedCornerShape(singleCorner)
        index == 0 -> RoundedCornerShape(
            topStart = topCorner,
            topEnd = topCorner,
            bottomStart = middleCorner,
            bottomEnd = middleCorner,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = middleCorner,
            topEnd = middleCorner,
            bottomStart = bottomCorner,
            bottomEnd = bottomCorner,
        )
        else -> RoundedCornerShape(middleCorner)
    }
}

/**
 * Low-level borderless containment surface.
 */
@Composable
fun PetalContainmentSurface(
    modifier: Modifier = Modifier,
    shape: Shape = PetalContainmentDefaults.shape,
    containerColor: Color = PetalContainmentDefaults.elevatedColor,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            enabled = enabled,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/**
 * Standard padded borderless containment.
 */
@Composable
fun PetalContainment(
    modifier: Modifier = Modifier,
    shape: Shape = PetalContainmentDefaults.shape,
    containerColor: Color = PetalContainmentDefaults.elevatedColor,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    PetalContainmentSurface(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = PetalContainmentDefaults.contentPaddingHorizontal,
                    vertical = PetalContainmentDefaults.contentPaddingVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(PetalContainmentDefaults.contentSpacing),
            content = content,
        )
    }
}

/**
 * Expressive Section Container with header label and optional icon.
 */
@Composable
fun PetalSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PetalContainmentDefaults.groupSpacing),
        ) {
            content()
        }
    }
}
