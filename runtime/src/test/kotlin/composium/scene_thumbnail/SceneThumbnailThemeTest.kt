package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.theme.ComposiumThemeController
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SceneThumbnailThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun automaticThumbnailCapturesContentInLightDarkAndLightThemes() {
        var dark by mutableStateOf(false)
        val captures = mutableMapOf<Boolean, Color>()
        val entry = SceneEntry(Scene(null, "Theme-dependent content") {
            Box(Modifier.size(24.dp).background(if (host.theme.isDark) Color.Blue else Color.Red))
        })
        var captureRequest by mutableStateOf<SceneThumbnailCaptureRequest?>(request(entry, dark = false))

        composeRule.setContent {
            CompositionLocalProvider(
                LocalComposiumThemeController provides ComposiumThemeController(dark),
            ) {
                SceneThumbnailCaptureHost(
                    request = captureRequest,
                    onCaptured = { key, result ->
                        val pixels = result.image.toPixelMap()
                        captures[key.isDarkTheme] = pixels[pixels.width / 2, pixels.height / 2]
                        captureRequest = null
                    },
                    onFailed = { _, failure -> throw AssertionError(failure.reason, failure.throwable) },
                )
            }
        }
        advanceCaptureFrames()
        composeRule.runOnIdle {
            assertEquals(Color.Red, captures[false])
            dark = true
            captureRequest = request(entry, dark = true)
        }
        advanceCaptureFrames()
        composeRule.runOnIdle {
            assertEquals(Color.Blue, captures[true])
            captures.remove(false)
            dark = false
            captureRequest = request(entry, dark = false)
        }
        advanceCaptureFrames()
        composeRule.runOnIdle { assertEquals(Color.Red, captures[false]) }
    }

    @Test
    fun customThumbnailReadsCaptureThemeAndCannotControlTheHost() {
        var themeRequests = 0
        var captured = false
        lateinit var scope: SceneScope
        val entry = SceneEntry(Scene(
            group = null,
            name = "Read-only thumbnail host",
            thumbnail = {
                SideEffect { scope = this }
                Box(Modifier.size(24.dp).background(if (host.theme.isDark) Color.Blue else Color.Red))
            },
            content = { error("Custom thumbnail must not execute main content") },
        ))

        composeRule.setContent {
            // An in-flight capture must render its key's theme even if the controller differs.
            CompositionLocalProvider(
                LocalComposiumThemeController provides ComposiumThemeController(false) { themeRequests++ },
            ) {
                SceneThumbnailCaptureHost(
                    request = request(entry, dark = true),
                    onCaptured = { _, _ -> captured = true },
                    onFailed = { _, failure -> throw AssertionError(failure.reason, failure.throwable) },
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { captured }
        composeRule.runOnIdle {
            assertTrue(scope.host.theme.isDark)
            scope.host.theme.setDark(false)
            scope.host.theme.toggle()
            scope.host.controls.show()
            scope.host.controls.toggle()
            scope.host.eyedropper.show()
            scope.host.eyedropper.toggle()
            scope.host.onBack()
            scope.host.closeScene()
            assertEquals(0, themeRequests)
            assertTrue(scope.host.theme.isDark)
            assertFalse(scope.host.controls.isVisible)
            assertFalse(scope.host.eyedropper.isVisible)
        }
    }

    private fun request(entry: SceneEntry, dark: Boolean) = SceneThumbnailCaptureRequest(
        key = SceneThumbnailKey(
            sceneId = entry.id,
            isDarkTheme = dark,
        ),
        sceneEntry = entry,
    )

    private fun advanceCaptureFrames() {
        repeat(12) {
            composeRule.mainClock.advanceTimeBy(200)
            composeRule.waitForIdle()
        }
    }
}
