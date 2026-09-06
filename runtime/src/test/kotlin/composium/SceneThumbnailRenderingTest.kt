package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailRenderSurface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneThumbnailRenderingTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun automaticThumbnailRendersContentWithZeroPadding() {
        var receivedPadding: PaddingValues? = null
        val automatic by scene { padding ->
            SideEffect { receivedPadding = padding }
            BasicText("Automatic content")
        }

        render(automatic)

        composeRule.onNodeWithText("Automatic content").assertExists()
        composeRule.runOnIdle { assertEquals(PaddingValues(0.dp), receivedPadding) }
    }

    @Test
    fun customThumbnailDoesNotExecuteMainContent() {
        var contentExecutions = 0
        val custom by scene(thumbnail = { BasicText("Custom thumbnail") }) {
            SideEffect { contentExecutions++ }
            BasicText("Main content")
        }

        render(custom)

        composeRule.onNodeWithText("Custom thumbnail").assertExists()
        composeRule.onNodeWithText("Main content").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, contentExecutions) }
    }

    @Test
    fun disabledThumbnailDoesNotExecuteMainContent() {
        var contentExecutions = 0
        val disabled by scene(thumbnail = null) {
            SideEffect { contentExecutions++ }
            BasicText("Disabled content")
        }

        render(disabled)

        composeRule.onNodeWithText("Disabled content").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, contentExecutions) }
    }

    private fun render(scene: Scene) {
        composeRule.setContent {
            SceneThumbnailRenderSurface(SceneEntry(scene), SceneScope())
        }
    }
}
