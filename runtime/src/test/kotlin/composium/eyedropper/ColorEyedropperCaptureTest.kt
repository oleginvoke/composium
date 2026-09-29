package oleginvoke.com.composium.eyedropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import oleginvoke.com.composium.scene_screen.SceneFloatingTools
import oleginvoke.com.composium.scene_screen.SceneInspectorLayoutMode
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ColorEyedropperCaptureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentButtonCanCloseAnAlreadyRenderedEyedropper() {
        val state = ColorEyedropperState(Offset.Unspecified)
        var visible by mutableStateOf(true)
        lateinit var owner: View
        composeRule.setContent {
            owner = LocalView.current
            ColorEyedropperHost(visible, { visible = it }, state, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.Blue)) {
                    Box(Modifier.size(64.dp).testTag("close").clickable { visible = false })
                }
            }
        }
        awaitColor(owner, state, Color.Blue)
        composeRule.onNodeWithTag("close").performTouchInput { click() }
        composeRule.runOnIdle { assertFalse(visible) }
    }

    @Test
    fun contentScrollWinsButUnclaimedDragMovesTarget() {
        val state = ColorEyedropperState(Offset.Unspecified)
        lateinit var owner: View
        lateinit var scroll: ScrollState
        composeRule.setContent {
            owner = LocalView.current
            scroll = rememberScrollState()
            ColorEyedropperHost(true, {}, state, modifier = Modifier.fillMaxSize().testTag("host")) {
                Box(Modifier.fillMaxSize().background(Color.Blue)) {
                    Box(Modifier.size(100.dp, 180.dp).testTag("scroll").verticalScroll(scroll)) {
                        Box(Modifier.height(1000.dp))
                    }
                }
            }
        }
        awaitColor(owner, state, Color.Blue)
        val original = state.target
        composeRule.onNodeWithTag("scroll").performTouchInput {
            swipe(Offset(20f, height - 20f), Offset(20f, 20f), 500)
        }
        composeRule.runOnIdle {
            assertTrue(scroll.value > 0)
            assertEquals(original, state.target)
        }
        composeRule.onNodeWithTag("host").performTouchInput {
            swipe(Offset(width - 10f, height - 100f), Offset(width - 10f, height - 20f), 500)
        }
        composeRule.runOnIdle { assertTrue(state.target != original) }
    }

    @Test
    fun contentChangesRefreshSampleWithoutReopeningAndOverlayDoesNotPolluteIt() {
        val state = ColorEyedropperState(Offset.Unspecified)
        var color by mutableStateOf(Color.Blue)
        lateinit var owner: View
        composeRule.setContent {
            owner = LocalView.current
            ColorEyedropperHost(true, {}, state, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().graphicsLayer().drawBehind { drawRect(color) }) {
                    Box(Modifier.size(64.dp).testTag("change").clickable { color = Color.Green })
                }
            }
        }
        awaitColor(owner, state, Color.Blue)
        composeRule.onNodeWithTag("change").performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(Color.Green, color) }
        awaitColor(owner, state, Color.Green)
        // Also refresh on changes not caused by pointer input (e.g. an async result).
        composeRule.runOnIdle { color = Color.Red }
        awaitColor(owner, state, Color.Red)
    }

    @Test
    fun lensDragDoesNotScrollContentAndItsArrowsStillNudge() {
        val state = ColorEyedropperState(Offset.Unspecified)
        lateinit var owner: View
        lateinit var scroll: ScrollState
        composeRule.setContent {
            owner = LocalView.current
            scroll = rememberScrollState()
            ColorEyedropperHost(true, {}, state, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.Blue).verticalScroll(scroll)) {
                    Box(Modifier.height(2000.dp))
                }
            }
        }
        awaitColor(owner, state, Color.Blue)
        val original = state.target
        composeRule.onNodeWithContentDescription("Color eyedropper magnifier").performTouchInput {
            swipe(center, center + Offset(0f, -60f), 500)
        }
        composeRule.runOnIdle {
            assertTrue(state.target.y < original.y)
            assertEquals(original.x, state.target.x, 0.1f)
            assertEquals(0, scroll.value)
        }
        val beforeNudge = state.target
        val rightArrow = composeRule.onAllNodes(
            hasClickAction() and hasAnyAncestor(hasContentDescription("Color eyedropper magnifier")),
        ).fetchSemanticsNodes().maxBy { it.boundsInRoot.center.x }.boundsInRoot.center
        val lensBounds = composeRule.onNodeWithContentDescription("Color eyedropper magnifier")
            .fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithContentDescription("Color eyedropper magnifier").performTouchInput {
            click(rightArrow - lensBounds.topLeft)
        }
        composeRule.runOnIdle {
            assertTrue(state.target.x > beforeNudge.x)
            assertEquals(0, scroll.value)
        }
    }

    @Test
    fun cursorDragWinsOverContentScrollInBothDirections() {
        val state = ColorEyedropperState(Offset(40f, 350f))
        lateinit var owner: View
        lateinit var scroll: ScrollState
        composeRule.setContent {
            owner = LocalView.current
            scroll = rememberScrollState()
            ColorEyedropperHost(true, {}, state, modifier = Modifier.fillMaxSize().testTag("host")) {
                Box(Modifier.fillMaxSize().background(Color.Blue).verticalScroll(scroll)) {
                    Box(Modifier.height(2000.dp))
                }
            }
        }
        awaitColor(owner, state, Color.Blue)
        val original = state.target
        // Touch the visible cursor itself, not a semantics action or the magnifier.
        val lensBounds = composeRule.onNodeWithContentDescription("Color eyedropper magnifier")
            .fetchSemanticsNode().boundsInRoot
        val start = original + Offset(-12f, 0f)
        assertFalse("The gesture must start outside the magnifier: $lensBounds / $start", lensBounds.contains(start))
        composeRule.onNodeWithTag("host").performTouchInput {
            swipe(start, start + Offset(0f, -60f), 500)
        }
        composeRule.runOnIdle {
            assertEquals(0, scroll.value)
            assertTrue(state.target.y < original.y - 20f)
            assertEquals(original.x, state.target.x, 0.1f)
        }
        val afterVertical = state.target
        composeRule.onNodeWithTag("host").performTouchInput {
            swipe(afterVertical, afterVertical + Offset(60f, 0f), 500)
        }
        composeRule.runOnIdle {
            assertEquals(0, scroll.value)
            assertTrue(state.target.x > afterVertical.x + 20f)
            assertEquals(afterVertical.y, state.target.y, 0.1f)
        }
    }

    private fun awaitColor(owner: View, state: ColorEyedropperState, expected: Color) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.waitForIdle()
            composeRule.runOnIdle { drawFrame(owner) }
            state.color == expected
        }
    }

    @Test
    fun samplingBeneathVisibleFloatingToolsReturnsScenePixelsOnEveryCapture() {
        val state = ColorEyedropperState(initialTarget = Offset.Unspecified)
        var visible by mutableStateOf(false)
        var sceneColor by mutableStateOf(Color.Blue)
        lateinit var ownerView: View
        composeRule.setContent {
            ownerView = LocalView.current
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    ColorEyedropperHost(
                        visible = visible,
                        onVisibleChange = { visible = it },
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(Modifier.fillMaxSize().background(sceneColor))
                    }
                    SceneFloatingTools(
                        controlsLayout = SceneInspectorLayoutMode.Closed,
                        isMinimized = false,
                        isDarkTheme = false,
                        isEyedropperVisible = visible,
                        onBack = {},
                        onToggleControls = {},
                        onToggleEyedropper = {},
                        onThemeChange = {},
                        onMinimize = {},
                        onShow = {},
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
        val backBounds = composeRule.onNodeWithContentDescription("Back")
            .fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle {
            // Inside the painted grid, away from its icon and rounded outer corners.
            state.updateTarget(Offset(backBounds.left + 8f, backBounds.center.y))
            visible = true
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.waitForIdle()
            composeRule.runOnIdle { drawFrame(ownerView) }
            state.color != null
        }
        composeRule.runOnIdle { assertEquals(Color.Blue, state.color) }
        composeRule.onNodeWithContentDescription("Back").assertExists()

        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            sceneColor = Color.Green
            visible = true
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.waitForIdle()
            composeRule.runOnIdle { drawFrame(ownerView) }
            state.color != null
        }
        composeRule.runOnIdle { assertEquals(Color.Green, state.color) }
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    private fun drawFrame(view: View) {
        // Robolectric does not drive a real window renderer. Draw the real Compose owner and
        // deliver its draw notification, so the production capture reads a rendered frame.
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            view.viewTreeObserver.dispatchOnDraw()
        } finally {
            bitmap.recycle()
        }
    }
}
