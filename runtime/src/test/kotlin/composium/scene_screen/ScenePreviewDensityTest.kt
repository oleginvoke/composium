package oleginvoke.com.composium.scene_screen

import android.os.Build
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 24, 35])
class ScenePreviewDensityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun displayDefaultWorksBeforeAndAfterDisplayZoomWasIntroduced() {
        val expected = if (Build.VERSION.SDK_INT >= 24) DisplayMetrics.DENSITY_DEVICE_STABLE / 160f else 3f
        assertEquals(expected, systemDefaultDisplayDensity(3f))
    }

    @Test fun densityAndPaddingRemainFiniteAtFloatLimits() {
        for (scale in listOf(Float.MIN_VALUE, 0.05f, 1.1375f, 12f, Float.MAX_VALUE)) {
            val density = resolvePreviewDensity(4f, scale)
            assertTrue(density.isFinite() && density > 0f)
            val padding = rescaleSceneContentPadding(
                PaddingValues(start = 0.dp, top = 24.dp, end = 8.dp, bottom = 0.dp),
                LayoutDirection.Rtl, hostDensity = 3f, previewDensity = density,
            )
            val edges = listOf(padding.calculateLeftPadding(LayoutDirection.Rtl), padding.calculateTopPadding(),
                padding.calculateRightPadding(LayoutDirection.Rtl), padding.calculateBottomPadding())
            assertTrue(edges.all { it.value.isFinite() && it.value >= 0f })
            assertEquals(0.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
            assertEquals(0.dp, padding.calculateBottomPadding())
        }
    }

    @Test fun smallestAcceptedDisplayScaleRendersRealScene() = renderScale(Float.MIN_VALUE, font = false)
    @Test fun largestAcceptedDisplayScaleRendersRealScene() = renderScale(Float.MAX_VALUE, font = false)
    @Test fun smallestAcceptedFontScaleRendersRealScene() = renderScale(Float.MIN_VALUE, font = true)
    @Test fun largestAcceptedFontScaleRendersRealScene() = renderScale(Float.MAX_VALUE, font = true)

    private fun renderScale(scale: Float, font: Boolean) {
        assertEquals(scale, parseScaleValue(scale.toString()))
        lateinit var scope: SceneScope
        val entry = SceneEntry(Scene(null, "Density limits") { padding ->
            scope = this
            Box(Modifier.fillMaxSize().padding(padding)) { BasicText("Example scene", Modifier.padding(16.dp)) }
        })
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f)) {
                ComposiumTheme(darkTheme = false) {
                    SceneScreen(entry, onBack = {}, contentWindowInsets = WindowInsets(top = 48, bottom = 64))
                }
            }
        }
        rule.runOnIdle {
            if (font) scope.preview.fontScaleOverride = scale else scope.preview.displayScaleOverride = scale
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(scale, if (font) scope.preview.fontScaleOverride else scope.preview.displayScaleOverride)
        }
    }
}
