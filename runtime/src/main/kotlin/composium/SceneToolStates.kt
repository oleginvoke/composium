package oleginvoke.com.composium

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Observable state and actions for the Properties / Environment panel.
 * Owned by [SceneHost]; do not create or remember a separate instance.
 * Commands are for event handlers or effects, not composition, and do nothing in
 * thumbnails, [Scene.RenderPreview], or after the owning scene leaves composition.
 */
@Stable
class SceneControlsState internal constructor() {
    internal var binding by mutableStateOf<SceneToolStateBinding?>(null)

    /** Whether the panel is open, either split or expanded. */
    val isVisible: Boolean get() = binding?.read() ?: false

    /** Opens the panel in split mode if closed; preserves an already open panel. */
    fun show() { binding?.change(true) }

    /** Closes the panel in either split or expanded mode. */
    fun hide() { binding?.change(false) }

    /** Opens a closed panel in split mode, or closes an open one. */
    fun toggle() { binding?.let { it.change(!it.read()) } }
}

/**
 * Observable state and actions for the scene's eyedropper, owned by [SceneHost].
 * Commands are for event handlers or effects, not composition, and do nothing in
 * thumbnails, [Scene.RenderPreview], or after the owning scene leaves composition.
 */
@Stable
class SceneEyedropperState internal constructor() {
    internal var binding by mutableStateOf<SceneToolStateBinding?>(null)

    /** Whether the eyedropper is open. */
    val isVisible: Boolean get() = binding?.read() ?: false

    /** Opens the eyedropper unless controls occupy the full screen. */
    fun show() { binding?.change(true) }

    /** Hides the eyedropper if open. */
    fun hide() { binding?.change(false) }

    /** Toggles the eyedropper unless controls occupy the full screen. */
    fun toggle() { binding?.let { it.change(!it.read()) } }
}

/**
 * Observable effective Composium theme and theme change requests, owned by [SceneHost].
 * Commands are for event handlers or effects, not composition, and do nothing in
 * thumbnails, [Scene.RenderPreview], or after the owning scene leaves composition.
 * An inactive state reports [isDark] as false.
 */
@Stable
class SceneThemeState internal constructor() {
    internal var binding by mutableStateOf<SceneToolStateBinding?>(null)

    /** Effective theme; external changes are reflected here, not only requests from this object. */
    val isDark: Boolean get() = binding?.read() ?: false

    /**
     * Requests a theme through [ComposiumScreen]'s existing theme handling.
     * When the host supplies isDarkTheme, it must apply the request before [isDark] changes.
     */
    fun setDark(isDark: Boolean) { binding?.change(isDark) }

    /** Requests the opposite of the current effective theme. */
    fun toggle() { binding?.let { it.change(!it.read()) } }
}

// Reads the owning host's snapshot state directly instead of maintaining a second copy.
internal class SceneToolStateBinding(
    val read: () -> Boolean,
    val change: (Boolean) -> Unit,
)
