package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorEyedropperFormatTest {

    @Test
    fun defaultUiFormatsAreCompactCopyFormats() {
        assertEquals(
            listOf(
                ColorEyedropperFormat.HEX,
                ColorEyedropperFormat.HEXA,
                ColorEyedropperFormat.ARGB_INT,
            ),
            ColorEyedropperDefaults.formats,
        )
    }

    @Test
    fun formatsOpaqueSrgbColorInSupportedFormats() {
        val color = Color(red = 0x33, green = 0x66, blue = 0x99, alpha = 0xFF)

        val values = ColorEyedropperFormat.entries.associate { format ->
            format to format.format(color)
        }

        assertEquals("#336699", values[ColorEyedropperFormat.HEX])
        assertEquals("#336699FF", values[ColorEyedropperFormat.HEXA])
        assertEquals("rgb(51, 102, 153)", values[ColorEyedropperFormat.RGB])
        assertEquals("rgba(51, 102, 153, 1)", values[ColorEyedropperFormat.RGBA])
        assertEquals("hsl(210, 50%, 40%)", values[ColorEyedropperFormat.HSL])
        assertEquals("hsla(210, 50%, 40%, 1)", values[ColorEyedropperFormat.HSLA])
        assertEquals("hsv(210, 67%, 60%)", values[ColorEyedropperFormat.HSV])
        assertEquals("0xFF336699", values[ColorEyedropperFormat.ARGB_INT])
    }

    @Test
    fun formatsAlphaWithoutNoisyTrailingZeros() {
        val color = Color(red = 255, green = 128, blue = 0, alpha = 128)

        assertEquals("rgba(255, 128, 0, 0.502)", ColorEyedropperFormat.RGBA.format(color))
        assertEquals("hsla(30, 100%, 50%, 0.502)", ColorEyedropperFormat.HSLA.format(color))
        assertEquals("#FF800080", ColorEyedropperFormat.HEXA.format(color))
        assertEquals("0x80FF8000", ColorEyedropperFormat.ARGB_INT.format(color))
    }
}
