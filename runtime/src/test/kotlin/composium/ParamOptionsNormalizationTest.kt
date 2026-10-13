package oleginvoke.com.composium

import org.junit.Test
import kotlin.test.assertEquals

class ParamOptionsNormalizationTest {
    @Test
    fun duplicateNamesSkipSuffixesAlreadyPresentAnywhereInTheList() {
        val options = listOf(
            ParamOption(1, "Size"), ParamOption(2, "Size"),
            ParamOption(3, "Size1"), ParamOption(4, "Size"), ParamOption(5, "Size2"),
        )
        assertEquals(
            listOf(1 to "Size", 2 to "Size3", 3 to "Size1", 4 to "Size4", 5 to "Size2"),
            options.ensureUniqueNames().map { it.value to it.name },
        )
    }

    @Test
    fun emptyLabelsAreDisambiguatedAndExplicitNumericLabelsAreReserved() {
        val options = listOf(ParamOption(1, ""), ParamOption(2, ""), ParamOption(3, "1"))
        assertEquals(listOf("", "2", "1"), options.ensureUniqueNames().map { it.name })
    }

    @Test
    fun labelsAreCaseSensitiveAndWhitespaceIsPreserved() {
        val options = listOf(ParamOption(1, "Size"), ParamOption(2, "size"), ParamOption(3, " Size "))
        assertEquals(listOf("Size", "size", " Size "), options.ensureUniqueNames().map { it.name })
    }

    @Test
    fun normalizationIsIdempotentForPreviouslyDisambiguatedNames() {
        val normalized = listOf(ParamOption(1, "Same"), ParamOption(2, "Same1"), ParamOption(3, "Same2"))
        assertEquals(normalized, normalized.ensureUniqueNames().ensureUniqueNames())
    }

    @Test
    fun generatedNamesCoverNullAndScalarValues() {
        val scope = SceneScope()
        val options = with(scope) { listOf(null, "Hello", 16, 2.5, false, 'x').toParamOptions() }
        assertEquals(listOf("null", "Hello", "16", "2.5", "false", "x"), options.map { it.name })
    }

    @Test
    fun generatedObjectNameUsesTheClassNameRatherThanToString() {
        val options = with(SceneScope()) { listOf(CustomDisplayValue()).toParamOptions() }
        assertEquals(listOf("CustomDisplayValue"), options.map { it.name })
    }

    private class CustomDisplayValue {
        override fun toString(): String = "custom display text"
    }
}
