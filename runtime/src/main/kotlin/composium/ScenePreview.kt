package oleginvoke.com.composium

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders this scene as regular Compose content for use from an Android Studio `@Preview`.
 *
 * The preview owns an isolated [SceneScope] and does not share state with a running
 * [ComposiumScreen].
 *
 * @param modifier Modifier applied to the preview container.
 */
@Composable
fun Scene.RenderPreview(
    modifier: Modifier = Modifier,
) {
    val sceneScope = remember(group, name) { SceneScope() }
    Box(modifier = modifier) {
        content(sceneScope)
    }
}
