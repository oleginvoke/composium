package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneTools
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneContentPaddingTest {
    @Test
    fun topBarPaddingIncludesSystemInsetAndTopBar() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.TopBar,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Closed,
        )

        assertEquals(96.dp, padding.calculateTopPadding())
        assertEquals(32.dp, padding.calculateBottomPadding())
    }

    @Test
    fun floatingPaddingExcludesFloatingSurfaceAndTopBar() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.Floating,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Closed,
        )

        assertEquals(24.dp, padding.calculateTopPadding())
        assertEquals(32.dp, padding.calculateBottomPadding())
    }

    @Test
    fun splitInspectorAlreadyBoundsThePreviewBottom() {
        val padding = calculateSceneContentPadding(
            tools = SceneTools.TopBar,
            statusBarInset = 24.dp,
            navigationBarInset = 32.dp,
            topBarHeight = 72.dp,
            inspectorLayoutMode = SceneInspectorLayoutMode.Split,
        )

        assertEquals(0.dp, padding.calculateBottomPadding())
    }
}
