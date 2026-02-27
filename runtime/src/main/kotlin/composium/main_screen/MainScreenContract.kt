package oleginvoke.com.composium.main_screen

import oleginvoke.com.composium.SceneEntry

internal data class MainScreenState(
    val query: String = "",
    val expandedGroups: Set<String> = emptySet(),
)

internal data class MainScreenUiState(
    val query: String,
    val scenes: List<SceneEntry>,
    val expandedGroups: Set<String>,
    val isDarkTheme: Boolean,
)

internal sealed interface MainScreenIntent {
    data class QueryChanged(val query: String) : MainScreenIntent
    data class GroupToggled(val group: String) : MainScreenIntent
}

internal interface MainScreenCallbacks {
    fun onQueryChange(query: String)
    fun onSceneSelected(sceneId: String)
    fun onGroupToggled(group: String)
    fun onThemeChange(isDarkTheme: Boolean)
}
