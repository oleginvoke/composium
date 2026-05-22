package oleginvoke.com.composium.eyedropper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Creates and remembers state for [ColorEyedropperHost].
 *
 * @param initialTarget Initial target position in host coordinates. When left unspecified, the
 * host places the target in the center of its bounds when the eyedropper becomes visible.
 */
@Composable
fun rememberColorEyedropperState(
    initialTarget: Offset = Offset.Unspecified,
): ColorEyedropperState = remember {
    ColorEyedropperState(initialTarget = initialTarget)
}

/**
 * State object for a color eyedropper instance.
 *
 * The state exposes the current target position, sampled color, formatted values, and the latest
 * copied value. The host owns clamping and sampling, while callers can observe this state to build
 * custom controls or feedback.
 */
@Stable
class ColorEyedropperState internal constructor(
    initialTarget: Offset,
) {
    /**
     * Current crosshair target in host coordinates.
     */
    var target: Offset by mutableStateOf(initialTarget)
        private set

    /**
     * Current sampled color, or `null` until the first sample is available.
     */
    var color: Color? by mutableStateOf(null)
        private set

    /**
     * Last value copied through the default floating island.
     */
    var copiedValue: ColorEyedropperValue? by mutableStateOf(null)
        private set

    /**
     * Monotonic counter incremented each time a value is copied.
     */
    var copyRevision: Int by mutableIntStateOf(0)
        private set

    /**
     * Returns the currently sampled color formatted with [formats].
     */
    fun values(
        formats: List<ColorEyedropperFormat> = ColorEyedropperDefaults.formats,
    ): List<ColorEyedropperValue> {
        val sampled = color ?: return emptyList()
        return buildColorEyedropperValues(
            color = sampled,
            formats = formats,
        )
    }

    internal fun updateTarget(target: Offset) {
        this.target = target
    }

    internal fun updateColor(color: Color?) {
        this.color = color
    }

    internal fun markCopied(value: ColorEyedropperValue) {
        copiedValue = value
        copyRevision += 1
    }
}

internal val Offset.isSpecifiedForEyedropper: Boolean
    get() = !x.isNaN() && !y.isNaN()
