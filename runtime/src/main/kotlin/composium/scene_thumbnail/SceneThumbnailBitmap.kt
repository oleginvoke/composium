package oleginvoke.com.composium.scene_thumbnail

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SceneThumbnailCaptureResult(
    val image: ImageBitmap,
    val byteSizeBytes: Int,
    val captureScale: Float,
)

/** One producer and one sequential consumer. The producer can draw one frame ahead. */
internal class SceneThumbnailBuffers : AutoCloseable {
    private val frames = arrayOfNulls<Bitmap>(2)
    private var nextSlot = 0
    private var pixels = IntArray(0)

    fun nextFrame(width: Int, height: Int): Bitmap {
        val slot = nextSlot
        nextSlot = 1 - slot
        val bitmap = frames[slot]
        if (bitmap != null && bitmap.width == width && bitmap.height == height) return bitmap
        bitmap?.recycle()
        return createBitmap(width, height).also { frames[slot] = it }
    }

    suspend fun process(frame: Bitmap, key: SceneThumbnailKey): SceneThumbnailCaptureResult =
        withContext(Dispatchers.Default) {
            val pixelCount = frame.width * frame.height
            if (pixels.size < pixelCount) pixels = IntArray(pixelCount)
            frame.toSceneThumbnailCaptureResult(pixels, key.targetWidthPx, key.targetHeightPx, key.captureScale)
        }

    override fun close() {
        frames.forEach { it?.recycle() }
    }
}

private fun Bitmap.toSceneThumbnailCaptureResult(
    pixels: IntArray,
    maxWidthPx: Int,
    maxHeightPx: Int,
    captureScale: Float,
): SceneThumbnailCaptureResult {
    getPixels(pixels, 0, width, 0, 0, width, height)
    val bounds = findSceneThumbnailContentBounds(pixels, width, height) ?: error("Scene has no visible content")
    val cropped = copyBounds(bounds)

    val targetSize = calculateSceneThumbnailScaledSize(
        contentWidthPx = cropped.width,
        contentHeightPx = cropped.height,
        maxWidthPx = maxWidthPx,
        maxHeightPx = maxHeightPx,
    )
    val thumbnailBitmap = if (
        targetSize.widthPx == cropped.width &&
        targetSize.heightPx == cropped.height
    ) {
        cropped
    } else {
        try {
            cropped.scale(targetSize.widthPx, targetSize.heightPx)
        } finally {
            cropped.recycle()
        }
    }
    return SceneThumbnailCaptureResult(
        image = thumbnailBitmap.asImageBitmap(),
        byteSizeBytes = thumbnailBitmap.width * thumbnailBitmap.height * 4,
        captureScale = captureScale,
    )
}

private fun Bitmap.copyBounds(bounds: SceneThumbnailContentBounds): Bitmap {
    val cropped = createBitmap(bounds.widthPx, bounds.heightPx)
    Canvas(cropped).drawBitmap(
        this,
        -bounds.leftPx.toFloat(),
        -bounds.topPx.toFloat(),
        null,
    )
    return cropped
}
