package oleginvoke.com.composium.eyedropper

import android.content.ClipData
import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

private const val CaptureRetryCount = 4

/**
 * Wraps arbitrary Compose content and provides an opt-in color eyedropper overlay.
 *
 * Only [content] is recorded in the capture layer. Sibling app chrome and the eyedropper overlay
 * are outside that layer, so neither can contaminate current or cached sampled colors.
 *
 * @param visible Whether the eyedropper overlay is currently visible.
 * @param onVisibleChange Callback reserved for integrations that want to close or toggle the tool.
 * @param state State object that exposes the current target, sampled color, and copied value.
 * @param formats Color formats shown in the floating island.
 * @param modifier Modifier applied to the host container.
 * @param overlaySafePadding Extra padding that keeps the eyedropper UI away from app chrome drawn
 * above this host, such as a floating toolbar.
 * @param content Content that should be available for color sampling.
 */
@Composable
internal fun ColorEyedropperHost(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    state: ColorEyedropperState = rememberColorEyedropperState(),
    formats: List<ColorEyedropperFormat> = ColorEyedropperDefaults.formats,
    modifier: Modifier = Modifier,
    overlaySafePadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val ownerView = LocalView.current
    val contentLayer = rememberGraphicsLayer()
    val metrics = ColorEyedropperDefaults.metrics
    val colors = ColorEyedropperDefaults.colors()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var islandSize by remember { mutableStateOf(IntSize.Zero) }
    var snapshot by remember { mutableStateOf<ColorEyedropperSnapshot?>(null) }

    LaunchedEffect(visible, containerSize) {
        if (!visible || containerSize.width <= 0 || containerSize.height <= 0) return@LaunchedEffect
        val initialTarget = if (state.target.isSpecifiedForEyedropper) {
            state.target
        } else {
            Offset(
                x = containerSize.width / 2f,
                y = containerSize.height / 2f,
            )
        }
        state.updateTarget(
            clampColorEyedropperTarget(
                target = initialTarget,
                containerSize = containerSize,
            ),
        )
    }

    LaunchedEffect(visible, containerSize, contentLayer, ownerView) {
        if (!visible) {
            snapshot = null
            state.updateColor(null)
            return@LaunchedEffect
        }
        if (containerSize.width <= 0 || containerSize.height <= 0) return@LaunchedEffect

        snapshot = null
        state.updateColor(null)

        repeat(CaptureRetryCount) {
            // Wait until the content layer has recorded the current scene at its measured size.
            awaitColorEyedropperDraw(ownerView)

            val nextSnapshot = contentLayer.captureColorEyedropperSnapshot(
                containerSize = containerSize,
            )
            if (nextSnapshot != null) {
                snapshot = nextSnapshot
                if (state.target.isSpecifiedForEyedropper) {
                    state.updateColor(nextSnapshot.sample(state.target))
                }
                return@LaunchedEffect
            }
        }

        snapshot = null
        state.updateColor(null)
    }

    LaunchedEffect(visible, snapshot) {
        if (!visible || !state.target.isSpecifiedForEyedropper) return@LaunchedEffect
        state.updateColor(snapshot?.sample(state.target))
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it },
    ) {
        Box(
            modifier = Modifier.drawWithContent {
                contentLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(contentLayer)
            },
        ) {
            content()
        }

        val activeSnapshot = snapshot
        if (
            visible &&
            activeSnapshot != null &&
            containerSize.width > 0 &&
            containerSize.height > 0
        ) {
            ColorEyedropperOverlay(
                snapshot = activeSnapshot,
                state = state,
                formats = formats,
                copyValue = { value ->
                    ColorEyedropperDefaults.copyValueToClipboard(value) { text ->
                        context.copyColorEyedropperText(text)
                    }
                },
                metrics = metrics,
                colors = colors,
                density = density,
                safeInsets = with(density) {
                    ColorEyedropperSafeInsets(
                        left = metrics.safePadding.toPx() +
                            overlaySafePadding.calculateStartPadding(layoutDirection).toPx(),
                        top = metrics.safePadding.toPx() +
                            overlaySafePadding.calculateTopPadding().toPx(),
                        right = metrics.safePadding.toPx() +
                            overlaySafePadding.calculateEndPadding(layoutDirection).toPx(),
                        bottom = metrics.safePadding.toPx() +
                            overlaySafePadding.calculateBottomPadding().toPx(),
                    )
                },
                containerSize = containerSize,
                islandSize = islandSize,
                onIslandSizeChange = { size -> islandSize = size },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun ColorEyedropperOverlay(
    snapshot: ColorEyedropperSnapshot,
    state: ColorEyedropperState,
    formats: List<ColorEyedropperFormat>,
    copyValue: (ColorEyedropperValue) -> Boolean,
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    density: androidx.compose.ui.unit.Density,
    safeInsets: ColorEyedropperSafeInsets,
    containerSize: IntSize,
    islandSize: IntSize,
    onIslandSizeChange: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.target.isSpecifiedForEyedropper) return

    val target = state.target
    val lensSizePx = with(density) { metrics.lensDiameter.roundToPx() }
    val lensSize = IntSize(width = lensSizePx, height = lensSizePx)
    val cursorSizePx = with(density) { metrics.cursorDiameter.roundToPx() }
    val cursorSize = IntSize(width = cursorSizePx, height = cursorSizePx)
    val lens = remember(snapshot, target, metrics.lensPixelRadius) {
        snapshot.createColorEyedropperPixelLens(
            target = target,
            radius = metrics.lensPixelRadius,
        )
    }
    val cursorOffset = IntOffset(
        x = (target.x - cursorSizePx / 2f).roundToInt(),
        y = (target.y - cursorSizePx / 2f).roundToInt(),
    )
    val overlayPlacement = calculateColorEyedropperOverlayPlacement(
        target = target,
        containerSize = containerSize,
        lensSize = lensSize,
        cursorSize = cursorSize,
        islandSize = islandSize,
        lensSpacingPx = with(density) { metrics.lensSpacing.toPx() },
        islandSpacingPx = with(density) { metrics.islandSpacing.toPx() },
        safeInsets = safeInsets,
    )
    fun updateTarget(offset: Offset) {
        val nextTarget = clampColorEyedropperTarget(
            target = offset,
            containerSize = containerSize,
        )
        state.updateTarget(nextTarget)
        state.updateColor(snapshot.sample(nextTarget))
    }
    fun nudgeTarget(direction: ColorEyedropperNudgeDirection) {
        updateTarget(
            snapshot.nudgeColorEyedropperTarget(
                target = state.target,
                direction = direction,
            ),
        )
    }

    Box(
        modifier = modifier.pointerInput(containerSize, snapshot) {
            detectDragGestures(
                onDragStart = { offset ->
                    updateTarget(offset)
                },
                onDrag = { change, _ ->
                    change.consume()
                    updateTarget(change.position)
                },
            )
        },
    ) {
        lens?.let { pixelLens ->
            ColorEyedropperLens(
                lens = pixelLens,
                onNudge = ::nudgeTarget,
                metrics = metrics,
                colors = colors,
                modifier = Modifier.offset { overlayPlacement.lensPlacement.offset },
            )
            ColorEyedropperCursor(
                metrics = metrics,
                colors = colors,
                modifier = Modifier.offset { cursorOffset },
            )
        }
        if (state.color != null) {
            ColorEyedropperIsland(
                values = state.values(formats),
                onValueClick = { value ->
                    if (copyValue(value)) {
                        state.markCopied(value)
                    }
                },
                metrics = metrics,
                colors = colors,
                modifier = Modifier
                    .offset { overlayPlacement.islandOffset }
                    .onSizeChanged(onIslandSizeChange),
            )
        }
    }
}

private suspend fun awaitColorEyedropperDraw(view: View) {
    withFrameNanos { }
    if (!view.isAttachedToWindow) return

    suspendCancellableCoroutine { continuation ->
        lateinit var listener: ViewTreeObserver.OnDrawListener

        fun removeListener() {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnDrawListener(listener)
            }
        }

        listener = ViewTreeObserver.OnDrawListener {
            view.post {
                removeListener()
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }

        view.viewTreeObserver.addOnDrawListener(listener)
        continuation.invokeOnCancellation {
            view.post {
                removeListener()
            }
        }
        view.invalidate()
    }
}

private suspend fun GraphicsLayer.captureColorEyedropperSnapshot(
    containerSize: IntSize,
): ColorEyedropperSnapshot? {
    if (size.width <= 0 || size.height <= 0) return null

    val bitmap = toImageBitmap()
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.readPixels(pixels)
    return createColorEyedropperSnapshot(
        pixels = pixels,
        bitmapSize = IntSize(bitmap.width, bitmap.height),
        containerSize = containerSize,
    )
}

private fun Context.copyColorEyedropperText(text: String) {
    val clipboardManager = getSystemService(
        Context.CLIPBOARD_SERVICE,
    ) as android.content.ClipboardManager
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText(
            "Composium color",
            text,
        ),
    )
}
