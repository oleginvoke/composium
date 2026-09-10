package oleginvoke.com.composium.scene_thumbnail

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import oleginvoke.com.composium.SceneKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SceneThumbnailQueueTest {
    @Test
    fun waitingConsumerStartsWhenWorkArrivesWithoutAdvancingTime() = runTest {
        val queue = SceneThumbnailQueue()
        val key = SceneThumbnailKey(SceneKey("sample", "First"), false)
        val next = async { queue.awaitNext() }
        runCurrent()
        assertFalse(next.isCompleted)
        queue.sync(listOf(key))
        runCurrent()
        assertEquals(key, next.await())
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun waitingConsumerUsesLatestPriorityAndSkipsRemovedWork() = runTest {
        val queue = SceneThumbnailQueue()
        val first = SceneThumbnailKey(SceneKey("sample", "First"), false)
        val second = SceneThumbnailKey(SceneKey("sample", "Second"), false)
        queue.sync(listOf(first))
        queue.remove(first)
        val next = async { queue.awaitNext() }
        runCurrent()
        assertFalse(next.isCompleted)
        queue.sync(listOf(first, second))
        queue.prioritize(listOf(second))
        runCurrent()
        assertEquals(second, next.await())
        assertEquals(first, queue.awaitNext())
    }

    @Test
    fun cancellingAnIdleConsumerDoesNotLoseFutureWork() = runTest {
        val queue = SceneThumbnailQueue()
        val abandoned = async { queue.awaitNext() }
        runCurrent()
        abandoned.cancel()
        runCurrent()
        val key = SceneThumbnailKey(SceneKey("sample", "Resumed"), false)
        queue.sync(listOf(key))
        assertEquals(key, queue.awaitNext())
    }
}
