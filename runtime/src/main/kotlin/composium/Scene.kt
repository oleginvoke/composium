package oleginvoke.com.composium

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Composium scene definition.
 *
 * @param group Optional group path for scene categorization. `null` or blank means top-level scene.
 * @param name Scene display name.
 * @param tools Selects how Composium presents scene tools.
 * @param thumbnail Optional lightweight content used only for catalog thumbnail capture.
 * If `null`, the runtime captures [content].
 * @param badge Optional content rendered over the catalog card thumbnail area. The runtime
 * positions it in the top-end corner and does not constrain its size.
 * @param content Scene content rendered inside [SceneScope].
 */
class Scene(
    val group: String?,
    val name: String,
    val tools: SceneTools = SceneTools.TopBar,
    val thumbnail: (@Composable SceneScope.() -> Unit)? = null,
    val badge: (@Composable () -> Unit)? = null,
    val content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
) {
    constructor(
        group: String?,
        name: String,
        tools: SceneTools = SceneTools.TopBar,
        content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
    ) : this(
        group = group,
        name = name,
        tools = tools,
        thumbnail = null,
        badge = null,
        content = content,
    )
}

internal fun Scene.thumbnailContent(): @Composable SceneScope.() -> Unit =
    thumbnail ?: {
        val scene = this@thumbnailContent
        scene.content(this, PaddingValues(0.dp))
    }
