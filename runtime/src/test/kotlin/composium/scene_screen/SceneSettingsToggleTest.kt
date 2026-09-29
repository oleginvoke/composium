package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
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
class SceneSettingsToggleTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun topBarSettingsTogglePanelWithoutLeavingScene() = checkToggle(SceneTools.TopBar)

    @Test
    fun floatingSettingsTogglePanelWithoutLeavingScene() = checkToggle(SceneTools.Floating(actionsInitiallyExpanded = true))

    private fun checkToggle(tools: SceneTools) {
        var closed = 0
        val entry = SceneEntry(Scene(null, "Toggle", tools = tools) { padding ->
            Box(Modifier.fillMaxSize().padding(padding))
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) { SceneScreen(entry, onBack = { closed++ }) }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNode(hasText("Environment") and hasClickAction()).performClick()
        composeRule.onNodeWithContentDescription("Close properties").performClick()
        composeRule.onNode(hasText("Environment") and hasClickAction()).assertDoesNotExist()
        composeRule.onNode(hasContentDescription("Back to split layout") and isEnabled()).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, closed) }

        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNode(hasText("Environment") and hasClickAction()).assertExists()
        composeRule.onNode(hasContentDescription("Back to split layout") and isEnabled()).assertDoesNotExist()
        composeRule.expandInspectorByGesture()
        composeRule.onNodeWithContentDescription("Back to split layout").performClick()
        composeRule.onNodeWithContentDescription("Close properties").assertExists()
        composeRule.onNode(hasContentDescription("Back to split layout") and isEnabled()).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, closed) }
    }
}
