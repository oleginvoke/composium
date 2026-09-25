package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneFloatingToolsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun collapsedMenuKeepsSettingsAndBothAnchorsRemainFixedWhenOpening() {
        renderTools()
        val settings = bounds("Open properties")
        val more = bounds("Show actions")
        composeRule.onNodeWithContentDescription("Open properties")
            .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithContentDescription("Show actions")
            .assertWidthIsEqualTo(44.dp).assertHeightIsEqualTo(44.dp)
        assertTrue(settings.bottom < more.top)
        assertEquals(settings.center.x, more.center.x, 0.1f)
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Show actions").performClick()
        composeRule.onNodeWithContentDescription("Hide actions").assertIsSelected()
        assertEquals(more, bounds("Hide actions"))
        assertEquals(settings, bounds("Open properties"))
        val fan = listOf("Open eyedropper", "Switch to dark theme", "Back").map(::bounds)
        listOf("Open eyedropper", "Switch to dark theme", "Back").forEach { description ->
            composeRule.onNodeWithContentDescription(description)
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        }
        fan.forEach {
            assertEquals(with(composeRule.density) { 54.dp.toPx() }, (it.center - more.center).getDistance(), 1f)
        }
        fan.zipWithNext().forEach { (above, below) -> assertTrue(above.center.y < below.center.y) }
        val buttons = fan + settings + more
        buttons.forEachIndexed { index, button -> buttons.drop(index + 1).forEach {
            assertTrue("Round buttons must not overlap", (button.center - it.center).getDistance() >= (button.width + it.width) / 2f)
        } }
        composeRule.onNodeWithContentDescription("Hide actions").performClick()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        assertEquals(settings, bounds("Open properties"))
        assertEquals(more, bounds("Show actions"))
    }

    @Test
    fun toolActionsRemainOpenAndDispatchTheirOwnCallbacks() {
        var backClicks = 0
        renderTools(onBack = { backClicks++ })
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Close properties").assertIsSelected()
        composeRule.onNodeWithContentDescription("Show actions").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithContentDescription("Close eyedropper").assertIsSelected().performClick()
        composeRule.onNodeWithContentDescription("Switch to dark theme").performClick()
        composeRule.onNodeWithContentDescription("Switch to light theme").assertExists()
        composeRule.onNodeWithContentDescription("Hide actions").assertIsSelected()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backClicks)
    }

    @Test
    fun adjacentSettingsAndEyedropperReceiveTheirOwnPhysicalTaps() {
        renderTools()
        composeRule.onNodeWithContentDescription("Show actions").performClick()
        val settings = bounds("Open properties")
        val eyedropper = bounds("Open eyedropper")
        val inset = with(composeRule.density) { 4.dp.toPx() }
        composeRule.onNodeWithTag("root").performTouchInput {
            click(Offset(settings.left + inset, settings.center.y))
        }
        composeRule.onNodeWithContentDescription("Close properties").assertIsSelected()
        composeRule.onNodeWithContentDescription("Open eyedropper").assertExists()
        composeRule.onNodeWithTag("root").performTouchInput {
            click(Offset(eyedropper.right - inset, eyedropper.center.y))
        }
        composeRule.onNodeWithContentDescription("Close eyedropper").assertIsSelected()
        composeRule.onNodeWithContentDescription("Close properties").assertIsSelected()
        composeRule.onNodeWithContentDescription("Hide actions").assertIsSelected()
    }

    @Test
    fun gapAndFormerFanAreaLetSceneReceiveClicks() {
        var sceneClicks = 0
        renderTools(onSceneClick = { sceneClicks++ })
        val settings = bounds("Open properties")
        val more = bounds("Show actions")
        composeRule.onNodeWithTag("root").performTouchInput {
            click(Offset(settings.center.x, (settings.bottom + more.top) / 2f))
        }
        assertEquals(1, sceneClicks)
        composeRule.onNodeWithContentDescription("Show actions").performClick()
        val action = bounds("Back").center
        composeRule.onNodeWithContentDescription("Hide actions").performClick()
        composeRule.onNodeWithTag("root").performTouchInput { click(action) }
        assertEquals(2, sceneClicks)
    }

    private fun renderTools(onBack: () -> Unit = {}, onSceneClick: () -> Unit = {}) {
        composeRule.setContent {
            var minimized by remember { mutableStateOf(true) }
            var controls by remember { mutableStateOf(false) }
            var eyedropper by remember { mutableStateOf(false) }
            var dark by remember { mutableStateOf(false) }
            ComposiumTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().testTag("root")) {
                    Box(Modifier.fillMaxSize().clickable(onClick = onSceneClick))
                    SceneFloatingTools(
                        controlsLayout = if (controls) SceneInspectorLayoutMode.Split else SceneInspectorLayoutMode.Closed,
                        isMinimized = minimized, isDarkTheme = dark, isEyedropperVisible = eyedropper,
                        onBack = onBack, onToggleControls = { controls = !controls },
                        onToggleEyedropper = { eyedropper = !eyedropper }, onThemeChange = { dark = it },
                        onMinimize = { minimized = true }, onShow = { minimized = false },
                    )
                }
            }
        }
    }

    private fun bounds(description: String) = composeRule.onNodeWithContentDescription(description)
        .fetchSemanticsNode().boundsInRoot
}
