package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
class SceneInspectorTabsLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun tallerTabsKeepPreviewClippedAtTheirMiddle() {
        checkTabsLayout(density = 1f)
    }

    @Test
    fun tallerTabsKeepPreviewClippedAtTheirMiddleAtDoubleDensity() {
        checkTabsLayout(density = 2f)
    }

    private fun checkTabsLayout(density: Float) {
        val entry = SceneEntry(
            Scene(null, "Tabs layout", tools = SceneTools.Floating, thumbnail = null) {
                Box(Modifier.fillMaxSize().testTag("preview"))
            },
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density)) {
                ComposiumTheme(darkTheme = false) {
                    SceneScreen(sceneEntry = entry, onBack = {})
                }
            }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        assertSplitBoundary(density)
        composeRule.onNodeWithText("Environment").performClick()
        assertSplitBoundary(density)
        composeRule.onNodeWithContentDescription("Expand settings").performClick()
        assertTabsHeight(density)
        composeRule.onNodeWithContentDescription("Back to split layout").performClick()
        assertSplitBoundary(density)
    }

    private fun assertTabsHeight(density: Float) {
        val tabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(40f * density, tabs.height, 1f)
    }

    private fun assertSplitBoundary(density: Float) {
        assertTabsHeight(density)
        val tabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val preview = composeRule.onNodeWithTag("preview", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("Preview must end at the tabs' center", tabs.center.y, preview.bottom, 1f)
    }
}
