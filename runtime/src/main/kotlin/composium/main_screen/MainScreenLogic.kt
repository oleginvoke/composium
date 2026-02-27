package oleginvoke.com.composium.main_screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import oleginvoke.com.composium.SceneEntry

internal data class SceneSearchIndexItem(
    val entry: SceneEntry,
    val nameLowercase: String,
    val groupLowercase: String,
    val groupPathPrefixes: List<String>,
)

internal fun buildSceneSearchIndex(
    scenes: List<SceneEntry>,
): List<SceneSearchIndexItem> {
    return scenes.map { entry ->
        SceneSearchIndexItem(
            entry = entry,
            nameLowercase = entry.scene.name.lowercase(),
            groupLowercase = entry.scene.group?.lowercase().orEmpty(),
            groupPathPrefixes = entry.groupPathPrefixes(),
        )
    }
}

private fun filterSceneIndexItems(
    sceneIndex: List<SceneSearchIndexItem>,
    query: String,
): List<SceneSearchIndexItem> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return sceneIndex

    return sceneIndex.filter { item ->
        item.groupLowercase.contains(normalizedQuery) || item.nameLowercase.contains(normalizedQuery)
    }
}

internal fun filterScenes(
    sceneIndex: List<SceneSearchIndexItem>,
    query: String,
): List<SceneEntry> {
    return filterSceneIndexItems(sceneIndex = sceneIndex, query = query)
        .map { item -> item.entry }
}

internal fun reduceMainScreen(
    state: MainScreenState,
    intent: MainScreenIntent,
    sceneIndex: List<SceneSearchIndexItem>,
): MainScreenState {
    return when (intent) {
        is MainScreenIntent.QueryChanged -> {
            val nextExpandedGroups = if (intent.query.isBlank()) {
                state.expandedGroups
            } else {
                filterSceneIndexItems(sceneIndex = sceneIndex, query = intent.query)
                    .flatMap { item -> item.groupPathPrefixes }
                    .toSet()
            }
            state.copy(
                query = intent.query,
                expandedGroups = nextExpandedGroups,
            )
        }

        is MainScreenIntent.GroupToggled -> {
            val nextGroups = state.expandedGroups.toMutableSet().apply {
                if (contains(intent.group)) remove(intent.group) else add(intent.group)
            }
            state.copy(expandedGroups = nextGroups)
        }
    }
}

private fun SceneEntry.groupPathPrefixes(): List<String> {
    val segments = scene.group
        ?.split("/")
        ?.map { segment -> segment.trim() }
        ?.filter { segment -> segment.isNotEmpty() }
        .orEmpty()

    if (segments.isEmpty()) return emptyList()

    val prefixes = ArrayList<String>(segments.size)
    var currentPath = ""
    segments.forEach { segment ->
        currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
        prefixes.add(currentPath)
    }
    return prefixes
}

@Stable
internal class MainScreenStore(
    private val stateHolder: MutableState<MainScreenState>,
) {
    constructor(initialState: MainScreenState = MainScreenState()) : this(mutableStateOf(initialState))

    var state: MainScreenState
        get() = stateHolder.value
        private set(value) {
            stateHolder.value = value
        }

    fun dispatch(
        intent: MainScreenIntent,
        sceneIndex: List<SceneSearchIndexItem>,
    ) {
        val reducedState = reduceMainScreen(
            state = state,
            intent = intent,
            sceneIndex = sceneIndex,
        )
        if (reducedState != state) {
            state = reducedState
        }
    }
}

private const val QUERY_KEY = "query"
private const val EXPANDED_GROUPS_KEY = "expanded_groups"

private val MainScreenStateSaver = mapSaver(
    save = { state ->
        mapOf(
            QUERY_KEY to state.query,
            EXPANDED_GROUPS_KEY to state.expandedGroups.toList(),
        )
    },
    restore = { restored ->
        MainScreenState(
            query = restored[QUERY_KEY] as? String ?: "",
            expandedGroups = (restored[EXPANDED_GROUPS_KEY] as? List<*>)
                ?.mapNotNull { item -> item as? String }
                ?.toSet()
                ?: emptySet(),
        )
    },
)

@Composable
internal fun rememberMainScreenStore(): MainScreenStore {
    val stateHolder = rememberSaveable(stateSaver = MainScreenStateSaver) {
        mutableStateOf(MainScreenState())
    }
    return remember(stateHolder) { MainScreenStore(stateHolder) }
}
