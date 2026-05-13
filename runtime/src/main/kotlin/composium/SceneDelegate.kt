package oleginvoke.com.composium

import androidx.compose.runtime.Composable
import kotlin.reflect.KProperty

/**
 * Property delegate used by [scene] for lazy [Scene] construction.
 *
 * @param explicitGroup Explicit scene group override.
 * @param explicitName Explicit scene name override.
 * @param enableEdgeToEdge Controls scene inset ownership. When `false` (default), Composium
 * keeps content out of the top bar and navigation bar. When `true`, the scene fills the whole
 * preview area and receives the top/bottom insets through [SceneScope.innerPadding].
 * @param content Scene content.
 */
class SceneDelegate(
    private val explicitGroup: String?,
    private val explicitName: String?,
    private val enableEdgeToEdge: Boolean,
    private val content: @Composable SceneScope.() -> Unit,
) {
    /**
     * Builds [Scene] from delegated property metadata.
     *
     * @param thisRef Owner instance of delegated property.
     * @param property Delegated property metadata.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Scene {
        // If group is not provided, we keep it null so the scene is rendered at the top level.
        val group = explicitGroup?.takeIf { it.isNotBlank() }
        val name = explicitName ?: property.name
        return Scene(
            group = group,
            name = name,
            enableEdgeToEdge = enableEdgeToEdge,
            content = content,
        )
    }
}

/**
 * Creates a [SceneDelegate] for scene declaration.
 *
 * Typical usage: `val MyScene by scene { ... }`.
 *
 * @param group Optional group path.
 * @param name Optional explicit scene name. If `null`, property name is used.
 * @param enableEdgeToEdge Controls scene inset ownership. When `false` (default), Composium
 * keeps content out of the top bar and navigation bar. When `true`, the scene fills the whole
 * preview area and receives the top/bottom insets through [SceneScope.innerPadding], so the
 * scene can decide where to apply them.
 * @param content Scene content lambda.
 */
fun scene(
    group: String? = null,
    name: String? = null,
    enableEdgeToEdge: Boolean = false,
    content: @Composable SceneScope.() -> Unit,
): SceneDelegate = SceneDelegate(
    explicitGroup = group,
    explicitName = name,
    enableEdgeToEdge = enableEdgeToEdge,
    content = content,
)
