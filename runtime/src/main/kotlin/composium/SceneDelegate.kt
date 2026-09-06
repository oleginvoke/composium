package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlin.reflect.KProperty

/**
 * Property delegate used by [scene] for lazy [Scene] construction.
 * Obtain instances through [scene], including in custom scene factories.
 *
 * @param explicitGroup Explicit scene group override.
 * @param explicitName Explicit scene name override.
 * @param tools Selects how Composium presents scene tools.
 * @param content Scene content.
 * @param thumbnail Capture content. Omit to capture [content]; pass `null` for no preview.
 * @param badge Optional content rendered in the scene card thumbnail area.
 */
class SceneDelegate internal constructor(
    private val explicitGroup: String?,
    private val explicitName: String?,
    private val tools: SceneTools,
    private val content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
    private val thumbnail: (@Composable SceneScope.() -> Unit)? = DefaultSceneThumbnail,
    private val badge: (@Composable () -> Unit)? = null,
) {
    @Volatile
    private var cachedScene: Scene? = null

    /**
     * Binds a separate lazy cache to each property, including when a delegate is reused.
     * Binding does not create the [Scene] or execute its content.
     */
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): SceneDelegate =
        SceneDelegate(
            explicitGroup = explicitGroup,
            explicitName = explicitName ?: property.name,
            tools = tools,
            content = content,
            thumbnail = thumbnail,
            badge = badge,
        )

    /**
     * Creates [Scene] on the first read and returns the same instance on subsequent reads.
     *
     * @param thisRef Owner instance of delegated property.
     * @param property Delegated property metadata.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Scene {
        cachedScene?.let { return it }
        return synchronized(this) {
            cachedScene ?: createScene(property).also { cachedScene = it }
        }
    }

    private fun createScene(property: KProperty<*>): Scene {
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
 * @param thumbnail Lightweight capture content. Omit to capture [content]; explicit `null`
 * disables capture and removes the catalog card preview area.
 * @param badge Optional content rendered over the thumbnail, or beside the title without a thumbnail.
 * @param content Scene content lambda.
 */
fun scene(
    group: String? = null,
    name: String? = null,
    tools: SceneTools = SceneTools.TopBar,
    thumbnail: (@Composable SceneScope.() -> Unit)? = DefaultSceneThumbnail,
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
