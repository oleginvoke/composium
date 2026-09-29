package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneApiTest {
    @Test
    fun sceneUsesTopBarToolsByDefault() {
        val content: @Composable SceneScope.(PaddingValues) -> Unit = { padding ->
            padding.calculateTopPadding()
        }
        val scene = Scene(group = null, name = "Default", content = content)

        assertEquals(SceneTools.TopBar, scene.tools)
        assertEquals(content, scene.content)
    }

    @Test
    fun sceneStoresFloatingToolsPresentation() {
        val tools = SceneTools.Floating(FloatingToolsPosition.CenterStart)
        val scene = Scene(
            group = null,
            name = "Fullscreen",
            tools = tools,
            content = { padding -> padding.calculateBottomPadding() },
        )

        assertEquals(tools, scene.tools)
    }
}
