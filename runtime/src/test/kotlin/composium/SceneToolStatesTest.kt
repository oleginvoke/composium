package oleginvoke.com.composium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import oleginvoke.com.composium.ui.theme.ComposiumThemeController
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneToolStatesTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun noneHidesBuiltInToolsAndPreservesSystemPaddingEvenWhenControlsExpand() {
        lateinit var scope: SceneScope
        var closed = 0
        val entry = SceneEntry(Scene(null, "Custom tools", tools = SceneTools.None) { padding ->
            SideEffect { scope = this }
            Box(Modifier.fillMaxSize().testTag("preview")) {
                Box(Modifier.fillMaxSize().padding(padding).testTag("safe content"))
            }
        })
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ComposiumTheme(darkTheme = false) {
                    SceneScreen(entry, onBack = { closed++ }, contentWindowInsets = WindowInsets(top = 24, bottom = 32))
                }
            }
        }
        val preview = composeRule.onNodeWithTag("preview").fetchSemanticsNode().boundsInRoot
        val safe = composeRule.onNodeWithTag("safe content").fetchSemanticsNode().boundsInRoot
        assertEquals(24f, safe.top - preview.top)
        assertEquals(32f, preview.bottom - safe.bottom)
        composeRule.onNodeWithContentDescription("Open properties").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Hide actions").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()

        composeRule.runOnIdle { scope.controlsState.show() }
        val tabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        composeRule.onRoot().performTouchInput { swipe(tabs.center, Offset(tabs.center.x, 0f), 600) }
        composeRule.onNodeWithContentDescription("Back to split layout").assertDoesNotExist()
        val expandedTabs = composeRule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertTrue(expandedTabs.top < 60f, "Fullscreen controls must not reserve space for a hidden top bar")
        composeRule.runOnIdle {
            scope.controlsState.show()
            scope.eyedropperState.show()
            assertFalse(scope.eyedropperState.isVisible)
        }
        composeRule.runOnIdle { scope.onBack() }
        composeRule.onNode(hasText("Properties") and hasClickAction()).assertExists()
        composeRule.runOnIdle {
            assertTrue(scope.controlsState.isVisible)
            assertEquals(0, closed)
            scope.onBack()
        }
        composeRule.onNode(hasText("Properties") and hasClickAction()).assertDoesNotExist()
        composeRule.runOnIdle {
            scope.onBack()
            assertEquals(1, closed)
        }
    }

    @Test
    fun commandsAndBuiltInButtonsShareObservableState() {
        lateinit var scope: SceneScope
        val entry = SceneEntry(Scene(null, "Shared tools", tools = SceneTools.Floating(initiallyExpanded = true)) { padding ->
            SideEffect { scope = this }
            BasicText("controls=${controlsState.isVisible};eye=${eyedropperState.isVisible}", Modifier.padding(padding))
        })
        composeRule.setContent { ComposiumTheme(false) { SceneScreen(entry, onBack = {}) } }
        val controls = composeRule.runOnIdle { scope.controlsState }
        val eyedropper = composeRule.runOnIdle { scope.eyedropperState }
        composeRule.runOnIdle {
            controls.show()
            controls.show()
            eyedropper.show()
            eyedropper.show()
        }
        composeRule.onNodeWithText("controls=true;eye=true").assertExists()
        composeRule.runOnIdle { scope.onBack() }
        composeRule.onNodeWithText("controls=true;eye=false").assertExists()
        composeRule.onNodeWithContentDescription("Close properties").performClick()
        composeRule.onNodeWithText("controls=false;eye=false").assertExists()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithText("controls=false;eye=true").assertExists()
        composeRule.runOnIdle {
            eyedropper.hide()
            eyedropper.hide()
            controls.toggle()
            controls.toggle()
            eyedropper.toggle()
            eyedropper.toggle()
            assertFalse(controls.isVisible)
            assertFalse(eyedropper.isVisible)
            assertSame(controls, scope.controlsState)
            assertSame(eyedropper, scope.eyedropperState)
            controls.show()
        }
        composeRule.onNodeWithText("controls=true;eye=false").assertExists()
        composeRule.runOnIdle { controls.hide() }
        composeRule.onNodeWithText("controls=false;eye=false").assertExists()
    }

    @Test
    fun controlledThemeRequestsChangesWithoutOverridingTheHost() {
        var dark by mutableStateOf(false)
        var newHandler by mutableStateOf(false)
        val oldRequests = mutableListOf<Boolean>()
        val newRequests = mutableListOf<Boolean>()
        lateinit var scope: SceneScope
        val scene = Scene(null, "Controlled theme", tools = SceneTools.None, thumbnail = null) { padding ->
            SideEffect { scope = this }
            BasicText("dark=${themeState.isDark}", Modifier.padding(padding))
        }
        ComposiumRuntime.registerAll(scene)
        composeRule.setContent {
            ComposiumScreen(
                isDarkTheme = dark,
                contentWindowInsets = WindowInsets(0),
                onThemeChange = if (newHandler) { { newRequests.add(it) } } else { { oldRequests.add(it) } },
            )
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Controlled theme"))
        composeRule.onNodeWithText("Controlled theme").performClick()
        val theme = composeRule.runOnIdle { scope.themeState }
        composeRule.runOnIdle {
            theme.toggle()
            assertEquals(listOf(true), oldRequests)
            assertFalse(theme.isDark)
            dark = true
            newHandler = true
        }
        composeRule.onNodeWithText("dark=true").assertExists()
        composeRule.runOnIdle {
            assertSame(theme, scope.themeState)
            theme.toggle()
            theme.setDark(true)
            assertEquals(listOf(false, true), newRequests)
            assertEquals(listOf(true), oldRequests)
            assertTrue(theme.isDark)
            dark = false
        }
        composeRule.onNodeWithText("dark=false").assertExists()
    }

    @Test
    fun uncontrolledThemeUpdatesThroughComposiumScreen() {
        val original = oleginvoke.com.composium.ui.theme.isDarkTheme.value
        try {
            oleginvoke.com.composium.ui.theme.isDarkTheme.value = false
            lateinit var scope: SceneScope
            val requests = mutableListOf<Boolean>()
            val scene = Scene(null, "Internal theme", tools = SceneTools.None, thumbnail = null) { padding ->
                SideEffect { scope = this }
                BasicText("dark=${themeState.isDark}", Modifier.padding(padding))
            }
            ComposiumRuntime.registerAll(scene)
            composeRule.setContent {
                ComposiumScreen(contentWindowInsets = WindowInsets(0), onThemeChange = { requests.add(it) })
            }
            composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Internal theme"))
            composeRule.onNodeWithText("Internal theme").performClick()
            composeRule.runOnIdle { scope.themeState.toggle() }
            composeRule.onNodeWithText("dark=true").assertExists()
            composeRule.runOnIdle {
                assertEquals(listOf(true), requests)
                scope.themeState.setDark(false)
            }
            composeRule.onNodeWithText("dark=false").assertExists()
        } finally {
            oleginvoke.com.composium.ui.theme.isDarkTheme.value = original
        }
    }

    @Test
    fun retainedStatesCannotChangeANewOpeningOrItsTheme() {
        var mounted by mutableStateOf(true)
        var themeRequests = 0
        lateinit var scope: SceneScope
        val entry = SceneEntry(Scene(null, "State lifetime", tools = SceneTools.None) { padding ->
            SideEffect { scope = this }
            BasicText("Scene content", Modifier.padding(padding))
        })
        composeRule.setContent {
            ComposiumTheme(false) {
                CompositionLocalProvider(LocalComposiumThemeController provides ComposiumThemeController(false) { themeRequests++ }) {
                    if (mounted) SceneScreen(entry, onBack = {})
                }
            }
        }
        val old = composeRule.runOnIdle { scope }
        composeRule.runOnIdle {
            old.controlsState.show()
            old.eyedropperState.show()
            mounted = false
        }
        composeRule.runOnIdle {
            assertFalse(old.controlsState.isVisible)
            assertFalse(old.eyedropperState.isVisible)
            mounted = true
        }
        composeRule.runOnIdle {
            old.controlsState.show()
            old.controlsState.toggle()
            old.eyedropperState.show()
            old.eyedropperState.toggle()
            old.themeState.setDark(true)
            old.themeState.toggle()
            assertFalse(scope.controlsState.isVisible)
            assertFalse(scope.eyedropperState.isVisible)
            assertEquals(0, themeRequests)
            scope.controlsState.show()
            scope.eyedropperState.show()
            old.controlsState.hide()
            old.eyedropperState.hide()
            assertTrue(scope.controlsState.isVisible)
            assertTrue(scope.eyedropperState.isVisible)
        }
    }
}
