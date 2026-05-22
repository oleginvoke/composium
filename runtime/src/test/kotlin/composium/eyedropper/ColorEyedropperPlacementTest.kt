package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorEyedropperPlacementTest {

    @Test
    fun placesIslandOnRightWhenItFits() {
        val result = calculateColorEyedropperIslandOffset(
            target = Offset(60f, 120f),
            containerSize = IntSize(width = 400, height = 320),
            islandSize = IntSize(width = 120, height = 90),
            spacingPx = 16f,
            safePaddingPx = 8f,
        )

        assertEquals(76, result.x)
        assertEquals(75, result.y)
    }

    @Test
    fun movesIslandToLeftWhenRightSideOverflows() {
        val result = calculateColorEyedropperIslandOffset(
            target = Offset(350f, 120f),
            containerSize = IntSize(width = 400, height = 320),
            islandSize = IntSize(width = 120, height = 90),
            spacingPx = 16f,
            safePaddingPx = 8f,
        )

        assertEquals(214, result.x)
        assertEquals(75, result.y)
    }

    @Test
    fun movesIslandBelowOrAboveWhenHorizontalSidesDoNotFit() {
        val below = calculateColorEyedropperIslandOffset(
            target = Offset(180f, 80f),
            containerSize = IntSize(width = 360, height = 360),
            islandSize = IntSize(width = 340, height = 100),
            spacingPx = 16f,
            safePaddingPx = 8f,
        )
        val above = calculateColorEyedropperIslandOffset(
            target = Offset(180f, 300f),
            containerSize = IntSize(width = 360, height = 360),
            islandSize = IntSize(width = 340, height = 100),
            spacingPx = 16f,
            safePaddingPx = 8f,
        )

        assertEquals(10, below.x)
        assertEquals(96, below.y)
        assertEquals(10, above.x)
        assertEquals(184, above.y)
    }

    @Test
    fun clampsIslandInsideTinyContainer() {
        val result = calculateColorEyedropperIslandOffset(
            target = Offset(24f, 24f),
            containerSize = IntSize(width = 80, height = 70),
            islandSize = IntSize(width = 120, height = 90),
            spacingPx = 12f,
            safePaddingPx = 8f,
        )

        assertEquals(8, result.x)
        assertEquals(8, result.y)
    }
}
