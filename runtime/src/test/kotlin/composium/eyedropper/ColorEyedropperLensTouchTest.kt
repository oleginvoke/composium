package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ColorEyedropperLensTouchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapsNearInnerCornersOfEnlargedButtonsNudgeOnlyTheirOwnDirection() {
        val nudges = mutableListOf<ColorEyedropperNudgeDirection>()
        composeRule.setContent {
            ColorEyedropperLens(
                lens = ColorEyedropperPixelLens(IntArray(121) { 0xff336699.toInt() }, 11, 60),
                onNudge = { nudges += it },
                metrics = ColorEyedropperDefaults.metrics,
                colors = ColorEyedropperDefaults.colors(),
                modifier = Modifier.testTag("lens"),
            )
        }
        val inset = with(composeRule.density) { 44.dp.toPx() }
        val side = with(composeRule.density) { 20.dp.toPx() }
        val buttons = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(4, buttons.size)
        buttons.forEachIndexed { index, button ->
            buttons.drop(index + 1).forEach { neighbor ->
                assertFalse(button.boundsInRoot.overlaps(neighbor.boundsInRoot),
                    "Adjacent nudge touch targets must not overlap")
            }
        }
        composeRule.onNodeWithTag("lens").performTouchInput {
            // Four dp inside the inner edge of each 48 dp target, off the arrow axis.
            click(Offset(center.x - side, inset))
            click(Offset(center.x + side, inset))
            click(Offset(center.x - side, height - inset))
            click(Offset(center.x + side, height - inset))
            click(Offset(inset, center.y - side))
            click(Offset(inset, center.y + side))
            click(Offset(width - inset, center.y - side))
            click(Offset(width - inset, center.y + side))
            // The pixel at the center must not become an accidental nudge button.
            click(center)
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    ColorEyedropperNudgeDirection.Top, ColorEyedropperNudgeDirection.Top,
                    ColorEyedropperNudgeDirection.Bottom, ColorEyedropperNudgeDirection.Bottom,
                    ColorEyedropperNudgeDirection.Start, ColorEyedropperNudgeDirection.Start,
                    ColorEyedropperNudgeDirection.End, ColorEyedropperNudgeDirection.End,
                ),
                nudges,
            )
        }
    }
}
