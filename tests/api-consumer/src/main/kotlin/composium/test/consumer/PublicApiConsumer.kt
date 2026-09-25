package composium.test.consumer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.RenderPreview
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.SceneTools
import oleginvoke.com.composium.FloatingToolsPosition
import oleginvoke.com.composium.SceneControlsState
import oleginvoke.com.composium.SceneEyedropperState
import oleginvoke.com.composium.SceneThemeState
import oleginvoke.com.composium.scene

val consumerScene by scene(thumbnail = null, tools = SceneTools.None) { contentPadding: PaddingValues ->
    Box(Modifier.padding(contentPadding))
}

val floatingConsumerScene by scene(
    tools = SceneTools.Floating(initialPosition = FloatingToolsPosition.CenterLeft, initiallyExpanded = true),
) { contentPadding ->
    Box(Modifier.padding(contentPadding))
}

fun consumerCloseCallback(scope: SceneScope): () -> Unit = scope::closeScene

fun consumerBackCallback(scope: SceneScope): () -> Unit = scope.onBack

fun consumerControls(scope: SceneScope): SceneControlsState = scope.controlsState
fun consumerEyedropper(scope: SceneScope): SceneEyedropperState = scope.eyedropperState
fun consumerTheme(scope: SceneScope): SceneThemeState = scope.themeState

fun consumerToolActions(scope: SceneScope): List<() -> Unit> = listOf(
    scope.controlsState::show, scope.controlsState::hide, scope.controlsState::toggle,
    scope.eyedropperState::show, scope.eyedropperState::hide, scope.eyedropperState::toggle,
    scope.themeState::toggle, { scope.themeState.setDark(true) },
)

fun consumerToolValues(scope: SceneScope): List<Boolean> = listOf(
    scope.controlsState.isVisible, scope.eyedropperState.isVisible, scope.themeState.isDark,
)

@Composable
fun ConsumerScreen() {
    ComposiumScreen()
    ComposiumScreen(modifier = Modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0))
    consumerScene.RenderPreview(modifier = Modifier)
}
