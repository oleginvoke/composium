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
import oleginvoke.com.composium.SceneHost
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
    tools = SceneTools.Floating(initialPosition = FloatingToolsPosition.CenterStart, actionsInitiallyExpanded = true),
) { contentPadding ->
    Box(Modifier.padding(contentPadding))
}

fun consumerCloseCallback(scope: SceneScope): () -> Unit = scope.host::closeScene

fun consumerBackCallback(scope: SceneScope): () -> Unit = scope.host.onBack

fun consumerHost(scope: SceneScope): SceneHost = scope.host

fun consumerControls(scope: SceneScope): SceneControlsState = scope.host.controls
fun consumerEyedropper(scope: SceneScope): SceneEyedropperState = scope.host.eyedropper
fun consumerTheme(scope: SceneScope): SceneThemeState = scope.host.theme

fun consumerToolActions(scope: SceneScope): List<() -> Unit> = listOf(
    scope.host.controls::show, scope.host.controls::hide, scope.host.controls::toggle,
    scope.host.eyedropper::show, scope.host.eyedropper::hide, scope.host.eyedropper::toggle,
    scope.host.theme::toggle, { scope.host.theme.setDark(true) },
)

fun consumerToolValues(scope: SceneScope): List<Boolean> = listOf(
    scope.host.controls.isVisible, scope.host.eyedropper.isVisible, scope.host.theme.isDark,
)

@Composable
fun ConsumerScreen() {
    ComposiumScreen()
    ComposiumScreen(modifier = Modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0))
    consumerScene.RenderPreview(modifier = Modifier)
}
