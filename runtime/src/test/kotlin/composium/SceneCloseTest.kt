package oleginvoke.com.composium

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import oleginvoke.com.composium.host_screen.ComposiumHostScreen
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.scene_screen.expandInspectorByGesture
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailRenderSurface
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneCloseTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun closeSceneBypassesControlsAndEyedropper() {
        var close: () -> Unit = {}
        var closed = 0
        val entry = SceneEntry(Scene(null, "Direct close", tools = SceneTools.Floating(actionsInitiallyExpanded = true)) { padding ->
            SideEffect { close = host::closeScene }
            Box(Modifier.padding(padding)) { BasicText("Scene content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneScreen(entry, onBack = { closed++ })
            }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.runOnIdle {
            close()
            assertEquals(1, closed)
        }
    }

    @Test
    fun closeSceneBypassesExpandedControls() {
        var close: () -> Unit = {}
        var closed = 0
        val entry = SceneEntry(Scene(null, "Expanded close", tools = SceneTools.Floating(actionsInitiallyExpanded = true)) { padding ->
            SideEffect { close = host::closeScene }
            Box(Modifier.padding(padding)) { BasicText("Scene content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneScreen(entry, onBack = { closed++ })
            }
        }
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.expandInspectorByGesture()
        composeRule.runOnIdle {
            close()
            assertEquals(1, closed)
        }
    }

    @Test
    fun retainedCloseUsesCurrentCallbackAndExpiresWhenSceneLeavesComposition() {
        var mounted by mutableStateOf(true)
        var useNewHandler by mutableStateOf(false)
        var oldHandlerCalls = 0
        var newHandlerCalls = 0
        var close: () -> Unit = {}
        val entry = SceneEntry(Scene(null, "Lifetime") { padding ->
            SideEffect { close = host::closeScene }
            Box(Modifier.padding(padding)) { BasicText("Mounted scene") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                if (mounted) {
                    SceneScreen(entry, onBack = if (useNewHandler) {
                        { newHandlerCalls++ }
                    } else {
                        { oldHandlerCalls++ }
                    })
                }
            }
        }
        val retained = composeRule.runOnIdle { close }
        composeRule.runOnIdle { useNewHandler = true }
        composeRule.runOnIdle {
            retained()
            assertEquals(0, oldHandlerCalls)
            assertEquals(1, newHandlerCalls)
            mounted = false
        }
        composeRule.runOnIdle {
            retained()
            assertEquals(1, newHandlerCalls)
            mounted = true
        }
        composeRule.runOnIdle {
            retained()
            assertEquals(1, newHandlerCalls, "Old scope must not close a new opening of the same scene")
            close()
            assertEquals(2, newHandlerCalls)
        }
    }

    @Test
    fun previewAndThumbnailCannotCloseAnOpenScene() {
        var closed = 0
        var isolatedEffects = 0
        val isolated = Scene(null, "Isolated") { padding ->
            LaunchedEffect(Unit) {
                host.closeScene()
                host.onBack()
                host.controls.show()
                host.controls.toggle()
                host.controls.hide()
                host.eyedropper.show()
                host.eyedropper.toggle()
                host.eyedropper.hide()
                host.theme.setDark(true)
                host.theme.toggle()
                assertEquals(false, host.controls.isVisible)
                assertEquals(false, host.eyedropper.isVisible)
                assertEquals(false, host.theme.isDark)
                isolatedEffects++
            }
            Box(Modifier.padding(padding)) { BasicText("Isolated content") }
        }
        val live = SceneEntry(Scene(null, "Live") { padding ->
            Box(Modifier.padding(padding)) { BasicText("Live content") }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Column {
                    Box(Modifier.weight(1f)) { SceneScreen(live, onBack = { closed++ }) }
                    Box(Modifier.weight(1f)) { isolated.RenderPreview() }
                    Box(Modifier.weight(1f)) {
                        SceneThumbnailRenderSurface(SceneEntry(isolated), remember { SceneScope() })
                    }
                }
            }
        }
        composeRule.runOnIdle {
            assertEquals(2, isolatedEffects)
            assertEquals(0, closed)
        }
    }

    @Test
    fun customBackReturnsToCatalogAndStaleCallbackCannotCloseReopenedScene() {
        var close: () -> Unit = {}
        var back: () -> Unit = {}
        val scene = Scene(null, "Custom back example", thumbnail = null) { padding ->
            SideEffect {
                close = host::closeScene
                back = host.onBack
            }
            BasicText("My back button", Modifier.padding(padding).clickable { host.closeScene() })
        }
        ComposiumRuntime.registerAll(scene)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                ComposiumHostScreen(contentWindowInsets = WindowInsets(0))
            }
        }
        composeRule.onNodeWithText("Custom back example").performClick()
        val retained = composeRule.runOnIdle { close }
        val retainedBack = composeRule.runOnIdle { back }
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { close() }
        composeRule.mainClock.advanceTimeByFrame()
        // Accessibility actions can reach the catalog despite the pointer blocker.
        composeRule.onNode(hasText("Custom back example") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithText("My back button").assertDoesNotExist()
        composeRule.onNodeWithText("Custom back example").performClick()
        composeRule.runOnIdle {
            retained()
            retainedBack()
        }
        composeRule.onNodeWithText("My back button").assertExists().performClick()
        composeRule.onNodeWithText("My back button").assertDoesNotExist()
    }
}
