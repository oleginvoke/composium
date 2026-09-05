package oleginvoke.com.composium.scene_screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneFloatingToolsPlacementTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var ownerView: View

    @Test
    fun minimizedToolsPaintOnlyTheEyeInLtrHost() {
        assertTransparentEyeTouchArea(LayoutDirection.Ltr)
    }

    @Test
    fun minimizedToolsPaintOnlyTheEyeInRtlHost() {
        assertTransparentEyeTouchArea(LayoutDirection.Rtl)
    }

    @Test
    fun activeToolFillsItsWholeQuadrant() {
        renderScene(LayoutDirection.Ltr)
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        val back = actionBounds("Back")
        val properties = actionBounds("Expand settings")
        val bitmap = drawScene("active-quadrant")
        try {
            val inactiveCorner = bitmap.getPixel(
                back.right.toInt() - 6,
                back.top.toInt() + 6,
            )
            val activeCorner = bitmap.getPixel(
                properties.left.toInt() + 6,
                properties.top.toInt() + 6,
            )
            assertNotEquals(
                "The active state must fill the rectangular quadrant, including its inner corner",
                inactiveCorner,
                activeCorner,
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun rtlHostKeepsPhysicalRightAnchorWhenMinimizingAndRestoring() {
        assertStablePhysicalAnchor(LayoutDirection.Rtl)
    }

    @Test
    fun ltrHostKeepsPhysicalRightAnchorWhenMinimizingAndRestoring() {
        assertStablePhysicalAnchor(LayoutDirection.Ltr)
    }

    @Test
    fun rtlHostKeepsBackPropertiesAndEyedropperThemeInPhysicalOrder() {
        renderScene(LayoutDirection.Rtl)
        val back = actionBounds("Back")
        val properties = actionBounds("Open properties")
        val eyedropper = actionBounds("Open eyedropper")
        val theme = actionBounds("Switch to dark theme")

        assertTrue("Back must be physically left of Properties", back.left < properties.left)
        assertEquals(back.right, properties.left, 0.01f)
        assertEquals(back.left, eyedropper.left, 0.01f)
        assertEquals(properties.left, theme.left, 0.01f)
        assertEquals(back.top, properties.top, 0.01f)
        assertEquals(back.bottom, eyedropper.top, 0.01f)
        assertEquals(eyedropper.top, theme.top, 0.01f)
    }

    private fun assertStablePhysicalAnchor(direction: LayoutDirection) {
        renderScene(direction)
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val expandedEye = actionBounds("Hide tools")
        val back = actionBounds("Back")
        val properties = actionBounds("Open properties")
        // At density 1, the 104 dp grid is inset by physical right 20 + margin 12,
        // while the 48 dp eye remains centered over the grid intersection.
        assertEquals(screen.right - 60f, expandedEye.right, 0.01f)
        assertEquals(screen.top + 64f, expandedEye.top, 0.01f)
        assertEquals(48f, expandedEye.width, 0.01f)
        assertEquals(48f, expandedEye.height, 0.01f)
        assertEquals(52f, back.width, 0.01f)
        assertEquals(52f, back.height, 0.01f)
        assertEquals(back.right, properties.left, 0.01f)
        assertEquals(back.right, expandedEye.center.x, 0.01f)
        assertEquals(back.bottom, expandedEye.center.y, 0.01f)

        composeRule.onNodeWithContentDescription("Hide tools").performClick()
        assertEquals(expandedEye, actionBounds("Show tools"))
        composeRule.onNodeWithContentDescription("Show tools").performClick()
        assertEquals(expandedEye, actionBounds("Hide tools"))
    }

    private fun actionBounds(description: String) = composeRule
        .onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun assertTransparentEyeTouchArea(direction: LayoutDirection) {
        renderScene(direction)
        drawScene("${direction.name.lowercase()}-expanded").recycle()
        composeRule.onNodeWithContentDescription("Hide tools").performClick()
        val eye = actionBounds("Show tools")
        val bitmap = drawScene("${direction.name.lowercase()}-minimized")
        try {
            // All four points lie inside the 48 x 48 touch target but outside its 40 dp eye.
            listOf(2 to 24, 24 to 2, 46 to 24, 24 to 46).forEach { (x, y) ->
                assertEquals(
                    "The eye touch area must show scene pixels at ($x, $y)",
                    Color.Blue.toArgb(),
                    bitmap.getPixel(eye.left.toInt() + x, eye.top.toInt() + y),
                )
            }
            assertNotEquals(
                "The visible eye control must remain discoverable",
                Color.Blue.toArgb(),
                bitmap.getPixel(eye.center.x.toInt(), eye.center.y.toInt()),
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawScene(name: String): Bitmap = composeRule.runOnIdle {
        val bitmap = Bitmap.createBitmap(ownerView.width, ownerView.height, Bitmap.Config.ARGB_8888)
        ownerView.draw(Canvas(bitmap))
        val evidence = File("build/reports/floating-tools/$name.png")
        checkNotNull(evidence.parentFile).mkdirs()
        evidence.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap
    }

    private fun renderScene(direction: LayoutDirection) {
        val entry = SceneEntry(Scene(group = null, name = "Placement regression", tools = SceneTools.Floating) { padding ->
            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                Box(Modifier.fillMaxSize().padding(padding))
            }
        })
        composeRule.setContent {
            ownerView = LocalView.current
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(1f),
            ) {
                ComposiumTheme(darkTheme = false) {
                    SceneScreen(
                        sceneEntry = entry,
                        onBack = {},
                        contentWindowInsets = WindowInsets(left = 7, top = 24, right = 20),
                        modifier = Modifier.testTag("screen"),
                    )
                }
            }
        }
    }
}
