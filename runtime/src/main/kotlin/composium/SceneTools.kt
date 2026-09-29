package oleginvoke.com.composium

import androidx.compose.runtime.Immutable

/** Selects how Composium exposes scene tools. */
@Immutable
sealed class SceneTools private constructor() {
    /** The built-in top bar. */
    data object TopBar : SceneTools()

    /** No built-in toolbar or floating tools. Actions remain available through [SceneScope]. */
    data object None : SceneTools()

    /**
     * Independently movable settings and actions buttons, initially placed as a vertical pair.
     * [initialPosition] and [actionsInitiallyExpanded] apply when the scene opens. Reopening the scene
     * restores these defaults; collapsing the actions does not reset either button's position.
     */
    class Floating(
        val initialPosition: FloatingToolsPosition = FloatingToolsPosition.CenterEnd,
        /** Whether secondary actions start expanded when the scene opens. */
        val actionsInitiallyExpanded: Boolean = false,
    ) : SceneTools()
}

/** Initial anchor inside the safe screen area. Start/end follow the host's layout direction. */
enum class FloatingToolsPosition {
    TopStart,
    CenterStart,
    BottomStart,
    TopEnd,
    CenterEnd,
    BottomEnd,
}
