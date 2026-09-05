package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
class SceneFloatingToolsDragTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun draggingEyeMovesToolsWithoutMinimizingThem() {
        renderScene()
        val initialEye = eyeBounds("Hide tools")

        composeRule.onNodeWithContentDescription("Hide tools").performTouchInput {
            down(center)
            moveBy(Offset(x = -60f, y = 80f))
            up()
        }

        val movedEye = eyeBounds("Hide tools")
        assertEquals(initialEye.left - 60f, movedEye.left, 1.1f)
        assertEquals(initialEye.top + 80f, movedEye.top, 1.1f)
    }

    @Test
    fun draggingBeyondTopLeftKeepsTheWholeGridInsideSafeBounds() {
        renderScene()

        composeRule.onNodeWithContentDescription("Hide tools").performTouchInput {
            down(center)
            moveBy(Offset(x = -1_000f, y = -1_000f))
            up()
        }

        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val back = eyeBounds("Back")
        assertEquals(screen.left + 7f + 12f, back.left, 1.1f)
        assertEquals(screen.top + 24f + 12f, back.top, 1.1f)
    }

    @Test
    fun draggedPositionSurvivesMinimizeAndRestore() {
        renderScene()
        composeRule.onNodeWithContentDescription("Hide tools").performTouchInput {
            down(center)
            moveBy(Offset(x = -48f, y = 64f))
            up()
        }
        val draggedEye = eyeBounds("Hide tools")

        composeRule.onNodeWithContentDescription("Hide tools").performTouchInput { click(center) }
        assertEquals(draggedEye, eyeBounds("Show tools"))
        composeRule.onNodeWithContentDescription("Show tools").performTouchInput { click(center) }
        assertEquals(draggedEye, eyeBounds("Hide tools"))
    }

    private fun eyeBounds(description: String) = composeRule
        .onNodeWithContentDescription(description)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun renderScene() {
        val entry = SceneEntry(
            Scene(group = null, name = "Draggable tools", tools = SceneTools.Floating) {
                Box(Modifier.fillMaxSize().background(Color.Blue))
            },
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ComposiumTheme(darkTheme = false) {
                    val insets = WindowInsets(left = 7, top = 24, right = 20, bottom = 30)
                    Box(Modifier.fillMaxSize().testTag("screen")) {
                        SceneScreen(
                            sceneEntry = entry,
                            onBack = {},
                            contentWindowInsets = insets,
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)),
                        )
                    }
                }
            }
        }
    }
}
