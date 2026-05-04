package oleginvoke.com.composium.scene_screen

import android.content.Context
import android.content.ContextWrapper
import android.util.DisplayMetrics
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import oleginvoke.com.composium.LocalScenePreviewContainer
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
import kotlin.math.roundToInt

private val SceneInspectorTabsHeight = 34.dp
private val SceneInspectorTabsTopGap = 2.dp

private fun Float.sanitizedInspectorFraction(): Float =
    if (isNaN()) 0f else coerceIn(0f, 1f)

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
        enabled = state.controlsSheet.isVisible,
        onBack = if (state.controlsSheet.layoutMode == SceneInspectorLayoutMode.Expanded) {
            callbacks::onNavigateBackFromExpandedControls
        } else {
            callbacks::onDismissControls
        },
    )

    val targetFraction = when (state.controlsSheet.layoutMode) {
        SceneInspectorLayoutMode.Closed -> 0f
        SceneInspectorLayoutMode.Split -> state.controlsSheet.splitFraction.sanitizedInspectorFraction()
        SceneInspectorLayoutMode.Expanded -> 1f
    }
    val inspectorFraction = remember { Animatable(targetFraction) }
    val inspectorAnimationScope = rememberCoroutineScope()
    var isInspectorComposed by remember { mutableStateOf(targetFraction > 0f) }
    var inspectorAnimationTick by remember { mutableStateOf(0) }
    val liveDragFraction = remember { mutableFloatStateOf(targetFraction) }

    LaunchedEffect(targetFraction, inspectorAnimationTick) {
        if (targetFraction > 0f) isInspectorComposed = true
        inspectorFraction.animateTo(targetFraction, Motion.tweenStandard())
        if (targetFraction == 0f) {
            isInspectorComposed = false
        }
    }

    // Mirror the visible inspector fraction into the live drag value continuously so that a
    // touch on the grabber after any non-drag transition (open/close/expand/restore/settle)
    // starts dragging from the actually-visible position rather than from a stale value.
    // During drag, the synchronous write inside onInspectorDragUpdate keeps liveDragFraction
    // ahead of this mirror; the mirror just re-confirms the same value.
    LaunchedEffect(inspectorFraction, liveDragFraction) {
        snapshotFlow { inspectorFraction.value }.collect { value ->
            liveDragFraction.floatValue = value
        }
    }

    val inspectorFractionProvider: () -> Float = remember(inspectorFraction) {
        { inspectorFraction.value }
    }
    val liveDragFractionProvider: () -> Float = remember(liveDragFraction) {
        { liveDragFraction.floatValue }
    }
    val onInspectorDragUpdate: (Float) -> Unit = remember(
        inspectorFraction,
        inspectorAnimationScope,
        liveDragFraction,
    ) {
        { fraction ->
            val sanitized = fraction.sanitizedInspectorFraction()
            liveDragFraction.floatValue = sanitized
            inspectorAnimationScope.launch {
                inspectorFraction.snapTo(sanitized)
            }
        }
    }

    ComposiumPreviewCanvas(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SceneScreenContent(
                sceneEntry = sceneEntry,
                sceneScope = sceneScope,
                paramsState = sceneScope.paramsState,
                controlsSheet = state.controlsSheet,
                isDarkTheme = themeController.isDarkTheme,
                callbacks = callbacks,
                paramsCallbacks = paramsCallbacks,
                contentWindowInsets = contentWindowInsets,
                inspectorFractionProvider = inspectorFractionProvider,
                isInspectorComposed = isInspectorComposed,
                onInspectorDragUpdate = onInspectorDragUpdate,
                onInspectorDragStop = { finalFraction ->
                    callbacks.onUpdateSplitFraction(finalFraction)
                    callbacks.onSettleSplitFraction()
                    inspectorAnimationTick++
                    // No manual liveDragFraction reset here — the snapshotFlow mirror above
                    // keeps it tracking inspectorFraction.value across the settle animation.
                },
                liveDragFractionProvider = liveDragFractionProvider,
                modifier = Modifier
                    .fillMaxSize(),
            )

            SceneScreenTopBar(
                sceneEntry = sceneEntry,
                controlsSheet = state.controlsSheet,
                isDarkTheme = themeController.isDarkTheme,
                callbacks = callbacks,
                statusBarInsets = contentWindowInsets,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun SceneScreenTopBar(
    sceneEntry: SceneEntry,
    controlsSheet: ControlsSheetUiState,
    isDarkTheme: Boolean,
    callbacks: SceneScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
    modifier: Modifier = Modifier,
) {
    val controlsLayout = controlsSheet.layoutMode
    val expandedProgressState = animateFloatAsState(
        targetValue = if (controlsLayout == SceneInspectorLayoutMode.Expanded) 1f else 0f,
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_expanded_progress",
    )
    val crossfadeState by remember(expandedProgressState) {
        derivedStateOf {
            calculateSceneTopBarCrossfadeState(expandedProgressState.value)
        }
    }

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
        Box(modifier = Modifier.fillMaxWidth()) {
            SplitTopBarRow(
                sceneEntry = sceneEntry,
                controlsLayout = controlsLayout,
                isVisible = controlsSheet.isVisible,
                isDarkTheme = isDarkTheme,
                callbacks = callbacks,
                interactive = crossfadeState.splitInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f - crossfadeState.expandedProgress }
                    .zIndex(crossfadeState.splitZIndex),
            )
            ExpandedTopBarRow(
                sceneEntry = sceneEntry,
                selectedTab = controlsSheet.selectedTab,
                callbacks = callbacks,
                interactive = crossfadeState.expandedInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = crossfadeState.expandedProgress }
                    .zIndex(crossfadeState.expandedZIndex),
            )
        }
    }
}

@Composable
private fun SplitTopBarRow(
    sceneEntry: SceneEntry,
    controlsLayout: SceneInspectorLayoutMode,
    isVisible: Boolean,
    isDarkTheme: Boolean,
    callbacks: SceneScreenCallbacks,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SceneTopBarActionButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (isVisible) "Close settings" else "Back",
            onClick = if (isVisible) callbacks::onDismissControls else callbacks::onBack,
            enabled = interactive,
        )
        SceneSceneTitleIsland(
            sceneEntry = sceneEntry,
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
                enabled = interactive,
            )
            ComposiumThemeToggle(
                isDark = isDarkTheme,
                onToggle = callbacks::onThemeChange,
                enabled = interactive,
            )
        }
    }
}

@Composable
private fun ExpandedTopBarRow(
    sceneEntry: SceneEntry,
    selectedTab: SceneInspectorTab,
    callbacks: SceneScreenCallbacks,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SceneTopBarActionButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to split layout",
            onClick = callbacks::onNavigateBackFromExpandedControls,
            enabled = interactive,
        )
        SceneExpandedControlsTitleIsland(
            sceneEntry = sceneEntry,
            selectedTab = selectedTab,
            modifier = Modifier.weight(1f),
        )
        SceneTopBarActionButton(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close settings",
            onClick = callbacks::onDismissControls,
            enabled = interactive,
        )
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
            .background(Tokens.colors.surface)
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.8f), Tokens.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Column {
            // Always render the group line, even when there is no group, so the island's
            // vertical extent stays the same as a scene that has one. Without the placeholder
            // line the Column collapses by labelSmall's line-height and the central island
            // visibly shrinks for ungrouped scenes.
            val group = sceneEntry.scene.group
            val hasGroup = !group.isNullOrBlank()
            ComposiumText(
                text = if (hasGroup) group!! else " ",
                style = Tokens.typography.labelSmall,
                color = if (hasGroup) Tokens.colors.primary else Color.Transparent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
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
    sceneEntry: SceneEntry,
    selectedTab: SceneInspectorTab,
    modifier: Modifier = Modifier,
) {
    val sceneLabel = buildString {
        val group = sceneEntry.scene.group
        if (!group.isNullOrBlank()) {
            append(group)
            append("/")
        }
        append(sceneEntry.scene.name)
    }
    val tabLabel = when (selectedTab) {
        SceneInspectorTab.Properties -> "Properties"
        SceneInspectorTab.Environment -> "Environment"
    }

    Box(
        modifier = modifier
            .clip(Tokens.shapes.extraLarge)
            .background(Tokens.colors.surface)
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
    sceneEntry: SceneEntry,
    sceneScope: SceneScope,
    paramsState: SceneParamsState,
    controlsSheet: ControlsSheetUiState,
    isDarkTheme: Boolean,
    callbacks: SceneScreenCallbacks,
    paramsCallbacks: SceneParamsCallbacks,
    inspectorFractionProvider: () -> Float,
    isInspectorComposed: Boolean,
    onInspectorDragUpdate: (Float) -> Unit,
    onInspectorDragStop: (Float) -> Unit,
    liveDragFractionProvider: () -> Float,
    contentWindowInsets: WindowInsets? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val topInset = contentWindowInsets?.getTop(density)?.let { inset ->
        with(density) { inset.toDp() }
    } ?: 0.dp
    val bottomInset = contentWindowInsets?.getBottom(density)?.let { inset ->
        with(density) { inset.toDp() }
    } ?: 0.dp
    val contentTopPadding = topInset + 72.dp

    // Tell the scene what insets the runtime is NOT applying for it.
    // - enableEdgeToEdge = true  → runtime applies nothing; scene renders behind top bar /
    //   nav bar and reads `innerPadding` to add the spacing it wants.
    // - enableEdgeToEdge = false → runtime applies the same spacing itself, so the scene
    //   sees PaddingValues(0) and just fills its visible area normally.
    val sceneInnerPadding = if (sceneEntry.scene.enableEdgeToEdge) {
        androidx.compose.foundation.layout.PaddingValues(
            top = contentTopPadding,
            bottom = if (controlsSheet.layoutMode == SceneInspectorLayoutMode.Closed) {
                bottomInset
            } else {
                0.dp
            },
        )
    } else {
        androidx.compose.foundation.layout.PaddingValues(0.dp)
    }
    SideEffect {
        sceneScope.internalInnerPadding = sceneInnerPadding
    }

    val previewAlpha = animateFloatAsState(
        targetValue = if (controlsSheet.layoutMode == SceneInspectorLayoutMode.Expanded) 0f else 1f,
        animationSpec = Motion.tweenStandard(),
        label = "scene_preview_alpha",
    )

    // The visible boundary between preview and inspector sits at the MIDDLE of the tabs in
    // Split mode. The preview pane is therefore extended downward by this much so that the
    // top half of the tabs floats over the (translucent) preview content, and the bottom
    // half marks where preview content is fully clipped. In Expanded mode the boundary
    // doesn't exist — animate to zero for the transition.
    val isSplitLayout = controlsSheet.layoutMode == SceneInspectorLayoutMode.Split
    val splitBoundaryShift by animateDpAsState(
        targetValue = if (isSplitLayout) {
            SceneInspectorTabsHeight / 2 + SceneInspectorTabsTopGap
        } else {
            0.dp
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_split_boundary_shift",
    )

    // BoxWithConstraints fills the full screen (no top padding). The preview pane's outer
    // .layout decides the scene placement based on enableEdgeToEdge — either filling the
    // pane edge-to-edge, or sitting inside the safe area below the top bar / above the nav
    // bar. Inspector positioning is preserved by treating `availableHeightPx` as the
    // safe-area height (= constraints.maxHeight − contentTopPaddingPx).
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        val contentTopPaddingPx = with(density) { contentTopPadding.roundToPx() }
        val availableHeightPx = (constraints.maxHeight - contentTopPaddingPx).coerceAtLeast(0)
        // The preview pane no longer manages its own scrolling, so there's no overflow to
        // signal to the inspector — divider stays hidden in this mode.
        val showSplitDivider = shouldShowSceneSplitDivider(
            mode = controlsSheet.layoutMode,
            hasScrollableOverflow = false,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            ScenePreviewPane(
                sceneEntry = sceneEntry,
                sceneScope = sceneScope,
                availableHeightPx = availableHeightPx,
                inspectorFractionProvider = inspectorFractionProvider,
                layoutMode = controlsSheet.layoutMode,
                splitBoundaryShift = splitBoundaryShift,
                contentTopPadding = contentTopPadding,
                bottomInset = bottomInset,
                enableEdgeToEdge = sceneEntry.scene.enableEdgeToEdge,
                onBackgroundTap = if (controlsSheet.layoutMode == SceneInspectorLayoutMode.Split) {
                    callbacks::onPreviewPaneTapped
                } else {
                    null
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = previewAlpha.value },
            )

            if (isInspectorComposed) {
                SceneInspectorPane(
                    paramsState = paramsState,
                    scenePreview = sceneScope.preview,
                    isDarkThemeEnabled = isDarkTheme,
                    paramsCallbacks = paramsCallbacks,
                    selectedTab = controlsSheet.selectedTab,
                    showSplitDivider = showSplitDivider,
                    onBackgroundTap = if (controlsSheet.layoutMode == SceneInspectorLayoutMode.Split) {
                        callbacks::onExpandControls
                    } else {
                        null
                    },
                    availableHeightPx = availableHeightPx,
                    splitFraction = controlsSheet.splitFraction,
                    inspectorFractionProvider = inspectorFractionProvider,
                    liveDragFractionProvider = liveDragFractionProvider,
                    onInspectorDragUpdate = onInspectorDragUpdate,
                    onInspectorDragStop = onInspectorDragStop,
                    onTabSelected = callbacks::onTabSelected,
                    onThemeChange = callbacks::onThemeChange,
                    bottomInset = bottomInset,
                    splitBoundaryShift = splitBoundaryShift,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
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
    enabled: Boolean = true,
) {
    val containerColor by animateColorAsState(
        // The light-theme primaryContainer token bakes in alpha 0.83 (0xD3B5D9E8). Override
        // to fully opaque so the active settings button is solid like the other top bar
        // elements rather than letting preview content bleed through.
        targetValue = if (active) {
            Tokens.colors.primaryContainer
        } else {
            Tokens.colors.surface
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) {
            Color.Transparent
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
        ComposiumIconButton(onClick = onClick, enabled = enabled) {
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
    availableHeightPx: Int,
    inspectorFractionProvider: () -> Float,
    layoutMode: SceneInspectorLayoutMode,
    splitBoundaryShift: Dp,
    contentTopPadding: Dp,
    bottomInset: Dp,
    enableEdgeToEdge: Boolean,
    onBackgroundTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // The preview pane visually occupies [Y=0, Y=paneHeight] in its parent. The pane height
    // covers everything from the top of the screen down to either the inspector boundary
    // (Split / Expanded) or the screen bottom (Closed). What changes between edge-to-edge
    // and not is only the placement of the SCENE inside this pane:
    //
    //   - enableEdgeToEdge = true  → scene fills the entire pane, including behind the top
    //     bar and (in Closed mode) behind the system nav bar. The scene receives both insets
    //     via SceneScope.innerPadding so it can apply them where appropriate.
    //   - enableEdgeToEdge = false → scene is offset down by contentTopPadding (so it sits
    //     below the top bar) and (in Closed mode) shortened by bottomInset (so it sits above
    //     the nav bar). SceneScope.innerPadding is reported as zero.
    //
    // Constraints passed to the scene are FULLY BOUNDED (minH = maxH = sceneHeight) so any
    // vertical scrollables inside the scene (LazyColumn / Modifier.verticalScroll) measure
    // without crashing on `hasBoundedHeight`. There's no outer scroll: if the scene is
    // taller than the available area in Split mode, it simply gets clipped — scenes that
    // need their own scrolling are expected to bring it themselves.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val fraction = inspectorFractionProvider().sanitizedInspectorFraction()
                val shiftPx = splitBoundaryShift.toPx()
                val topPaddingPx = contentTopPadding.toPx()
                val applyBottomInset = !enableEdgeToEdge &&
                    layoutMode == SceneInspectorLayoutMode.Closed
                val bottomPaddingPx = if (applyBottomInset) bottomInset.toPx() else 0f
                val ceiling = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE

                val visibleAreaPx = (1f - fraction) * availableHeightPx + shiftPx
                val paneHeight = (visibleAreaPx + topPaddingPx)
                    .roundToInt()
                    .coerceIn(1, ceiling)

                val sceneTopPx = if (enableEdgeToEdge) 0 else topPaddingPx.roundToInt()
                val sceneBottomPx = bottomPaddingPx.roundToInt()
                val sceneHeight = (paneHeight - sceneTopPx - sceneBottomPx).coerceAtLeast(0)

                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minWidth,
                        maxWidth = constraints.maxWidth,
                        minHeight = sceneHeight,
                        maxHeight = sceneHeight,
                    ),
                )
                layout(placeable.width, paneHeight) {
                    placeable.place(0, sceneTopPx)
                }
            }
            // Clip the pane to its reported size so a scene whose intrinsic content is taller
            // than `sceneHeight` doesn't overflow downward and get drawn behind the inspector
            // (which would visually "cut" the scene at the top edge of the tabs instead of at
            // the boundary). Compose doesn't clip overflow by default — without this the
            // scene's tail bleeds into the inspector area.
            .clipToBounds()
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
        ScenePreviewContent(
            sceneEntry = sceneEntry,
            sceneScope = sceneScope,
            modifier = Modifier.fillMaxWidth(),
        )
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
    inspectorFractionProvider: () -> Float,
    liveDragFractionProvider: () -> Float,
    onInspectorDragUpdate: (Float) -> Unit,
    onInspectorDragStop: (Float) -> Unit,
    onTabSelected: (SceneInspectorTab) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    bottomInset: Dp,
    splitBoundaryShift: Dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isSplitMode = onBackgroundTap != null

    // Tracks active finger drag on the grabber. The armed indicator must only react to user
    // drags — not to click-driven transitions that happen to pass through the armed range
    // (e.g. tap-to-expand or back-to-split, where the inspector fraction sweeps across
    // 0.78..0.85 as part of the animation but no arming gesture is in progress).
    var isDraggingGrabber by remember { mutableStateOf(false) }
    val armedProgressProvider: () -> Float = remember(inspectorFractionProvider) {
        {
            val fraction = inspectorFractionProvider().sanitizedInspectorFraction()
            if (!isDraggingGrabber) {
                0f
            } else {
                ((fraction - SCENE_INSPECTOR_ARMED_START_FRACTION) /
                    (SCENE_INSPECTOR_ARMED_FULL_FRACTION - SCENE_INSPECTOR_ARMED_START_FRACTION))
                    .coerceIn(0f, 1f)
            }
        }
    }

    // Header padding differs between Split (2dp grabber gap) and Expanded (8dp breathing room)
    // — animate it so the 6dp delta blends with the inspector size transition instead of
    // popping instantly the moment layoutMode changes (which the eye perceives as the panel
    // briefly jerking in the wrong direction at the start of the expand/restore animation).
    val tabsHeaderExtraPadding by animateDpAsState(
        targetValue = if (isSplitMode) SceneInspectorTabsTopGap else 8.dp,
        animationSpec = Motion.tweenStandard(),
        label = "scene_inspector_tabs_header_extra_padding",
    )

    // Distance from the boundary to the bottom of the tabs header. Used to push body content
    // down so it sits below the tabs at scroll=0 (= same visual default as before), even
    // though the body's scroll viewport now extends UP to the boundary so items can scroll
    // behind the (opaque) tabs and clip at their middle, mirroring the preview behavior.
    val tabsHeaderTotalHeight = SceneInspectorTabsHeight + tabsHeaderExtraPadding
    val bodyTopOffset = tabsHeaderTotalHeight - splitBoundaryShift
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 16.dp,
        top = 8.dp + bodyTopOffset,
        end = 16.dp,
        bottom = bottomInset + 28.dp,
    )

    val currentLiveDragFractionProvider by rememberUpdatedState(liveDragFractionProvider)
    val currentOnInspectorDragUpdate by rememberUpdatedState(onInspectorDragUpdate)
    val currentOnInspectorDragStop by rememberUpdatedState(onInspectorDragStop)
    val currentAvailableHeightPx by rememberUpdatedState(availableHeightPx)
    val draggableState = rememberDraggableState { delta ->
        val height = currentAvailableHeightPx
        if (height > 0 && delta.isFinite()) {
            val current = currentLiveDragFractionProvider()
            val next = current - delta / height.toFloat()
            val resolved = if (next.isFinite()) {
                next.coerceIn(0f, MAX_SCENE_INSPECTOR_DRAG_CAP_FRACTION)
            } else {
                current
            }
            currentOnInspectorDragUpdate(resolved)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val fraction = inspectorFractionProvider().sanitizedInspectorFraction()
                // Use the passed-in availableHeightPx (= screen − top bar) rather than
                // constraints.maxHeight: the parent BoxWithConstraints now spans the full
                // screen, so its constraints include the top bar region we don't own.
                val h = (fraction * availableHeightPx).roundToInt().coerceIn(0, availableHeightPx)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = h, maxHeight = h),
                )
                layout(placeable.width, h) { placeable.place(0, 0) }
            }
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
        // Body content area — extends UP to the boundary (middle of tabs in Split, top of
        // pane in Expanded). The .padding(top = splitBoundaryShift) clips the scroll viewport
        // there, so items scrolled past the boundary disappear behind the (opaque) tabs.
        // The contentPadding inside the panels offsets the first item to sit below the tabs
        // at scroll=0; that padding scrolls with content, mirroring the preview's behavior on
        // the other side of the boundary.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = splitBoundaryShift),
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = initialState.transitionDirectionTo(targetState)
                    (
                        slideInHorizontally(
                            animationSpec = Motion.tweenStandard(),
                            initialOffsetX = { width -> width * direction },
                        ) + fadeIn(Motion.tweenStandard())
                    ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = Motion.tweenStandard(),
                            targetOffsetX = { width -> -width * direction },
                        ) + fadeOut(Motion.tweenFast())
                    )
                },
                label = "scene_inspector_tab_content",
            ) { tab ->
                when (tab) {
                    SceneInspectorTab.Properties -> {
                        if (paramsState.params.isEmpty()) {
                            // Empty state isn't scrollable — push it below the tabs explicitly.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = bodyTopOffset),
                            ) {
                                SceneInspectorEmptyState(
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            ControlsPanel(
                                state = paramsState,
                                callbacks = paramsCallbacks,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    SceneInspectorTab.Environment -> {
                        EnvironmentPanel(
                            preview = scenePreview,
                            isDarkThemeEnabled = isDarkThemeEnabled,
                            onThemeChange = onThemeChange,
                            contentPadding = contentPadding,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            SceneInspectorBottomGradient(
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomInset = bottomInset,
            )
        }

        if (isSplitMode) {
            // Divider sits on the new boundary — at the middle of the tabs (= shift below
            // the inspector pane top), not at the top of the tabs as before.
            SceneSplitDivider(
                visible = showSplitDivider,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = splitBoundaryShift),
            )
        }

        // Tabs header — last in z-order so it draws ON TOP of body content scrolled up
        // behind it (the opaque tabs visually clip those items at the boundary).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tabsHeaderTotalHeight)
                .align(Alignment.TopCenter)
                .then(
                    if (isSplitMode) {
                        Modifier.draggable(
                            orientation = Orientation.Vertical,
                            state = draggableState,
                            onDragStarted = { isDraggingGrabber = true },
                            onDragStopped = {
                                isDraggingGrabber = false
                                currentOnInspectorDragStop(
                                    currentLiveDragFractionProvider(),
                                )
                            },
                        )
                    } else {
                        Modifier
                    }
                ),
        ) {
            SceneInspectorTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            if (isSplitMode) {
                SceneInspectorArmedIndicator(
                    progressProvider = armedProgressProvider,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(SceneInspectorTabsHeight),
                )
            }
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
                .background(Tokens.colors.surface)
                .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.72f), Tokens.shapes.pill),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(28.dp)
                    .align(Alignment.CenterStart)
                    .clip(Tokens.shapes.pill)
                    .background(Tokens.colors.primaryContainer.copy(alpha = 0.6f)),
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(SceneInspectorTabsHeight)
            .clip(Tokens.shapes.pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
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
private fun SceneInspectorArmedIndicator(
    progressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val isArmed by remember(progressProvider) {
        derivedStateOf { progressProvider() >= 1f }
    }
    val glowAlpha by animateFloatAsState(
        targetValue = if (isArmed) 1f else 0f,
        animationSpec = Motion.tweenStandard(),
        label = "scene_inspector_armed_glow",
    )
    // Soft cloud-sky blue — lighter and airier than the brand primary.
    val strokeColor = Color(0xFF8FC4DF)

    Canvas(modifier = modifier) {
        val progress = progressProvider().coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        if (progress <= 0f && glowAlpha <= 0f) return@Canvas

        val r = h / 2f
        val centerX = w / 2f
        val strokeWidth = 3.dp.toPx()

        // Right half: bottom-center → bottom flat → right semicircle → top flat → top-center.
        val rightPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX, h)
            lineTo(w - r, h)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(w - 2 * r, 0f, w, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false,
            )
            lineTo(centerX, 0f)
        }
        // Left half (mirror): bottom-center → left flat → left semicircle → top flat → top-center.
        val leftPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX, h)
            lineTo(r, h)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(0f, 0f, 2 * r, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(centerX, 0f)
        }

        // Glow layers — drawn under the main stroke, only present when armed.
        // Butt caps (instead of Round) so the two halves meet flush at the top/bottom centers
        // rather than producing visible cap "dots" where their endpoints overlap.
        if (glowAlpha > 0f) {
            for (i in 0..3) {
                val layerWidth = strokeWidth + (3 + i * 3).dp.toPx()
                val layerAlpha = (0.24f - i * 0.05f).coerceAtLeast(0f) * glowAlpha
                if (layerAlpha <= 0f) continue
                val glowStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = layerWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                )
                drawPath(rightPath, strokeColor.copy(alpha = layerAlpha), style = glowStyle)
                drawPath(leftPath, strokeColor.copy(alpha = layerAlpha), style = glowStyle)
            }
        }

        if (progress > 0f) {
            val rightMeasure = androidx.compose.ui.graphics.PathMeasure().apply {
                setPath(rightPath, false)
            }
            val leftMeasure = androidx.compose.ui.graphics.PathMeasure().apply {
                setPath(leftPath, false)
            }
            val rightSegment = androidx.compose.ui.graphics.Path()
            rightMeasure.getSegment(0f, rightMeasure.length * progress, rightSegment, true)
            val leftSegment = androidx.compose.ui.graphics.Path()
            leftMeasure.getSegment(0f, leftMeasure.length * progress, leftSegment, true)

            val mainStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Butt,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            )
            drawPath(rightSegment, strokeColor, style = mainStyle)
            drawPath(leftSegment, strokeColor, style = mainStyle)
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
            // Propagate the parent's min-height to the decorator so its `fillMaxSize()`
            // actually fills the visible preview area. Without this, Box's default behavior
            // relaxes minHeight to 0 when measuring children — and because the surrounding
            // verticalScroll passes maxHeight = Infinity, fillMaxSize then can't determine
            // a height to fill and the decorator collapses to its content size, causing
            // any contentAlignment inside the decorator (e.g. TopCenter) to have no effect
            // on where the scene actually appears.
            propagateMinConstraints = true,
        ) {
            LocalScenePreviewContainer.current.Decoration {
                sceneEntry.scene.content(sceneScope)
            }
        }
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
