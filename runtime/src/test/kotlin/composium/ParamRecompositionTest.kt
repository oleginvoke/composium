package oleginvoke.com.composium

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamRecompositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reorderingDistinctOptionsPreservesTheSelectedValueAndName() {
        var reversed by mutableStateOf(false)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            val options = listOf(8 named "Small", 24 named "Large")
            var choice: Int by param(if (reversed) options.reversed() else options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { reversed = true }
        driver.assertObject(24, "Large", options = listOf(24 to "Large", 8 to "Small"))
    }

    @Test
    fun reorderingEqualOptionsPreservesTheSelectedName() {
        var reversed by mutableStateOf(false)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            val options = listOf(8 named "First", 8 named "Second")
            var choice: Int by param(if (reversed) options.reversed() else options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("First")
        driver.assertObject(8, "First")
        composeRule.runOnIdle { reversed = true }
        driver.assertObject(8, "First", options = listOf(8 to "Second", 8 to "First"))
    }

    @Test
    fun reintroducingAnEqualOptionRestoresItsSavedSelectionName() {
        var includeSecond by mutableStateOf(true)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(buildList {
                add(8 named "First")
                if (includeSecond) add(8 named "Second")
            })
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assertObject(8, "Second")
        composeRule.runOnIdle { includeSecond = false }
        driver.assertObject(8, "First", options = listOf(8 to "First"))
        composeRule.runOnIdle { includeSecond = true }
        driver.assertObject(8, "Second", options = listOf(8 to "First", 8 to "Second"))
    }

    @Test
    fun renamingTheOptionWithTheSelectedValueUpdatesItsDisplayedName() {
        var label by mutableStateOf("Large")
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = listOf(8 named "Small", 24 named label))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { label = "Renamed" }
        driver.assertObject(24, "Renamed", options = listOf(8 to "Small", 24 to "Renamed"))
    }

    @Test
    fun removingTheSelectedOptionFallsBackToTheDefaultValue() {
        var includeLarge by mutableStateOf(true)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = buildList {
                add(8 named "Small")
                if (includeLarge) add(24 named "Large")
            })
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { includeLarge = false }
        driver.assertObject(8, "Small", options = listOf(8 to "Small"))
    }

    @Test
    fun replacingTheSelectedOptionsValueUpdatesTheDelegatedValue() {
        var large by mutableStateOf(24)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = listOf(8 named "Small", large named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { large = 32 }
        driver.assertObject(32, "Large", options = listOf(8 to "Small", 32 to "Large"))
        driver.select("Large")
        driver.assertObject(32, "Large")
    }

    @Test
    fun equalValuesBecomingDistinctFollowTheClickedName() {
        var second by mutableStateOf(8)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(8 named "First", second named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        composeRule.runOnIdle { second = 24 }
        driver.assertObject(24, "Second", options = listOf(8 to "First", 24 to "Second"))
        driver.select("Second")
        driver.assertObject(24, "Second")
    }

    @Test
    fun distinctValuesBecomingEqualPreserveTheSelectedName() {
        var first by mutableStateOf(8)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(listOf(first named "First", 24 named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        composeRule.runOnIdle { first = 24 }
        driver.assertObject(24, "Second", options = listOf(24 to "First", 24 to "Second"))
    }

    @Test
    fun changesToRawDefaultDoNotOverwriteAUserSelection() {
        var default by mutableStateOf(8)
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = default, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { default = 16 }
        driver.assertObject(24, "Large", options = listOf(16 to "16", 8 to "Small", 24 to "Large"))
    }

    @Test
    fun changingANamedDefaultWithoutOptionsCurrentlyKeepsTheOriginalSelectedName() {
        var default by mutableStateOf(ParamOption(8, "Original"))
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        composeRule.runOnIdle { default = ParamOption(16, "Renamed default") }
        driver.assertObject(24, "Original", options = emptyList())
    }

    @Test
    fun themeValueChangesUpdateTheSelectedColorWithoutAnotherClick() {
        var options by mutableStateOf(listOf(ParamOption(Color.Blue, "background"), ParamOption(Color.Red, "surface")))
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            var choice: Color by param(options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("surface")
        composeRule.runOnIdle { options = listOf(ParamOption(Color.Green, "background"), ParamOption(Color.Yellow, "surface")) }
        driver.assertObject(Color.Yellow, "surface", options = listOf(Color.Green to "background", Color.Yellow to "surface"))
        driver.select("surface")
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun reactivationRestoresTheUpdatedValueForThePreviouslySelectedName() {
        var large by mutableStateOf(24)
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = 8, options = listOf(8 named "Small", large named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        driver.activate(false)
        composeRule.runOnIdle { large = 32 }
        driver.activate(true)
        driver.assertObject(32, "Large", isActive = true)
    }

    @Test
    fun reactivationAfterSelectedOptionRemovalCurrentlyRestoresTheDefaultBeforeTheOldValue() {
        var includeLarge by mutableStateOf(true)
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = 8, options = buildList {
                add(8 named "Small")
                if (includeLarge) add(24 named "Large")
            })
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        driver.activate(false)
        composeRule.runOnIdle { includeLarge = false }
        driver.activate(true)
        driver.assertObject(8, "Small", isActive = true)
    }

    @Test
    fun reactivationAfterARenameCurrentlyRestoresTheDefaultBeforeMatchingTheOldValue() {
        var label by mutableStateOf("Large")
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = 8, options = listOf(8 named "Small", 24 named label))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        driver.activate(false)
        composeRule.runOnIdle { label = "Renamed" }
        driver.activate(true)
        driver.assertObject(8, "Small", isActive = true)
    }

    @Test
    fun reactivationWithNullDefaultAfterARenameRestoresThePreviousValueUnderItsNewName() {
        var label by mutableStateOf("Large")
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = listOf(8 named "Small", 24 named label))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.select("Large")
        driver.activate(false)
        composeRule.runOnIdle { label = "Renamed" }
        driver.activate(true)
        driver.assertObject(24, "Renamed", isActive = true)
    }

    @Test
    fun reactivationWithNullDefaultAfterSelectedOptionRemovalRestoresTheFirstOption() {
        var includeLarge by mutableStateOf(true)
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = buildList {
                add(8 named "Small")
                if (includeLarge) add(24 named "Large")
            })
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.select("Large")
        driver.activate(false)
        composeRule.runOnIdle { includeLarge = false }
        driver.activate(true)
        driver.assertObject(8, "Small", isActive = true)
    }

    @Test
    fun differentValuesWithCollidingHashesUpdateRegisteredOptions() {
        var second by mutableStateOf(CollidingValue(2))
        val driver = ParamTestDriver<CollidingValue>(composeRule)
        driver.setContent {
            var choice: CollidingValue by param(
                default = CollidingValue(1),
                options = listOf(CollidingValue(1) named "One", second named "Two"),
            )
            driver.Observe(choice, { choice }, { choice = it })
        }
        composeRule.runOnIdle { second = CollidingValue(3) }
        driver.assertObject(CollidingValue(1), "One", options = listOf(CollidingValue(1) to "One", CollidingValue(3) to "Two"))
        driver.select("Two")
        driver.assertObject(CollidingValue(3), "Two")
    }

    @Test
    fun differentLabelsWithCollidingHashesUpdateRegisteredLabels() {
        var label by mutableStateOf("Aa")
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = listOf(8 named "Small", 24 named label))
            driver.Observe(choice, { choice }, { choice = it })
        }
        composeRule.runOnIdle { label = "BB" }
        driver.assertObject(8, "Small", options = listOf(8 to "Small", 24 to "BB"))
    }

    private data class CollidingValue(val number: Int) {
        override fun hashCode(): Int = 0
    }
}
