package oleginvoke.com.composium.scene_screen

import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
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
class SceneInspectorPaddingAnimationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun floatingContentDoesNotJumpWhenOpeningOrClosing() = checkAnimation(SceneTools.Floating())
    @Test fun topBarContentDoesNotJumpWhenOpeningOrClosing() = checkAnimation(SceneTools.TopBar)
    @Test fun customToolsAndOverriddenDensityPreservePhysicalPadding() = checkAnimation(SceneTools.None, 2f)
    @Test fun reversingAnUnfinishedTransitionKeepsPaddingAligned() = checkAnimation(SceneTools.Floating(), interrupt = true)
    @Test fun draggingTowardNavigationAreaKeepsPaddingAligned() = checkAnimation(SceneTools.None, drag = true)

    private fun checkAnimation(tools: SceneTools, previewDensity: Float? = null, interrupt: Boolean = false, drag: Boolean = false) {
        lateinit var scope: SceneScope
        val entry = SceneEntry(Scene(null, "Animated padding", tools = tools) { padding ->
            SideEffect { scope = this }
            val cardSize = with(LocalDensity.current) { 40.toDp() }
            Box(Modifier.fillMaxSize().testTag("pane")) {
                Box(Modifier.fillMaxSize().padding(padding).testTag("safe"), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(cardSize).testTag("card"))
                }
            }
        })
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ComposiumTheme(false) {
                    Box(Modifier.fillMaxSize().testTag("host")) {
                        SceneScreen(entry, onBack = {}, contentWindowInsets = WindowInsets(0, 24, 0, 24))
                    }
                }
            }
        }
        if (previewDensity != null) rule.runOnIdle {
            scope.preview.displayScaleOverride = previewDensity / (DisplayMetrics.DENSITY_DEVICE_STABLE / 160f)
        }
        fun bounds(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val host = bounds("host")
        fun checkPadding() {
            val pane = bounds("pane")
            val safe = bounds("safe")
            val expectedBottom = minOf(pane.bottom, host.bottom - 24f)
            assertEquals("Safe content must follow the actual preview boundary", expectedBottom, safe.bottom, 1f)
            assertEquals(if (tools == SceneTools.TopBar) 96f else 24f, safe.top - pane.top, 1f)
            assertEquals("Card must stay centered in the safe area", safe.center.y, bounds("card").center.y, 1f)
        }
        checkPadding()
        rule.mainClock.autoAdvance = false
        var previousY = bounds("card").top
        rule.runOnIdle { scope.controlsState.show() }
        repeat(if (interrupt) 7 else 30) {
            rule.mainClock.advanceTimeByFrame()
            val y = bounds("card").top
            assertTrue("Opening frame $it jumped down: $previousY -> $y", y <= previousY + 1f)
            checkPadding()
            previousY = y
        }
        if (drag) {
            val tabs = rule.onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
            rule.onRoot().performTouchInput { down(tabs.center - host.topLeft) }
            repeat(3) {
                rule.onRoot().performTouchInput { moveBy(Offset(0f, 60f)) }
                rule.mainClock.advanceTimeBy(32)
                checkPadding()
            }
            rule.onRoot().performTouchInput { up() }
            rule.mainClock.advanceTimeBy(500)
            checkPadding()
            previousY = bounds("card").top
        }
        rule.runOnIdle { scope.onBack() }
        repeat(30) {
            rule.mainClock.advanceTimeByFrame()
            val y = bounds("card").top
            // An interrupted tween can deliver another opening frame before reversal starts.
            // Its padding and centered content must still match that frame's actual bounds.
            if (!interrupt) assertTrue("Closing frame $it jumped up: $previousY -> $y", y >= previousY - 1f)
            checkPadding()
            previousY = y
        }
        assertEquals(host.bottom, bounds("pane").bottom, 1f)
        checkPadding()
    }
}
