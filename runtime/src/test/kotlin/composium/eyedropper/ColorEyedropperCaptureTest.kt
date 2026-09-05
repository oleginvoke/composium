package oleginvoke.com.composium.eyedropper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import oleginvoke.com.composium.scene_screen.SceneFloatingTools
import oleginvoke.com.composium.scene_screen.SceneInspectorLayoutMode
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
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
