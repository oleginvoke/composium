package oleginvoke.com.composium.host_screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.scene
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposiumHostThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disabledSceneIsNotCapturedButStillOpensNormally() {
        var disabledExecutions = 0
        var thumbnailExecutions = 0
        val disabled by scene(name = "Without thumbnail", thumbnail = null) {
            SideEffect { disabledExecutions++ }
            BasicText("Opened content")
        }
        val enabled by scene(
            name = "With thumbnail",
            thumbnail = {
                SideEffect { thumbnailExecutions++ }
                BasicText("Captured content")
            },
            content = {},
        )
        ComposiumRuntime.registerAll(disabled, enabled)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                ComposiumHostScreen(contentWindowInsets = WindowInsets(0))
            }
        }
        repeat(12) {
            composeRule.mainClock.advanceTimeBy(200)
            composeRule.waitForIdle()
        }

        composeRule.runOnIdle {
            assertTrue(thumbnailExecutions > 0, "Capture queue must actually run")
            assertEquals(0, disabledExecutions)
        }
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Without thumbnail"))
        composeRule.onNodeWithText("Without thumbnail").performClick()
        composeRule.onNodeWithText("Opened content").assertExists()
        composeRule.runOnIdle { assertTrue(disabledExecutions > 0) }
    }
}
