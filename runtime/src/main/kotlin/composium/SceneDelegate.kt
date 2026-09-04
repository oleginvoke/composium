package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlin.reflect.KProperty

/**
 * Property delegate used by [scene] for lazy [Scene] construction.
 *
 * @param explicitGroup Explicit scene group override.
 * @param explicitName Explicit scene name override.
 * @param tools Selects how Composium presents scene tools.
 * @param content Scene content.
 * @param thumbnail Optional content used only for catalog thumbnail capture.
 * @param badge Optional content rendered in the scene card thumbnail area.
 */
class SceneDelegate(
    private val explicitGroup: String?,
    private val explicitName: String?,
    private val tools: SceneTools,
    private val content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
    private val thumbnail: (@Composable SceneScope.() -> Unit)? = null,
    private val badge: (@Composable () -> Unit)? = null,
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
            tools = tools,
            content = content,
            thumbnail = thumbnail,
            badge = badge,
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
 * @param tools Selects how Composium presents scene tools.
 * @param thumbnail Optional lightweight content used only for catalog thumbnail capture. If `null`, [content] is used.
 * @param badge Optional content rendered in the top-end corner of the scene card thumbnail area.
 * @param content Scene content lambda.
 */
fun scene(
    group: String? = null,
    name: String? = null,
    tools: SceneTools = SceneTools.TopBar,
    thumbnail: (@Composable SceneScope.() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
): SceneDelegate = SceneDelegate(
    explicitGroup = group,
    explicitName = name,
    tools = tools,
    content = content,
    thumbnail = thumbnail,
    badge = badge,
)
