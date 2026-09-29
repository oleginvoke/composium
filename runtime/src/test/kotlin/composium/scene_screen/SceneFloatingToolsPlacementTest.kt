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
import oleginvoke.com.composium.FloatingToolsPosition
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneFloatingToolsPlacementTest {
    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var ownerView: View

    @Test
    fun collapsedFanRevealsSceneWhereActionsWere() {
        renderScene(LayoutDirection.Ltr)
        val back = actionBounds("Back")
        val theme = actionBounds("Switch to dark theme")
        composeRule.onNodeWithContentDescription("Hide actions").performClick()
        val eye = actionBounds("Show actions")
        val bitmap = drawScene()
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
    fun activeSettingsHaveDistinctFill() {
        renderScene(LayoutDirection.Ltr)
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        val back = actionBounds("Back")
        val properties = actionBounds("Close properties")
        val bitmap = drawScene()
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
    fun darkButtonsRemainVisibleOverDarkScene() {
        renderScene(LayoutDirection.Ltr, backgroundColor = Color.Black, darkTheme = true)
        val back = actionBounds("Back")
        val eye = actionBounds("Hide actions")
        val bitmap = drawScene()
        try {
            assertNotEquals(Color.Black.toArgb(), bitmap.getPixel(back.center.x.toInt(), back.top.toInt() + 6))
            assertNotEquals(
                bitmap.getPixel(back.center.x.toInt(), back.top.toInt() + 6),
                bitmap.getPixel(eye.center.x.toInt(), eye.top.toInt() + 6),
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun rtlHostKeepsEndAnchorWhenMinimizingAndRestoring() {
        assertStableEndAnchor(LayoutDirection.Rtl)
    }

    @Test
    fun ltrHostKeepsEndAnchorWhenMinimizingAndRestoring() {
        assertStableEndAnchor(LayoutDirection.Ltr)
    }

    @Test
    fun rtlHostStillOpensFanToThePhysicalLeft() {
        renderScene(LayoutDirection.Rtl, initialPosition = FloatingToolsPosition.TopStart)
        val more = actionBounds("Hide actions")
        val actions = listOf("Open eyedropper", "Switch to dark theme", "Back")
            .map(::actionBounds)
        actions.zipWithNext().forEach { (above, below) ->
            assertTrue(above.center.y < below.center.y)
        }
        actions.take(2).forEach { assertTrue(it.center.x < more.center.x) }
        assertEquals(more.center.x, actions.last().center.x, 0.01f)
    }

    private fun assertStableEndAnchor(direction: LayoutDirection) {
        renderScene(direction)
        val screen = composeRule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val expandedEye = actionBounds("Hide actions")
        val settings = actionBounds("Open properties")
        // Centered 48/44 dp buttons separated by 4 dp; physical insets stay asymmetric.
        if (direction == LayoutDirection.Ltr) {
            assertEquals(screen.right - 34f, expandedEye.right, 0.01f)
        } else {
            assertEquals(screen.left + 21f, expandedEye.left, 0.01f)
        }
        assertEquals(screen.top + 88f, expandedEye.top, 0.01f)
        assertEquals(44f, expandedEye.width, 0.01f)
        assertEquals(44f, expandedEye.height, 0.01f)
        assertEquals(screen.top + 36f, settings.top, 0.01f)
        assertEquals(settings.center.x, expandedEye.center.x, 0.01f)

        composeRule.onNodeWithContentDescription("Hide actions").performClick()
        assertEquals(expandedEye, actionBounds("Show actions"))
        composeRule.onNodeWithContentDescription("Show actions").performClick()
        assertEquals(expandedEye, actionBounds("Hide actions"))
    }

    private fun actionBounds(description: String) = composeRule
        .onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot

    private fun drawScene(): Bitmap = composeRule.runOnIdle {
        val bitmap = Bitmap.createBitmap(ownerView.width, ownerView.height, Bitmap.Config.ARGB_8888)
        ownerView.draw(Canvas(bitmap))
        bitmap
    }

    private fun renderScene(
        direction: LayoutDirection,
        backgroundColor: Color = Color.Blue,
        darkTheme: Boolean = false,
        initialPosition: FloatingToolsPosition = FloatingToolsPosition.TopEnd,
    ) {
        val entry = SceneEntry(Scene(group = null, name = "Placement regression", tools = SceneTools.Floating(initialPosition, actionsInitiallyExpanded = true)) { padding ->
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
