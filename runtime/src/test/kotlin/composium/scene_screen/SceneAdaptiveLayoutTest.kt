package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1000dp-h1000dp-mdpi")
class SceneAdaptiveLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun inspectorStaysBelowFullWidthPreviewInPortraitLandscapeAndRtl() {
        val wide = mutableStateOf(false)
        val rtl = mutableStateOf(false)
        val entry = entry()
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (rtl.value) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
            ) {
                ComposiumTheme(darkTheme = false) {
                    Box(Modifier.requiredSize(if (wide.value) 1000.dp else 400.dp, if (wide.value) 500.dp else 900.dp)) {
                        SceneScreen(entry, onBack = {}, contentWindowInsets = WindowInsets(0))
                    }
                }
            }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        val portraitPreview = composeRule.onNodeWithTag("preview", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val portraitTabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(400f, portraitPreview.width, 1f)
        assertEquals(portraitTabs.center.y, portraitPreview.bottom, 1f)

        composeRule.runOnIdle { wide.value = true }
        val landscapePreview = composeRule.onNodeWithTag("preview", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val landscapeTabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(1000f, landscapePreview.width, 1f)
        assertEquals(landscapeTabs.center.y, landscapePreview.bottom, 1f)

        composeRule.runOnIdle { rtl.value = true }
        val rtlPreview = composeRule.onNodeWithTag("preview", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val rtlTabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(1000f, rtlPreview.width, 1f)
        assertEquals(rtlTabs.center.y, rtlPreview.bottom, 1f)
    }

    @Test
    fun inspectorAndEnvironmentSurviveSavedStateRestoration() {
        lateinit var scope: SceneScope
        val entry = entry { scope = this }
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            ComposiumTheme(darkTheme = false) { SceneScreen(entry, onBack = {}) }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithText("Environment").performClick()
        composeRule.runOnIdle {
            scope.preview.rtl = true
            scope.preview.fontScaleOverride = 1.5f
            scope.preview.displayScaleOverride = 0.85f
        }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithContentDescription("Close properties").assertExists()
        composeRule.onNodeWithText("Font size").assertExists()
        composeRule.runOnIdle {
            assertEquals(true, scope.preview.rtl)
            assertEquals(1.5f, scope.preview.fontScaleOverride)
            assertEquals(0.85f, scope.preview.displayScaleOverride)
        }
    }

    private fun entry(content: SceneScope.() -> Unit = {}) = SceneEntry(
        Scene(null, "Adaptive scene", tools = SceneTools.TopBar, thumbnail = null) {
            content()
            Box(Modifier.fillMaxSize().testTag("preview"))
        },
    )
}
