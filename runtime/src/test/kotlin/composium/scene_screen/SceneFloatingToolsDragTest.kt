package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.FloatingToolsPosition
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneTools
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
class SceneFloatingToolsDragTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun draggingMoreMovesItsFanButNotSettings() {
        renderScene()
        val initialEye = eyeBounds("Hide actions")
        val initialSettings = eyeBounds("Open properties")
        val initialTheme = eyeBounds("Switch to dark theme")

        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            down(center)
            moveBy(Offset(x = -60f, y = 80f))
            up()
        }

        val movedEye = eyeBounds("Hide actions")
        assertEquals(initialEye.left - 60f, movedEye.left, 1.1f)
        assertEquals(initialEye.top + 80f, movedEye.top, 1.1f)
        assertEquals(initialSettings, eyeBounds("Open properties"))
        assertEquals(initialTheme.left - 60f, eyeBounds("Switch to dark theme").left, 1.1f)
        assertEquals(initialTheme.top + 80f, eyeBounds("Switch to dark theme").top, 1.1f)
    }

    @Test
    fun holdingMoreBeforeDraggingKeepsMovementOneToOneWithFinger() {
        renderScene()
        val initialEye = eyeBounds("Hide actions")
        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            moveBy(Offset(x = -60f, y = 80f))
            up()
        }

        val movedEye = eyeBounds("Hide actions")
        assertEquals(initialEye.left - 60f, movedEye.left, 1.1f)
        assertEquals(initialEye.top + 80f, movedEye.top, 1.1f)
    }

    @Test
    fun draggingBeyondTopLeftClampsOnlyMoreAndAllowsFanOffscreen() {
        renderScene()
        val settings = eyeBounds("Open properties")

        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            down(center)
            moveBy(Offset(x = -1_000f, y = -1_000f))
            up()
        }

        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val more = eyeBounds("Hide actions")
        assertEquals(screen.left + 7f + 12f, more.left, 1.1f)
        assertEquals(screen.top + 24f + 12f, more.top, 1.1f)
        assertEquals(settings, eyeBounds("Open properties"))
        val themePosition = composeRule.onNodeWithContentDescription("Switch to dark theme")
            .fetchSemanticsNode().positionInRoot
        assertTrue(themePosition.x < screen.left)
        assertEquals(more.left - 2f - 46.765f, themePosition.x, 1.1f)
    }

    @Test
    fun settingsCanOverlapMoreWithoutMovingItOrOpeningControls() {
        renderScene()
        val more = eyeBounds("Hide actions")
        val settings = eyeBounds("Open properties")
        val theme = eyeBounds("Switch to dark theme")
        composeRule.onNodeWithContentDescription("Open properties").performTouchInput {
            down(center)
            moveBy(more.center - settings.center)
            up()
        }
        assertEquals(more.center, eyeBounds("Open properties").center)
        assertEquals(more, eyeBounds("Hide actions"))
        assertEquals(theme, eyeBounds("Switch to dark theme"))
    }

    @Test
    fun draggedPositionSurvivesMinimizeAndRestore() {
        renderScene()
        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            down(center)
            moveBy(Offset(x = -48f, y = 64f))
            up()
        }
        val draggedEye = eyeBounds("Hide actions")

        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput { click(center) }
        assertEquals(draggedEye, eyeBounds("Show actions"))
        composeRule.onNodeWithContentDescription("Show actions").performTouchInput { click(center) }
        assertEquals(draggedEye, eyeBounds("Hide actions"))
    }

    @Test
    fun eachAnchorReachesBottomRightSafeEdgeUsingItsOwnSize() {
        renderScene()
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        listOf("Hide actions", "Open properties").forEach { description ->
            composeRule.onNodeWithContentDescription(description).performTouchInput {
                down(center)
                moveBy(Offset(1_000f, 1_000f))
                up()
            }
            val button = eyeBounds(description)
            assertEquals(screen.right - 20f - 12f, button.right, 1.1f)
            assertEquals(screen.bottom - 30f - 12f, button.bottom, 1.1f)
        }
    }

    @Test
    fun firstDragStartsAtClampedVisiblePositionWhenPairDoesNotFit() {
        renderScene(height = 170.dp)
        val initial = eyeBounds("Hide actions")
        composeRule.onNodeWithContentDescription("Hide actions").performTouchInput {
            down(center)
            moveBy(Offset(-30f, -30f))
            up()
        }
        val moved = eyeBounds("Hide actions")
        assertEquals(initial.left - 30f, moved.left, 1.1f)
        assertEquals(initial.top - 30f, moved.top, 1.1f)
    }

    private fun eyeBounds(description: String) = composeRule
        .onNodeWithContentDescription(description)
        .fetchSemanticsNode()
        .boundsInRoot

    private fun renderScene(height: Dp? = null) {
        val entry = SceneEntry(
            Scene(group = null, name = "Draggable tools", tools = SceneTools.Floating(FloatingToolsPosition.TopEnd, actionsInitiallyExpanded = true)) {
                Box(Modifier.fillMaxSize().background(Color.Blue))
            },
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ComposiumTheme(darkTheme = false) {
                    val insets = WindowInsets(left = 7, top = 24, right = 20, bottom = 30)
                    Box(Modifier.then(if (height != null) Modifier.height(height) else Modifier).fillMaxSize().testTag("screen")) {
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
