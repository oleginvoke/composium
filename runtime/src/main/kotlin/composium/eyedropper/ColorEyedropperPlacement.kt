package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal fun calculateColorEyedropperIslandOffset(
    target: Offset,
    containerSize: IntSize,
    islandSize: IntSize,
    spacingPx: Float,
    safePaddingPx: Float,
): IntOffset {
    if (containerSize.width <= 0 || containerSize.height <= 0) return IntOffset.Zero
    if (islandSize.width <= 0 || islandSize.height <= 0) {
        return IntOffset(
            x = target.x.roundToInt().coerceAtLeast(0),
            y = target.y.roundToInt().coerceAtLeast(0),
        )
    }

    val minX = safePaddingPx
    val minY = safePaddingPx
    val maxX = (containerSize.width - islandSize.width - safePaddingPx).coerceAtLeast(minX)
    val maxY = (containerSize.height - islandSize.height - safePaddingPx).coerceAtLeast(minY)

    val candidates = listOf(
        Offset(
            x = target.x + spacingPx,
            y = target.y - islandSize.height / 2f,
        ),
        Offset(
            x = target.x - spacingPx - islandSize.width,
            y = target.y - islandSize.height / 2f,
        ),
        Offset(
            x = target.x - islandSize.width / 2f,
            y = target.y + spacingPx,
        ),
        Offset(
            x = target.x - islandSize.width / 2f,
            y = target.y - spacingPx - islandSize.height,
        ),
    )

    val fitting = candidates.firstOrNull { candidate ->
        candidate.x >= minX &&
            candidate.y >= minY &&
            candidate.x + islandSize.width <= containerSize.width - safePaddingPx &&
            candidate.y + islandSize.height <= containerSize.height - safePaddingPx
    }
    val selected = fitting ?: candidates.first()

    return IntOffset(
        x = selected.x.coerceIn(minX, maxX).roundToInt(),
        y = selected.y.coerceIn(minY, maxY).roundToInt(),
    )
}
