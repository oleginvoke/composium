package oleginvoke.com.composium.scene_screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
    fun collapsedCapsuleRevealsSceneWhereActionsWere() {
        renderScene(LayoutDirection.Ltr)
        val back = actionBounds("Back")
        val theme = actionBounds("Switch to dark theme")
        drawScene("light-expanded").recycle()
        composeRule.onNodeWithContentDescription("Hide tools").performClick()
        val eye = actionBounds("Show tools")
        val bitmap = drawScene("light-minimized")
        try {
            listOf(back.center, theme.center).forEach { point ->
                assertEquals(Color.Blue.toArgb(), bitmap.getPixel(point.x.toInt(), point.y.toInt()))
            }
            assertNotEquals(Color.Blue.toArgb(), bitmap.getPixel(eye.center.x.toInt(), eye.center.y.toInt()))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun activeActionHasDistinctFillInsideCapsule() {
        renderScene(LayoutDirection.Ltr)
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        val back = actionBounds("Back")
        val properties = actionBounds("Expand settings")
        val bitmap = drawScene("light-active")
        try {
            assertNotEquals(
                bitmap.getPixel(back.center.x.toInt(), back.top.toInt() + 6),
                bitmap.getPixel(properties.center.x.toInt(), properties.top.toInt() + 6),
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun darkCapsuleRemainsVisibleOverDarkScene() {
        renderScene(LayoutDirection.Ltr, backgroundColor = Color.Black, darkTheme = true)
        val back = actionBounds("Back")
        val eye = actionBounds("Hide tools")
        val bitmap = drawScene("dark-expanded")
        try {
            assertNotEquals(Color.Black.toArgb(), bitmap.getPixel(back.center.x.toInt(), back.top.toInt() - 3))
            assertNotEquals(
                bitmap.getPixel(back.center.x.toInt(), back.top.toInt() + 6),
                bitmap.getPixel(eye.center.x.toInt(), eye.top.toInt() + 6),
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
    fun rtlHostKeepsActionsInVerticalOrder() {
        renderScene(LayoutDirection.Rtl)
        val actions = listOf("Back", "Open properties", "Hide tools", "Open eyedropper", "Switch to dark theme")
            .map(::actionBounds)
        actions.zipWithNext().forEach { (above, below) ->
            assertTrue(above.bottom < below.top)
            assertEquals(above.center.x, below.center.x, 0.01f)
        }
    }

    private fun assertStablePhysicalAnchor(direction: LayoutDirection) {
        renderScene(direction)
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val expandedEye = actionBounds("Hide tools")
        val back = actionBounds("Back")
        // 64 dp capsule, 8 dp internal padding, and physical right inset 20 + margin 12.
        assertEquals(screen.right - 40f, expandedEye.right, 0.01f)
        assertEquals(screen.top + 152f, expandedEye.top, 0.01f)
        assertEquals(48f, expandedEye.width, 0.01f)
        assertEquals(56f, expandedEye.height, 0.01f)
        assertEquals(screen.top + 44f, back.top, 0.01f)
        assertEquals(back.center.x, expandedEye.center.x, 0.01f)

        composeRule.onNodeWithContentDescription("Hide tools").performClick()
        assertEquals(expandedEye, actionBounds("Show tools"))
        composeRule.onNodeWithContentDescription("Show tools").performClick()
        assertEquals(expandedEye, actionBounds("Hide tools"))
    }

    private fun actionBounds(description: String) = composeRule
        .onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun drawScene(name: String): Bitmap = composeRule.runOnIdle {
        val bitmap = Bitmap.createBitmap(ownerView.width, ownerView.height, Bitmap.Config.ARGB_8888)
        ownerView.draw(Canvas(bitmap))
        val evidence = File("build/reports/floating-tools/$name.png")
        checkNotNull(evidence.parentFile).mkdirs()
        evidence.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap
    }

    private fun renderScene(
        direction: LayoutDirection,
        backgroundColor: Color = Color.Blue,
        darkTheme: Boolean = false,
    ) {
        val entry = SceneEntry(Scene(group = null, name = "Placement regression", tools = SceneTools.Floating) { padding ->
            Box(Modifier.fillMaxSize().background(backgroundColor)) {
                Box(Modifier.fillMaxSize().padding(padding))
            }
        })
        composeRule.setContent {
            ownerView = LocalView.current
            CompositionLocalProvider(
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(1f),
            ) {
                ComposiumTheme(darkTheme = darkTheme) {
                    val insets = WindowInsets(left = 7, top = 24, right = 20)
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
