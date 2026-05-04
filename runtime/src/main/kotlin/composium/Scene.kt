package oleginvoke.com.composium

import androidx.compose.runtime.Composable

/**
 * Composium scene definition.
 *
 * @param group Optional group path for scene categorization. `null` or blank means top-level scene.
 * @param name Scene display name.
 * @param enableEdgeToEdge When `true`, scene content is allowed to render behind the top bar
 * and the system navigation bar; the runtime exposes the corresponding insets via
 * [SceneScope.innerPadding] so the scene author can pad their own content as desired.
 * When `false` (default) the runtime applies those insets itself and [SceneScope.innerPadding]
 * is reported as zero.
 * @param content Scene content rendered inside [SceneScope].
 */
class Scene(
    val group: String?,
    val name: String,
    val enableEdgeToEdge: Boolean = false,
    val content: @Composable SceneScope.() -> Unit,
)
