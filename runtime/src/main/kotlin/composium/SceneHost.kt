package oleginvoke.com.composium

import androidx.compose.runtime.Stable

/**
 * Access to the Composium UI hosting a scene: controls, eyedropper, theme and navigation.
 *
 * Owned by [SceneScope] and available as [SceneScope.host]; do not create or remember a
 * separate instance. This object and its state holders keep their identity for the scope's
 * lifetime. Read their observable properties during composition and invoke commands from
 * event handlers or effects.
 *
 * Commands do nothing in catalog thumbnails, [Scene.RenderPreview], or after the owning
 * scene leaves composition. Retaining a host does not let it control a subsequent opening.
 */
@Stable
class SceneHost internal constructor() {
    /** State and commands for the built-in Properties / Environment panel. */
    val controls: SceneControlsState = SceneControlsState()

    /** State and commands for the built-in eyedropper. */
    val eyedropper: SceneEyedropperState = SceneEyedropperState()

    /** Effective Composium theme and theme change requests. */
    val theme: SceneThemeState = SceneThemeState()

    internal var onCloseScene: (() -> Unit)? = null
    internal var onBackAction: (() -> Unit)? = null

    /**
     * Performs one step of Composium's Back behavior: hides the eyedropper, restores
     * expanded controls to split mode, hides controls, or returns to the catalog.
     * Use [closeScene] to return directly to the catalog instead.
     *
     * This callback keeps its identity for this host's lifetime.
     */
    val onBack: () -> Unit = { onBackAction?.invoke() }

    /** Closes this scene and returns directly to the catalog, even when scene tools are open. */
    fun closeScene() {
        onCloseScene?.invoke()
    }
}
