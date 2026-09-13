package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneInspectorLayoutTest {

    @Test
    fun splitInspectorContentClipsAtTabsMiddle() {
        val offset = calculateSceneInspectorContentClipOffset(
            layoutMode = SceneInspectorLayoutMode.Split,
            tabsHeight = 40.dp,
            tabsTopPadding = 2.dp,
        )

        assertEquals(22.dp, offset)
    }

    @Test
    fun expandedInspectorContentClipsAtTabsMiddle() {
        val offset = calculateSceneInspectorContentClipOffset(
            layoutMode = SceneInspectorLayoutMode.Expanded,
            tabsHeight = 40.dp,
            tabsTopPadding = 8.dp,
        )

        assertEquals(28.dp, offset)
    }
}
