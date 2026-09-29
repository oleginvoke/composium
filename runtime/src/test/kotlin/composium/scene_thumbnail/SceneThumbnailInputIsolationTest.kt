package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.main_screen.MainScreen
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalComposeUiApi::class)
class SceneThumbnailInputIsolationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thumbnailDoesNotForwardExplicitKeyboardShowOrHide() {
        val keyboard = RecordingKeyboardController()
        var effectExecuted = false
        val scene = Scene(null, "Keyboard effects") {
            val controller = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) {
                controller?.show()
                controller?.hide()
                effectExecuted = true
            }
            BasicTextField(value = "Thumbnail", onValueChange = {})
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                SceneThumbnailRenderSurface(SceneEntry(scene), remember { SceneScope() })
            }
        }

        composeRule.runOnIdle {
            assertTrue(effectExecuted, "The scene must run, not be skipped")
            assertEquals(0, keyboard.showCalls)
            assertEquals(0, keyboard.hideCalls)
        }
    }

    @Test
    fun thumbnailBlocksInputSessionButOpenedSceneStillStartsIt() {
        var inputSessions = 0
        var effects = 0
        var thumbnail by mutableStateOf(true)
        val scene = Scene(null, "Autofocus") {
            AutofocusField { effects++ }
        }
        composeRule.setContent {
            InterceptPlatformTextInput(interceptor = { _, _ ->
                inputSessions++
                awaitCancellation()
            }) {
                if (thumbnail) {
                    SceneThumbnailRenderSurface(SceneEntry(scene), remember { SceneScope() })
                } else {
                    scene.content(remember { SceneScope() }, PaddingValues(0.dp))
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(1, effects)
            assertEquals(0, inputSessions, "Thumbnail must not reach the platform input handler")
            thumbnail = false
        }
        composeRule.runOnIdle {
            assertEquals(2, effects)
            assertEquals(1, inputSessions, "Opened scene must retain normal text input")
        }
    }

    @Test
    fun captureCannotStealFocusFromCatalogSearch() {
        var request by mutableStateOf<SceneThumbnailCaptureRequest?>(null)
        var effectExecuted = false
        var captured = false
        var searchInputSessions = 0
        var activeSearchSessions = 0
        // Changing the interceptor itself restarts active input sessions in Compose.
        val searchInterceptor = PlatformTextInputInterceptor { _, _ ->
            searchInputSessions++
            activeSearchSessions++
            try {
                awaitCancellation()
            } finally {
                activeSearchSessions--
            }
        }
        val entry = SceneEntry(Scene(null, "Input scene") {
            AutofocusField { effectExecuted = true }
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box {
                    InterceptPlatformTextInput(interceptor = searchInterceptor) {
                        MainScreen(
                            scenes = listOf(entry),
                            onSceneSelected = {},
                            contentWindowInsets = WindowInsets(0),
                        )
                    }
                    SceneThumbnailCaptureHost(
                        request = request,
                        onCaptured = { _, _ -> captured = true },
                        onFailed = { _, failure -> throw AssertionError(failure.reason, failure.throwable) },
                    )
                }
            }
        }

        val search = composeRule.onNode(hasSetTextAction())
        search.performClick().performTextInput("Input")
        search.assertIsFocused()
        val sessionsBeforeCapture = composeRule.runOnIdle { searchInputSessions }
        composeRule.runOnIdle {
            assertTrue(searchInputSessions > 0, "Search must start a platform input session")
            request = SceneThumbnailCaptureRequest(
                key = SceneThumbnailKey(
                    sceneId = entry.id,
                    isDarkTheme = false,
                    captureScale = 1f,
                    viewportWidthPx = 240,
                    viewportHeightPx = 120,
                ),
                sceneEntry = entry,
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { captured }
        composeRule.runOnIdle {
            assertTrue(effectExecuted, "Autofocus must actually be attempted")
            assertTrue(captured, "Input isolation must not prevent thumbnail capture")
            assertEquals(sessionsBeforeCapture, searchInputSessions, "Search session must not restart")
            assertEquals(1, activeSearchSessions, "Search session must not be cancelled")
        }
        // The thumbnail is a separate Android ComposeView, just as in the real catalog.
        val focusedSearch = composeRule.onNode(hasSetTextAction() and hasText("Input"))
        focusedSearch.assertIsFocused().performTextInput(" scene")
        val populatedSearch = composeRule.onNode(hasSetTextAction() and hasText("Input scene"))
        populatedSearch.assertTextEquals("Input scene")
        populatedSearch.performImeAction()
        populatedSearch.performClick().assertIsFocused()
        composeRule.runOnIdle {
            assertTrue(searchInputSessions > sessionsBeforeCapture,
                "Search must also be able to start a new input session while capture is mounted")
        }
        composeRule.runOnIdle { request = null }
        search.assertIsFocused().performTextInput("!")
        search.assertTextEquals("Input scene!")
    }
}

@Composable
private fun AutofocusField(onEffect: () -> Unit) {
    val requester = remember { FocusRequester() }
    BasicTextField(
        value = "Thumbnail",
        onValueChange = {},
        modifier = Modifier.focusRequester(requester),
    )
    LaunchedEffect(Unit) {
        requester.requestFocus()
        onEffect()
    }
}

private class RecordingKeyboardController : SoftwareKeyboardController {
    var showCalls = 0
    var hideCalls = 0
    override fun show() { showCalls++ }
    override fun hide() { hideCalls++ }
}
