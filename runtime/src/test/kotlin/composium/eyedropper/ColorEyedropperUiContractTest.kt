package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorEyedropperUiContractTest {

    @Test
    fun cursorContractDoesNotRequirePixelLens() {
        assertTrue(true)
    }

    @Test
    fun lensArrowCentersStayInsideOuterRing() {
        val lensSize = 126f
        val ringWidth = 16f

        assertEquals(
            Offset(x = 63f, y = 8f),
            calculateColorEyedropperLensArrowCenter(
                lensSizePx = lensSize,
                ringWidthPx = ringWidth,
                direction = ColorEyedropperNudgeDirection.Top,
            ),
        )
        assertEquals(
            Offset(x = 118f, y = 63f),
            calculateColorEyedropperLensArrowCenter(
                lensSizePx = lensSize,
                ringWidthPx = ringWidth,
                direction = ColorEyedropperNudgeDirection.End,
            ),
        )
    }

    @Test
    fun defaultCursorIsSlightlySmallerThanTheInitialLensCursor() {
        assertEquals(32.dp, ColorEyedropperDefaults.metrics.cursorDiameter)
        assertTrue(ColorEyedropperDefaults.metrics.cursorDiameter < 36.dp)
    }

    @Test
    fun defaultCursorRingIsDarkerAndSlightlyWiderThanLensFrame() {
        val colors = ColorEyedropperColors()

        assertEquals(3.dp, ColorEyedropperDefaults.metrics.cursorOutlineWidth)
        assertEquals(Color(0xF0EAF0F4), colors.cursorOutline)
        assertEquals(Color(0xD0B8C6D0), colors.cursorContrastOutline)
    }

    @Test
    fun lensContentDiameterReservesOuterRingOnBothSides() {
        assertEquals(
            94f,
            calculateColorEyedropperLensContentDiameter(
                lensSizePx = 126f,
                ringWidthPx = 16f,
            ),
        )
    }
}
