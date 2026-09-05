package oleginvoke.com.composium.scene_screen

import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
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
class SceneScreenPaddingDensityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smallerPreviewDensityPreservesPhysicalTopBarAndSystemInsets() {
        assertPhysicalPadding(SceneTools.TopBar, previewDensity = 1f, expectedTopPx = 192f)
    }

    @Test
    fun largerPreviewDensityPreservesPhysicalTopBarAndSystemInsets() {
        assertPhysicalPadding(SceneTools.TopBar, previewDensity = 4f, expectedTopPx = 192f)
    }

    @Test
    fun smallerPreviewDensityPreservesPhysicalFloatingSystemInsets() {
        assertPhysicalPadding(SceneTools.Floating, previewDensity = 1f, expectedTopPx = 48f)
    }

    @Test
    fun largerPreviewDensityPreservesPhysicalFloatingSystemInsets() {
        assertPhysicalPadding(SceneTools.Floating, previewDensity = 4f, expectedTopPx = 48f)
    }

    private fun assertPhysicalPadding(
        tools: SceneTools,
        previewDensity: Float,
        expectedTopPx: Float,
    ) {
        lateinit var scope: SceneScope
        var renderedDensity = 0f
        val entry = SceneEntry(Scene(group = null, name = "Density regression", tools = tools) { padding ->
            scope = this
            renderedDensity = LocalDensity.current.density
            Box(Modifier.fillMaxSize().testTag("preview")) {
                Box(Modifier.fillMaxSize().padding(padding).testTag("safe content"))
            }
        })
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                ComposiumTheme(darkTheme = false) {
                    SceneScreen(
                        sceneEntry = entry,
                        onBack = {},
                        contentWindowInsets = WindowInsets(top = 48, bottom = 64),
                    )
                }
            }
        }
        composeRule.runOnIdle {
            // Set the actual display-size override used by the Environment panel.
            scope.preview.displayScaleOverride = previewDensity / (DisplayMetrics.DENSITY_DEVICE_STABLE / 160f)
        }
        val preview = composeRule.onNodeWithTag("preview").fetchSemanticsNode().boundsInRoot
        val safeContent = composeRule.onNodeWithTag("safe content").fetchSemanticsNode().boundsInRoot
        assertEquals(previewDensity, renderedDensity, 0.001f)
        // Host density is 2: the 72 dp top bar occupies 144 px, plus a 48 px status bar.
        // Floating mode contributes only the 48 px status bar. The navigation bar is 64 px.
        assertEquals(expectedTopPx, safeContent.top - preview.top, 0.01f)
        assertEquals(64f, preview.bottom - safeContent.bottom, 0.01f)
    }
}
