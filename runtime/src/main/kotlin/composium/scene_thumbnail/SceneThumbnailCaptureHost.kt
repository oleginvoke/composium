package oleginvoke.com.composium.scene_thumbnail

import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oleginvoke.com.composium.SceneEntry

internal data class SceneThumbnailCaptureRequest(
    val key: SceneThumbnailKey,
    val sceneEntry: SceneEntry,
)

@Composable
internal fun SceneThumbnailCaptureHost(
    queue: SceneThumbnailQueue,
    requestFor: (SceneThumbnailKey) -> SceneThumbnailCaptureRequest?,
    isPaused: () -> Boolean,
    onCaptured: (SceneThumbnailKey, SceneThumbnailCaptureResult) -> Unit,
    onFailed: (SceneThumbnailKey, SceneThumbnailCaptureFailure) -> Unit,
) {
    val view = LocalView.current
    val parent = rememberCompositionContext()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var origin by remember(view) { mutableStateOf<IntOffset?>(null) }
    Spacer(Modifier.onGloballyPositioned { origin = it.positionInWindow().round() })
    if (origin == null) return

    val insets by rememberUpdatedState(thumbnailInsets(view) { checkNotNull(origin) })
    val currentRequestFor by rememberUpdatedState(requestFor)
    val paused by rememberUpdatedState(isPaused)
    val captured by rememberUpdatedState(onCaptured)
    val failed by rememberUpdatedState(onFailed)
    LaunchedEffect(queue, parent, lifecycle, view) {
        withContext(Dispatchers.Main.immediate) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    queue.awaitWork()
                    snapshotFlow { !paused() }.first { it }
                    captureBatch(
                        queue = queue,
                        view = view,
                        parent = parent,
                        insets = { insets(it) },
                        requestFor = { currentRequestFor(it) },
                        isPaused = { paused() },
                        onCaptured = { key, result -> captured(key, result) },
                        onFailed = { key, failure -> failed(key, failure) },
                    )
                }
            }
        }
    }
}

private suspend fun captureBatch(
    queue: SceneThumbnailQueue,
    view: View,
    parent: CompositionContext,
    insets: (SceneThumbnailKey) -> WindowInsetsCompat,
    requestFor: (SceneThumbnailKey) -> SceneThumbnailCaptureRequest?,
    isPaused: () -> Boolean,
    onCaptured: (SceneThumbnailKey, SceneThumbnailCaptureResult) -> Unit,
    onFailed: (SceneThumbnailKey, SceneThumbnailCaptureFailure) -> Unit,
) {
    val inFlight = linkedSetOf<SceneThumbnailKey>()
    val buffers = SceneThumbnailBuffers()
    try {
        coroutineScope {
            var window: SceneThumbnailWindow? = null
            var processing: Job? = null
            try {
                while (queue.hasWork) {
                    if (isPaused()) {
                        window?.clearContent()
                        snapshotFlow { !isPaused() }.first { it }
                    }
                    val key = queue.next() ?: break
                    if (key in inFlight) continue
                    val request = requestFor(key) ?: continue
                    inFlight.add(key)
                    try {
                        if (window == null || window.width != key.viewportWidthPx || window.height != key.viewportHeightPx) {
                            window?.close()
                            window = null
                            window = SceneThumbnailWindow.open(view, parent, key.viewportWidthPx, key.viewportHeightPx) {
                                insets(key)
                            }
                        }
                        // The previous worker owns the other slot. Join before handing it this frame.
                        val frame = buffers.nextFrame(window.width, window.height)
                        window.draw(request, frame)
                        processing?.join()
                        processing = launch {
                            val result = try {
                                buffers.process(frame, key)
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (failure: Exception) {
                                inFlight.remove(key)
                                onFailed(key, failure.toSceneThumbnailCaptureFailure())
                                return@launch
                            }
                            snapshotFlow { !isPaused() }.first { it }
                            inFlight.remove(key)
                            onCaptured(key, result)
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (failure: Exception) {
                        // A failed draw may have taken the spare slot. Finish its sibling before reuse.
                        processing?.join()
                        window?.close()
                        window = null
                        inFlight.remove(key)
                        onFailed(key, failure.toSceneThumbnailCaptureFailure())
                    }
                }
            } finally {
                window?.close()
            }
        }
    } finally {
        // Cancellation on navigation or onStop leaves unfinished keys available on return.
        queue.prioritize(inFlight.toList())
        buffers.close()
    }
}
