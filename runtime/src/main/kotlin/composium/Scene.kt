package oleginvoke.com.composium

import androidx.compose.runtime.Composable

/**
 * Composium scene definition.
 *
 * @param group Optional group path for scene categorization. `null` or blank means top-level scene.
 * @param name Scene display name.
 * @param content Scene content rendered inside [SceneScope].
 */
class Scene(
    val group: String?,
    val name: String,
    val content: @Composable SceneScope.() -> Unit,
)
