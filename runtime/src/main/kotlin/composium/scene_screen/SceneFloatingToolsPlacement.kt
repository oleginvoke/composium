package oleginvoke.com.composium.scene_screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
    fun initialOffset(position: FloatingToolsPosition, layoutDirection: LayoutDirection): Offset {
        val startX = if (layoutDirection == LayoutDirection.Ltr) minX else maxX
        val endX = if (layoutDirection == LayoutDirection.Ltr) maxX else minX
        return when (position) {
            FloatingToolsPosition.TopStart -> Offset(startX, minY)
            FloatingToolsPosition.CenterStart -> Offset(startX, (minY + maxY) / 2f)
            FloatingToolsPosition.BottomStart -> Offset(startX, maxY)
            FloatingToolsPosition.TopEnd -> Offset(endX, minY)
            FloatingToolsPosition.CenterEnd -> Offset(endX, (minY + maxY) / 2f)
            FloatingToolsPosition.BottomEnd -> Offset(endX, maxY)
        }
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
