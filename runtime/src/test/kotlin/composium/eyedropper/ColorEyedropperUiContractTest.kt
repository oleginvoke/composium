package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorEyedropperUiContractTest {

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
