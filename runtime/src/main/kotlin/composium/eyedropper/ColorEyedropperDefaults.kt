package oleginvoke.com.composium.eyedropper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default configuration values for [ColorEyedropperHost].
 */
object ColorEyedropperDefaults {

    /**
     * Formats displayed by the default floating island.
     */
    val formats: List<ColorEyedropperFormat> = listOf(
        ColorEyedropperFormat.HEX,
        ColorEyedropperFormat.HEXA,
        ColorEyedropperFormat.RGB,
        ColorEyedropperFormat.RGBA,
        ColorEyedropperFormat.HSL,
        ColorEyedropperFormat.HSLA,
        ColorEyedropperFormat.HSV,
        ColorEyedropperFormat.ARGB_INT,
    )

    /**
     * Default visual metrics used by the overlay.
     */
    val metrics: ColorEyedropperMetrics = ColorEyedropperMetrics()

    /**
     * Default light, airy colors for the eyedropper overlay.
     */
    @Composable
    fun colors(): ColorEyedropperColors = ColorEyedropperColors()

    /**
     * Copies a formatted color value and reports whether the platform accepted the request.
     *
     * Android does not require a permission for writing plain text into the clipboard, but platform
     * services can still reject the operation in restricted environments. This helper keeps the
     * default UI from reporting a successful copy when the platform call throws.
     */
    fun copyValueToClipboard(
        value: ColorEyedropperValue,
        copyText: (String) -> Unit,
    ): Boolean {
        return runCatching {
            copyText(value.text)
        }.isSuccess
    }
}

/**
 * Visual metrics used by the default eyedropper UI.
 */
@Immutable
data class ColorEyedropperMetrics(
    val islandSpacing: Dp = 14.dp,
    val safePadding: Dp = 8.dp,
    val crosshairArm: Dp = 42.dp,
    val crosshairGap: Dp = 5.dp,
    val crosshairDash: Dp = 4.dp,
    val crosshairDashGap: Dp = 1.dp,
    val islandMinWidth: Dp = 188.dp,
    val swatchSize: Dp = 34.dp,
)

/**
 * Color palette used by the default eyedropper UI.
 */
@Immutable
data class ColorEyedropperColors(
    val islandContainer: Color = Color(0xF7FFFFFF),
    val islandContent: Color = Color(0xFF0E1820),
    val islandSecondaryContent: Color = Color(0xFF5A6874),
    val islandOutline: Color = Color(0xCCD4DCE2),
    val rowPressedContainer: Color = Color(0xFFEAF2F7),
    val swatchOutline: Color = Color(0xFFFFFFFF),
    val shadow: Color = Color(0x330B1218),
)
