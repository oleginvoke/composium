package oleginvoke.com.composium

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamNullableTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun implicitNullFirstOptionStartsInactiveAndShowsTheFirstNonNullOption() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(null named "None", 8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false,
            options = listOf(8 to "Small", 24 to "Large"), typeName = "Object?")
        driver.activate(true)
        driver.assertObject(8, "Small", isActive = true)
    }

    @Test
    fun explicitRawNullDefaultIsNotAddedToTheSelectableOptions() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false,
            options = listOf(8 to "Small", 24 to "Large"))
    }

    @Test
    fun namedNullDefaultCurrentlyShowsTheFirstOptionButRestoresByItsExplicitName() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null named "Large", options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun nullOptionsAreRemovedBeforeResolvingDuplicateNames() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = 24, options = listOf(
                null named "Size", 8 named "Size", null named "Size1", 24 named "Size",
            ))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(24, "Size1", isActive = true, options = listOf(8 to "Size", 24 to "Size1"))
    }

    @Test
    fun allNullOptionsCannotActivateWithoutARestorableValue() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(null named "None", null named "Another null"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
        driver.activate(true)
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
    }

    @Test
    fun explicitEmptyOptionsWithNullDefaultHaveNoRestorableValue() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = emptyList())
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
    }

    @Test
    fun nonNullDefaultIsInsertedWhenAllExplicitOptionsAreNull() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = 16, options = listOf(null named "None"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(16, "16", options = listOf(16 to "16"), isActive = true)
    }

    @Test
    fun deactivationClearsTheValueAndActivationRestoresTheLastDistinctSelection() {
        val driver = nullableChoices()
        driver.select("Large")
        driver.activate(false)
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun equalValueSelectionIsPreservedWhenDeactivatedAndRestored() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(8 named "First", 8 named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(8, "Second", isActive = true)
    }

    @Test
    fun reactivationOfEqualObjectsRestoresTheInstanceOfTheSelectedOption() {
        val first = EqualValue(8)
        val second = EqualValue(8)
        val driver = ParamTestDriver<EqualValue?>(composeRule)
        driver.setContent {
            var choice: EqualValue? by param(listOf(first named "First", second named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(EqualValue(8), "Second", isActive = true)
        // The equal click keeps the existing state instance; null restoration can replace it.
        composeRule.runOnIdle { assertSame(first, driver.value()) }
        driver.activate(false)
        driver.assertObject(null, "First", selectedValue = first, isActive = false)
        driver.activate(true)
        driver.assertObject(EqualValue(8), "Second", isActive = true)
        composeRule.runOnIdle {
            assertSame(second, driver.value())
            val descriptor = driver.scope.params.single() as ObjectParamDescriptor
            assertSame(second, descriptor.selectedOption.value)
        }
    }

    @Test
    fun directNullAssignmentDeactivatesAndCanBeRestoredWithOneActivation() {
        val driver = nullableChoices()
        driver.select("Large")
        driver.assign(null)
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun directNonNullAssignmentWhileInactiveActivatesTheParameter() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        driver.assertObject(24, "Large", isActive = true)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun nullableObjectWithoutOptionsRestoresItsNonNullDefault() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(16 named "Spacing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(false)
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
        driver.activate(true)
        driver.assertObject(16, "Spacing", options = emptyList(), isActive = true)
    }

    @Test
    fun nullableObjectWithoutOptionsRestoresTheLastAssignedValueBeforeItsDefault() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(16 named "Spacing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Spacing", options = emptyList(), isActive = true)
    }

    @Test
    fun nullableBooleanWithoutOptionsStartsInactiveAndRestoresFalse() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(null)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertValue(null)
        composeRule.runOnIdle {
            assertEquals(BooleanParamDescriptor("choice", "Boolean?", ParamActivation(false), false), driver.scope.params.single())
        }
        driver.activate(true)
        driver.assertValue(false)
        composeRule.runOnIdle { driver.scope.paramsCallbacks.onBooleanParamChange("choice", true) }
        driver.assertValue(true)
        driver.activate(false)
        driver.assertValue(null)
        driver.activate(true)
        driver.assertValue(true)
    }

    @Test
    fun nullableStringWithoutOptionsStartsInactiveAndRestoresAnEmptyString() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param(null named "Missing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertValue(null)
        composeRule.runOnIdle {
            assertEquals(StringParamDescriptor("choice", "String?", ParamActivation(false), ""), driver.scope.params.single())
        }
        driver.activate(true)
        driver.assertValue("")
        composeRule.runOnIdle { driver.scope.paramsCallbacks.onStringParamChange("choice", "Edited") }
        driver.activate(false)
        driver.activate(true)
        driver.assertValue("Edited")
    }

    @Test
    fun activationCallbacksForANonNullableParameterAreIgnored() {
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(false)
        driver.assertObject(8, "Small", isActive = null)
    }

    @Test
    fun nullableStringWithExplicitOptionsUsesObjectSelectionAndRestoresItsLabel() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param(default = null named "Beta", options = listOf("a" named "Alpha", "b" named "Beta"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Alpha", selectedValue = "a", isActive = false, typeName = "Object?")
        driver.activate(true)
        driver.assertObject("b", "Beta", isActive = true)
    }

    @Test
    fun nullableBooleanWithExplicitOptionsUsesObjectSelectionInsteadOfASwitch() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(default = null, options = listOf(false named "Off", true named "On"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Off", selectedValue = false, isActive = false, typeName = "Object?")
        driver.activate(true)
        driver.select("On")
        driver.assertObject(true, "On", isActive = true)
    }

    private fun nullableChoices(): ParamTestDriver<Int?> {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        return driver
    }

    private data class EqualValue(val number: Int)
}
