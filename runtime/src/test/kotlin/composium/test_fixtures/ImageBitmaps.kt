package oleginvoke.com.composium.test_fixtures

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/** Metadata-only image for tests that neither render nor sample pixels. */
internal fun fakeImageBitmap(): ImageBitmap = object : ImageBitmap {
    override val width: Int = 10
    override val height: Int = 10
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha: Boolean = true
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ): Unit = error("Metadata-only bitmap cannot supply pixels")

    override fun prepareToDraw(): Unit = error("Metadata-only bitmap cannot be rendered")
}
