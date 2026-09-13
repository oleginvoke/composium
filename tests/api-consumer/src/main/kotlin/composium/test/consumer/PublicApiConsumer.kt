package composium.test.consumer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import oleginvoke.com.composium.ComposiumScreen
import oleginvoke.com.composium.RenderPreview
import oleginvoke.com.composium.scene

val consumerScene by scene(thumbnail = null) { contentPadding: PaddingValues ->
    Box(Modifier.padding(contentPadding))
}

@Composable
fun ConsumerScreen() {
    ComposiumScreen()
    ComposiumScreen(modifier = Modifier, contentWindowInsets = WindowInsets(0, 0, 0, 0))
    consumerScene.RenderPreview(modifier = Modifier)
}
