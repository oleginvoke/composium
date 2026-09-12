package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneFloatingToolsDragLogicTest {
    @Test
    fun placementUsesPhysicalSafeBoundsAndStartsAtTopRight() {
        val bounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(width = 400, height = 800),
            toolsSizePx = IntSize(width = 64, height = 288),
            safeInsets = SceneFloatingToolsSafeInsets(left = 7, top = 24, right = 20, bottom = 30),
            marginPx = 12,
        )

        assertEquals(19f, bounds.minX)
        assertEquals(36f, bounds.minY)
        assertEquals(304f, bounds.maxX)
        assertEquals(470f, bounds.maxY)
        assertEquals(Offset(x = 304f, y = 36f), bounds.topRightOffset)
    }

    @Test
    fun placementClampsTheWholeCapsuleToEveryScreenEdge() {
        val bounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(width = 400, height = 800),
            toolsSizePx = IntSize(width = 64, height = 288),
            safeInsets = SceneFloatingToolsSafeInsets(left = 7, top = 24, right = 20, bottom = 30),
            marginPx = 12,
        )

        assertEquals(Offset(x = 19f, y = 36f), bounds.clamp(Offset(-500f, -500f)))
        assertEquals(Offset(x = 304f, y = 470f), bounds.clamp(Offset(900f, 900f)))
    }

    @Test
    fun placementStillHasOneValidPositionWhenViewportIsTooSmall() {
        val bounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(width = 80, height = 90),
            toolsSizePx = IntSize(width = 64, height = 288),
            safeInsets = SceneFloatingToolsSafeInsets(left = 7, top = 8, right = 9, bottom = 10),
            marginPx = 12,
        )

        assertEquals(bounds.minX, bounds.maxX)
        assertEquals(bounds.minY, bounds.maxY)
        assertEquals(bounds.topRightOffset, bounds.clamp(Offset(500f, 500f)))
    }
}
