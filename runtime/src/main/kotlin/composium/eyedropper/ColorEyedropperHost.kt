package oleginvoke.com.composium.eyedropper

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

private const val CaptureRetryCount = 4

/**
 * Wraps arbitrary Compose content and provides an opt-in color eyedropper overlay.
 *
 * The default Android implementation samples the already-rendered app pixels covered by this
 * host's bounds. The overlay is hidden while the snapshot is captured, so the crosshair and
 * floating island do not affect sampled colors.
 *
 * @param visible Whether the eyedropper overlay is currently visible.
 * @param onVisibleChange Callback reserved for integrations that want to close or toggle the tool.
 * @param state State object that exposes the current target, sampled color, and copied value.
 * @param formats Color formats shown in the floating island.
 * @param modifier Modifier applied to the host container.
 * @param content Content that should be available for color sampling.
 */
@Composable
fun ColorEyedropperHost(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    state: ColorEyedropperState = rememberColorEyedropperState(),
    formats: List<ColorEyedropperFormat> = ColorEyedropperDefaults.formats,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ownerView = LocalView.current
    val metrics = ColorEyedropperDefaults.metrics
    val colors = ColorEyedropperDefaults.colors()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var hostTopLeft by remember { mutableStateOf(IntOffset.Zero) }
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

    LaunchedEffect(visible, containerSize, hostTopLeft, ownerView) {
        if (!visible) {
            snapshot = null
            state.updateColor(null)
            return@LaunchedEffect
        }
        if (containerSize.width <= 0 || containerSize.height <= 0) return@LaunchedEffect

        snapshot = null
        state.updateColor(null)

        repeat(CaptureRetryCount) {
            // The overlay is hidden while snapshot is null. Waiting for the next draw lets the
            // root View render the plain app surface first, so the cached bitmap does not include
            // the crosshair or island.
            awaitColorEyedropperDraw(ownerView)

            val nextSnapshot = ownerView.captureColorEyedropperSnapshot(
                hostTopLeft = hostTopLeft,
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

    LaunchedEffect(visible, state.target, snapshot) {
        if (!visible || !state.target.isSpecifiedForEyedropper) return@LaunchedEffect
        state.updateColor(snapshot?.sample(state.target))
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
                val position = coordinates.positionInRoot()
                hostTopLeft = IntOffset(
                    x = position.x.roundToInt(),
                    y = position.y.roundToInt(),
                )
            },
    ) {
        Box {
            content()
        }

        val activeSnapshot = snapshot
        if (
            visible &&
            activeSnapshot != null &&
            state.target.isSpecifiedForEyedropper &&
            containerSize.width > 0 &&
            containerSize.height > 0
        ) {
            val target = state.target
            val islandOffset = calculateColorEyedropperIslandOffset(
                target = target,
                containerSize = containerSize,
                islandSize = islandSize,
                spacingPx = with(density) { metrics.islandSpacing.toPx() },
                safePaddingPx = with(density) { metrics.safePadding.toPx() },
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(containerSize, snapshot) {
                        fun updateTarget(offset: Offset) {
                            val target = clampColorEyedropperTarget(
                                target = offset,
                                containerSize = containerSize,
                            )
                            state.updateTarget(target)
                            state.updateColor(activeSnapshot.sample(target))
                        }

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
                ColorEyedropperCrosshair(
                    target = target,
                    metrics = metrics,
                    modifier = Modifier.matchParentSize(),
                )
                state.color?.let { color ->
                    ColorEyedropperIsland(
                        color = color,
                        values = state.values(formats),
                        onValueClick = { value ->
                            val copied = ColorEyedropperDefaults.copyValueToClipboard(value) { text ->
                                context.copyColorEyedropperText(text)
                            }
                            if (copied) {
                                state.markCopied(value)
                            }
                        },
                        metrics = metrics,
                        colors = colors,
                        modifier = Modifier
                            .offset { islandOffset }
                            .onSizeChanged { size -> islandSize = size }
                    )
                }
            }
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

private fun View.captureColorEyedropperSnapshot(
    hostTopLeft: IntOffset,
    containerSize: IntSize,
): ColorEyedropperSnapshot? {
    if (width <= 0 || height <= 0) return null

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        draw(Canvas(bitmap))

        val pixels = IntArray(width * height)
        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height,
        )
        return createColorEyedropperSnapshotFromRootPixels(
            rootPixels = pixels,
            rootSize = IntSize(width = width, height = height),
            hostTopLeft = hostTopLeft,
            containerSize = containerSize,
        )
    } finally {
        bitmap.recycle()
    }
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
