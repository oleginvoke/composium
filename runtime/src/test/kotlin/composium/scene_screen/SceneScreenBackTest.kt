package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneScreenBackTest {
    @get:Rule
    val composeRule = createComposeRule()
    private var closedScenes = 0

    @Test
    fun floatingBackDismissesActiveEyedropperBeforeLeavingScene() {
        renderScene()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Open eyedropper").assertExists()
        assertEquals(0, closedScenes)
    }

    @Test
    fun floatingBackClosesSplitInspectorBeforeLeavingScene() {
        renderScene()
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithContentDescription("Open properties").assertExists()
        assertEquals(0, closedScenes)
    }

    @Test
    fun floatingBackLeavesSceneWhenBothToolsAreClosed() {
        renderScene()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, closedScenes)
    }

    @Test
    fun floatingBackDismissesEyedropperThenInspectorThenScene() {
        renderScene()
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").assertExists()
        composeRule.onNodeWithContentDescription("Expand settings").assertExists()
        assertEquals(0, closedScenes)

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Open properties").assertExists()
        assertEquals(0, closedScenes)
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, closedScenes)
    }

    @Test
    fun expandedInspectorBackRestoresSplitBeforeLeavingScene() {
        renderScene()
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Expand settings").performClick()
        composeRule.onNodeWithContentDescription("Back to split layout").performClick()

        composeRule.onNodeWithContentDescription("Expand settings").assertExists()
        assertEquals(0, closedScenes)
    }

    private fun renderScene() {
        val entry = SceneEntry(Scene(group = null, name = "Back regression", tools = SceneTools.Floating) { padding ->
            Box(Modifier.fillMaxSize().padding(padding))
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneScreen(sceneEntry = entry, onBack = { closedScenes++ })
            }
        }
    }
}
