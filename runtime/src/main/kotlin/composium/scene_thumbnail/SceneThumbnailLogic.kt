package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.roundToInt

internal val DefaultSceneThumbnailCaptureTimeoutMillis: Long? = null
internal const val DefaultSceneThumbnailMemoryBudgetBytes: Int = 48 * 1024 * 1024
internal const val DefaultSceneThumbnailCaptureScale: Float = 3f
internal const val DefaultSceneThumbnailViewportWidthPx: Int = 1080
internal const val DefaultSceneThumbnailViewportHeightPx: Int = 1920
internal const val DefaultSceneThumbnailTargetWidthPx: Int = 960
internal const val DefaultSceneThumbnailTargetHeightPx: Int = 960
internal const val DefaultSceneThumbnailFailureRetryCount: Int = 1

internal data class SceneThumbnailKey(
    val sceneId: String,
    val isDarkTheme: Boolean,
    val captureScale: Float = DefaultSceneThumbnailCaptureScale,
    val viewportWidthPx: Int = DefaultSceneThumbnailViewportWidthPx,
    val viewportHeightPx: Int = DefaultSceneThumbnailViewportHeightPx,
    val targetWidthPx: Int = DefaultSceneThumbnailTargetWidthPx,
    val targetHeightPx: Int = DefaultSceneThumbnailTargetHeightPx,
)

internal sealed interface SceneThumbnailState {
    data object Pending : SceneThumbnailState
    data object Capturing : SceneThumbnailState

    data class Ready(
        val image: ImageBitmap,
        val byteSizeBytes: Int,
        val captureScale: Float = DefaultSceneThumbnailCaptureScale,
    ) : SceneThumbnailState

    data class Failed(
        val reason: String,
    ) : SceneThumbnailState
}

internal data class SceneThumbnailCaptureFailure(
    val reason: String,
    val throwable: Throwable,
)

internal data class SceneThumbnailCaptureSurfaceState(
    val key: SceneThumbnailKey? = null,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
)

internal data class SceneThumbnailContentBounds(
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int,
) {
    val widthPx: Int get() = rightPx - leftPx
    val heightPx: Int get() = bottomPx - topPx
}

internal data class SceneThumbnailSize(
    val widthPx: Int,
    val heightPx: Int,
)

internal data class SceneThumbnailDisplaySize(
    val widthDp: Float,
    val heightDp: Float,
)

internal enum class SceneThumbnailPreviewHorizontalAlignment {
    Start,
    Center,
}

internal data class SceneThumbnailCardLayout(
    val hasDivider: Boolean,
    val previewHorizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    val previewHeightDp: Float,
)

internal enum class SceneThumbnailFailureDecision {
    Retry,
    Fail,
}

internal enum class SceneThumbnailUnavailableDecision {
    Retry,
    Drop,
}

internal class SceneThumbnailFailureRetryTracker(
    private val maxRetriesPerKey: Int = DefaultSceneThumbnailFailureRetryCount,
) {
    private val failureCounts = mutableMapOf<SceneThumbnailKey, Int>()

    val maxAttemptCount: Int = maxRetriesPerKey + 1

    init {
        require(maxRetriesPerKey >= 0) { "maxRetriesPerKey must be >= 0" }
    }

    fun recordFailure(key: SceneThumbnailKey): SceneThumbnailFailureDecision {
        val nextCount = failureCountFor(key) + 1
        failureCounts[key] = nextCount
        return if (nextCount <= maxRetriesPerKey) {
            SceneThumbnailFailureDecision.Retry
        } else {
            SceneThumbnailFailureDecision.Fail
        }
    }

    fun clear(key: SceneThumbnailKey) {
        failureCounts.remove(key)
    }

    fun retain(keys: Set<SceneThumbnailKey>) {
        failureCounts.keys.retainAll(keys)
    }

    fun failureCountFor(key: SceneThumbnailKey): Int =
        failureCounts[key] ?: 0
}

internal fun Throwable.sceneThumbnailFailureReason(): String =
    message
        ?.takeIf { reason -> reason.isNotBlank() }
        ?: this::class.java.simpleName

internal fun Throwable.toSceneThumbnailCaptureFailure(): SceneThumbnailCaptureFailure =
    SceneThumbnailCaptureFailure(
        reason = sceneThumbnailFailureReason(),
        throwable = this,
    )

internal fun buildSceneThumbnailFailureLogMessage(
    key: SceneThumbnailKey,
    sceneName: String?,
    sceneGroup: String?,
    reason: String,
    attemptNumber: Int,
    maxAttemptCount: Int,
    decision: SceneThumbnailFailureDecision,
): String =
    buildString {
        append("Scene thumbnail capture failed")
        append(": sceneId=").append(key.sceneId)
        append(", sceneName=").append(sceneName ?: "<unknown>")
        append(", sceneGroup=").append(sceneGroup ?: "<none>")
        append(", attempt=").append(attemptNumber).append('/').append(maxAttemptCount)
        append(", decision=").append(decision.name)
        append(", theme=").append(if (key.isDarkTheme) "dark" else "light")
        append(", captureScale=").append(key.captureScale)
        append(", viewport=").append(key.viewportWidthPx).append('x').append(key.viewportHeightPx)
        append(", target=").append(key.targetWidthPx).append('x').append(key.targetHeightPx)
        append(", reason=").append(reason)
    }

internal fun resolveSceneThumbnailUnavailableDecision(
    key: SceneThumbnailKey,
    currentKeys: Set<SceneThumbnailKey>,
): SceneThumbnailUnavailableDecision =
    if (key in currentKeys) {
        SceneThumbnailUnavailableDecision.Retry
    } else {
        SceneThumbnailUnavailableDecision.Drop
    }

internal fun canCaptureSceneThumbnail(
    key: SceneThumbnailKey,
    captureSurfaceState: SceneThumbnailCaptureSurfaceState,
): Boolean =
    captureSurfaceState.key == key &&
        captureSurfaceState.widthPx > 0 &&
        captureSurfaceState.heightPx > 0

internal fun findSceneThumbnailContentBounds(
    pixels: IntArray,
    widthPx: Int,
    heightPx: Int,
    alphaThreshold: Int = 0,
): SceneThumbnailContentBounds? {
    require(widthPx > 0) { "widthPx must be > 0" }
    require(heightPx > 0) { "heightPx must be > 0" }
    require(pixels.size >= widthPx * heightPx) {
        "pixels must contain at least widthPx * heightPx values"
    }

    var left = widthPx
    var top = heightPx
    var right = -1
    var bottom = -1

    for (y in 0 until heightPx) {
        val rowOffset = y * widthPx
        for (x in 0 until widthPx) {
            val alpha = pixels[rowOffset + x] ushr 24
            if (alpha > alphaThreshold) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (right < left || bottom < top) return null
    return SceneThumbnailContentBounds(
        leftPx = left,
        topPx = top,
        rightPx = right + 1,
        bottomPx = bottom + 1,
    )
}

internal fun calculateSceneThumbnailScaledSize(
    contentWidthPx: Int,
    contentHeightPx: Int,
    maxWidthPx: Int,
    maxHeightPx: Int,
): SceneThumbnailSize {
    require(contentWidthPx > 0) { "contentWidthPx must be > 0" }
    require(contentHeightPx > 0) { "contentHeightPx must be > 0" }
    require(maxWidthPx > 0) { "maxWidthPx must be > 0" }
    require(maxHeightPx > 0) { "maxHeightPx must be > 0" }

    val scale = minOf(
        1f,
        maxWidthPx.toFloat() / contentWidthPx.toFloat(),
        maxHeightPx.toFloat() / contentHeightPx.toFloat(),
    )

    return SceneThumbnailSize(
        widthPx = (contentWidthPx * scale).roundToInt().coerceAtLeast(1),
        heightPx = (contentHeightPx * scale).roundToInt().coerceAtLeast(1),
    )
}

internal fun calculateSceneThumbnailDisplaySize(
    imageWidthPx: Int,
    imageHeightPx: Int,
    maxWidthDp: Float,
    maxHeightDp: Float,
): SceneThumbnailDisplaySize {
    require(imageWidthPx > 0) { "imageWidthPx must be > 0" }
    require(imageHeightPx > 0) { "imageHeightPx must be > 0" }
    require(maxWidthDp > 0f) { "maxWidthDp must be > 0" }
    require(maxHeightDp > 0f) { "maxHeightDp must be > 0" }

    val scale = minOf(
        1f,
        maxWidthDp / imageWidthPx.toFloat(),
        maxHeightDp / imageHeightPx.toFloat(),
    )

    return SceneThumbnailDisplaySize(
        widthDp = imageWidthPx * scale,
        heightDp = imageHeightPx * scale,
    )
}

internal fun calculateSceneThumbnailReadyPreviewSize(
    imageWidthPx: Int,
    imageHeightPx: Int,
    maxWidthDp: Float,
    maxHeightDp: Float,
    captureScale: Float = DefaultSceneThumbnailCaptureScale,
): SceneThumbnailDisplaySize {
    require(captureScale > 0f) { "captureScale must be > 0" }

    val displayScale = 1f / captureScale
    val scaledWidthDp = imageWidthPx.toFloat() * displayScale
    val scaledHeightDp = imageHeightPx.toFloat() * displayScale
    val boundsScale = minOf(
        1f,
        maxWidthDp / scaledWidthDp,
        maxHeightDp / scaledHeightDp,
    )

    return SceneThumbnailDisplaySize(
        widthDp = scaledWidthDp * boundsScale,
        heightDp = scaledHeightDp * boundsScale,
    )
}

internal fun sceneThumbnailCardLayout(): SceneThumbnailCardLayout =
    SceneThumbnailCardLayout(
        hasDivider = true,
        previewHorizontalAlignment = SceneThumbnailPreviewHorizontalAlignment.Center,
        previewHeightDp = 86f,
    )

internal class SceneThumbnailQueue {
    private val pendingKeys = ArrayDeque<SceneThumbnailKey>()
    private val pendingSet = linkedSetOf<SceneThumbnailKey>()

    fun sync(keys: List<SceneThumbnailKey>) {
        keys.forEach(::enqueueBack)
    }

    fun prioritize(keys: List<SceneThumbnailKey>) {
        val unique = keys.distinct()
        unique.asReversed().forEach { key ->
            remove(key)
            enqueueFront(key)
        }
    }

    fun retain(keys: Set<SceneThumbnailKey>) {
        val retained = pendingKeys.filterTo(ArrayList(pendingKeys.size)) { key -> key in keys }
        pendingKeys.clear()
        pendingSet.clear()
        retained.forEach(::enqueueBack)
    }

    fun remove(key: SceneThumbnailKey) {
        if (pendingSet.remove(key)) {
            pendingKeys.remove(key)
        }
    }

    fun next(): SceneThumbnailKey? {
        val key = pendingKeys.removeFirstOrNull() ?: return null
        pendingSet.remove(key)
        return key
    }

    private fun enqueueBack(key: SceneThumbnailKey) {
        if (pendingSet.add(key)) {
            pendingKeys.addLast(key)
        }
    }

    private fun enqueueFront(key: SceneThumbnailKey) {
        if (pendingSet.add(key)) {
            pendingKeys.addFirst(key)
        }
    }
}

internal class SceneThumbnailStore(
    private val memoryBudgetBytes: Int = DefaultSceneThumbnailMemoryBudgetBytes,
) {
    private val states = mutableStateMapOf<SceneThumbnailKey, SceneThumbnailState>()
    private val readyRecency = LinkedHashMap<SceneThumbnailKey, Int>(0, 0.75f, true)

    var currentMemoryBytes by mutableIntStateOf(0)
        private set

    fun thumbnailFor(key: SceneThumbnailKey): SceneThumbnailState? {
        readyRecency[key]
        return states[key]
    }

    fun statesBySceneId(): Map<String, SceneThumbnailState> =
        states.entries.associate { (key, state) -> key.sceneId to state }

    fun needsCapture(key: SceneThumbnailKey): Boolean =
        when (states[key]) {
            is SceneThumbnailState.Ready,
            is SceneThumbnailState.Failed,
            SceneThumbnailState.Capturing,
            -> false

            SceneThumbnailState.Pending,
            null,
            -> true
        }

    fun putPending(key: SceneThumbnailKey) {
        if (states[key] == null || states[key] == SceneThumbnailState.Capturing) {
            states[key] = SceneThumbnailState.Pending
        }
    }

    fun putCapturing(key: SceneThumbnailKey) {
        states[key] = SceneThumbnailState.Capturing
    }

    fun putReady(
        key: SceneThumbnailKey,
        image: ImageBitmap,
        byteSizeBytes: Int,
        captureScale: Float = key.captureScale,
    ) {
        removeReadyMemory(key)
        states[key] = SceneThumbnailState.Ready(
            image = image,
            byteSizeBytes = byteSizeBytes,
            captureScale = captureScale,
        )
        readyRecency[key] = byteSizeBytes
        currentMemoryBytes += byteSizeBytes
        trimToBudget()
    }

    fun putFailed(
        key: SceneThumbnailKey,
        reason: String,
    ) {
        removeReadyMemory(key)
        states[key] = SceneThumbnailState.Failed(reason = reason)
    }

    fun remove(key: SceneThumbnailKey) {
        removeReadyMemory(key)
        states.remove(key)
    }

    fun retain(keys: Set<SceneThumbnailKey>) {
        val staleKeys = states.keys.filter { key -> key !in keys }
        staleKeys.forEach { key ->
            remove(key)
        }
    }

    private fun removeReadyMemory(key: SceneThumbnailKey) {
        val byteSize = readyRecency.remove(key) ?: return
        currentMemoryBytes = (currentMemoryBytes - byteSize).coerceAtLeast(0)
    }

    private fun trimToBudget() {
        while (currentMemoryBytes > memoryBudgetBytes && readyRecency.isNotEmpty()) {
            val eldestKey = readyRecency.entries.first().key
            removeReadyMemory(eldestKey)
            states.remove(eldestKey)
        }
    }
}
