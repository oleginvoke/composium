package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope

internal enum class ControlsSheetTab {
    Properties,
    SystemParams,
}

internal data class ControlsSheetUiState(
    val isVisible: Boolean = false,
    val selectedTab: ControlsSheetTab = ControlsSheetTab.Properties,
)

internal data class SceneScreenState(
    val controlsSheet: ControlsSheetUiState = ControlsSheetUiState(),
)

internal data class SceneScreenUiState(
    val sceneEntry: SceneEntry,
    val sceneScope: SceneScope,
    val paramsState: SceneParamsState,
    val controlsSheet: ControlsSheetUiState,
    val isDarkTheme: Boolean,
)

internal sealed interface SceneScreenIntent {
    data object ShowControls : SceneScreenIntent
    data object HideControls : SceneScreenIntent
    data class ControlsTabChanged(val tab: ControlsSheetTab) : SceneScreenIntent
}

internal interface SceneScreenCallbacks {
    fun onBack()
    fun onShowParams()
    fun onDismissControls()
    fun onThemeChange(isDarkTheme: Boolean)
    fun onControlsTabChanged(tab: ControlsSheetTab)
}
