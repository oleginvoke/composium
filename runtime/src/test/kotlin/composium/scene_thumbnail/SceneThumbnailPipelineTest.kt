package oleginvoke.com.composium.scene_thumbnail

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneThumbnailPipelineTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun queuedScenesShareAPrivateDisplayAndInheritTheCatalogComposition() {
        val color = compositionLocalOf { Color.Red }
        val displayIds = mutableMapOf<String, Int>()
        val colors = mutableListOf<Color>()
        lateinit var manager: DisplayManager
        var catalogDisplay = -1
        val entries = listOf("A", "B", "C").map { name ->
            SceneEntry(Scene(null, name) {
                val view = LocalView.current
                SideEffect { displayIds[name] = view.display.displayId }
                Box(Modifier.size(12.dp).background(color.current))
            })
        }
        val requests = entries.associateBy { entry -> keyFor(entry) }
        val queue = SceneThumbnailQueue().apply { sync(requests.keys.toList()) }
        rule.setContent {
            val host = LocalView.current
            SideEffect {
                manager = host.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                catalogDisplay = host.display.displayId
            }
            CompositionLocalProvider(color provides Color.Blue) {
                SceneThumbnailCaptureHost(
                    queue, { key -> requests[key]?.let { SceneThumbnailCaptureRequest(key, it) } }, { false },
                    { _, result -> colors.add(result.image.toPixelMap()[1, 1]) },
                    { _, failure -> throw AssertionError(failure.reason, failure.throwable) },
                )
            }
        }
        rule.waitUntil(5_000) { rule.waitForIdle(); colors.size == entries.size }
        rule.runOnIdle {
            assertEquals(listOf(Color.Blue, Color.Blue, Color.Blue), colors)
            assertEquals(entries.size, displayIds.size)
            val displayId = displayIds.values.distinct().single()
            assertNotEquals(catalogDisplay, displayId)
            assertNull(manager.getDisplay(displayId))
        }
    }

    @Test
    fun leavingWhilePausedRequeuesTheUnpublishedFrame() {
        var paused by mutableStateOf(false)
        var mounted by mutableStateOf(true)
        var composed = false
        var resultCount = 0
        val entry = SceneEntry(Scene(null, "Interrupted") {
            SideEffect { if (!composed) { composed = true; paused = true } }
            Box(Modifier.size(12.dp).background(Color.Blue))
        })
        val key = keyFor(entry)
        val queue = SceneThumbnailQueue().apply { sync(listOf(key)) }
        rule.setContent {
            if (mounted) {
                SceneThumbnailCaptureHost(
                    queue, { SceneThumbnailCaptureRequest(it, entry) }, { paused },
                    { _, _ -> resultCount++ },
                    { _, failure -> throw AssertionError(failure.reason, failure.throwable) },
                )
            }
        }
        rule.waitUntil(5_000) { rule.waitForIdle(); composed }
        rule.runOnIdle { assertEquals(0, resultCount); mounted = false }
        rule.waitForIdle()
        rule.runOnIdle { paused = false; mounted = true }
        rule.waitUntil(5_000) { rule.waitForIdle(); resultCount > 0 }
        rule.runOnIdle { assertEquals(1, resultCount); assertTrue(composed) }
    }

    private fun keyFor(entry: SceneEntry) =
        SceneThumbnailKey(entry.id, false, captureScale = 1f, viewportWidthPx = 120, viewportHeightPx = 160)
}
