package oleginvoke.com.composium.scene_thumbnail

import oleginvoke.com.composium.SceneKey
import oleginvoke.com.composium.test_fixtures.fakeImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneThumbnailLogicTest {

    private val keyA = SceneThumbnailKey(
        sceneId = SceneKey("group", "A"),
        isDarkTheme = false,
        viewportWidthPx = 360,
        viewportHeightPx = 640,
        targetWidthPx = 180,
        targetHeightPx = 320,
    )
    private val keyB = keyA.copy(sceneId = SceneKey("group", "B"))
    private val keyC = keyA.copy(sceneId = SceneKey("group", "C"))

    @Test
    fun collidingLegacyIdsKeepIndependentThumbnailStates() {
        val first = keyA.copy(sceneId = SceneKey("Buttons::Primary", "Disabled"))
        val second = keyA.copy(sceneId = SceneKey("Buttons", "Primary::Disabled"))
        val store = SceneThumbnailStore()
        store.putFailed(first, "first failure")
        store.putCapturing(second)

        assertEquals(SceneThumbnailState.Failed("first failure"), store.thumbnailFor(first))
        assertEquals(SceneThumbnailState.Capturing, store.thumbnailFor(second))
        assertEquals(2, store.statesBySceneId().size)

        val queue = SceneThumbnailQueue()
        queue.sync(listOf(first, second))
        assertEquals(first, queue.next())
        assertEquals(second, queue.next())
    }

    @Test
    fun defaultMemoryBudgetKeepsInitialBatchButEvictsOverflow() {
        val store = SceneThumbnailStore()
        // Twelve full-size ARGB thumbnails are about 42 MiB. No real bitmaps are
        // allocated here: this checks the default store's accounting and eviction.
        val keys = List(12) { keyA.copy(sceneId = SceneKey("batch", "$it")) }
        val imageBytes = 960 * 960 * 4
        keys.forEach { store.putReady(it, fakeImageBitmap(), byteSizeBytes = imageBytes) }

        keys.forEach { assertTrue(store.thumbnailFor(it) is SceneThumbnailState.Ready) }
        assertEquals(keys.size * imageBytes, store.currentMemoryBytes)

        val extraKeys = List(12) { keyA.copy(sceneId = SceneKey("overflow", "$it")) }
        extraKeys.forEach { store.putReady(it, fakeImageBitmap(), byteSizeBytes = imageBytes) }

        // 48 MiB fits thirteen of these images, not the whole 84 MiB batch.
        keys.dropLast(1).forEach { assertNull(store.thumbnailFor(it)) }
        (keys.takeLast(1) + extraKeys).forEach {
            assertTrue(store.thumbnailFor(it) is SceneThumbnailState.Ready)
        }
        assertEquals(13 * imageBytes, store.currentMemoryBytes)
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

        assertTrue(message.contains("sceneId=${keyA.sceneId}"))
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

        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 40)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 40)
        assertTrue(store.thumbnailFor(keyA) is SceneThumbnailState.Ready)
        store.putReady(keyC, fakeImageBitmap(), byteSizeBytes = 40)

        // Reading A must protect it ahead of B: FIFO would evict A instead.
        assertTrue(store.thumbnailFor(keyA) is SceneThumbnailState.Ready)
        assertNull(store.thumbnailFor(keyB))
        assertTrue(store.thumbnailFor(keyC) is SceneThumbnailState.Ready)
        assertEquals(80, store.currentMemoryBytes)
    }

    @Test
    fun backgroundCapturesDoNotEvictVisibleThumbnails() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 100)
        store.setVisibleKeys(setOf(keyA))
        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 60)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 40)
        store.putReady(keyC, fakeImageBitmap(), byteSizeBytes = 40)

        assertTrue(store.thumbnailFor(keyA) is SceneThumbnailState.Ready)
        assertNull(store.thumbnailFor(keyB))
        assertTrue(store.thumbnailFor(keyC) is SceneThumbnailState.Ready)
        assertEquals(100, store.currentMemoryBytes)
    }

    @Test
    fun onlyVisibleThumbnailsMayExceedBudgetAndLeavingVisibilityTrimsImmediately() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 100)
        store.setVisibleKeys(setOf(keyA, keyB))
        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 70)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 70)
        store.putReady(keyC, fakeImageBitmap(), byteSizeBytes = 40)

        assertTrue(store.thumbnailFor(keyA) is SceneThumbnailState.Ready)
        assertTrue(store.thumbnailFor(keyB) is SceneThumbnailState.Ready)
        assertNull(store.thumbnailFor(keyC))
        assertEquals(140, store.currentMemoryBytes)

        store.setVisibleKeys(setOf(keyB))

        assertNull(store.thumbnailFor(keyA))
        assertTrue(store.thumbnailFor(keyB) is SceneThumbnailState.Ready)
        assertEquals(70, store.currentMemoryBytes)
    }

    @Test
    fun evictedThumbnailCanBeCapturedAgainWhenItBecomesVisible() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 100)
        store.setVisibleKeys(setOf(keyA))
        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 70)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 70)
        assertTrue(store.needsCapture(keyB))

        store.setVisibleKeys(setOf(keyB))
        store.putCapturing(keyB)
        store.putReady(keyB, fakeImageBitmap(), byteSizeBytes = 70)

        assertNull(store.thumbnailFor(keyA))
        assertTrue(store.thumbnailFor(keyB) is SceneThumbnailState.Ready)
        assertEquals(70, store.currentMemoryBytes)
    }

    @Test
    fun changingThemeReleasesProtectedImagesFromPreviousTheme() {
        val store = SceneThumbnailStore(memoryBudgetBytes = 100)
        store.setVisibleKeys(setOf(keyA))
        store.putReady(keyA, fakeImageBitmap(), byteSizeBytes = 140)
        val darkKey = keyA.copy(isDarkTheme = true)

        store.retain(setOf(darkKey))
        store.setVisibleKeys(setOf(darkKey))
        store.putReady(darkKey, fakeImageBitmap(), byteSizeBytes = 140)

        assertNull(store.thumbnailFor(keyA))
        assertTrue(store.thumbnailFor(darkKey) is SceneThumbnailState.Ready)
        assertEquals(140, store.currentMemoryBytes)

        store.setVisibleKeys(emptySet())
        assertNull(store.thumbnailFor(darkKey))
        assertEquals(0, store.currentMemoryBytes)
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
        assertFalse(canCaptureSceneThumbnail(keyA, recordedForCurrentScene.copy(widthPx = 0)))
        assertFalse(canCaptureSceneThumbnail(keyA, recordedForCurrentScene.copy(heightPx = 0)))
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
        // Different aspect ratios distinguish proportional scaling from simply
        // clamping each dimension, and exercise both limiting dimensions.
        listOf(
            SceneThumbnailSize(400, 100) to SceneThumbnailSize(180, 45),
            SceneThumbnailSize(100, 800) to SceneThumbnailSize(40, 320),
        ).forEach { (content, expected) ->
            assertEquals(
                expected,
                calculateSceneThumbnailScaledSize(
                    contentWidthPx = content.widthPx,
                    contentHeightPx = content.heightPx,
                    maxWidthPx = 180,
                    maxHeightPx = 320,
                ),
                "Content: $content",
            )
        }
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

}
