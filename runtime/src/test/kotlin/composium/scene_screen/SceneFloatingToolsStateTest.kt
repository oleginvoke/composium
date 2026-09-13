package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.SceneTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneFloatingToolsStateTest {
    @Test
    fun floatingToolsStartExpandedAndCanBeMinimizedAndRestored() {
        val initial = SceneScreenState()
        val minimized = reduceSceneScreen(initial, SceneScreenIntent.MinimizeFloatingTools)
        val restored = reduceSceneScreen(minimized, SceneScreenIntent.ShowFloatingTools)

        assertFalse(initial.isFloatingToolsMinimized)
        assertTrue(minimized.isFloatingToolsMinimized)
        assertFalse(restored.isFloatingToolsMinimized)
    }

    @Test
    fun inspectorTransitionsPreserveMinimizedState() {
        val minimized = SceneScreenState(isFloatingToolsMinimized = true)
        val split = reduceSceneScreen(minimized, SceneScreenIntent.ShowControls)
        val expanded = reduceSceneScreen(split, SceneScreenIntent.ExpandControls)
        val restored = reduceSceneScreen(expanded, SceneScreenIntent.NavigateBackFromExpandedControls)

        assertTrue(split.isFloatingToolsMinimized)
        assertTrue(expanded.isFloatingToolsMinimized)
        assertTrue(restored.isFloatingToolsMinimized)
    }

    @Test
    fun floatingToolsAreHiddenOnlyForTopBarScenesAndExpandedInspector() {
        assertTrue(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Closed))
        assertTrue(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Split))
        assertFalse(shouldShowFloatingTools(SceneTools.Floating, SceneInspectorLayoutMode.Expanded))
        assertFalse(shouldShowFloatingTools(SceneTools.TopBar, SceneInspectorLayoutMode.Closed))
    }

    @Test
    fun toggleDescriptionsDescribeTheResultingAction() {
        assertEquals("Hide tools", floatingToolsToggleContentDescription(isMinimized = false))
        assertEquals("Show tools", floatingToolsToggleContentDescription(isMinimized = true))
    }
}
