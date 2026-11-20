package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun CaptureTestThumbnail(
    request: SceneThumbnailCaptureRequest?,
    onCaptured: (SceneThumbnailKey, SceneThumbnailCaptureResult) -> Unit,
    onFailed: (SceneThumbnailKey, SceneThumbnailCaptureFailure) -> Unit,
) {
    if (request == null) return
    val queue = remember(request) { SceneThumbnailQueue().apply { sync(listOf(request.key)) } }
    SceneThumbnailCaptureHost(queue, { request }, { false }, onCaptured, onFailed)
}
