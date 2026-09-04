package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneFloatingToolsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedToolsExposeAllFourActions() {
        var backClicks = 0
        var propertiesClicks = 0
        var eyedropperClicks = 0
        var requestedDarkTheme: Boolean? = null

        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = { backClicks++ },
                    onToggleControls = { propertiesClicks++ },
                    onToggleEyedropper = { eyedropperClicks++ },
                    onThemeChange = { requestedDarkTheme = it },
                    onMinimize = {},
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithContentDescription("Open eyedropper").performClick()
        composeRule.onNodeWithContentDescription("Switch to dark theme").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, propertiesClicks)
        assertEquals(1, eyedropperClicks)
        assertEquals(true, requestedDarkTheme)
    }

    @Test
    fun minimizedToolsExposeOnlyTheRestoreHandle() {
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = true,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = {},
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = {},
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Show tools").assertExists()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open properties").assertDoesNotExist()
    }

    @Test
    fun minimizeHandleDispatchesMinimize() {
        var minimizeClicks = 0
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = {},
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = { minimizeClicks++ },
                    onShow = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Minimize tools").performClick()
        assertEquals(1, minimizeClicks)
    }

    @Test
    fun splitInspectorShowsExpandAction() {
        var clicks = 0
        renderTools(
            controlsLayout = SceneInspectorLayoutMode.Split,
            onToggleControls = { clicks++ },
        )

        composeRule.onNodeWithContentDescription("Expand settings").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun activeEyedropperShowsCloseAction() {
        var clicks = 0
        renderTools(
            isEyedropperVisible = true,
            onToggleEyedropper = { clicks++ },
        )

        composeRule.onNodeWithContentDescription("Close eyedropper").performClick()
        assertEquals(1, clicks)
    }

    private fun renderTools(
        controlsLayout: SceneInspectorLayoutMode = SceneInspectorLayoutMode.Closed,
        isEyedropperVisible: Boolean = false,
        onToggleControls: () -> Unit = {},
        onToggleEyedropper: () -> Unit = {},
    ) {
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = controlsLayout,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = isEyedropperVisible,
                    onBack = {},
                    onToggleControls = onToggleControls,
                    onToggleEyedropper = onToggleEyedropper,
                    onThemeChange = {},
                    onMinimize = {},
                    onShow = {},
                )
            }
        }
    }
}
