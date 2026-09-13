package oleginvoke.com.composium

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneRegistrationTest {
    @Test
    fun differentScenesWithOneKeyWarnOnceAndKeepTheFirst() {
        val first = scene("Warning regression", "Primary")
        val duplicate = scene("Warning regression", "Primary")
        val start = ComposiumRuntime.scenes.size
        ShadowLog.clear()

        Composium.registerAll(first, duplicate)
        Composium.register(duplicate)
        Composium.register(scene("Warning regression", "Primary"))

        assertSame(first, ComposiumRuntime.scenes.drop(start).single().scene)
        val warnings = ShadowLog.getLogsForTag("Composium").filter { it.type == Log.WARN }
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().msg.contains("Warning regression"))
        assertTrue(warnings.single().msg.contains("Primary"))
        assertTrue(warnings.single().msg.contains("ignored"))
    }

    @Test
    fun repeatedReadsAndRegistrationOfOnePropertyDoNotWarn() {
        val Primary by oleginvoke.com.composium.scene(group = "Quiet property regression") {}
        val start = ComposiumRuntime.scenes.size
        ShadowLog.clear()

        Composium.register(Primary)
        Composium.registerAll(listOf(Primary, Primary))

        assertSame(Primary, ComposiumRuntime.scenes.drop(start).single().scene)
        assertTrue(ShadowLog.getLogsForTag("Composium").isEmpty())
    }

    @Test
    fun separatorInGroupDoesNotHideSceneWithSeparatorInName() {
        val first = scene("Buttons::Primary", "Disabled")
        val second = scene("Buttons", "Primary::Disabled")
        val start = ComposiumRuntime.scenes.size

        ComposiumRuntime.registerAll(first, second)

        assertEquals(listOf(first, second), ComposiumRuntime.scenes.drop(start).map { it.scene })
    }

    @Test
    fun nullAndEmptyGroupsRemainEquivalent() {
        val first = scene(null, "Ungrouped")
        val start = ComposiumRuntime.scenes.size

        ComposiumRuntime.registerAll(first, scene("", "Ungrouped"))

        assertSame(first, ComposiumRuntime.scenes.drop(start).single().scene)
    }

    @Test
    fun groupWhitespaceRemainsSignificant() {
        val first = scene("Buttons/Primary", "Whitespace")
        val second = scene(" Buttons / Primary ", "Whitespace")
        val start = ComposiumRuntime.scenes.size

        ComposiumRuntime.registerAll(first, second)

        assertEquals(listOf(first, second), ComposiumRuntime.scenes.drop(start).map { it.scene })
    }

    private fun scene(group: String?, name: String) = Scene(group = group, name = name) {}
}
