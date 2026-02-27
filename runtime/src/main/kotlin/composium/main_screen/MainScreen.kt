package oleginvoke.com.composium.main_screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.ui.components.ComposiumBadge
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumScaffold
import oleginvoke.com.composium.ui.components.ComposiumSurface
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun MainScreen(
    scenes: List<SceneEntry>,
    onSceneSelected: (sceneId: String) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets? = null,
) {
    val themeController = LocalComposiumThemeController.current
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

    ComposiumScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MainScreenTopBar(
                state = uiState,
                callbacks = callbacks,
                statusBarInsets = contentWindowInsets,
            )
        },
    ) { padding ->
        MainScreenContent(
            state = uiState,
            callbacks = callbacks,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
private fun MainScreenTopBar(
    state: MainScreenUiState,
    callbacks: MainScreenCallbacks,
    statusBarInsets: WindowInsets? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (statusBarInsets != null) Modifier.windowInsetsPadding(statusBarInsets) else Modifier)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposiumIcon(
                    imageVector = Icons.Outlined.ViewInAr,
                    contentDescription = null,
                    tint = Tokens.colors.primary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(12.dp))
                ComposiumText(
                    modifier = Modifier.weight(1f),
                    text = "Composium",
                    style = Tokens.typography.headlineMedium,
                    color = Tokens.colors.onSurface,
                )
                AnimatedContent(targetState = state.isDarkTheme) { isDark ->
                    if (isDark) {
                        ComposiumIconButton(
                            onClick = { callbacks.onThemeChange(false) },
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Outlined.WbSunny,
                                contentDescription = "Change theme",
                                tint = Color.Yellow,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    } else {
                        ComposiumIconButton(
                            onClick = { callbacks.onThemeChange(true) },
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Outlined.NightsStay,
                                contentDescription = "Change theme",
                                tint = Color.Blue,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SearchStoriesField(
                value = state.query,
                onValueChange = callbacks::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ComposiumText(
                    text = "Scenes",
                    style = Tokens.typography.headlineSmall,
                    color = Tokens.colors.onSurface,
                )
                ComposiumBadge(
                    text = state.scenes.size.toString(),
                    containerColor = Tokens.colors.primaryContainer,
                    contentColor = Tokens.colors.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MainScreenContent(
    state: MainScreenUiState,
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

    @Composable
    fun SceneRow(entry: SceneEntry, depth: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 14).dp)
                .clip(Tokens.shapes.medium)
                .clickable { callbacks.onSceneSelected(entry.id) }
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.ViewInAr,
                contentDescription = null,
                tint = Tokens.colors.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            ComposiumText(
                text = entry.scene.name,
                style = Tokens.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Tokens.colors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = listItems,
            key = { item -> item.key },
        ) { item ->
            when (item) {
                is MainScreenListItem.SceneItem -> {
                    SceneRow(entry = item.entry, depth = item.depth)
                }

                is MainScreenListItem.GroupHeader -> {
                    val expanded = state.expandedGroups.contains(item.path)
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 0f else -90f,
                        animationSpec = tween(220),
                        label = "chevron",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (item.depth * 14).dp)
                            .clip(Tokens.shapes.medium)
                            .clickable { callbacks.onGroupToggled(item.path) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = if (expanded) "Collapse group" else "Expand group",
                                tint = Tokens.colors.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = rotation },
                            )
                            Spacer(Modifier.width(8.dp))
                            ComposiumText(
                                text = item.name,
                                style = Tokens.typography.titleMedium,
                                color = Tokens.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ComposiumBadge(
                            text = item.scenesCount.toString(),
                            containerColor = Tokens.colors.primaryContainer,
                            contentColor = Tokens.colors.onPrimaryContainer,
                            compact = true,
                        )
                    }
                }
            }
        }
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

@Composable
private fun SearchStoriesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.5.dp else 1.5.dp,
        animationSpec = tween(180),
        label = "search_border",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Tokens.colors.primary else Tokens.colors.outlineVariant,
        animationSpec = tween(180),
        label = "search_border_color",
    )
    ComposiumSurface(
        modifier = modifier,
        shape = Tokens.shapes.pill,
        color = Tokens.colors.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = if (isFocused) Tokens.colors.primary else Tokens.colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
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
                            text = "Search Scenes...",
                            style = Tokens.typography.titleMedium,
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
