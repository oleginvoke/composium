package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorEyedropperSamplingTest {

    @Test
    fun clampsTargetInsideContainerBounds() {
        val result = clampColorEyedropperTarget(
            target = Offset(-20f, 140f),
            containerSize = IntSize(width = 100, height = 80),
        )

        assertEquals(0f, result.x)
        assertEquals(79f, result.y)
    }

    @Test
    fun convertsTargetToBitmapPixelUsingScaleFactor() {
        val result = calculateColorEyedropperPixelCoordinate(
            target = Offset(75f, 40f),
            containerSize = IntSize(width = 150, height = 100),
            bitmapSize = IntSize(width = 300, height = 200),
        )

        assertEquals(150, result?.x)
        assertEquals(80, result?.y)
    }

    @Test
    fun clampsBitmapPixelToLastAvailablePixel() {
        val result = calculateColorEyedropperPixelCoordinate(
            target = Offset(150f, 100f),
            containerSize = IntSize(width = 150, height = 100),
            bitmapSize = IntSize(width = 300, height = 200),
        )

        assertEquals(299, result?.x)
        assertEquals(199, result?.y)
    }

    @Test
    fun returnsNullWhenContainerOrBitmapIsEmpty() {
        assertNull(
            calculateColorEyedropperPixelCoordinate(
                target = Offset.Zero,
                containerSize = IntSize.Zero,
                bitmapSize = IntSize(width = 10, height = 10),
            ),
        )
        assertNull(
            calculateColorEyedropperPixelCoordinate(
                target = Offset.Zero,
                containerSize = IntSize(width = 10, height = 10),
                bitmapSize = IntSize.Zero,
            ),
        )
    }

    @Test
    fun readsArgbPixelAsComposeColor() {
        val color = readColorEyedropperPixel(
            pixels = intArrayOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0x80336699.toInt(),
            ),
            bitmapSize = IntSize(width = 2, height = 2),
            coordinate = IntPixelCoordinate(x = 1, y = 1),
        )

        assertEquals(Color(0x80336699), color)
    }

    @Test
    fun snapshotSamplesCachedPixelsWithoutRecapturing() {
        val snapshot = ColorEyedropperSnapshot(
            pixels = intArrayOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0xFFFFFFFF.toInt(),
            ),
            bitmapSize = IntSize(width = 2, height = 2),
            containerSize = IntSize(width = 20, height = 20),
        )

        assertEquals(Color(0xFFFF0000), snapshot.sample(Offset(0f, 0f)))
        assertEquals(Color(0xFF00FF00), snapshot.sample(Offset(15f, 0f)))
        assertEquals(Color(0xFF0000FF), snapshot.sample(Offset(0f, 15f)))
        assertEquals(Color(0xFFFFFFFF), snapshot.sample(Offset(15f, 15f)))
    }

    @Test
    fun rejectsFullyTransparentSnapshotsAsNotReady() {
        val snapshot = createColorEyedropperSnapshot(
            pixels = intArrayOf(
                0x00000000,
                0x00000000,
                0x00000000,
                0x00000000,
            ),
            bitmapSize = IntSize(width = 2, height = 2),
            containerSize = IntSize(width = 20, height = 20),
        )

        assertNull(snapshot)
    }

    @Test
    fun acceptsOpaqueBlackSnapshotsAsValidColors() {
        val snapshot = createColorEyedropperSnapshot(
            pixels = intArrayOf(
                0xFF000000.toInt(),
                0x00000000,
                0x00000000,
                0x00000000,
            ),
            bitmapSize = IntSize(width = 2, height = 2),
            containerSize = IntSize(width = 20, height = 20),
        )

        assertEquals(Color(0xFF000000), snapshot?.sample(Offset.Zero))
    }

    @Test
    fun createsHostSnapshotByCroppingRootPixels() {
        val snapshot = createColorEyedropperSnapshotFromRootPixels(
            rootPixels = intArrayOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0xFFFFFFFF.toInt(),
            ),
            rootSize = IntSize(width = 2, height = 2),
            hostTopLeft = IntOffset(x = 1, y = 0),
            containerSize = IntSize(width = 1, height = 2),
        )

        assertEquals(Color(0xFF00FF00), snapshot?.sample(Offset(0f, 0f)))
        assertEquals(Color(0xFFFFFFFF), snapshot?.sample(Offset(0f, 1f)))
    }
}
