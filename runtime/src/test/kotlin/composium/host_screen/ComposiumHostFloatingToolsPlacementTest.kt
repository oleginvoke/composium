package oleginvoke.com.composium.host_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposiumHostFloatingToolsPlacementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hostConsumesHorizontalInsetsExactlyOnceForFloatingTools() {
        val sceneName = "Host inset placement regression"
        ComposiumRuntime.register(
            Scene(group = null, name = sceneName, tools = SceneTools.Floating) {
                Box(Modifier.fillMaxSize().background(Color.Blue))
            },
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                ComposiumTheme(darkTheme = false) {
                    ComposiumHostScreen(
                        contentWindowInsets = WindowInsets(
                            left = 7,
                            top = 24,
                            right = 20,
                            bottom = 30,
                        ),
                        modifier = Modifier.testTag("host"),
                    )
                }
            }
        }

        composeRule.onNodeWithText(sceneName).performClick()

        val host = composeRule.onNodeWithTag("host").fetchSemanticsNode().boundsInRoot
        val back = composeRule.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        assertEquals(host.right - 20f - 12f - 104f, back.left, 1.1f)
        assertEquals(host.top + 24f + 12f, back.top, 1.1f)
    }
}
