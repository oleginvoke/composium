package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneThumbnailLogicTest {

    private val keyA = SceneThumbnailKey(
        sceneId = "group::A",
        isDarkTheme = false,
        viewportWidthPx = 360,
        viewportHeightPx = 640,
        targetWidthPx = 180,
        targetHeightPx = 320,
    )
    private val keyB = keyA.copy(sceneId = "group::B")
    private val keyC = keyA.copy(sceneId = "group::C")

    @Test
    fun defaultThumbnailKeyUsesTripleResolutionCapture() {
        val key = SceneThumbnailKey(
            sceneId = "group::Default",
            isDarkTheme = false,
        )

        assertEquals(3f, key.captureScale)
        assertEquals(1080, key.viewportWidthPx)
        assertEquals(1920, key.viewportHeightPx)
        assertEquals(960, key.targetWidthPx)
        assertEquals(960, key.targetHeightPx)
    }

    @Test
    fun defaultMemoryBudgetKeepsInitialLargeThumbnailBatch() {
        assertEquals(48 * 1024 * 1024, DefaultSceneThumbnailMemoryBudgetBytes)
    }

    @Test
    fun defaultCapturePolicyDoesNotLimitIndividualSceneDuration() {
        assertNull(DefaultSceneThumbnailCaptureTimeoutMillis)
    }

    @Test
    fun queueDoesNotDuplicateKeysAndKeepsPriorityOrder() {
        val queue = SceneThumbnailQueue()

        queue.sync(listOf(keyA, keyB, keyA))
        queue.prioritize(listOf(keyB, keyC, keyB))

        assertEquals(keyB, queue.next())
        assertEquals(keyC, queue.next())
        assertEquals(keyA, queue.next())
        assertNull(queue.next())
    }

    @Test
    fun queueRemovesStaleKeys() {
        val queue = SceneThumbnailQueue()

        queue.sync(listOf(keyA, keyB, keyC))
        queue.retain(setOf(keyB))

        assertEquals(keyB, queue.next())
        assertNull(queue.next())
    }

    @Test
    fun failureRetryTrackerRetriesFirstFailureOnly() {
        val tracker = SceneThumbnailFailureRetryTracker()

        assertEquals(SceneThumbnailFailureDecision.Retry, tracker.recordFailure(keyA))
        assertEquals(1, tracker.failureCountFor(keyA))
        assertEquals(SceneThumbnailFailureDecision.Fail, tracker.recordFailure(keyA))
        assertEquals(2, tracker.failureCountFor(keyA))
    }

    @Test
    fun failureRetryTrackerClearsAttemptsAfterSuccessfulCapture() {
        val tracker = SceneThumbnailFailureRetryTracker()

        assertEquals(SceneThumbnailFailureDecision.Retry, tracker.recordFailure(keyA))
        tracker.clear(keyA)

        assertEquals(0, tracker.failureCountFor(keyA))
        assertEquals(SceneThumbnailFailureDecision.Retry, tracker.recordFailure(keyA))
    }

    @Test
    fun throwableFailureReasonUsesMessageOrClassNameFallback() {
        assertEquals(
            "Broken thumbnail",
            IllegalStateException("Broken thumbnail").sceneThumbnailFailureReason(),
        )
        assertEquals(
            "IllegalArgumentException",
            IllegalArgumentException().sceneThumbnailFailureReason(),
        )
    }

    @Test
    fun failureLogMessageIncludesSceneMetadataAttemptDecisionAndReason() {
        val message = buildSceneThumbnailFailureLogMessage(
            key = keyA,
            sceneName = "Nested playground",
            sceneGroup = "Buttons/Secondary/Tonal",
            reason = "Scene has no visible content",
            attemptNumber = 2,
            maxAttemptCount = 2,
            decision = SceneThumbnailFailureDecision.Fail,
        )

        assertTrue(message.contains("sceneId=group::A"))
        assertTrue(message.contains("sceneName=Nested playground"))
        assertTrue(message.contains("sceneGroup=Buttons/Secondary/Tonal"))
        assertTrue(message.contains("attempt=2/2"))
        assertTrue(message.contains("decision=Fail"))
        assertTrue(message.contains("reason=Scene has no visible content"))
    }

    @Test
    fun unavailableSceneCaptureRetriesWhenKeyIsStillCurrent() {
        val decision = resolveSceneThumbnailUnavailableDecision(
            key = keyA,
            currentKeys = setOf(keyA, keyB),
        )

        assertEquals(SceneThumbnailUnavailableDecision.Retry, decision)
    }

    @Test
    fun unavailableSceneCaptureDropsWhenKeyIsStale() {
        val decision = resolveSceneThumbnailUnavailableDecision(
            key = keyA,
            currentKeys = setOf(keyB),
        )

        assertEquals(SceneThumbnailUnavailableDecision.Drop, decision)
    }

    @Test
    fun storeEvictsLeastRecentlyUsedReadyImagesByMemoryBudget() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 100)

        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 60)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 60)

        assertNull(store.thumbnailFor(keyA))
        assertTrue(store.thumbnailFor(keyB) is SceneThumbnailState.Ready)
        assertEquals(60, store.currentMemoryBytes)
    }

    @Test
    fun storeRetainsOnlyCurrentKeysAndDropsStaleMemory() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 200)

        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 60)
        store.putFailed(keyB, "timeout")
        store.retain(setOf(keyB))

        assertNull(store.thumbnailFor(keyA))
        assertTrue(store.thumbnailFor(keyB) is SceneThumbnailState.Failed)
        assertEquals(0, store.currentMemoryBytes)
    }

    @Test
    fun captureDoesNotRunUntilCurrentRequestHasRecordedContent() {
        val empty = SceneThumbnailCaptureSurfaceState()
        val recordedForOtherScene = SceneThumbnailCaptureSurfaceState(
            key = keyB,
            widthPx = keyA.viewportWidthPx,
            heightPx = keyA.viewportHeightPx,
        )
        val recordedForCurrentScene = SceneThumbnailCaptureSurfaceState(
            key = keyA,
            widthPx = keyA.viewportWidthPx,
            heightPx = keyA.viewportHeightPx,
        )

        assertFalse(canCaptureSceneThumbnail(keyA, empty))
        assertFalse(canCaptureSceneThumbnail(keyA, recordedForOtherScene))
        assertTrue(canCaptureSceneThumbnail(keyA, recordedForCurrentScene))
    }

    @Test
    fun contentBoundsCropTransparentMargins() {
        val transparent = 0x00000000
        val visible = 0xFF123456.toInt()
        val pixels = intArrayOf(
            transparent, transparent, transparent, transparent,
            transparent, visible, visible, transparent,
            transparent, visible, visible, transparent,
            transparent, transparent, transparent, transparent,
        )

        val bounds = findSceneThumbnailContentBounds(
            pixels = pixels,
            widthPx = 4,
            heightPx = 4,
        )

        assertEquals(
            SceneThumbnailContentBounds(
                leftPx = 1,
                topPx = 1,
                rightPx = 3,
                bottomPx = 3,
            ),
            bounds,
        )
    }

    @Test
    fun contentBoundsReturnsNullWhenAllPixelsAreTransparent() {
        val bounds = findSceneThumbnailContentBounds(
            pixels = IntArray(9),
            widthPx = 3,
            heightPx = 3,
        )

        assertNull(bounds)
    }

    @Test
    fun scaledSizePreservesSmallContentSize() {
        val size = calculateSceneThumbnailScaledSize(
            contentWidthPx = 80,
            contentHeightPx = 32,
            maxWidthPx = 180,
            maxHeightPx = 320,
        )

        assertEquals(SceneThumbnailSize(widthPx = 80, heightPx = 32), size)
    }

    @Test
    fun scaledSizeDownscalesLargeContentIntoMaxBounds() {
        val size = calculateSceneThumbnailScaledSize(
            contentWidthPx = 360,
            contentHeightPx = 640,
            maxWidthPx = 180,
            maxHeightPx = 320,
        )

        assertEquals(SceneThumbnailSize(widthPx = 180, heightPx = 320), size)
    }

    @Test
    fun displaySizeDoesNotUpscaleSmallThumbnails() {
        val size = calculateSceneThumbnailDisplaySize(
            imageWidthPx = 120,
            imageHeightPx = 48,
            maxWidthDp = 280f,
            maxHeightDp = 150f,
        )

        assertEquals(SceneThumbnailDisplaySize(widthDp = 120f, heightDp = 48f), size)
    }

    @Test
    fun displaySizeDownscalesLargeThumbnailsToFitPreviewArea() {
        val size = calculateSceneThumbnailDisplaySize(
            imageWidthPx = 180,
            imageHeightPx = 320,
            maxWidthDp = 280f,
            maxHeightDp = 150f,
        )

        assertEquals(SceneThumbnailDisplaySize(widthDp = 84.375f, heightDp = 150f), size)
    }

    @Test
    fun readyPreviewSizeDisplaysScreenshotAtCapturedDpSizeWhenItFits() {
        val size = calculateSceneThumbnailReadyPreviewSize(
            imageWidthPx = 360,
            imageHeightPx = 144,
            maxWidthDp = 280f,
            maxHeightDp = 150f,
        )

        assertEquals(120f, size.widthDp, absoluteTolerance = 0.001f)
        assertEquals(48f, size.heightDp, absoluteTolerance = 0.001f)
    }

    @Test
    fun readyPreviewSizeFitsCapturedDpSizeIntoMaxHeight() {
        val size = calculateSceneThumbnailReadyPreviewSize(
            imageWidthPx = 1080,
            imageHeightPx = 1920,
            maxWidthDp = 280f,
            maxHeightDp = 150f,
        )

        assertEquals(84.375f, size.widthDp, absoluteTolerance = 0.001f)
        assertEquals(150f, size.heightDp, absoluteTolerance = 0.001f)
    }

    @Test
    fun sceneCardLayoutUsesTopPreviewDividerAndCenteredPreview() {
        val layout = sceneThumbnailCardLayout()

        assertTrue(layout.hasDivider)
        assertEquals(SceneThumbnailPreviewHorizontalAlignment.Center, layout.previewHorizontalAlignment)
    }

    @Test
    fun sceneCardLayoutUsesSinglePreviewHeight() {
        val layout = sceneThumbnailCardLayout()

        assertEquals(86f, layout.previewHeightDp)
    }

    private fun fakeImageBitmap(): ImageBitmap = object : ImageBitmap {
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
        ) = Unit

        override fun prepareToDraw() = Unit
    }
}
