package oleginvoke.com.composium.eyedropper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.min

/**
 * Source used by the eyedropper to sample a color at a host-local coordinate.
 *
 * Custom implementations can bridge the eyedropper UI to another capture mechanism later, while
 * the default host implementation samples the Compose content wrapped by [ColorEyedropperHost].
 */
fun interface ColorSampleSource {
    /**
     * Returns a color for [target], or `null` when sampling is temporarily unavailable.
     */
    suspend fun sample(target: Offset): Color?
}

internal data class IntPixelCoordinate(
    val x: Int,
    val y: Int,
)

internal class GraphicsLayerColorSampleSource(
    private val graphicsLayer: GraphicsLayer,
    private val containerSize: () -> IntSize,
) : ColorSampleSource {

    override suspend fun sample(target: Offset): Color? {
        val bitmap = graphicsLayer.toImageBitmap()
        val bitmapSize = IntSize(width = bitmap.width, height = bitmap.height)
        val coordinate = calculateColorEyedropperPixelCoordinate(
            target = target,
            containerSize = containerSize(),
            bitmapSize = bitmapSize,
        ) ?: return null
        return bitmap.readColorEyedropperPixel(coordinate)
    }
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

internal fun ImageBitmap.toColorEyedropperSnapshot(
    containerSize: IntSize,
): ColorEyedropperSnapshot? {
    if (width <= 0 || height <= 0) return null
    if (containerSize.width <= 0 || containerSize.height <= 0) return null

    val pixels = IntArray(width * height)
    readPixels(
        buffer = pixels,
        startX = 0,
        startY = 0,
        width = width,
        height = height,
        bufferOffset = 0,
        stride = width,
    )
    return createColorEyedropperSnapshot(
        pixels = pixels,
        bitmapSize = IntSize(width = width, height = height),
        containerSize = containerSize,
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

private fun ImageBitmap.readColorEyedropperPixel(
    coordinate: IntPixelCoordinate,
): Color? {
    val pixel = IntArray(1)
    readPixels(
        buffer = pixel,
        startX = coordinate.x,
        startY = coordinate.y,
        width = 1,
        height = 1,
        bufferOffset = 0,
        stride = 1,
    )
    return readColorEyedropperPixel(
        pixels = pixel,
        bitmapSize = IntSize(width = 1, height = 1),
        coordinate = IntPixelCoordinate(x = 0, y = 0),
    )
}
