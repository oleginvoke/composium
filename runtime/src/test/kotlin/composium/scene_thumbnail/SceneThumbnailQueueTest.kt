package oleginvoke.com.composium.scene_thumbnail

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import oleginvoke.com.composium.SceneKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SceneThumbnailQueueTest {
    @Test
    fun waitingConsumerStartsWhenWorkArrivesWithoutAdvancingTime() = runTest {
        val queue = SceneThumbnailQueue()
        val key = SceneThumbnailKey(SceneKey("sample", "First"), false)
        val available = async { queue.awaitWork() }
        runCurrent()
        assertFalse(available.isCompleted)
        queue.sync(listOf(key))
        runCurrent()
        assertTrue(available.isCompleted, "Enqueue must wake the consumer without a polling delay")
        assertEquals(key, queue.next())
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun waitingConsumerUsesLatestPriorityAndSkipsRemovedWork() = runTest {
        val queue = SceneThumbnailQueue()
        val first = SceneThumbnailKey(SceneKey("sample", "First"), false)
        val second = SceneThumbnailKey(SceneKey("sample", "Second"), false)
        queue.sync(listOf(first))
        queue.remove(first)
        val available = async { queue.awaitWork() }
        runCurrent()
        assertFalse(available.isCompleted)
        queue.sync(listOf(first, second))
        queue.prioritize(listOf(second))
        runCurrent()
        assertTrue(available.isCompleted)
        assertEquals(second, queue.next())
        assertEquals(first, queue.next())
    }

    @Test
    fun cancellingAnIdleConsumerDoesNotLoseFutureWork() = runTest {
        val queue = SceneThumbnailQueue()
        val abandoned = async { queue.awaitWork() }
        runCurrent()
        abandoned.cancel()
        runCurrent()
        val key = SceneThumbnailKey(SceneKey("sample", "Resumed"), false)
        queue.sync(listOf(key))
        queue.awaitWork()
        assertEquals(key, queue.next())
    }
}
