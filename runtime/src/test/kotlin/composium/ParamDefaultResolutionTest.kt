package oleginvoke.com.composium

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamDefaultResolutionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rawDefaultWithoutOptionsUsesItsGeneratedName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(16)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "16", options = emptyList(), typeName = "Object")
    }

    @Test
    fun namedDefaultWithoutOptionsKeepsItsLabelSeparateFromTheParameterName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(16 named "Comfortable", name = "Spacing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "Comfortable", options = emptyList(), paramName = "Spacing")
    }

    @Test
    fun optionsOnlyUsesTheFirstOptionWithoutSorting() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(24 named "Large", 8 named "Small"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Large", options = listOf(24 to "Large", 8 to "Small"))
    }

    @Test
    fun rawDefaultMatchingANamedOptionUsesThatOptionsName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 24, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Large", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun nullableOptionsOnlyWithANonNullFirstValueStartsActive() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "Small", isActive = true, typeName = "Object?")
    }

    @Test
    fun rawDefaultMissingFromOptionsIsPrependedWithAnAutomaticName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "16", options = listOf(16 to "16", 8 to "Small", 24 to "Large"))
    }

    @Test
    fun namedDefaultMissingFromOptionsIsPrependedWithItsExplicitName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16 named "Medium", options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "Medium", options = listOf(16 to "Medium", 8 to "Small", 24 to "Large"))
    }

    @Test
    fun namedDefaultMatchingByValueCurrentlyUsesTheOptionsNameInsteadOfItsOwn() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 24 named "Default label", options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Large", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun namedDefaultMatchingOnlyByNameCurrentlyTakesTheOptionsValue() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16 named "Large", options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Large", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun defaultValueMatchCurrentlyWinsOverAConflictingNameMatch() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8 named "Large", options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "Small", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun generatedDefaultNameMatchingAnOptionCurrentlyReplacesTheRawDefaultValue() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16, options = listOf(8 named "16", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "16", options = listOf(8 to "16", 24 to "Large"))
    }

    @Test
    fun rawDefaultWithEqualOptionValuesCurrentlySelectsTheFirstName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = listOf(8 named "First", 8 named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "First", options = listOf(8 to "First", 8 to "Second"))
    }

    @Test
    fun namedDefaultPointingAtTheSecondEqualValueCurrentlySelectsTheFirstName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8 named "Second", options = listOf(8 named "First", 8 named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "First", options = listOf(8 to "First", 8 to "Second"))
    }

    @Test
    fun duplicateOptionNamesAreDisambiguatedWithoutLosingTheDefaultValue() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 24, options = listOf(8 named "Size", 24 named "Size"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Size1", options = listOf(8 to "Size", 24 to "Size1"))
        driver.select("Size")
        driver.assertObject(8, "Size")
        driver.select("Size1")
        driver.assertObject(24, "Size1")
    }

    @Test
    fun defaultNameMatchingDuplicatedLabelsCurrentlyUsesTheFirstValue() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16 named "Size", options = listOf(8 named "Size", 24 named "Size"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "Size", options = listOf(8 to "Size", 24 to "Size1"))
    }

    @Test
    fun explicitEmptyOptionsWithARawDefaultCurrentlyProduceASingleChoice() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16, options = emptyList())
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "16", options = listOf(16 to "16"))
    }

    @Test
    fun explicitEmptyOptionsWithANamedDefaultCurrentlyProduceASingleNamedChoice() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 16 named "Medium", options = emptyList())
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "Medium", options = listOf(16 to "Medium"))
    }

    @Test
    fun optionsOnlyRejectsAnEmptyListAtRuntime() {
        val driver = ParamTestDriver<Int>(composeRule)
        assertFailsWith<NoSuchElementException> {
            driver.setContent {
                var choice: Int by param(emptyList())
                driver.Observe(choice, { choice }, { choice = it })
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun explicitStringOptionsUseObjectSelectionInsteadOfAFreeTextControl() {
        val driver = ParamTestDriver<String>(composeRule)
        driver.setContent {
            var choice: String by param(default = "a", options = listOf("a" named "Alpha", "b" named "Beta"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject("a", "Alpha", typeName = "Object")
        driver.select("Beta")
        driver.assertObject("b", "Beta")
    }

    @Test
    fun explicitBooleanOptionsUseObjectSelectionInsteadOfASwitch() {
        val driver = ParamTestDriver<Boolean>(composeRule)
        driver.setContent {
            var choice: Boolean by param(default = false, options = listOf(false named "Off", true named "On"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(false, "Off", typeName = "Object")
        driver.select("On")
        driver.assertObject(true, "On")
    }

    @Test
    fun toParamOptionsProvidesGeneratedNamesWhenLabelsAreNotSpecified() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 24, options = listOf(8, 16, 24).toParamOptions())
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "24", options = listOf(8 to "8", 16 to "16", 24 to "24"))
    }

    @Test
    fun toParamOptionsSupportsCustomLabels() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8, 24).toParamOptions { "$it dp" })
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(8, "8 dp", options = listOf(8 to "8 dp", 24 to "24 dp"))
    }
}
