package oleginvoke.com.composium.scene_screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SceneScreenLogicTest {

    @Test
    fun `calculate scene top bar crossfade state keeps split row on top for split inspector`() {
        val result = calculateSceneTopBarCrossfadeState(
            expandedProgress = 0f,
        )

        assertEquals(0f, result.expandedProgress)
        assertTrue(result.splitInteractive)
        assertFalse(result.expandedInteractive)
        assertTrue(result.splitZIndex > result.expandedZIndex)
    }

    @Test
    fun `calculate scene top bar crossfade state puts expanded row on top for expanded inspector`() {
        val result = calculateSceneTopBarCrossfadeState(
            expandedProgress = 1f,
        )

        assertEquals(1f, result.expandedProgress)
        assertFalse(result.splitInteractive)
        assertTrue(result.expandedInteractive)
        assertTrue(result.expandedZIndex > result.splitZIndex)
    }

    @Test
    fun `scene title island layout centers title when group is missing`() {
        assertEquals(
            SceneTitleIslandLayout.CenteredTitle,
            calculateSceneTitleIslandLayout(group = null),
        )
        assertEquals(
            SceneTitleIslandLayout.CenteredTitle,
            calculateSceneTitleIslandLayout(group = ""),
        )
        assertEquals(
            SceneTitleIslandLayout.CenteredTitle,
            calculateSceneTitleIslandLayout(group = "   "),
        )
    }

    @Test
    fun `scene title island layout keeps two lines when group is present`() {
        assertEquals(
            SceneTitleIslandLayout.GroupAndTitle,
            calculateSceneTitleIslandLayout(group = "Buttons/Primary"),
        )
    }

    @Test
    fun `calculateSceneLayoutMetrics keeps preview full screen when inspector is closed`() {
        val result = calculateSceneLayoutMetrics(
            availableHeightPx = 1000,
            mode = SceneInspectorLayoutMode.Closed,
            splitFraction = DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION,
        )

        assertEquals(1000, result.previewVisibleHeightPx)
        assertEquals(1000, result.previewComposedHeightPx)
        assertEquals(0, result.inspectorHeightPx)
    }

    @Test
    fun `calculateSceneLayoutMetrics keeps preview composed when inspector is expanded`() {
        val result = calculateSceneLayoutMetrics(
            availableHeightPx = 1000,
            mode = SceneInspectorLayoutMode.Expanded,
            splitFraction = DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION,
        )

        assertEquals(0, result.previewVisibleHeightPx)
        assertEquals(1, result.previewComposedHeightPx)
        assertEquals(1000, result.inspectorHeightPx)
    }

    @Test
    fun `calculateSceneLayoutMetrics uses sixty forty split by default`() {
        val result = calculateSceneLayoutMetrics(
            availableHeightPx = 1000,
            mode = SceneInspectorLayoutMode.Split,
            splitFraction = DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION,
        )

        assertEquals(400, result.previewVisibleHeightPx)
        assertEquals(400, result.previewComposedHeightPx)
        assertEquals(600, result.inspectorHeightPx)
    }

    @Test
    fun `split divider is shown only when preview content overflows visible preview area`() {
        assertFalse(
            shouldShowSceneSplitDivider(
                mode = SceneInspectorLayoutMode.Closed,
                hasScrollableOverflow = true,
            ),
        )
        assertFalse(
            shouldShowSceneSplitDivider(
                mode = SceneInspectorLayoutMode.Split,
                hasScrollableOverflow = false,
            ),
        )
        assertTrue(
            shouldShowSceneSplitDivider(
                mode = SceneInspectorLayoutMode.Split,
                hasScrollableOverflow = true,
            ),
        )
        assertFalse(
            shouldShowSceneSplitDivider(
                mode = SceneInspectorLayoutMode.Expanded,
                hasScrollableOverflow = true,
            ),
        )
    }

    @Test
    fun `show controls opens split inspector from closed state`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(),
            intent = SceneScreenIntent.ShowControls,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION, result.controlsSheet.splitFraction)
    }

    @Test
    fun `expand controls promotes split inspector to full inspector`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = 0.66f,
                ),
            ),
            intent = SceneScreenIntent.ExpandControls,
        )

        assertEquals(SceneInspectorLayoutMode.Expanded, result.controlsSheet.layoutMode)
        assertEquals(0.66f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `restore split controls returns expanded inspector to split`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Expanded,
                    splitFraction = 0.66f,
                ),
            ),
            intent = SceneScreenIntent.RestoreSplitControls,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(0.66f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `navigate back from expanded controls restores split inspector`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Expanded,
                    splitFraction = 0.66f,
                ),
            ),
            intent = SceneScreenIntent.NavigateBackFromExpandedControls,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(0.66f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `navigate back from expanded controls is ignored outside expanded mode`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(layoutMode = SceneInspectorLayoutMode.Split),
            ),
            intent = SceneScreenIntent.NavigateBackFromExpandedControls,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
    }

    @Test
    fun `show controls restores default split fraction after closed snap state`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Closed,
                    splitFraction = 0.12f,
                ),
            ),
            intent = SceneScreenIntent.ShowControls,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION, result.controlsSheet.splitFraction)
    }

    @Test
    fun `drag update changes split fraction while inspector is split`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = DEFAULT_SCENE_INSPECTOR_SPLIT_FRACTION,
                ),
            ),
            intent = SceneScreenIntent.UpdateSplitFraction(0.72f),
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(0.72f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `settle split fraction closes inspector when dragged low enough`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = 0.14f,
                ),
            ),
            intent = SceneScreenIntent.SettleSplitFraction,
        )

        assertEquals(SceneInspectorLayoutMode.Closed, result.controlsSheet.layoutMode)
        assertEquals(0.14f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `settle split fraction expands inspector when dragged high enough`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = 0.93f,
                ),
            ),
            intent = SceneScreenIntent.SettleSplitFraction,
        )

        assertEquals(SceneInspectorLayoutMode.Expanded, result.controlsSheet.layoutMode)
        assertEquals(0.93f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `settle split fraction keeps split inspector within snap thresholds`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = 0.68f,
                ),
            ),
            intent = SceneScreenIntent.SettleSplitFraction,
        )

        assertEquals(SceneInspectorLayoutMode.Split, result.controlsSheet.layoutMode)
        assertEquals(0.68f, result.controlsSheet.splitFraction)
    }

    @Test
    fun `preview pane tap closes split inspector`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(
                    layoutMode = SceneInspectorLayoutMode.Split,
                    splitFraction = 0.66f,
                ),
            ),
            intent = SceneScreenIntent.PreviewPaneTapped,
        )

        assertEquals(SceneInspectorLayoutMode.Closed, result.controlsSheet.layoutMode)
    }

    @Test
    fun `hide controls closes expanded inspector`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(
                controlsSheet = ControlsSheetUiState(layoutMode = SceneInspectorLayoutMode.Expanded),
            ),
            intent = SceneScreenIntent.HideControls,
        )

        assertEquals(SceneInspectorLayoutMode.Closed, result.controlsSheet.layoutMode)
    }

    @Test
    fun `select tab changes active inspector tab`() {
        val result = reduceSceneScreen(
            state = SceneScreenState(),
            intent = SceneScreenIntent.SelectTab(SceneInspectorTab.Environment),
        )

        assertEquals(SceneInspectorTab.Environment, result.controlsSheet.selectedTab)
    }

    @Test
    fun `tab transition direction is positive when moving to the right`() {
        assertEquals(
            1,
            SceneInspectorTab.Properties.transitionDirectionTo(SceneInspectorTab.Environment),
        )
    }

    @Test
    fun `tab transition direction is negative when moving to the left`() {
        assertEquals(
            -1,
            SceneInspectorTab.Environment.transitionDirectionTo(SceneInspectorTab.Properties),
        )
    }

    @Test
    fun `tab transition direction is zero when tab does not change`() {
        assertEquals(
            0,
            SceneInspectorTab.Properties.transitionDirectionTo(SceneInspectorTab.Properties),
        )
    }
}
