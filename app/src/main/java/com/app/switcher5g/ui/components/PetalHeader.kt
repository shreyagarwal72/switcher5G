package com.app.switcher5g.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Petal's collapsing Medium top app bar (ported from `PetalCollapsingTopAppBar`).
 *
 * When [title] changes the outgoing title fades + scales out while the incoming one fades +
 * scales in (Zenith's `HeaderTitleAnimation`). As the bar collapses the title nudges inwards and
 * the back button morphs from a tonal to a fully filled icon button.
 *
 * [onNavigateBack] == null hides the back button (used by the root Home screen).
 * Use together with [TopAppBarDefaults.exitUntilCollapsedScrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetalCollapsingTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
        title = {
            val titleStartPadding = animateDpAsState(
                targetValue = if (scrollBehavior.state.collapsedFraction > 0.5f) 6.dp else 0.dp,
                animationSpec = tween(250),
                label = "petalHeaderTitleStartPadding",
            )
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )).togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                            scaleOut(
                                targetScale = 0.92f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                            ),
                    )
                },
                label = "petalHeaderTitleAnimation",
                modifier = Modifier.padding(start = titleStartPadding.value),
            ) { currentTitle ->
                Text(
                    text = currentTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                Crossfade(
                    targetState = scrollBehavior.state.collapsedFraction > 0.5f,
                    animationSpec = tween(500),
                    label = "petalHeaderBackButtonMorph",
                ) { scrolled ->
                    if (scrolled) {
                        FilledIconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        FilledTonalIconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

/** Creates the exit-until-collapsed behaviour every Petal screen header uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberPetalHeaderScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

/**
 * Standard screen frame: Petal header on top, scrolling containment column below.
 *
 * The column leaves room for the floating bottom navigation ([bottomInset]) and lets the header
 * collapse as the content scrolls. [scrollState] is exposed so callers (pull-to-refresh) can
 * tell when the content is at the very top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetalScreenScaffold(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PetalCollapsingTopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onNavigateBack = onNavigateBack,
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                // Clear the floating bottom nav bar.
                .padding(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}
