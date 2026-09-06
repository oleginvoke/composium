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
 * When omitted, the runtime captures [content]. Explicit `null` disables thumbnail capture
 * and removes the preview area from the catalog card.
 * @param badge Optional content rendered over the catalog card thumbnail area. The runtime
 * positions it in the top-end corner, or beside the title when thumbnails are disabled.
 * @param content Scene content rendered inside [SceneScope].
 */
class Scene(
    val group: String?,
    val name: String,
    val tools: SceneTools = SceneTools.TopBar,
    thumbnail: (@Composable SceneScope.() -> Unit)? = DefaultSceneThumbnail,
    val badge: (@Composable () -> Unit)? = null,
    val content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
) {
    /** Resolved capture content, or `null` when the catalog card has no preview. */
    val thumbnail: (@Composable SceneScope.() -> Unit)? =
        if (thumbnail === DefaultSceneThumbnail) {
            { content(PaddingValues(0.dp)) }
        } else {
            thumbnail
        }

    constructor(
        group: String?,
        name: String,
        tools: SceneTools = SceneTools.TopBar,
        content: @Composable SceneScope.(contentPadding: PaddingValues) -> Unit,
    ) : this(
        group = group,
        name = name,
        tools = tools,
        thumbnail = DefaultSceneThumbnail,
        badge = null,
        content = content,
    )
}

// Distinguishes an omitted argument from explicit null. Resolved once by Scene;
// this sentinel must never enter composition or escape through Scene.thumbnail.
internal val DefaultSceneThumbnail: @Composable SceneScope.() -> Unit = {
    error("Default thumbnail must be resolved to scene content before rendering")
}

internal fun Scene.thumbnailContent(): (@Composable SceneScope.() -> Unit)? = thumbnail
