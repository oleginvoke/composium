package oleginvoke.com.composium.main_screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
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
import oleginvoke.com.composium.ui.components.ComposiumBadge
import oleginvoke.com.composium.ui.components.ComposiumButton
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumOutlinedButton
import oleginvoke.com.composium.ui.components.ComposiumSceneCard
import oleginvoke.com.composium.ui.components.ComposiumSceneRow
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.ComposiumThemeToggle
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.staggeredAppear

@Composable
internal fun MainScreen(
    scenes: List<SceneEntry>,
    onSceneSelected: (sceneId: String) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val themeController = LocalComposiumThemeController.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val backgroundInteractionSource = remember { MutableInteractionSource() }
    val store = rememberMainScreenStore()
    val state = store.state
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
    val uiState = MainScreenUiState(
        query = state.query,
        scenes = filteredScenes,
        expandedGroups = state.expandedGroups,
        isDarkTheme = themeController.isDarkTheme,
    )

    val callbacks = remember(store, sceneSearchIndex, onSceneSelected, themeController.onThemeChange) {
        object : MainScreenCallbacks {
            override fun onQueryChange(query: String) {
                store.dispatch(
                    intent = MainScreenIntent.QueryChanged(query),
                    sceneIndex = sceneSearchIndex,
                )
            }

            override fun onSceneSelected(sceneId: String) {
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
            .background(Tokens.colors.background)
            .clickable(
                interactionSource = backgroundInteractionSource,
                indication = null,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MainScreenTopBar(
                state = uiState,
                catalogStatus = catalogStatus,
                callbacks = callbacks,
                statusBarInsets = contentWindowInsets,
            )
            MainScreenContent(
                state = uiState,
                catalogStatus = catalogStatus,
                callbacks = callbacks,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues()),
            )
        }
    }
}

@Composable
private fun MainScreenTopBar(
    state: MainScreenUiState,
    catalogStatus: MainScreenCatalogStatus,
    callbacks: MainScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (statusBarInsets != null) Modifier.windowInsetsPadding(statusBarInsets) else Modifier)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
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
                isDark = state.isDarkTheme,
                onToggle = callbacks::onThemeChange,
            )
        }

        Spacer(Modifier.height(12.dp))

        SearchStoriesField(
            value = state.query,
            onValueChange = callbacks::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MainScreenContent(
    state: MainScreenUiState,
    catalogStatus: MainScreenCatalogStatus,
    callbacks: MainScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val sceneGroupTree = remember(state.scenes) {
        buildSceneGroupTree(state.scenes)
    }
    val listItems = remember(sceneGroupTree, state.expandedGroups) {
        val (ungroupedScenes, rootGroups) = sceneGroupTree
        buildMainScreenListItems(
            ungroupedScenes = ungroupedScenes,
            rootGroups = rootGroups,
            expandedGroups = state.expandedGroups,
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
        ) { index, item ->
            when (item) {
                is MainScreenListItem.SceneItem -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (item.depth * 12).dp)
                            .staggeredAppear(index = index),
                    ) {
                        if (item.depth == 0) {
                            ComposiumSceneCard(
                                name = item.entry.scene.name,
                                group = item.entry.scene.group,
                                onClick = { callbacks.onSceneSelected(item.entry.id) },
                            )
                        } else {
                            ComposiumSceneRow(
                                name = item.entry.scene.name,
                                onClick = { callbacks.onSceneSelected(item.entry.id) },
                            )
                        }
                    }
                }

                is MainScreenListItem.GroupHeader -> {
                    GroupHeaderRow(
                        name = item.name,
                        depth = item.depth,
                        scenesCount = item.scenesCount,
                        expanded = state.expandedGroups.contains(item.path),
                        onToggled = { callbacks.onGroupToggled(item.path) },
                        modifier = Modifier.staggeredAppear(index = index),
                    )
                }
            }
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
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
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
                painter = painterResource(
                    if (status.mode == MainScreenCatalogMode.EmptyCatalog) {
                        R.drawable.ic_composium_brand
                    } else {
                        R.drawable.ic_composium_scene
                    }
                ),
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp)
            .clip(Tokens.shapes.medium)
            .background(Tokens.colors.background.copy(alpha = if (expanded) 0.06f else 0f))
            .clickable(onClick = onToggled)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotation },
            ) {
                ComposiumIcon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse group" else "Expand group",
                    tint = Tokens.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            ComposiumText(
                text = name,
                style = Tokens.typography.titleMedium,
                color = Tokens.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ComposiumBadge(
            text = scenesCount.toString(),
            containerColor = Tokens.colors.surfaceVariant,
            contentColor = Tokens.colors.onSurfaceVariant,
            compact = true,
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
    ) : MainScreenListItem {
        override val key: String = "group_$path"
    }

    data class SceneItem(
        val entry: SceneEntry,
        val depth: Int,
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
            ),
        )
    }

    fun SceneGroupNode.totalScenesCount(): Int =
        countCache.getOrPut(path) {
            scenes.size + children.values.sumOf { child -> child.totalScenesCount() }
        }

    fun addGroup(node: SceneGroupNode, depth: Int) {
        items.add(
            MainScreenListItem.GroupHeader(
                name = node.name,
                path = node.path,
                depth = depth,
                scenesCount = node.totalScenesCount(),
            ),
        )

        if (!expandedGroups.contains(node.path)) return

        node.children.values
            .sortedBy { child -> child.name.lowercase() }
            .forEach { child -> addGroup(child, depth + 1) }

        node.scenes.forEach { sceneEntry ->
            items.add(
                MainScreenListItem.SceneItem(
                    entry = sceneEntry,
                    depth = depth + 1,
                ),
            )
        }
    }

    rootGroups
        .sortedBy { group -> group.name.lowercase() }
        .forEach { group -> addGroup(group, depth = 0) }

    return items
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchStoriesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
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

    val iconTint by animateColorAsState(
        targetValue = Tokens.colors.onSurfaceVariant,
        animationSpec = Motion.tweenStandard(),
        label = "search_icon_tint",
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.96f else 0.88f,
        animationSpec = Motion.tweenStandard(),
        label = "search_fill_alpha",
    )

    Box(
        modifier = modifier
            .clip(Tokens.shapes.pill)
            .background(Tokens.colors.surface.copy(alpha = fillAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = iconTint,
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
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        ComposiumText(
                            text = "Search scenes, groups, or states",
                            style = Tokens.typography.bodyMedium,
                            color = Tokens.colors.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(modifier = Modifier.weight(1f)) { inner() }
                        if (value.isNotEmpty()) {
                            ComposiumIconButton(
                                modifier = Modifier.size(20.dp),
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
