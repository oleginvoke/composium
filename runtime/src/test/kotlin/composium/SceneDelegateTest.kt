package oleginvoke.com.composium

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class SceneDelegateTest {
    @Test
    fun repeatedReadsReturnTheSameScene() {
        val Primary by scene(group = "Buttons") {}

        assertSame(Primary, Primary)
        assertEquals("Primary", Primary.name)
        assertEquals("Buttons", Primary.group)
    }

    @Test
    fun reusedDelegateKeepsEachPropertyIndependent() {
        val definition = scene {}
        val First by definition
        val Second by definition

        assertEquals("First", First.name)
        assertEquals("Second", Second.name)
        assertNotSame(First, Second)
        assertSame(First, First)
        assertSame(Second, Second)
    }

    @Test
    fun propertiesWithTheSameExplicitNameStillHaveSeparateScenes() {
        val definition = scene(name = "Primary") {}
        val First by definition
        val Second by definition

        assertNotSame(First, Second)
        assertSame(First, First)
        assertSame(Second, Second)
    }

    @Test
    fun sceneBelongsToItsOwnerInstance() {
        class Owner {
            val Entry by scene {}
        }
        val first = Owner()
        val second = Owner()

        assertSame(first.Entry, first.Entry)
        assertNotSame(first.Entry, second.Entry)
    }

    @Test
    fun concurrentFirstReadsReturnOneScene() {
        val Primary by scene {}
        val executor = Executors.newFixedThreadPool(8)
        val barrier = CyclicBarrier(8)
        try {
            val results = executor.invokeAll(
                List(8) {
                    Callable {
                        barrier.await(10, TimeUnit.SECONDS)
                        Primary
                    }
                },
                15,
                TimeUnit.SECONDS,
            ).map { it.get() }

            results.forEach { assertSame(results.first(), it) }
            assertSame(results.first(), Primary)
        } finally {
            executor.shutdownNow()
        }
    }
}
