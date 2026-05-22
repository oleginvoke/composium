package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneInspectorLayoutTest {

    @Test
    fun splitInspectorContentClipsAtTabsMiddle() {
        val offset = calculateSceneInspectorContentClipOffset(
            layoutMode = SceneInspectorLayoutMode.Split,
            tabsHeight = 34.dp,
            tabsTopPadding = 2.dp,
        )

        assertEquals(19.dp, offset)
    }

    @Test
    fun expandedInspectorContentClipsAtTabsMiddle() {
        val offset = calculateSceneInspectorContentClipOffset(
            layoutMode = SceneInspectorLayoutMode.Expanded,
            tabsHeight = 34.dp,
            tabsTopPadding = 8.dp,
        )

        assertEquals(25.dp, offset)
    }
}
