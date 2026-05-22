package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.min

internal data class IntPixelCoordinate(
    val x: Int,
    val y: Int,
)

internal enum class ColorEyedropperNudgeDirection(
    val deltaX: Int,
    val deltaY: Int,
) {
    Top(deltaX = 0, deltaY = -1),
    Bottom(deltaX = 0, deltaY = 1),
    Start(deltaX = -1, deltaY = 0),
    End(deltaX = 1, deltaY = 0),
}

internal data class ColorEyedropperSnapshot(
    val pixels: IntArray,
    val bitmapSize: IntSize,
    val containerSize: IntSize,
) {
    fun sample(target: Offset): Color? {
        val coordinate = calculateColorEyedropperPixelCoordinate(
            target = target,
            containerSize = containerSize,
            bitmapSize = bitmapSize,
        ) ?: return null
        return readColorEyedropperPixel(
            pixels = pixels,
            bitmapSize = bitmapSize,
            coordinate = coordinate,
        )
    }

    fun pixelCoordinate(target: Offset): IntPixelCoordinate? =
        calculateColorEyedropperPixelCoordinate(
            target = target,
            containerSize = containerSize,
            bitmapSize = bitmapSize,
        )

    fun nudgeColorEyedropperTarget(
        target: Offset,
        direction: ColorEyedropperNudgeDirection,
    ): Offset {
        val coordinate = pixelCoordinate(target) ?: return target
        val nextCoordinate = IntPixelCoordinate(
            x = (coordinate.x + direction.deltaX).coerceIn(0, bitmapSize.width - 1),
            y = (coordinate.y + direction.deltaY).coerceIn(0, bitmapSize.height - 1),
        )
        return calculateColorEyedropperTargetCenter(
            coordinate = nextCoordinate,
            containerSize = containerSize,
            bitmapSize = bitmapSize,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorEyedropperSnapshot) return false
        return pixels.contentEquals(other.pixels) &&
            bitmapSize == other.bitmapSize &&
            containerSize == other.containerSize
    }

    override fun hashCode(): Int {
        var result = pixels.contentHashCode()
        result = 31 * result + bitmapSize.hashCode()
        result = 31 * result + containerSize.hashCode()
        return result
    }
}

internal data class ColorEyedropperPixelLens(
    val pixels: IntArray,
    val side: Int,
    val centerIndex: Int,
) {
    val centerColor: Color
        get() = Color(pixels[centerIndex])

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorEyedropperPixelLens) return false
        return pixels.contentEquals(other.pixels) &&
            side == other.side &&
            centerIndex == other.centerIndex
    }

    override fun hashCode(): Int {
        var result = pixels.contentHashCode()
        result = 31 * result + side
        result = 31 * result + centerIndex
        return result
    }
}

internal fun ColorEyedropperSnapshot.createColorEyedropperPixelLens(
    target: Offset,
    radius: Int,
): ColorEyedropperPixelLens? {
    if (radius < 0) return null
    val coordinate = calculateColorEyedropperPixelCoordinate(
        target = target,
        containerSize = containerSize,
        bitmapSize = bitmapSize,
    ) ?: return null

    val side = radius * 2 + 1
    val lensPixels = IntArray(side * side)
    repeat(side) { row ->
        val sourceY = (coordinate.y + row - radius).coerceIn(0, bitmapSize.height - 1)
        repeat(side) { column ->
            val sourceX = (coordinate.x + column - radius).coerceIn(0, bitmapSize.width - 1)
            lensPixels[row * side + column] = pixels[sourceY * bitmapSize.width + sourceX]
        }
    }

    return ColorEyedropperPixelLens(
        pixels = lensPixels,
        side = side,
        centerIndex = radius * side + radius,
    )
}

internal fun createColorEyedropperSnapshot(
    pixels: IntArray,
    bitmapSize: IntSize,
    containerSize: IntSize,
): ColorEyedropperSnapshot? {
    if (bitmapSize.width <= 0 || bitmapSize.height <= 0) return null
    if (containerSize.width <= 0 || containerSize.height <= 0) return null
    if (pixels.size < bitmapSize.width * bitmapSize.height) return null
    if (pixels.none { pixel -> pixel ushr 24 != 0 }) return null

    return ColorEyedropperSnapshot(
        pixels = pixels,
        bitmapSize = bitmapSize,
        containerSize = containerSize,
    )
}

internal fun createColorEyedropperSnapshotFromRootPixels(
    rootPixels: IntArray,
    rootSize: IntSize,
    hostTopLeft: IntOffset,
    containerSize: IntSize,
): ColorEyedropperSnapshot? {
    if (rootSize.width <= 0 || rootSize.height <= 0) return null
    if (containerSize.width <= 0 || containerSize.height <= 0) return null
    if (rootPixels.size < rootSize.width * rootSize.height) return null

    val left = hostTopLeft.x.coerceIn(0, rootSize.width)
    val top = hostTopLeft.y.coerceIn(0, rootSize.height)
    val width = min(containerSize.width, rootSize.width - left)
    val height = min(containerSize.height, rootSize.height - top)
    if (width <= 0 || height <= 0) return null

    val pixels = IntArray(width * height)
    repeat(height) { row ->
        rootPixels.copyInto(
            destination = pixels,
            destinationOffset = row * width,
            startIndex = (top + row) * rootSize.width + left,
            endIndex = (top + row) * rootSize.width + left + width,
        )
    }

    return createColorEyedropperSnapshot(
        pixels = pixels,
        bitmapSize = IntSize(width = width, height = height),
        containerSize = containerSize,
    )
}

internal fun clampColorEyedropperTarget(
    target: Offset,
    containerSize: IntSize,
): Offset {
    if (containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
    return Offset(
        x = target.x.coerceIn(0f, (containerSize.width - 1).coerceAtLeast(0).toFloat()),
        y = target.y.coerceIn(0f, (containerSize.height - 1).coerceAtLeast(0).toFloat()),
    )
}

internal fun calculateColorEyedropperPixelCoordinate(
    target: Offset,
    containerSize: IntSize,
    bitmapSize: IntSize,
): IntPixelCoordinate? {
    if (containerSize.width <= 0 || containerSize.height <= 0) return null
    if (bitmapSize.width <= 0 || bitmapSize.height <= 0) return null

    val x = floor(
        target.x.coerceIn(0f, containerSize.width.toFloat()) / containerSize.width * bitmapSize.width,
    ).toInt().coerceIn(0, bitmapSize.width - 1)
    val y = floor(
        target.y.coerceIn(0f, containerSize.height.toFloat()) / containerSize.height * bitmapSize.height,
    ).toInt().coerceIn(0, bitmapSize.height - 1)

    return IntPixelCoordinate(x = x, y = y)
}

internal fun calculateColorEyedropperTargetCenter(
    coordinate: IntPixelCoordinate,
    containerSize: IntSize,
    bitmapSize: IntSize,
): Offset {
    if (containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
    if (bitmapSize.width <= 0 || bitmapSize.height <= 0) return Offset.Zero

    return clampColorEyedropperTarget(
        target = Offset(
            x = (coordinate.x + 0.5f) / bitmapSize.width * containerSize.width,
            y = (coordinate.y + 0.5f) / bitmapSize.height * containerSize.height,
        ),
        containerSize = containerSize,
    )
}

internal fun readColorEyedropperPixel(
    pixels: IntArray,
    bitmapSize: IntSize,
    coordinate: IntPixelCoordinate,
): Color? {
    if (bitmapSize.width <= 0 || bitmapSize.height <= 0) return null
    val index = coordinate.y * bitmapSize.width + coordinate.x
    if (index !in pixels.indices) return null
    return Color(pixels[index])
}
