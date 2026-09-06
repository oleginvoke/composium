package oleginvoke.com.composium

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneRegistrationTest {
    @Test
    fun separatorInGroupDoesNotHideSceneWithSeparatorInName() {
        val first = scene("Buttons::Primary", "Disabled")
        val second = scene("Buttons", "Primary::Disabled")
        val start = ComposiumRuntime.scenes.size

        ComposiumRuntime.registerAll(first, second)

        assertEquals(listOf(first, second), ComposiumRuntime.scenes.drop(start).map { it.scene })
    }

    @Test
    fun repeatedRegistrationStillKeepsTheFirstScene() {
        val first = scene("Repeated", "Primary")
        val duplicate = scene("Repeated", "Primary")
        val start = ComposiumRuntime.scenes.size

        ComposiumRuntime.registerAll(first, duplicate)
        ComposiumRuntime.register(first)

        val added = ComposiumRuntime.scenes.drop(start)
        assertEquals(1, added.size)
        assertSame(first, added.single().scene)
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
