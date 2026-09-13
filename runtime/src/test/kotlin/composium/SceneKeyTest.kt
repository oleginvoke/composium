package oleginvoke.com.composium

import kotlin.test.Test
import kotlin.test.assertEquals

class SceneKeyTest {
    @Test
    fun saveableKeysKeepAmbiguousTextPairsDistinct() {
        // Includes pairs that collide under concatenation, the old "::" delimiter,
        // or data-class toString formatting.
        val keys = listOf(
            SceneKey("Buttons::Primary", "Disabled"),
            SceneKey("Buttons", "Primary::Disabled"),
            SceneKey("A, name=B", "C"),
            SceneKey("A", "B, name=C"),
            SceneKey("", "12:Кнопка::"),
            SceneKey("12:Кнопка::", ""),
            SceneKey("a", "bc"),
            SceneKey("ab", "c"),
        )

        assertEquals(8, keys.map { it.toSaveableKey() }.toSet().size)
    }
}
