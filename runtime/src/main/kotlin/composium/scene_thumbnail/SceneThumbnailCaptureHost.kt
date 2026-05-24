package oleginvoke.com.composium.scene_thumbnail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import oleginvoke.com.composium.ui.theme.LocalComposiumThemeController
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

internal data class SceneThumbnailCaptureRequest(
    val key: SceneThumbnailKey,
    val sceneEntry: SceneEntry,
)

internal data class SceneThumbnailCaptureResult(
    val image: ImageBitmap,
    val byteSizeBytes: Int,
    val captureScale: Float,
)

@Composable
internal fun SceneThumbnailCaptureHost(
    request: SceneThumbnailCaptureRequest?,
    onCaptured: (SceneThumbnailKey, SceneThumbnailCaptureResult) -> Unit,
    onFailed: (SceneThumbnailKey, SceneThumbnailCaptureFailure) -> Unit,
    modifier: Modifier = Modifier,
) {
    val captureRequest = request ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    val themeController = LocalComposiumThemeController.current
    val sceneScope = remember(captureRequest.key) { SceneScope() }
    val installedContent = remember { SceneThumbnailInstalledContent() }
    var captureView by remember(captureRequest.key) { mutableStateOf<ComposeView?>(null) }
    var captureSurfaceState by remember(captureRequest.key) {
        mutableStateOf(SceneThumbnailCaptureSurfaceState())
    }

    LaunchedEffect(captureRequest.key, captureView, captureSurfaceState) {
        val view = captureView ?: return@LaunchedEffect
        if (!canCaptureSceneThumbnail(captureRequest.key, captureSurfaceState)) {
            return@LaunchedEffect
        }

        val result = runCatching {
            withFrameNanos { }
            withFrameNanos { }
            val rawBitmap = view.drawToBitmap()
            rawBitmap.toSceneThumbnailCaptureResult(
                maxWidthPx = captureRequest.key.targetWidthPx,
                maxHeightPx = captureRequest.key.targetHeightPx,
                captureScale = captureRequest.key.captureScale,
            )
        }

        result
            .onSuccess { captureResult ->
                onCaptured(captureRequest.key, captureResult)
            }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                onFailed(
                    captureRequest.key,
                    throwable.toSceneThumbnailCaptureFailure(),
                )
            }
    }

    val width = with(density) { captureRequest.key.viewportWidthPx.toDp() }
    val height = with(density) { captureRequest.key.viewportHeightPx.toDp() }

    AndroidView(
        factory = {
            ComposeView(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                isClickable = false
                isFocusable = false
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            }
        },
        update = { view ->
            captureView = view
            if (installedContent.key != captureRequest.key) {
                installedContent.key = captureRequest.key
                view.setContent {
                    ComposiumTheme(darkTheme = captureRequest.key.isDarkTheme) {
                        CompositionLocalProvider(
                            LocalComposiumThemeController provides themeController,
                        ) {
                            SceneThumbnailRenderSurface(
                                sceneEntry = captureRequest.sceneEntry,
                                sceneScope = sceneScope,
                                captureScale = captureRequest.key.captureScale,
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
            .size(width = width, height = height)
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                val nextState = SceneThumbnailCaptureSurfaceState(
                    key = captureRequest.key,
                    widthPx = size.width,
                    heightPx = size.height,
                )
                if (captureSurfaceState != nextState) {
                    captureSurfaceState = nextState
                }
            },
    )
}

private class SceneThumbnailInstalledContent {
    var key: SceneThumbnailKey? = null
}

private fun ComposeView.drawToBitmap(): Bitmap {
    check(width > 0 && height > 0) {
        "Capture view is not measured"
    }
    val bitmap = createBitmap(width, height)
    draw(Canvas(bitmap))
    return bitmap
}

private fun Bitmap.toSceneThumbnailCaptureResult(
    maxWidthPx: Int,
    maxHeightPx: Int,
    captureScale: Float,
): SceneThumbnailCaptureResult {
    val bounds = visibleContentBounds()
        ?: run {
            recycle()
            error("Scene has no visible content")
        }
    val cropped = copyBounds(bounds)
    recycle()

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
        cropped.scale(targetSize.widthPx, targetSize.heightPx).also {
            cropped.recycle()
        }
    }
    return SceneThumbnailCaptureResult(
        image = thumbnailBitmap.asImageBitmap(),
        byteSizeBytes = thumbnailBitmap.width * thumbnailBitmap.height * 4,
        captureScale = captureScale,
    )
}

private fun Bitmap.visibleContentBounds(): SceneThumbnailContentBounds? {
    val pixels = IntArray(width * height)
    getPixels(
        pixels,
        0,
        width,
        0,
        0,
        width,
        height,
    )
    return findSceneThumbnailContentBounds(
        pixels = pixels,
        widthPx = width,
        heightPx = height,
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
