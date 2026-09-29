package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe

internal fun ComposeContentTestRule.expandInspectorByGesture() {
    val tabs = onNode(hasText("Properties") and hasClickAction()).fetchSemanticsNode().boundsInRoot
    val root = onRoot().fetchSemanticsNode().boundsInRoot
    onRoot().performTouchInput {
        val start = tabs.center - root.topLeft
        swipe(start, Offset(start.x, 0f), durationMillis = 600)
    }
    onNodeWithContentDescription("Back to split layout").assertIsEnabled()
}
