package oleginvoke.com.composium.main_screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oleginvoke.com.composium.R
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.onlyTopAndHorizontalOrNull
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailState
import oleginvoke.com.composium.ui.components.ComposiumButton
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumOutlinedButton
import oleginvoke.com.composium.ui.components.ComposiumSceneCard
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumThemeToggle
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun MainScreen(
    scenes: List<SceneEntry>,
    onSceneSelected: (sceneId: String) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
    thumbnailStates: Map<String, SceneThumbnailState> = emptyMap(),
    onVisibleSceneIdsChanged: (List<String>) -> Unit = {},
    onListScrollInProgressChanged: (Boolean) -> Unit = {},
) {
    val themeController = LocalComposiumThemeController.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val backgroundInteractionSource = remember { MutableInteractionSource() }
    val store = rememberMainScreenStore()
    val state = store.state
    var topBarHeightDp by remember { mutableStateOf(0f) }
    val scenesContentKey by remember(scenes) {
        derivedStateOf {
            scenes.map { entry ->
                "${entry.id}|${entry.scene.group.orEmpty()}|${entry.scene.name}"
            }
        }
    }
    val sceneSearchIndex = remember(scenesContentKey) {
        buildSceneSearchIndex(scenes)
    }

    val filteredScenes = remember(sceneSearchIndex, state.query) {
        filterScenes(
            sceneIndex = sceneSearchIndex,
            query = state.query,
        )
    }
    val catalogStatus = remember(state.query, filteredScenes.size, scenes.size) {
        buildCatalogStatus(
            query = state.query,
            visibleCount = filteredScenes.size,
            totalCount = scenes.size,
        )
    }
    val listViewportLayout = remember(topBarHeightDp) {
        mainScreenListViewportLayout(topBarHeightDp = topBarHeightDp)
    }

    val callbacks = remember(
        store,
        sceneSearchIndex,
        onSceneSelected,
        themeController.onThemeChange,
        focusManager,
        keyboardController,
    ) {
        object : MainScreenCallbacks {
            override fun onQueryChange(query: String) {
                store.dispatch(
                    intent = MainScreenIntent.QueryChanged(query),
                    sceneIndex = sceneSearchIndex,
                )
            }

            override fun onSceneSelected(sceneId: String) {
                val inputDismissal = calculateSceneSelectionInputDismissal()
                if (inputDismissal.clearFocus) {
                    focusManager.clearFocus()
                }
                if (inputDismissal.hideKeyboard) {
                    keyboardController?.hide()
                }
                onSceneSelected.invoke(sceneId)
            }

            override fun onGroupToggled(group: String) {
                store.dispatch(
                    intent = MainScreenIntent.GroupToggled(group),
                    sceneIndex = sceneSearchIndex,
                )
            }

            override fun onThemeChange(isDarkTheme: Boolean) {
                themeController.onThemeChange.invoke(isDarkTheme)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.surface)
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            ),
    ) {
        MainScreenContent(
            scenes = filteredScenes,
            expandedGroups = state.expandedGroups,
            catalogStatus = catalogStatus,
            callbacks = callbacks,
            contentWindowInsets = contentWindowInsets,
            thumbnailStates = thumbnailStates,
            onVisibleSceneIdsChanged = onVisibleSceneIdsChanged,
            onListScrollInProgressChanged = onListScrollInProgressChanged,
            extraTopPaddingDp = listViewportLayout.extraTopPaddingDp,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = listViewportLayout.topOffsetDp.dp),
        )
        MainScreenTopBar(
            query = state.query,
            isDarkTheme = themeController.isDarkTheme,
            callbacks = callbacks,
            statusBarInsets = contentWindowInsets,
            modifier = Modifier.onSizeChanged { size ->
                topBarHeightDp = with(density) { size.height.toDp().value }
            },
        )
    }
}

@Composable
private fun MainScreenTopBar(
    query: String,
    isDarkTheme: Boolean,
    callbacks: MainScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                statusBarInsets
                    .onlyTopAndHorizontalOrNull()
                    ?.let(Modifier::windowInsetsPadding)
                    ?: Modifier,
            )
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = MainScreenTopBarBottomPaddingDp.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposiumText(
                    text = "Composium",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                    ),
                    color = Tokens.colors.onSurface,
                )
            }
            ComposiumThemeToggle(
                isDark = isDarkTheme,
                onToggle = callbacks::onThemeChange,
            )
        }

        Spacer(Modifier.height(12.dp))

        SearchStoriesField(
            value = query,
            onValueChange = callbacks::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MainScreenContent(
    scenes: List<SceneEntry>,
    expandedGroups: Set<String>,
    catalogStatus: MainScreenCatalogStatus,
    callbacks: MainScreenCallbacks,
    contentWindowInsets: WindowInsets?,
    thumbnailStates: Map<String, SceneThumbnailState>,
    onVisibleSceneIdsChanged: (List<String>) -> Unit,
    onListScrollInProgressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    extraTopPaddingDp: Float = 0f,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val listContentPadding = mainScreenListContentPadding(
        contentWindowInsets = contentWindowInsets,
        density = density,
        extraTopPaddingDp = extraTopPaddingDp,
    )
    val sceneGroupTree = remember(scenes) {
        buildSceneGroupTree(scenes)
    }
    val listItems = remember(sceneGroupTree, expandedGroups) {
        val (ungroupedScenes, rootGroups) = sceneGroupTree
        buildMainScreenListItems(
            ungroupedScenes = ungroupedScenes,
            rootGroups = rootGroups,
            expandedGroups = expandedGroups,
        )
    }

    LaunchedEffect(listState, listItems, onVisibleSceneIdsChanged) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { visibleItem ->
                    (listItems.getOrNull(visibleItem.index) as? MainScreenListItem.SceneItem)
                        ?.entry
                        ?.id
                }
                .distinct()
        }.collect(onVisibleSceneIdsChanged)
    }

    LaunchedEffect(listState, onListScrollInProgressChanged) {
        snapshotFlow { listState.isScrollInProgress }
            .collect(onListScrollInProgressChanged)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = listContentPadding,
    ) {
        if (catalogStatus.totalCount > 0) {
            item("catalog_meta") {
                CatalogMetaRow(
                    totalCount = catalogStatus.totalCount,
                    visibleCount = catalogStatus.visibleCount,
                    isFiltered = catalogStatus.query.isNotBlank(),
                )
            }
        }

        if (listItems.isEmpty()) {
            item("empty_state") {
                MainScreenEmptyState(
                    status = catalogStatus,
                    onClearSearch = { callbacks.onQueryChange("") },
                )
            }
            return@LazyColumn
        }

        itemsIndexed(
            items = listItems,
            key = { _, item -> item.key },
            contentType = { _, item ->
                when (item) {
                    is MainScreenListItem.GroupHeader -> "group_header"
                    is MainScreenListItem.SceneItem -> "scene_item"
                }
            },
        ) { index, item ->
            when (item) {
                is MainScreenListItem.SceneItem -> {
                    HierarchyItemFrame(
                        depth = item.depth,
                        connectorContinuations = item.connectorContinuations,
                        drawChildStem = false,
                    ) {
                        ComposiumSceneCard(
                            name = item.entry.scene.name,
                            group = item.entry.scene.group,
                            thumbnailState = thumbnailStates[item.entry.id],
                            badge = item.entry.scene.badge,
                            onClick = { callbacks.onSceneSelected(item.entry.id) },
                        )
                    }
                }

                is MainScreenListItem.GroupHeader -> {
                    val expanded = expandedGroups.contains(item.path)
                    HierarchyItemFrame(
                        depth = item.depth,
                        connectorContinuations = item.connectorContinuations,
                        drawChildStem = expanded,
                    ) {
                        GroupHeaderRow(
                            name = item.name,
                            depth = item.depth,
                            scenesCount = item.scenesCount,
                            expanded = expanded,
                            onToggled = { callbacks.onGroupToggled(item.path) },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HierarchyItemFrame(
    depth: Int,
    connectorContinuations: List<Boolean>,
    drawChildStem: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layout = remember(depth) { calculateMainScreenHierarchyLayout(depth) }
    val connectorStyle = remember { mainScreenHierarchyConnectorStyle() }
    val connectorColor = Tokens.colors.primary.copy(alpha = 0.58f)
    val shouldDrawConnectors = depth > 0 || drawChildStem

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (shouldDrawConnectors) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        val itemBottomGapPx = layout.childStemStartInsetFromBottomDp.dp.toPx()
                        val itemTargetY = mainScreenHierarchyConnectorTargetYPx(
                            itemHeightPx = size.height,
                            bottomGapPx = itemBottomGapPx,
                        )

                        layout.parentConnectorCentersDp.forEachIndexed { index, centerDp ->
                            val x = centerDp.dp.toPx()
                            val shouldContinue = connectorContinuations.getOrElse(index) { false }
                            val isCurrentConnector = index == layout.parentConnectorCentersDp.lastIndex
                            if (isCurrentConnector) return@forEachIndexed

                            val endY = when {
                                shouldContinue -> size.height
                                else -> 0f
                            }
                            if (endY > 0f) {
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, endY),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round,
                                )
                            }
                        }

                        layout.currentConnectorCenterDp?.let { centerDp ->
                            val x = centerDp.dp.toPx()
                            val elbowEndX = layout.elbowEndDp.dp.toPx()
                            val currentConnectorContinues = connectorContinuations
                                .getOrElse(layout.parentConnectorCentersDp.lastIndex) { false }
                            val elbow = calculateMainScreenHierarchyConnectorElbow(
                                connectorX = x,
                                elbowEndX = elbowEndX,
                                targetYPx = itemTargetY,
                                requestedRadiusPx = connectorStyle.cornerRadiusDp.dp.toPx(),
                            )
                            if (elbow.radiusPx > 0f) {
                                if (currentConnectorContinues) {
                                    drawLine(
                                        color = connectorColor,
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = strokeWidth,
                                        cap = StrokeCap.Round,
                                    )
                                }
                                val path = Path().apply {
                                    moveTo(x, 0f)
                                    if (currentConnectorContinues) {
                                        moveTo(x, elbow.verticalEndYPx)
                                    } else {
                                        lineTo(x, elbow.verticalEndYPx)
                                    }
                                    quadraticTo(
                                        x1 = x,
                                        y1 = itemTargetY,
                                        x2 = elbow.horizontalStartXPx,
                                        y2 = itemTargetY,
                                    )
                                    lineTo(elbowEndX, itemTargetY)
                                }
                                drawPath(
                                    path = path,
                                    color = connectorColor,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                    ),
                                )
                            } else {
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, itemTargetY),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round,
                                )
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(x, itemTargetY),
                                    end = Offset(elbowEndX, itemTargetY),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round,
                                )
                            }
                            if (currentConnectorContinues && elbow.radiusPx <= 0f) {
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(x, itemTargetY),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round,
                                )
                            }
                        }

                        if (drawChildStem) {
                            val childX = layout.childConnectorCenterDp.dp.toPx()
                            val startY = (size.height - itemBottomGapPx)
                                .coerceAtLeast(0f)
                            drawLine(
                                color = connectorColor,
                                start = Offset(childX, startY),
                                end = Offset(childX, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                } else {
                    Modifier
                },
            )
            .padding(bottom = layout.childStemStartInsetFromBottomDp.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = layout.contentStartDp.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CatalogMetaRow(
    totalCount: Int,
    visibleCount: Int,
    isFiltered: Boolean,
) {
    ComposiumText(
        text = if (isFiltered) {
            "$totalCount scenes total · $visibleCount visible"
        } else {
            "$totalCount scenes"
        },
        style = Tokens.typography.labelSmall,
        color = Tokens.colors.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun MainScreenEmptyState(
    status: MainScreenCatalogStatus,
    onClearSearch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Tokens.shapes.large)
            .background(Tokens.colors.surface)
            .border(1.dp, Tokens.colors.outlineVariant, Tokens.shapes.large)
            .padding(18.dp),
    ) {
        Column {
            ComposiumIcon(
                painter = painterResource(R.drawable.empty_search_icon),
                contentDescription = null,
                tint = Tokens.colors.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(12.dp))
            ComposiumText(
                text = emptyStateTitle(status),
                style = Tokens.typography.titleLarge,
                color = Tokens.colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            ComposiumText(
                text = emptyStateBody(status),
                style = Tokens.typography.bodyMedium,
                color = Tokens.colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (status.mode == MainScreenCatalogMode.EmptyResults) {
                ComposiumOutlinedButton(onClick = onClearSearch) {
                    ComposiumText(
                        text = "Clear filter",
                        style = Tokens.typography.titleMedium,
                        color = Tokens.colors.onSurface,
                    )
                }
            } else {
                ComposiumButton(
                    onClick = {},
                    enabled = false,
                    containerColor = Tokens.colors.surfaceVariant,
                ) {
                    ComposiumText(
                        text = "Awaiting scene registration",
                        style = Tokens.typography.titleMedium,
                        color = Tokens.colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeaderRow(
    name: String,
    depth: Int,
    scenesCount: Int,
    expanded: Boolean,
    onToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = Motion.springSnappy(),
        label = "group_chevron",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val layout = remember(depth) { mainScreenGroupHeaderLayout(depth) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (expanded) 2.dp else 1.dp,
                shape = Tokens.shapes.medium,
                ambientColor = Tokens.colors.scrim.copy(alpha = 0.08f),
                spotColor = Tokens.colors.scrim.copy(alpha = 0.10f),
            )
            .clip(Tokens.shapes.medium)
            .background(
                if (expanded) {
                    Tokens.colors.primaryContainer.copy(alpha = if (depth == 0) 0.38f else 0.30f)
                } else {
                    Tokens.colors.surface.copy(alpha = 0.96f)
                },
            )
            .border(
                width = 1.dp,
                color = if (expanded) {
                    Tokens.colors.primary.copy(alpha = 0.22f)
                } else {
                    Tokens.colors.outlineVariant.copy(alpha = 0.78f)
                },
                shape = Tokens.shapes.medium,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggled,
            )
            .padding(
                start = layout.horizontalPaddingDp.dp,
                end = 16.dp,
                top = layout.verticalPaddingDp.dp,
                bottom = layout.verticalPaddingDp.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(Tokens.shapes.small)
                    .background(Tokens.colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.72f), Tokens.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                ComposiumIcon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse group" else "Expand group",
                    tint = Tokens.colors.primary,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = rotation },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ComposiumText(
                    text = name,
                    style = Tokens.typography.titleMedium,
                    color = Tokens.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                ComposiumText(
                    text = "Group",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        GroupSceneCountBadge(scenesCount = scenesCount)
    }
}

@Composable
private fun GroupSceneCountBadge(
    scenesCount: Int,
    modifier: Modifier = Modifier,
) {
    val layout = remember { mainScreenGroupCountBadgeLayout() }
    val shape = RoundedCornerShape(layout.cornerRadiusDp.dp)

    Box(
        modifier = modifier
            .size(layout.sizeDp.dp)
            .clip(shape)
            .background(Tokens.colors.primaryContainer.copy(alpha = 0.76f))
            .border(1.dp, Tokens.colors.primary.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumText(
            text = scenesCount.toString(),
            style = Tokens.typography.labelSmall,
            color = Tokens.colors.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

private data class SceneGroupNode(
    val name: String,
    val path: String,
    val scenes: MutableList<SceneEntry> = mutableListOf(),
    val children: MutableMap<String, SceneGroupNode> = linkedMapOf(),
)

private sealed interface MainScreenListItem {
    val key: String

    data class GroupHeader(
        val name: String,
        val path: String,
        val depth: Int,
        val scenesCount: Int,
        val connectorContinuations: List<Boolean>,
    ) : MainScreenListItem {
        override val key: String = "group_$path"
    }

    data class SceneItem(
        val entry: SceneEntry,
        val depth: Int,
        val connectorContinuations: List<Boolean>,
    ) : MainScreenListItem {
        override val key: String = "scene_${entry.id}"
    }
}

private fun buildSceneGroupTree(
    scenes: List<SceneEntry>,
): Pair<List<SceneEntry>, List<SceneGroupNode>> {
    val ungroupedScenes = mutableListOf<SceneEntry>()
    val rootGroups: MutableMap<String, SceneGroupNode> = linkedMapOf()

    scenes.forEach { entry ->
        val segments = entry.scene.group
            ?.split("/")
            ?.map { segment -> segment.trim() }
            ?.filter { segment -> segment.isNotEmpty() }
            .orEmpty()

        if (segments.isEmpty()) {
            ungroupedScenes.add(entry)
            return@forEach
        }

        var currentChildren: MutableMap<String, SceneGroupNode> = rootGroups
        var currentPath = ""
        var node: SceneGroupNode? = null

        segments.forEach { segment ->
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
            node = currentChildren.getOrPut(segment) {
                SceneGroupNode(name = segment, path = currentPath)
            }
            currentChildren = requireNotNull(node).children
        }

        requireNotNull(node).scenes.add(entry)
    }

    return ungroupedScenes to rootGroups.values.toList()
}

private fun buildMainScreenListItems(
    ungroupedScenes: List<SceneEntry>,
    rootGroups: List<SceneGroupNode>,
    expandedGroups: Set<String>,
): List<MainScreenListItem> {
    val items = mutableListOf<MainScreenListItem>()
    val countCache = hashMapOf<String, Int>()

    ungroupedScenes.forEach { entry ->
        items.add(
            MainScreenListItem.SceneItem(
                entry = entry,
                depth = 0,
                connectorContinuations = emptyList(),
            ),
        )
    }

    fun SceneGroupNode.totalScenesCount(): Int =
        countCache.getOrPut(path) {
            scenes.size + children.values.sumOf { child -> child.totalScenesCount() }
        }

    fun addGroup(
        node: SceneGroupNode,
        depth: Int,
        connectorContinuations: List<Boolean>,
    ) {
        items.add(
            MainScreenListItem.GroupHeader(
                name = node.name,
                path = node.path,
                depth = depth,
                scenesCount = node.totalScenesCount(),
                connectorContinuations = connectorContinuations,
            ),
        )

        if (!expandedGroups.contains(node.path)) return

        val visibleChildren = buildList {
            node.children.values
            .sortedBy { child -> child.name.lowercase() }
                .forEach { child -> add(MainScreenGroupChild.Group(child)) }
            node.scenes.forEach { sceneEntry -> add(MainScreenGroupChild.Scene(sceneEntry)) }
        }

        visibleChildren.forEachIndexed { index, child ->
            val childConnectorContinuations = mainScreenConnectorContinuationsForChild(
                parentConnectorContinuations = connectorContinuations,
                hasNextSibling = index < visibleChildren.lastIndex,
            )
            when (child) {
                is MainScreenGroupChild.Group -> addGroup(
                    node = child.node,
                    depth = depth + 1,
                    connectorContinuations = childConnectorContinuations,
                )

                is MainScreenGroupChild.Scene -> {
                    items.add(
                        MainScreenListItem.SceneItem(
                            entry = child.entry,
                            depth = depth + 1,
                            connectorContinuations = childConnectorContinuations,
                        ),
                    )
                }
            }
        }
    }

    rootGroups
        .sortedBy { group -> group.name.lowercase() }
        .forEach { group ->
            addGroup(
                node = group,
                depth = 0,
                connectorContinuations = emptyList(),
            )
        }

    return items
}

private sealed interface MainScreenGroupChild {
    data class Group(val node: SceneGroupNode) : MainScreenGroupChild
    data class Scene(val entry: SceneEntry) : MainScreenGroupChild
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchStoriesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = remember { mainScreenSearchFieldLayout() }
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    var hasSeenVisibleImeForCurrentFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused, imeVisible) {
        val nextState = reduceSearchFieldImeState(
            isFocused = isFocused,
            isImeVisible = imeVisible,
            hasSeenVisibleImeForCurrentFocus = hasSeenVisibleImeForCurrentFocus,
        )
        hasSeenVisibleImeForCurrentFocus = nextState.hasSeenVisibleImeForCurrentFocus
        if (nextState.clearFocus) {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = modifier
            .height(layout.heightDp.dp)
            .clip(Tokens.shapes.extraLarge)
            .background(Tokens.colors.surface.copy(alpha = layout.containerAlpha))
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.8f), Tokens.shapes.extraLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { focusRequester.requestFocus() },
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layout.horizontalPaddingDp.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Tokens.colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                ),
                cursorBrush = SolidColor(Tokens.colors.primary),
                textStyle = TextStyle(
                    color = Tokens.colors.onSurface,
                    fontSize = Tokens.typography.titleMedium.fontSize,
                    fontWeight = Tokens.typography.titleMedium.fontWeight,
                    fontFamily = Tokens.typography.titleMedium.fontFamily,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(layout.inputHitTargetHeightDp.dp)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                ComposiumText(
                                    text = "Search scenes, groups, or states",
                                    style = Tokens.typography.bodyMedium,
                                    color = Tokens.colors.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                        if (value.isNotEmpty()) {
                            ComposiumIconButton(
                                modifier = Modifier.size(layout.clearButtonSizeDp.dp),
                                size = layout.clearButtonSizeDp.dp,
                                onClick = { onValueChange("") },
                            ) {
                                ComposiumIcon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                    tint = Tokens.colors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun emptyStateTitle(status: MainScreenCatalogStatus): String =
    when (status.mode) {
        MainScreenCatalogMode.EmptyCatalog -> "No scenes registered yet"
        MainScreenCatalogMode.EmptyResults -> "No matching scenes"
        MainScreenCatalogMode.FullCatalog,
        MainScreenCatalogMode.FilteredResults,
        -> ""
    }

private fun emptyStateBody(status: MainScreenCatalogStatus): String =
    when (status.mode) {
        MainScreenCatalogMode.EmptyCatalog ->
            "Composium is ready, but the catalog is still empty. Add scenes and this workspace will become your QA-ready inspection surface."

        MainScreenCatalogMode.EmptyResults ->
            "Nothing in the current catalog matches \"${status.query}\". Clearing the filter will bring back the full scene list."

        MainScreenCatalogMode.FullCatalog,
        MainScreenCatalogMode.FilteredResults,
        -> ""
    }
