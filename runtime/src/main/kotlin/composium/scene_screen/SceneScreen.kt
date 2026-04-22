package oleginvoke.com.composium.scene_screen

import android.util.DisplayMetrics
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import oleginvoke.com.composium.LocalScenePreviewContainer
import oleginvoke.com.composium.ScenePreviewDecorator
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumPreviewCanvas
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumThemeToggle
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens

private val SceneInspectorTabsHeight = 34.dp
private val SceneInspectorTabsTopGap = 2.dp
private val SceneInspectorGrabberOverlayOffset = (-24).dp

@Composable
internal fun SceneScreen(
    sceneEntry: SceneEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val themeController = LocalComposiumThemeController.current
    val sceneScope = remember(sceneEntry.id) { SceneScope() }
    val store = rememberSceneScreenStore(sceneEntry.id)
    val state = store.state
    val paramsCallbacks: SceneParamsCallbacks = sceneScope.paramsCallbacks

    val uiState = SceneScreenUiState(
        sceneEntry = sceneEntry,
        sceneScope = sceneScope,
        paramsState = sceneScope.paramsState,
        controlsSheet = state.controlsSheet,
        isDarkTheme = themeController.isDarkTheme,
    )

    val callbacks = remember(store, onBack, themeController.onThemeChange) {
        object : SceneScreenCallbacks {
            override fun onBack() {
                onBack.invoke()
            }

            override fun onToggleControls() {
                val intent = when (store.state.controlsSheet.layoutMode) {
                    SceneInspectorLayoutMode.Closed -> SceneScreenIntent.ShowControls
                    SceneInspectorLayoutMode.Split -> SceneScreenIntent.HideControls
                    SceneInspectorLayoutMode.Expanded -> null
                }
                intent?.let(store::dispatch)
            }

            override fun onDismissControls() {
                store.dispatch(SceneScreenIntent.HideControls)
            }

            override fun onExpandControls() {
                store.dispatch(SceneScreenIntent.ExpandControls)
            }

            override fun onRestoreSplitControls() {
                store.dispatch(SceneScreenIntent.RestoreSplitControls)
            }

            override fun onNavigateBackFromExpandedControls() {
                store.dispatch(SceneScreenIntent.NavigateBackFromExpandedControls)
            }

            override fun onUpdateSplitFraction(fraction: Float) {
                store.dispatch(SceneScreenIntent.UpdateSplitFraction(fraction))
            }

            override fun onSettleSplitFraction() {
                store.dispatch(SceneScreenIntent.SettleSplitFraction)
            }

            override fun onPreviewPaneTapped() {
                store.dispatch(SceneScreenIntent.PreviewPaneTapped)
            }

            override fun onTabSelected(tab: SceneInspectorTab) {
                store.dispatch(SceneScreenIntent.SelectTab(tab))
            }

            override fun onThemeChange(isDarkTheme: Boolean) {
                themeController.onThemeChange.invoke(isDarkTheme)
            }
        }
    }

    SceneScreenBackHandler(
        enabled = state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Expanded,
        onBack = callbacks::onNavigateBackFromExpandedControls,
    )

    ComposiumPreviewCanvas(
        modifier = modifier
            .fillMaxSize(),
    ) {
        CompositionLocalProvider(
            LocalScenePreviewContainer provides ScenePreviewDecorator { scenePreview ->
                scenePreview()
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SceneScreenContent(
                    state = uiState,
                    callbacks = callbacks,
                    paramsCallbacks = paramsCallbacks,
                    contentWindowInsets = contentWindowInsets,
                    modifier = Modifier
                        .fillMaxSize(),
                )

                SceneScreenTopBar(
                    state = uiState,
                    callbacks = callbacks,
                    statusBarInsets = contentWindowInsets,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
    }
}

@Composable
private fun SceneScreenTopBar(
    state: SceneScreenUiState,
    callbacks: SceneScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
    modifier: Modifier = Modifier,
) {
    val controlsLayout = state.controlsSheet.layoutMode

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (statusBarInsets != null) Modifier.windowInsetsPadding(statusBarInsets)
                else Modifier,
            )
            .padding(horizontal = 12.dp)
            .padding(top = 10.dp, bottom = 12.dp),
    ) {
        AnimatedContent(
            targetState = controlsLayout == SceneInspectorLayoutMode.Expanded,
            transitionSpec = {
                fadeIn(Motion.tweenStandard()) togetherWith fadeOut(Motion.tweenFast())
            },
            label = "scene_top_bar_mode",
        ) { isExpandedInspector ->
            if (isExpandedInspector) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SceneTopBarActionButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to split layout",
                        onClick = callbacks::onNavigateBackFromExpandedControls,
                    )
                    SceneExpandedControlsTitleIsland(
                        state = state,
                        modifier = Modifier.weight(1f),
                    )
                    SceneTopBarActionButton(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close settings",
                        onClick = callbacks::onDismissControls,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SceneTopBarActionButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = callbacks::onBack,
                    )
                    SceneSceneTitleIsland(
                        sceneEntry = state.sceneEntry,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SceneTopBarActionButton(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = if (controlsLayout == SceneInspectorLayoutMode.Split) {
                                "Close properties"
                            } else {
                                "Open properties"
                            },
                            onClick = callbacks::onToggleControls,
                            active = controlsLayout == SceneInspectorLayoutMode.Split,
                        )
                        ComposiumThemeToggle(
                            isDark = state.isDarkTheme,
                            onToggle = callbacks::onThemeChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneSceneTitleIsland(
    sceneEntry: SceneEntry,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(Tokens.shapes.extraLarge)
            .background(Tokens.colors.surface.copy(alpha = 0.9f))
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.8f), Tokens.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Column {
            val group = sceneEntry.scene.group
            if (!group.isNullOrBlank()) {
                ComposiumText(
                    text = group,
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
            }
            ComposiumText(
                text = sceneEntry.scene.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Tokens.typography.titleLarge,
                color = Tokens.colors.onSurface,
            )
        }
    }
}

@Composable
private fun SceneExpandedControlsTitleIsland(
    state: SceneScreenUiState,
    modifier: Modifier = Modifier,
) {
    val sceneLabel = buildString {
        val group = state.sceneEntry.scene.group
        if (!group.isNullOrBlank()) {
            append(group)
            append("/")
        }
        append(state.sceneEntry.scene.name)
    }
    val tabLabel = when (state.controlsSheet.selectedTab) {
        SceneInspectorTab.Properties -> "Properties"
        SceneInspectorTab.Environment -> "Environment"
    }

    Box(
        modifier = modifier
            .clip(Tokens.shapes.extraLarge)
            .background(Tokens.colors.surface.copy(alpha = 0.9f))
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.8f), Tokens.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Column {
            ComposiumText(
                text = sceneLabel,
                style = Tokens.typography.labelSmall,
                color = Tokens.colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            ComposiumText(
                text = tabLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Tokens.typography.titleLarge,
                color = Tokens.colors.onSurface,
            )
        }
    }
}

@Composable
private fun SceneScreenContent(
    state: SceneScreenUiState,
    callbacks: SceneScreenCallbacks,
    paramsCallbacks: SceneParamsCallbacks,
    contentWindowInsets: WindowInsets? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var isInspectorDragging by remember(state.controlsSheet.layoutMode) { mutableStateOf(false) }
    var hasPreviewOverflow by remember(state.sceneEntry.id) { mutableStateOf(false) }
    val topInset = contentWindowInsets?.getTop(density)?.let { inset ->
        with(density) { inset.toDp() }
    } ?: 0.dp
    val bottomInset = contentWindowInsets?.getBottom(density)?.let { inset ->
        with(density) { inset.toDp() }
    } ?: 0.dp
    val contentTopPadding = topInset + 88.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(top = contentTopPadding),
    ) {
        val availableHeightPx = constraints.maxHeight
        val layoutMetrics = calculateSceneLayoutMetrics(
            availableHeightPx = availableHeightPx,
            mode = state.controlsSheet.layoutMode,
            splitFraction = state.controlsSheet.splitFraction,
        )
        val sizeAnimationSpec = if (isInspectorDragging) snap() else Motion.tweenStandard<Dp>()
        val previewComposedHeight by animateDpAsState(
            targetValue = with(density) { layoutMetrics.previewComposedHeightPx.toDp() },
            animationSpec = sizeAnimationSpec,
            label = "scene_preview_composed_height",
        )
        val inspectorHeight by animateDpAsState(
            targetValue = with(density) { layoutMetrics.inspectorHeightPx.toDp() },
            animationSpec = sizeAnimationSpec,
            label = "scene_inspector_height",
        )
        val previewAlpha by animateFloatAsState(
            targetValue = if (layoutMetrics.previewVisibleHeightPx > 0) 1f else 0f,
            animationSpec = if (isInspectorDragging) snap() else Motion.tweenStandard(),
            label = "scene_preview_alpha",
        )
        val showSplitDivider = shouldShowSceneSplitDivider(
            mode = state.controlsSheet.layoutMode,
            hasScrollableOverflow = hasPreviewOverflow,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            ScenePreviewPane(
                sceneEntry = state.sceneEntry,
                sceneScope = state.sceneScope,
                minimumCanvasHeight = previewComposedHeight,
                layoutMode = state.controlsSheet.layoutMode,
                onOverflowChanged = { hasOverflow ->
                    hasPreviewOverflow = hasOverflow
                },
                onBackgroundTap = if (state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Split) {
                    callbacks::onPreviewPaneTapped
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(previewComposedHeight)
                    .graphicsLayer { alpha = previewAlpha },
            )

            if (inspectorHeight > 0.5.dp) {
                SceneInspectorPane(
                    paramsState = state.paramsState,
                    scenePreview = state.sceneScope.preview,
                    isDarkThemeEnabled = state.isDarkTheme,
                    paramsCallbacks = paramsCallbacks,
                    selectedTab = state.controlsSheet.selectedTab,
                    showSplitDivider = showSplitDivider,
                    onBackgroundTap = if (state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Split) {
                        callbacks::onExpandControls
                    } else {
                        null
                    },
                    availableHeightPx = availableHeightPx,
                    splitFraction = state.controlsSheet.splitFraction,
                    onSplitFractionChanged = callbacks::onUpdateSplitFraction,
                    onSplitFractionDragStart = { isInspectorDragging = true },
                    onSplitFractionDragStop = {
                        isInspectorDragging = false
                        callbacks.onSettleSplitFraction()
                    },
                    onTabSelected = callbacks::onTabSelected,
                    onThemeChange = callbacks::onThemeChange,
                    bottomInset = bottomInset,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(inspectorHeight),
                )
            }
        }
    }
}

@Composable
private fun SceneTopBarActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            Tokens.colors.primaryContainer.copy(alpha = 0.92f)
        } else {
            Tokens.colors.surface.copy(alpha = 0.9f)
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) {
            Tokens.colors.primary.copy(alpha = 0.45f)
        } else {
            Tokens.colors.outlineVariant.copy(alpha = 0.8f)
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_border",
    )
    val tint by animateColorAsState(
        targetValue = if (active) Tokens.colors.primary else Tokens.colors.onSurface,
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_tint",
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(Tokens.shapes.pill)
            .background(containerColor)
            .border(1.dp, borderColor, Tokens.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumIconButton(onClick = onClick) {
            ComposiumIcon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ScenePreviewPane(
    sceneEntry: SceneEntry,
    sceneScope: SceneScope,
    minimumCanvasHeight: androidx.compose.ui.unit.Dp,
    layoutMode: SceneInspectorLayoutMode,
    onOverflowChanged: (Boolean) -> Unit,
    onBackgroundTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scrollState = rememberScrollState()
    val showOverflowDivider = shouldShowSceneSplitDivider(
        mode = layoutMode,
        hasScrollableOverflow = scrollState.maxValue > 0,
    )

    SideEffect {
        onOverflowChanged(showOverflowDivider)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onBackgroundTap != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBackgroundTap,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                ScenePreviewContent(
                    sceneEntry = sceneEntry,
                    sceneScope = sceneScope,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minimumCanvasHeight),
                )
            }
        }
    }
}

@Composable
private fun SceneInspectorPane(
    paramsState: SceneParamsState,
    scenePreview: oleginvoke.com.composium.SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    paramsCallbacks: SceneParamsCallbacks,
    selectedTab: SceneInspectorTab,
    showSplitDivider: Boolean,
    onBackgroundTap: (() -> Unit)?,
    availableHeightPx: Int,
    splitFraction: Float,
    onSplitFractionChanged: (Float) -> Unit,
    onSplitFractionDragStart: () -> Unit,
    onSplitFractionDragStop: () -> Unit,
    onTabSelected: (SceneInspectorTab) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentPadding = remember(bottomInset) {
        androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = bottomInset + 28.dp,
        )
    }
    val isSplitMode = onBackgroundTap != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onBackgroundTap != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBackgroundTap,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        if (isSplitMode) {
            SceneSplitDivider(
                visible = showSplitDivider,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = SceneInspectorTabsTopGap),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        SceneInspectorTabsHeight +
                            if (isSplitMode) SceneInspectorTabsTopGap else 8.dp,
                    ),
            ) {
                SceneInspectorTabs(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(Motion.tweenStandard()) togetherWith fadeOut(Motion.tweenFast())
                    },
                    label = "scene_inspector_tab_content",
                ) { tab ->
                    when (tab) {
                        SceneInspectorTab.Properties -> {
                            if (paramsState.params.isEmpty()) {
                                SceneInspectorEmptyState(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .inspectorEdgeFade(
                                            topFade = 18.dp,
                                            bottomFade = bottomInset + 36.dp,
                                        ),
                                )
                            } else {
                                ControlsPanel(
                                    state = paramsState,
                                    callbacks = paramsCallbacks,
                                    contentPadding = contentPadding,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .inspectorEdgeFade(
                                            topFade = 18.dp,
                                            bottomFade = bottomInset + 36.dp,
                                        ),
                                )
                            }
                        }

                        SceneInspectorTab.Environment -> {
                            EnvironmentPanel(
                                preview = scenePreview,
                                isDarkThemeEnabled = isDarkThemeEnabled,
                                onThemeChange = onThemeChange,
                                contentPadding = contentPadding,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .inspectorEdgeFade(
                                        topFade = 18.dp,
                                        bottomFade = bottomInset + 36.dp,
                                    ),
                            )
                        }
                    }
                }
                SceneInspectorBottomGradient(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    bottomInset = bottomInset,
                )
            }
        }

        if (isSplitMode) {
            SceneInspectorGrabber(
                onClick = onBackgroundTap,
                availableHeightPx = availableHeightPx,
                splitFraction = splitFraction,
                onSplitFractionChanged = onSplitFractionChanged,
                onDragStart = onSplitFractionDragStart,
                onDragStop = onSplitFractionDragStop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = SceneInspectorGrabberOverlayOffset),
            )
        }
    }
}

@Composable
private fun SceneInspectorTabs(
    selectedTab: SceneInspectorTab,
    onTabSelected: (SceneInspectorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
    ) {
        val inset = 3.dp
        val tabWidth = maxWidth / 2
        val indicatorWidth = tabWidth - (inset * 2)
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedTab == SceneInspectorTab.Properties) {
                inset
            } else {
                tabWidth + inset
            },
            animationSpec = Motion.tweenStandard(),
            label = "scene_inspector_tab_indicator_offset",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SceneInspectorTabsHeight)
                .clip(Tokens.shapes.pill)
                .background(Tokens.colors.surface.copy(alpha = 0.68f))
                .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.72f), Tokens.shapes.pill),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(28.dp)
                    .align(Alignment.CenterStart)
                    .clip(Tokens.shapes.pill)
                    .background(Tokens.colors.primaryContainer.copy(alpha = 0.82f))
                    .border(
                        width = 1.dp,
                        color = Tokens.colors.primary.copy(alpha = 0.32f),
                        shape = Tokens.shapes.pill,
                    ),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                SceneInspectorTabButton(
                    modifier = Modifier.weight(1f),
                    text = "Properties",
                    selected = selectedTab == SceneInspectorTab.Properties,
                    onClick = { onTabSelected(SceneInspectorTab.Properties) },
                )
                SceneInspectorTabButton(
                    modifier = Modifier.weight(1f),
                    text = "Environment",
                    selected = selectedTab == SceneInspectorTab.Environment,
                    onClick = { onTabSelected(SceneInspectorTab.Environment) },
                )
            }
        }
    }
}

@Composable
private fun SceneInspectorTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) Tokens.colors.onPrimaryContainer else Tokens.colors.onSurfaceVariant,
        animationSpec = Motion.tweenStandard(),
        label = "scene_inspector_tab_text",
    )
    Box(
        modifier = modifier
            .height(SceneInspectorTabsHeight)
            .clip(Tokens.shapes.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumText(
            text = text,
            style = Tokens.typography.titleSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun SceneInspectorGrabber(
    onClick: () -> Unit,
    availableHeightPx: Int,
    splitFraction: Float,
    onSplitFractionChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var liveSplitFraction by remember { mutableStateOf(splitFraction) }

    SideEffect {
        if (!isDragging) {
            liveSplitFraction = splitFraction
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (availableHeightPx > 0) {
            liveSplitFraction -= (delta / availableHeightPx.toFloat())
            onSplitFractionChanged(liveSplitFraction)
        }
    }
    Box(
        modifier = modifier
            .width(68.dp)
            .height(22.dp)
            .clip(Tokens.shapes.pill)
            .background(Tokens.colors.surface.copy(alpha = 0.72f))
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.66f), Tokens.shapes.pill)
            .draggable(
                orientation = Orientation.Vertical,
                state = draggableState,
                onDragStarted = {
                    isDragging = true
                    liveSplitFraction = splitFraction
                    onDragStart()
                },
                onDragStopped = {
                    isDragging = false
                    onDragStop()
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .clip(Tokens.shapes.pill)
                    .background(Tokens.colors.onSurfaceVariant.copy(alpha = 0.68f)),
            )
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .clip(Tokens.shapes.pill)
                    .background(Tokens.colors.primary.copy(alpha = 0.78f)),
            )
        }
    }
}

@Composable
private fun SceneInspectorBottomGradient(
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bottomInset + 84.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Tokens.colors.background.copy(alpha = 0.44f),
                        Tokens.colors.background.copy(alpha = 0.88f),
                    ),
                ),
            ),
    )
}

@Composable
private fun SceneSplitDivider(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.tweenFast(),
        label = "scene_split_divider_alpha",
    )
    val shadowHeight by animateDpAsState(
        targetValue = if (visible) 3.dp else 1.dp,
        animationSpec = Motion.tweenFast(),
        label = "scene_split_divider_shadow_height",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp + shadowHeight)
            .graphicsLayer { this.alpha = alpha },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(Tokens.colors.outlineVariant.copy(alpha = 0.56f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(shadowHeight)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Tokens.colors.scrim.copy(alpha = 0.06f),
                            Tokens.colors.scrim.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SceneInspectorEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Tokens.shapes.large)
                .background(Tokens.colors.surface.copy(alpha = 0.92f))
                .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.78f), Tokens.shapes.large)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            ComposiumText(
                text = "No editable properties",
                style = Tokens.typography.titleMedium,
                color = Tokens.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScenePreviewContent(
    sceneEntry: SceneEntry,
    sceneScope: SceneScope,
    modifier: Modifier = Modifier,
) {
    val settings = sceneScope.preview
    val baseDensity = LocalDensity.current

    val stableDensity = DisplayMetrics.DENSITY_DEVICE_STABLE / 160f
    val resolvedDensityValue = settings.displayScaleOverride?.let { stableDensity * it } ?: baseDensity.density
    val resolvedFontScale = settings.fontScaleOverride ?: baseDensity.fontScale
    val layoutDirection = if (settings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides Density(density = resolvedDensityValue, fontScale = resolvedFontScale),
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            LocalScenePreviewContainer.current.Decoration {
                sceneEntry.scene.content(sceneScope)
            }
        }
    }
}

private fun Modifier.inspectorEdgeFade(
    topFade: Dp,
    bottomFade: Dp,
): Modifier = composed {
    val density = LocalDensity.current
    val topFadePx = with(density) { topFade.toPx() }
    val bottomFadePx = with(density) { bottomFade.toPx() }

    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            if (size.height <= 0f) return@drawWithContent

            val topStop = (topFadePx / size.height).coerceIn(0f, 1f)
            val bottomStop = (1f - (bottomFadePx / size.height)).coerceIn(0f, 1f)

            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to if (topFadePx > 0f) Color.Transparent else Color.Black,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        1f to if (bottomFadePx > 0f) Color.Transparent else Color.Black,
                    ),
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}

@Composable
private fun SceneScreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBack = rememberUpdatedState(onBack)
    val callback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack.value.invoke()
            }
        }
    }
    val dispatcher = remember(context) {
        context.findSceneScreenOnBackPressedDispatcherOwner()?.onBackPressedDispatcher
    }

    SideEffect {
        callback.isEnabled = enabled
    }

    DisposableEffect(dispatcher, lifecycleOwner, callback) {
        if (dispatcher == null) {
            onDispose {}
        } else {
            dispatcher.addCallback(lifecycleOwner, callback)
            onDispose {
                callback.remove()
            }
        }
    }
}

private fun Context.findSceneScreenOnBackPressedDispatcherOwner(): OnBackPressedDispatcherOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is OnBackPressedDispatcherOwner) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
