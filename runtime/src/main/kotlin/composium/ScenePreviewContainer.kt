package oleginvoke.com.composium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

fun interface ScenePreviewDecorator {

    /**
     * To allow you to control the placement of the inner scene screen relative to your decorations,
     * the scene implementation will pass in a framework-controlled composable parameter
     * [scenePreview] to this method. You must not call [scenePreview] more than once.
     *
     * The preview canvas is owned by the runtime. Custom decorators should usually add layout,
     * padding, or extra chrome around [scenePreview] and then invoke it directly.
     */
    @Composable
    fun Decoration(scenePreview: @Composable () -> Unit)
}

@PublishedApi
internal val NoOpScenePreviewDecorator: ScenePreviewDecorator =
    ScenePreviewDecorator { scenePreview ->
        scenePreview()
    }

@PublishedApi
internal fun defaultScenePreviewDecorator(): ScenePreviewDecorator = NoOpScenePreviewDecorator

@PublishedApi
internal val LocalScenePreviewContainer = staticCompositionLocalOf<ScenePreviewDecorator> {
    defaultScenePreviewDecorator()
}
