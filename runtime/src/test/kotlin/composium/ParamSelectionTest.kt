package oleginvoke.com.composium

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamSelectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingDistinctThemeColorsUpdatesTheValueAndTheSelectedName() {
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Color.Blue, surface = Color.Red)) {
                var choice: Color by param(listOf(
                    MaterialTheme.colorScheme.background named "colorScheme.background",
                    MaterialTheme.colorScheme.surface named "colorScheme.surface",
                ))
                driver.Observe(choice, { choice }, { choice = it })
            }
        }
        driver.select("colorScheme.surface")
        driver.assertObject(Color.Red, "colorScheme.surface")
        driver.select("colorScheme.background")
        driver.assertObject(Color.Blue, "colorScheme.background")
    }

    @Test
    fun selectingEqualThemeColorsUpdatesTheSelectedName() {
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Color.Blue, surface = Color.Blue)) {
                var choice: Color by param(listOf(
                    MaterialTheme.colorScheme.background named "colorScheme.background",
                    MaterialTheme.colorScheme.surface named "colorScheme.surface",
                ))
                driver.Observe(choice, { choice }, { choice = it })
            }
        }
        driver.select("colorScheme.surface")
        driver.assertObject(Color.Blue, "colorScheme.surface", options = listOf(
            Color.Blue to "colorScheme.background", Color.Blue to "colorScheme.surface",
        ))
        // Repeating the click must also pin the no-op path after the saved name changed.
        driver.select("colorScheme.surface")
        driver.assertObject(Color.Blue, "colorScheme.surface")
    }

    @Test
    fun selectingAnEqualObjectUpdatesTheNameWhileKeepingTheCurrentInstance() {
        val first = EqualValue(8)
        val second = EqualValue(8)
        assertNotSame(first, second)
        val driver = ParamTestDriver<EqualValue>(composeRule)
        driver.setContent {
            var choice: EqualValue by param(listOf(first named "First", second named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(EqualValue(8), "Second")
        composeRule.runOnIdle { assertSame(first, driver.value()) }
    }

    @Test
    fun referenceEqualityObjectsWithTheSameDisplayTextRemainSeparatelySelectable() {
        val first = ReferenceValue()
        val second = ReferenceValue()
        val driver = ParamTestDriver<ReferenceValue>(composeRule)
        driver.setContent {
            var choice: ReferenceValue by param(listOf(first named "First", second named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(second, "Second")
        composeRule.runOnIdle { assertSame(second, driver.value()) }
    }

    @Test
    fun directAssignmentToAListedValueOverridesThePreviouslySavedName() {
        val driver = numericChoices()
        driver.select("Large")
        driver.assign(8)
        driver.assertObject(8, "Small")
    }

    @Test
    fun assigningAwayAndBackToEqualOptionsKeepsThePreviouslyClickedName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8 named "First", 8 named "Second", 24 named "Third"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(8, "Second")
        driver.assign(24)
        driver.assertObject(24, "Third")
        driver.assign(8)
        driver.assertObject(8, "Second")
    }

    @Test
    fun directAssignmentToAnUnlistedValueCurrentlyKeepsThePreviouslySelectedName() {
        val driver = numericChoices()
        driver.select("Large")
        driver.assign(16)
        driver.assertObject(16, "Large", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun directAssignmentToAnUnlistedValueBeforeAnyClickCurrentlyKeepsTheDefaultName() {
        val driver = numericChoices()
        driver.assign(16)
        driver.assertObject(16, "Small", options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun directAssignmentWithoutOptionsCurrentlyKeepsTheOriginalGeneratedDefaultName() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(8)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        driver.assertObject(24, "8", options = emptyList())
    }

    @Test
    fun directAssignmentWithoutOptionsKeepsAnExplicitLabel() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(8 named "Spacing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        driver.assertObject(24, "Spacing", options = emptyList())
    }

    @Test
    fun assigningAnEqualObjectCurrentlyKeepsTheExistingInstance() {
        val first = EqualValue(8)
        val replacement = EqualValue(8)
        val driver = ParamTestDriver<EqualValue>(composeRule)
        driver.setContent {
            var choice: EqualValue by param(first named "Original")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(replacement)
        driver.assertObject(EqualValue(8), "Original")
        composeRule.runOnIdle { assertSame(first, driver.value()) }
    }

    @Test
    fun assignmentReadOnlyInAChildCurrentlyLeavesTheInspectorDescriptorStale() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8 named "Small", 24 named "Large"))
            driver.ObserveInChild({ choice }, { choice = it })
        }
        driver.assign(24)
        // The child renders the new delegate value, while the declaration was skipped.
        driver.assertObject(24, "Small", selectedValue = 8)
        driver.select("Large")
        driver.assertObject(24, "Large")
    }

    @Test
    fun equalNamedOptionsDoNotReplaceAnExplicitDefaultsObjectInstance() {
        val default = EqualValue(8)
        val option = EqualValue(8)
        val driver = ParamTestDriver<EqualValue>(composeRule)
        driver.setContent {
            var choice: EqualValue by param(default = default, options = listOf(option named "Option"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(EqualValue(8), "Option")
        composeRule.runOnIdle {
            assertSame(default, driver.value())
            val descriptor = driver.scope.params.single() as ObjectParamDescriptor
            assertSame(default, descriptor.selectedOption.value)
            assertSame(option, descriptor.options.single().value)
        }
    }

    @Test
    fun boxedNumericValuesOfDifferentTypesRemainDistinctOptions() {
        val driver = ParamTestDriver<Any>(composeRule)
        driver.setContent {
            var choice: Any by param(listOf(1 named "Int", 1L named "Long", 1.0 named "Double"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Long")
        driver.assertObject(1L, "Long")
        driver.select("Double")
        driver.assertObject(1.0, "Double")
    }

    @Test
    fun equalNaNOptionsCanBeSelectedByName() {
        val driver = ParamTestDriver<Double>(composeRule)
        driver.setContent {
            var choice: Double by param(listOf(Double.NaN named "First", Double.NaN named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(Double.NaN, "Second")
    }

    @Test
    fun signedZeroValuesAreDistinctUnderBoxedEquality() {
        val driver = ParamTestDriver<Double>(composeRule)
        driver.setContent {
            var choice: Double by param(listOf(0.0 named "Positive", -0.0 named "Negative"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Negative")
        driver.assertObject(-0.0, "Negative")
        driver.select("Positive")
        driver.assertObject(0.0, "Positive")
    }

    private fun numericChoices(): ParamTestDriver<Int> {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        return driver
    }

    private data class EqualValue(val number: Int)

    private class ReferenceValue {
        override fun toString(): String = "Same display text"
    }
}
