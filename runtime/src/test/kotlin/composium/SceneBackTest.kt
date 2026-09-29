package oleginvoke.com.composium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.scene_screen.expandInspectorByGesture
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backClosesEyedropperBeforeControlsAndThenScene() {
        var back: () -> Unit = {}
        var closed = 0
        val entry = SceneEntry(Scene(null, "Back order", tools = SceneTools.Floating(actionsInitiallyExpanded = true)) { padding ->
            SideEffect { back = host.onBack }
            Box(Modifier.padding(padding)) { BasicText("Scene content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) { SceneScreen(entry, onBack = { closed++ }) }
        }
        val retained = composeRule.runOnIdle { back }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.runOnIdle { retained() }
        composeRule.onNodeWithContentDescription("Open eyedropper").assertExists()
        composeRule.onNodeWithText("Properties").assertExists()
        composeRule.runOnIdle { assertEquals(0, closed) }

        composeRule.runOnIdle { retained() }
        composeRule.onNodeWithText("Properties").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, closed) }

        composeRule.runOnIdle {
            retained()
            assertEquals(1, closed)
        }
    }

    @Test
    fun backRestoresSplitControlsBeforeClosingThem() {
        var back: () -> Unit = {}
        var closed = 0
        val entry = SceneEntry(Scene(null, "Expanded back", tools = SceneTools.Floating(actionsInitiallyExpanded = true)) { padding ->
            SideEffect { back = host.onBack }
            Box(Modifier.padding(padding)) { BasicText("Scene content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) { SceneScreen(entry, onBack = { closed++ }) }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.expandInspectorByGesture()
        composeRule.onNodeWithContentDescription("Back to split layout").assertExists()
        composeRule.runOnIdle { back() }
        composeRule.onNodeWithContentDescription("Back to split layout").assertDoesNotExist()
        composeRule.onNodeWithText("Properties").assertExists()
        composeRule.runOnIdle { assertEquals(0, closed) }
        composeRule.runOnIdle { back() }
        composeRule.onNodeWithText("Properties").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, closed)
            back()
            assertEquals(1, closed)
        }
    }

    @Test
    fun retainedBackUsesLatestHandlerAndExpiresOnDisposal() {
        var mounted by mutableStateOf(true)
        var useNewHandler by mutableStateOf(false)
        var oldCalls = 0
        var newCalls = 0
        var back: () -> Unit = {}
        val entry = SceneEntry(Scene(null, "Back lifetime", tools = SceneTools.Floating(actionsInitiallyExpanded = true)) { padding ->
            SideEffect { back = host.onBack }
            Box(Modifier.padding(padding)) { BasicText("Scene content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                if (mounted) {
                    SceneScreen(entry, onBack = if (useNewHandler) { { newCalls++ } } else { { oldCalls++ } })
                }
            }
        }
        val retained = composeRule.runOnIdle { back }
        composeRule.runOnIdle { useNewHandler = true }
        composeRule.runOnIdle {
            assertSame(retained, back)
            retained()
            assertEquals(0, oldCalls)
            assertEquals(1, newCalls)
            mounted = false
        }
        composeRule.runOnIdle {
            retained()
            assertEquals(1, newCalls)
            mounted = true
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.runOnIdle { retained() }
        composeRule.onNodeWithText("Properties").assertExists()
        composeRule.runOnIdle { back() }
        composeRule.onNodeWithText("Properties").assertDoesNotExist()
        composeRule.runOnIdle {
            retained()
            assertEquals(1, newCalls)
            back()
            assertEquals(2, newCalls)
        }
    }
}
