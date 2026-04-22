package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope

internal enum class SceneInspectorLayoutMode {
    Closed,
    Split,
    Expanded,
}

internal enum class SceneInspectorTab {
    Properties,
    Environment,
}

internal data class ControlsSheetUiState(
    val layoutMode: SceneInspectorLayoutMode = SceneInspectorLayoutMode.Closed,
    val selectedTab: SceneInspectorTab = SceneInspectorTab.Properties,
    val splitFraction: Float = DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION,
) {
    val isVisible: Boolean
        get() = layoutMode != SceneInspectorLayoutMode.Closed
}

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
    data object ExpandControls : SceneScreenIntent
    data object RestoreSplitControls : SceneScreenIntent
    data object NavigateBackFromExpandedControls : SceneScreenIntent
    data class UpdateSplitFraction(val fraction: Float) : SceneScreenIntent
    data object SettleSplitFraction : SceneScreenIntent
    data object PreviewPaneTapped : SceneScreenIntent
    data class SelectTab(val tab: SceneInspectorTab) : SceneScreenIntent
}

internal interface SceneScreenCallbacks {
    fun onBack()
    fun onToggleControls()
    fun onDismissControls()
    fun onExpandControls()
    fun onRestoreSplitControls()
    fun onNavigateBackFromExpandedControls()
    fun onUpdateSplitFraction(fraction: Float)
    fun onSettleSplitFraction()
    fun onPreviewPaneTapped()
    fun onTabSelected(tab: SceneInspectorTab)
    fun onThemeChange(isDarkTheme: Boolean)
}
