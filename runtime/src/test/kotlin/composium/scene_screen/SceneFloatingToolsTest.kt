package oleginvoke.com.composium.scene_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
    fun centeredEyeDispatchesHide() {
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

        composeRule.onNodeWithContentDescription("Hide tools").performClick()
        assertEquals(1, minimizeClicks)
    }

    @Test
    fun areaImmediatelyOutsideEyeGapDispatchesPropertiesClick() {
        var minimizeClicks = 0
        var propertiesClicks = 0
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = { propertiesClicks++ },
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = { minimizeClicks++ },
                    onShow = {},
                )
            }
        }

        // The point is outside both the 40 dp eye and the new 48 dp circular gap,
        // while still lying inside the old square 48 dp touch box.
        composeRule.onNodeWithContentDescription("Open properties").performTouchInput {
            click(Offset(x = 10f, y = 30f))
        }

        assertEquals(1, propertiesClicks)
        assertEquals(0, minimizeClicks)
    }

    @Test
    fun gapAroundEyeDoesNotDispatchAnyAction() {
        var minimizeClicks = 0
        var propertiesClicks = 0
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = false,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = { propertiesClicks++ },
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = { minimizeClicks++ },
                    onShow = {},
                )
            }
        }

        // Relative to the eye center this is (2, -21): outside the 20 dp eye radius,
        // but inside the requested 24 dp transparent gap radius.
        composeRule.onNodeWithContentDescription("Open properties").performTouchInput {
            click(Offset(x = 2f, y = 31f))
        }

        assertEquals(0, propertiesClicks)
        assertEquals(0, minimizeClicks)
    }

    @Test
    fun eyeStaysAtTheGridCenterWhenToolsCollapse() {
        composeRule.setContent {
            var minimized by remember { mutableStateOf(false) }
            ComposiumTheme(darkTheme = false) {
                SceneFloatingTools(
                    controlsLayout = SceneInspectorLayoutMode.Closed,
                    isMinimized = minimized,
                    isDarkTheme = false,
                    isEyedropperVisible = false,
                    onBack = {},
                    onToggleControls = {},
                    onToggleEyedropper = {},
                    onThemeChange = {},
                    onMinimize = { minimized = true },
                    onShow = { minimized = false },
                )
            }
        }
        val back = actionBounds("Back")
        val properties = actionBounds("Open properties")
        val eyedropper = actionBounds("Open eyedropper")
        val theme = actionBounds("Switch to dark theme")
        val expandedEye = actionBounds("Hide tools")

        assertEquals(52f, back.width, 0.01f)
        assertEquals(52f, back.height, 0.01f)
        assertEquals(back.right, properties.left, 0.01f)
        assertEquals(back.bottom, eyedropper.top, 0.01f)
        assertEquals(properties.left, theme.left, 0.01f)
        assertEquals(eyedropper.top, theme.top, 0.01f)
        assertEquals(back.right, expandedEye.center.x, 0.01f)
        assertEquals(back.bottom, expandedEye.center.y, 0.01f)

        composeRule.onNodeWithContentDescription("Hide tools").performClick()

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open properties").assertDoesNotExist()
        assertEquals(expandedEye, actionBounds("Show tools"))
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

    private fun actionBounds(description: String) = composeRule
        .onNodeWithContentDescription(description)
        .fetchSemanticsNode()
        .boundsInRoot
}
