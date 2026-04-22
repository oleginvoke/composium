package oleginvoke.com.composium

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import oleginvoke.com.composium.ui.components.ComposiumPreviewCanvas

fun interface ScenePreviewDecorator {

    /**
     * To allow you to control the placement of the inner scene screen relative to your decorations,
     * the scene implementation will pass in a framework-controlled composable parameter
     * [scenePreview] to this method. You must not call [scenePreview] more than once.
     */
    @Composable
    fun Decoration(scenePreview: @Composable () -> Unit)
}

@PublishedApi
internal val DefaultScenePreviewContainerSlot: ScenePreviewDecorator =
    ScenePreviewDecorator { scenePreview ->
        DefaultScenePreviewContainer(
            content = scenePreview,
        )
    }

@PublishedApi
internal val LocalScenePreviewContainer = staticCompositionLocalOf<ScenePreviewDecorator> {
    DefaultScenePreviewContainerSlot
}

/**
 * Default Composium scene preview container.
 *
 * @param content Scene content.
 */
@Composable
fun DefaultScenePreviewContainer(
    content: @Composable () -> Unit,
) {
    ComposiumPreviewCanvas(
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}
