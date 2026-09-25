package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import oleginvoke.com.composium.FloatingToolsPosition

internal data class SceneFloatingToolsSafeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
)

internal data class SceneFloatingToolsPlacementBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    fun initialOffset(position: FloatingToolsPosition): Offset = when (position) {
        FloatingToolsPosition.TopLeft -> Offset(minX, minY)
        FloatingToolsPosition.CenterLeft -> Offset(minX, (minY + maxY) / 2f)
        FloatingToolsPosition.BottomLeft -> Offset(minX, maxY)
        FloatingToolsPosition.TopRight -> Offset(maxX, minY)
        FloatingToolsPosition.CenterRight -> Offset(maxX, (minY + maxY) / 2f)
        FloatingToolsPosition.BottomRight -> Offset(maxX, maxY)
    }

    fun clamp(offset: Offset): Offset = Offset(
        x = offset.x.coerceIn(minX, maxX),
        y = offset.y.coerceIn(minY, maxY),
    )
}

internal fun calculateSceneFloatingToolsPlacementBounds(
    containerSize: IntSize,
    toolsSizePx: IntSize,
    safeInsets: SceneFloatingToolsSafeInsets,
    marginPx: Int,
): SceneFloatingToolsPlacementBounds {
    val minX = (safeInsets.left + marginPx).toFloat()
    val minY = (safeInsets.top + marginPx).toFloat()
    val maxX = (containerSize.width - safeInsets.right - marginPx - toolsSizePx.width)
        .toFloat()
        .coerceAtLeast(minX)
    val maxY = (containerSize.height - safeInsets.bottom - marginPx - toolsSizePx.height)
        .toFloat()
        .coerceAtLeast(minY)
    return SceneFloatingToolsPlacementBounds(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY,
    )
}
