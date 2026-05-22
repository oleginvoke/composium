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
     *
     * The eyedropper can still format every [ColorEyedropperFormat], but the built-in UI keeps
     * the copy surface compact and focused on the most useful native/app handoff values.
     */
    val formats: List<ColorEyedropperFormat> = listOf(
        ColorEyedropperFormat.HEX,
        ColorEyedropperFormat.HEXA,
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
    val islandSpacing: Dp = 6.dp,
    val lensSpacing: Dp = 10.dp,
    val safePadding: Dp = 8.dp,
    val crosshairArm: Dp = 42.dp,
    val crosshairGap: Dp = 5.dp,
    val crosshairDash: Dp = 4.dp,
    val crosshairDashGap: Dp = 1.dp,
    /** Minimum width for the compact copy island. */
    val islandMinWidth: Dp = 106.dp,
    /** Diameter of the draggable cursor that marks the sampled point. */
    val cursorDiameter: Dp = 32.dp,
    /** Reserved padding for custom cursor interiors; the default cursor is a transparent ring. */
    val cursorInnerPadding: Dp = 2.dp,
    /** Stroke width for the cursor ring. */
    val cursorOutlineWidth: Dp = 3.dp,
    /** Diameter of the magnified pixel lens. */
    val lensDiameter: Dp = 126.dp,
    /** Width of the outer lens ring that separates controls from the magnified pixel grid. */
    val lensInnerPadding: Dp = 16.dp,
    /** Invisible touch target size for each pixel nudge arrow in the lens ring. */
    val lensArrowTouchTarget: Dp = 30.dp,
    /** Number of source pixels sampled from the selected pixel to each lens edge. */
    val lensPixelRadius: Int = 5,
    /** Stroke width for the pixel grid inside the lens. */
    val lensGridStroke: Dp = 0.65.dp,
    /** Stroke width for the selected center pixel. */
    val lensCenterStroke: Dp = 2.dp,
    /** Stroke width for the outer lens ring. */
    val lensOutlineWidth: Dp = 1.dp,
    /** Stroke width for the inner border around magnified content. */
    val lensContentOutlineWidth: Dp = 1.dp,
    /** Stroke width for chevron nudge arrows inside the lens ring. */
    val lensChevronStroke: Dp = 1.7.dp,
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
    val cursorOutline: Color = Color(0xF0EAF0F4),
    val cursorContrastOutline: Color = Color(0xD0B8C6D0),
    val lensContainer: Color = Color(0xF4F8FBFD),
    val lensOutline: Color = Color(0xCCD4DCE2),
    val lensContentOutline: Color = Color(0xD0C8D4DE),
    val lensArrow: Color = Color(0xE6689EC2),
    val lensGrid: Color = Color(0x66101820),
    val lensCenterOutline: Color = Color(0xFF0E1820),
    val lensCenterContrast: Color = Color(0xFFFFFFFF),
    val shadow: Color = Color(0x330B1218),
)
