package oleginvoke.com.composium

import androidx.compose.runtime.Composable

/**
 * Composium scene definition.
 *
 * @param group Optional group path for scene categorization. `null` or blank means top-level scene.
 * @param name Scene display name.
 * @param enableEdgeToEdge Controls how Composium handles top-bar and system navigation-bar
 * insets for this scene.
 *
 * When `false` (default), Composium keeps the scene content below its top bar and above the
 * navigation bar. [SceneScope.innerPadding] is reported as zero because the runtime has already
 * applied the required spacing.
 *
 * When `true`, Composium lets the scene fill the whole preview area, including the area behind
 * the top bar and navigation bar. The corresponding top and bottom padding is exposed through
 * [SceneScope.innerPadding], and the scene is responsible for applying it where it makes sense
 * for its own layout.
 * @param content Scene content rendered inside [SceneScope].
 */
class Scene(
    val group: String?,
    val name: String,
    val enableEdgeToEdge: Boolean = false,
    val content: @Composable SceneScope.() -> Unit,
)
