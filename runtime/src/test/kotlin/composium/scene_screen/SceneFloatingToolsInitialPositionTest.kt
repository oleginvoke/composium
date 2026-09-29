package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import oleginvoke.com.composium.FloatingToolsPosition
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
class SceneFloatingToolsInitialPositionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sixLogicalAnchorsRespectInsetsInBothLayoutDirections() {
        var position by mutableStateOf(FloatingToolsPosition.TopStart)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                ComposiumTheme(false) {
                    val insets = WindowInsets(left = 7, top = 24, right = 20, bottom = 30)
                    Box(Modifier.fillMaxSize().testTag("screen")) {
                        SceneScreen(
                            SceneEntry(Scene(null, "Initial position", tools = SceneTools.Floating(position, actionsInitiallyExpanded = true)) { padding ->
                                Box(Modifier.fillMaxSize().padding(padding))
                            }),
                            onBack = {},
                            contentWindowInsets = insets,
                            modifier = Modifier.windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)),
                        )
                    }
                }
            }
        }
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        // Initial pair is 48 x 96 dp; the fan does not participate in placement bounds.
        val left = screen.left + 7f + 12f
        val right = screen.right - 20f - 12f - 48f
        val top = screen.top + 24f + 12f
        val bottom = screen.bottom - 30f - 12f - 96f
        val center = (top + bottom) / 2f
        for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            val start = if (layoutDirection == LayoutDirection.Ltr) left else right
            val end = if (layoutDirection == LayoutDirection.Ltr) right else left
            val cases = listOf(
                FloatingToolsPosition.TopStart to Offset(start, top),
                FloatingToolsPosition.CenterStart to Offset(start, center),
                FloatingToolsPosition.BottomStart to Offset(start, bottom),
                FloatingToolsPosition.TopEnd to Offset(end, top),
                FloatingToolsPosition.CenterEnd to Offset(end, center),
                FloatingToolsPosition.BottomEnd to Offset(end, bottom),
            )
            cases.forEach { (anchor, expected) ->
                composeRule.runOnIdle { direction = layoutDirection; position = anchor }
                val back = bounds("Open properties")
                assertEquals("$anchor / $layoutDirection x", expected.x, back.left, 1.1f)
                assertEquals("$anchor / $layoutDirection y", expected.y, back.top, 1.1f)
                val eye = bounds("Hide actions")
                composeRule.onNodeWithContentDescription("Hide actions").performClick()
                assertEquals(eye, bounds("Show actions"))
                composeRule.onNodeWithContentDescription("Show actions").performClick()
                assertEquals(eye, bounds("Hide actions"))
            }
        }
    }

    @Test
    fun draggedPositionSurvivesLayoutDirectionChangesButReopeningUsesConfiguredAnchor() {
        var mounted by mutableStateOf(true)
        var revision by mutableStateOf(0)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                ComposiumTheme(false) {
                    if (mounted) {
                        SceneScreen(
                            SceneEntry(Scene(null, "Reopen", tools = SceneTools.Floating(FloatingToolsPosition.BottomStart, actionsInitiallyExpanded = true)) { padding ->
                                Box(Modifier.fillMaxSize().padding(padding).testTag("revision $revision"))
                            }),
                            onBack = {},
                        )
                    }
                }
            }
        }
        val initial = bounds("Hide actions")
        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            down(center)
            moveBy(Offset(40f, -40f))
            up()
        }
        val moved = bounds("Hide actions")
        assertEquals(initial.left + 40f, moved.left, 1.1f)
        assertEquals(initial.top - 40f, moved.top, 1.1f)
        composeRule.runOnIdle { revision++ }
        assertEquals(moved, bounds("Hide actions"))
        composeRule.runOnIdle { direction = LayoutDirection.Rtl }
        assertEquals(moved, bounds("Hide actions"))
        composeRule.runOnIdle { direction = LayoutDirection.Ltr }
        assertEquals(moved, bounds("Hide actions"))
        composeRule.runOnIdle { mounted = false }
        composeRule.runOnIdle { mounted = true }
        assertEquals(initial, bounds("Hide actions"))
    }

    @Test
    fun expansionDefaultAppliesOnlyOnOpeningAndDoesNotOverrideUserState() {
        var actionsInitiallyExpanded by mutableStateOf(false)
        var mounted by mutableStateOf(true)
        composeRule.setContent {
            ComposiumTheme(false) {
                if (mounted) SceneScreen(
                    SceneEntry(Scene(null, "Expansion default", tools = SceneTools.Floating(actionsInitiallyExpanded = actionsInitiallyExpanded)) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding))
                    }), onBack = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Show actions").assertExists()
        composeRule.onNodeWithContentDescription("Open properties").assertExists()
        composeRule.runOnIdle { actionsInitiallyExpanded = true }
        composeRule.onNodeWithContentDescription("Show actions").assertExists()
        composeRule.runOnIdle { mounted = false }
        composeRule.runOnIdle { mounted = true }
        composeRule.onNodeWithContentDescription("Hide actions").assertExists().performClick()
        composeRule.runOnIdle { actionsInitiallyExpanded = false }
        composeRule.onNodeWithContentDescription("Show actions").assertExists()
    }

    private fun bounds(description: String) = composeRule.onNodeWithContentDescription(description)
        .fetchSemanticsNode().boundsInRoot
}
