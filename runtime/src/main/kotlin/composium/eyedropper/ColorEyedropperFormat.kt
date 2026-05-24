package oleginvoke.com.composium.eyedropper

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A text representation supported by the color eyedropper.
 *
 * Each format can turn the currently sampled sRGB color into the exact string that will be
 * displayed in the floating island and copied to the clipboard.
 */
internal enum class ColorEyedropperFormat(
    /**
     * Short label shown next to the formatted color value.
     */
    val label: String,
) {
    /**
     * Six-digit web hex notation, for example `#336699`.
     */
    HEX("HEX"),

    /**
     * Eight-digit web hex notation with alpha at the end, for example `#336699FF`.
     */
    HEXA("HEXA"),

    /**
     * CSS `rgb(...)` notation using integer sRGB channels.
     */
    RGB("RGB"),

    /**
     * CSS `rgba(...)` notation using integer sRGB channels and normalized alpha.
     */
    RGBA("RGBA"),

    /**
     * CSS `hsl(...)` notation using rounded hue, saturation, and lightness values.
     */
    HSL("HSL"),

    /**
     * CSS `hsla(...)` notation using rounded hue, saturation, lightness, and normalized alpha.
     */
    HSLA("HSLA"),

    /**
     * HSV notation using rounded hue, saturation, and value values.
     */
    HSV("HSV"),

    /**
     * Android-style ARGB integer notation, for example `0xFF336699`.
     */
    ARGB_INT("ARGB");

    /**
     * Formats [color] in this representation.
     */
    fun format(color: Color): String {
        val channels = color.toSrgbChannels()
        return when (this) {
            HEX -> "#${channels.red.hex2()}${channels.green.hex2()}${channels.blue.hex2()}"
            HEXA -> "#${channels.red.hex2()}${channels.green.hex2()}${channels.blue.hex2()}${channels.alpha.hex2()}"
            RGB -> "rgb(${channels.red}, ${channels.green}, ${channels.blue})"
            RGBA -> "rgba(${channels.red}, ${channels.green}, ${channels.blue}, ${channels.alpha.alphaText()})"
            HSL -> channels.hsl().let { hsl ->
                "hsl(${hsl.hue}, ${hsl.saturation}%, ${hsl.lightness}%)"
            }
            HSLA -> channels.hsl().let { hsl ->
                "hsla(${hsl.hue}, ${hsl.saturation}%, ${hsl.lightness}%, ${channels.alpha.alphaText()})"
            }
            HSV -> channels.hsv().let { hsv ->
                "hsv(${hsv.hue}, ${hsv.saturation}%, ${hsv.value}%)"
            }
            ARGB_INT -> "0x${channels.alpha.hex2()}${channels.red.hex2()}${channels.green.hex2()}${channels.blue.hex2()}"
        }
    }
}

/**
 * A formatted color value ready to display and copy.
 *
 * @param format Source format used to create [text].
 * @param label Short human-readable format label.
 * @param text Formatted color value.
 */
@Immutable
internal data class ColorEyedropperValue(
    val format: ColorEyedropperFormat,
    val label: String,
    val text: String,
)

internal fun buildColorEyedropperValues(
    color: Color,
    formats: List<ColorEyedropperFormat>,
): List<ColorEyedropperValue> =
    formats.map { format ->
        ColorEyedropperValue(
            format = format,
            label = format.label,
            text = format.format(color),
        )
    }

private data class SrgbChannels(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
)

private data class Hsl(
    val hue: Int,
    val saturation: Int,
    val lightness: Int,
)

private data class Hsv(
    val hue: Int,
    val saturation: Int,
    val value: Int,
)

private fun Color.toSrgbChannels(): SrgbChannels {
    val srgb = convert(ColorSpaces.Srgb)
    return SrgbChannels(
        red = srgb.red.toChannel(),
        green = srgb.green.toChannel(),
        blue = srgb.blue.toChannel(),
        alpha = srgb.alpha.toChannel(),
    )
}

private fun Float.toChannel(): Int =
    (coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)

private fun Int.hex2(): String =
    toString(radix = 16).uppercase().padStart(length = 2, padChar = '0')

private fun Int.alphaText(): String {
    val rounded = ((coerceIn(0, 255) / 255f) * 1000f).roundToInt().coerceIn(0, 1000)
    if (rounded == 0) return "0"
    if (rounded == 1000) return "1"
    val fraction = (rounded % 1000).toString().padStart(length = 3, padChar = '0').trimEnd('0')
    return "0.$fraction"
}

private fun SrgbChannels.hsl(): Hsl {
    val r = red / 255f
    val g = green / 255f
    val b = blue / 255f
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val lightness = (max + min) / 2f
    val saturation = if (delta == 0f) {
        0f
    } else {
        delta / (1f - abs(2f * lightness - 1f))
    }
    return Hsl(
        hue = hueOf(r = r, g = g, b = b, max = max, delta = delta),
        saturation = saturation.percent(),
        lightness = lightness.percent(),
    )
}

private fun SrgbChannels.hsv(): Hsv {
    val r = red / 255f
    val g = green / 255f
    val b = blue / 255f
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val saturation = if (max == 0f) 0f else delta / max
    return Hsv(
        hue = hueOf(r = r, g = g, b = b, max = max, delta = delta),
        saturation = saturation.percent(),
        value = max.percent(),
    )
}

private fun hueOf(
    r: Float,
    g: Float,
    b: Float,
    max: Float,
    delta: Float,
): Int {
    if (delta == 0f) return 0
    val raw = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return ((raw.roundToInt() % 360) + 360) % 360
}

private fun Float.percent(): Int =
    (coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
