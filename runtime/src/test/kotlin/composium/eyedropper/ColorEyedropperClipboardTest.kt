package oleginvoke.com.composium.eyedropper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorEyedropperClipboardTest {

    @Test
    fun copyValueToClipboardWritesFormattedText() {
        var copiedText: String? = null
        val value = ColorEyedropperValue(
            format = ColorEyedropperFormat.HEX,
            label = "HEX",
            text = "#336699",
        )

        val copied = ColorEyedropperDefaults.copyValueToClipboard(value) { text ->
            copiedText = text
        }

        assertTrue(copied)
        assertEquals("#336699", copiedText)
    }

    @Test
    fun copyValueToClipboardReportsPlatformFailures() {
        val value = ColorEyedropperValue(
            format = ColorEyedropperFormat.HEX,
            label = "HEX",
            text = "#336699",
        )

        val copied = ColorEyedropperDefaults.copyValueToClipboard(value) {
            error("Clipboard unavailable")
        }

        assertFalse(copied)
    }
}
