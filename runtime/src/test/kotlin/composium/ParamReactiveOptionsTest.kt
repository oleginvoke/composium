package oleginvoke.com.composium

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamReactiveOptionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedRoleFollowsSeveralProductThemesWithoutReclicking() {
        var scheme by mutableStateOf(lightColorScheme(background = Color.Blue, surface = Color.Red))
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            MaterialTheme(colorScheme = scheme) {
                var choice: Color by param(listOf(
                    MaterialTheme.colorScheme.background named "background",
                    MaterialTheme.colorScheme.surface named "surface",
                ))
                driver.Observe(choice, { choice }, { choice = it })
            }
        }
        driver.select("surface")
        composeRule.runOnIdle { scheme = lightColorScheme(background = Color.Green, surface = Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface")
        composeRule.runOnIdle { scheme = lightColorScheme(background = Color.Red, surface = Color.Magenta) }
        driver.assertObject(Color.Magenta, "surface")
        composeRule.runOnIdle { scheme = lightColorScheme(background = Color.Blue, surface = Color.Red) }
        driver.assertObject(Color.Red, "surface")
    }

    @Test
    fun oldSelectedValueMatchingAnotherNewOptionDoesNotSwitchRoles() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        composeRule.runOnIdle { options = colorOptions(Color.Red, Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun validAssignmentBeforeThemeChangeSelectsTheRoleToFollow() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        driver.assign(Color.Blue)
        driver.assertObject(Color.Blue, "background")
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Blue) }
        driver.assertObject(Color.Green, "background")
    }

    @Test
    fun sameValueAssignmentDuringThemeChangeKeepsTheSelectedRole() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        composeRule.runOnIdle {
            options = colorOptions(Color.Red, Color.Yellow)
            driver.assignNow(Color.Red)
        }
        driver.assertObject(Color.Yellow, "surface")
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Magenta) }
        driver.assertObject(Color.Magenta, "surface")
    }

    @Test
    fun equalAssignmentsFromSideEffectsSettleWithoutARecompositionLoop() {
        composeRule.mainClock.autoAdvance = false
        val driver = ParamTestDriver<Color>(composeRule)
        var compositions = 0
        driver.setContent {
            var choice: Color by param(colorOptions(Color.Blue, Color.Red))
            SideEffect {
                compositions++
                choice = Color.Red
            }
            driver.Observe(choice, { choice }, { choice = it })
        }
        repeat(8) { composeRule.mainClock.advanceTimeByFrame() }
        composeRule.runOnIdle { assertTrue(compositions <= 3, "compositions=$compositions") }
    }

    @Test
    fun equalAssignmentsInTheCompositionBodySettleWithoutARecompositionLoop() {
        composeRule.mainClock.autoAdvance = false
        val driver = ParamTestDriver<Color>(composeRule)
        var compositions = 0
        driver.setContent {
            var choice: Color by param(colorOptions(Color.Blue, Color.Red))
            choice = Color.Red
            SideEffect { compositions++ }
            driver.Observe(choice, { choice }, { choice = it })
        }
        repeat(8) { composeRule.mainClock.advanceTimeByFrame() }
        composeRule.runOnIdle { assertTrue(compositions <= 3, "compositions=$compositions") }
    }

    @Test
    fun aSettledEqualAssignmentDoesNotOverrideALaterThemeChange() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        driver.assign(Color.Red)
        driver.assertObject(Color.Red, "surface")
        composeRule.runOnIdle { options = colorOptions(Color.Red, Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun aDiscardedSnapshotAssignmentDoesNotChangeTheRoleToFollow() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        driver.assertObject(Color.Red, "surface")
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle { snapshot.enter { driver.assignNow(Color.Blue) } }
        } finally {
            snapshot.dispose()
        }
        driver.assertObject(Color.Red, "surface")
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun aDelayedSnapshotAssignmentRefreshesChildOnlyReadsAfterApply() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        var unrelatedTick by mutableStateOf(0)
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            @Suppress("UNUSED_VARIABLE")
            val observedTick = unrelatedTick
            var choice: Color by param(options)
            driver.ObserveInChild({ choice }, { choice = it })
        }
        driver.select("surface")
        driver.assertObject(Color.Red, "surface")
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle {
                snapshot.enter {
                    options = colorOptions(Color.Green, Color.Blue)
                    driver.assignNow(Color.Blue)
                }
            }
            composeRule.runOnIdle { unrelatedTick++ }
            driver.assertObject(Color.Red, "surface")
            composeRule.runOnIdle { snapshot.apply().check() }
        } finally {
            snapshot.dispose()
        }
        driver.assertObject(Color.Blue, "surface")
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun assigningANewOptionsValueDuringThemeChangeIsNotOverwritten() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        composeRule.runOnIdle {
            options = colorOptions(Color.Green, Color.Yellow)
            driver.assignNow(Color.Yellow)
        }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun theLastProgrammaticAssignmentWinsDuringAnOptionsUpdate() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        composeRule.runOnIdle {
            options = colorOptions(Color.Green, Color.Yellow)
            driver.assignNow(Color.Green)
            driver.assignNow(Color.Yellow)
        }
        driver.assertObject(Color.Yellow, "surface")
        composeRule.runOnIdle { options = colorOptions(Color.Cyan, Color.Magenta) }
        driver.assertObject(Color.Magenta, "surface")
    }

    @Test
    fun aRepeatedOptionClickOverridesPendingProgrammaticAssignments() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        composeRule.runOnIdle {
            val surface = (driver.scope.params.single() as ObjectParamDescriptor).options.single { it.name == "surface" }
            options = colorOptions(Color.Red, Color.Yellow)
            driver.assignNow(Color.Blue)
            driver.assignNow(Color.Red)
            driver.scope.paramsCallbacks.onObjectParamChange("choice", surface)
        }
        driver.assertObject(Color.Yellow, "surface")
    }

    @Test
    fun parameterRenameAndThemeChangePreserveTheChoice() {
        var name by mutableStateOf("Old")
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            var choice: Color by param(options, name = name)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("surface", paramName = "Old")
        composeRule.runOnIdle {
            name = "New"
            options = colorOptions(Color.Green, Color.Yellow)
        }
        driver.assertObject(Color.Yellow, "surface", paramName = "New")
        composeRule.runOnIdle { assertEquals(listOf("New"), driver.scope.params.map { it.name }) }
    }

    @Test
    fun nullableDisabledSelectionStaysNullAndRestoresTheLatestColor() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = ParamTestDriver<Color?>(composeRule)
        driver.setContent {
            var choice: Color? by param(default = null, options = options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.select("surface")
        driver.activate(false)
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertObject(null, "background", selectedValue = Color.Green, isActive = false)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun activatingAnAssignedNonNullValueKeepsTheAssignedRole() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = ParamTestDriver<Color?>(composeRule)
        driver.setContent {
            var choice: Color? by param(default = null, options = options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(Color.Red)
        driver.assertObject(Color.Red, "surface", isActive = false)
        driver.activate(true)
        driver.assertObject(Color.Red, "surface", isActive = true)
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun directlyAssignedNullIsNotAutomaticallyRestoredDuringAnOptionsUpdate() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = ParamTestDriver<Color?>(composeRule)
        driver.setContent {
            var choice: Color? by param(options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(null)
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertObject(null, "background", selectedValue = Color.Green, isActive = true)
    }

    @Test
    fun emptyNullableOptionsCannotRestoreARemovedValue() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = ParamTestDriver<Color?>(composeRule)
        driver.setContent {
            var choice: Color? by param(default = null, options = options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.select("surface")
        composeRule.runOnIdle { options = emptyList() }
        driver.assertValue(null)
        driver.activate(false)
        driver.activate(true)
        driver.assertValue(null)
        composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
        driver.assertValue(null)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun removedChoiceFallsBackAndFollowsItsNewValueWhenReintroduced() {
        var options by mutableStateOf(listOf(ParamOption(8, "Small"), ParamOption(24, "Large")))
        val driver = ParamTestDriver<Int>(composeRule)
        driver.setContent {
            var choice: Int by param(default = 8, options = options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Large")
        composeRule.runOnIdle { options = listOf(ParamOption(8, "Small")) }
        driver.assertObject(8, "Small")
        composeRule.runOnIdle { options = listOf(ParamOption(16, "Small"), ParamOption(32, "Large")) }
        driver.assertObject(32, "Large")
    }

    @Test
    fun renamedChoiceCanContinueFollowingItsNewValue() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        val driver = colorDriver { options }
        driver.select("surface")
        composeRule.runOnIdle { options = listOf(ParamOption(Color.Blue, "background"), ParamOption(Color.Red, "renamed")) }
        driver.assertObject(Color.Red, "renamed")
        composeRule.runOnIdle { options = listOf(ParamOption(Color.Green, "background"), ParamOption(Color.Yellow, "renamed")) }
        driver.assertObject(Color.Yellow, "renamed")
    }

    @Test
    fun equalContentRecompositionDoesNotWriteParameterState() {
        var tick by mutableStateOf(0)
        lateinit var property: ParamProperty<Color>
        var compositions = 0
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            tick
            val delegate = param<Color>(colorOptions(Color.Blue, Color.Red))
            var choice: Color by delegate
            SideEffect { property = delegate; compositions++ }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("surface")
        driver.assertObject(Color.Red, "surface")
        var writes = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> if (property.state in changed) writes++ }
        try {
            repeat(5) {
                composeRule.runOnIdle { tick++ }
                driver.assertObject(Color.Red, "surface")
            }
            composeRule.runOnIdle {
                Snapshot.sendApplyNotifications()
                assertEquals(0, writes)
                assertTrue(compositions >= 6, "The test must actually recompose the declaration")
            }
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun changesToAnUnselectedOptionDoNotWriteTheSelectedState() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        lateinit var property: ParamProperty<Color>
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            val delegate = param<Color>(options)
            var choice: Color by delegate
            SideEffect { property = delegate }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("surface")
        driver.assertObject(Color.Red, "surface")
        var writes = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> if (property.state in changed) writes++ }
        try {
            composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Red) }
            driver.assertObject(Color.Red, "surface")
            composeRule.runOnIdle { Snapshot.sendApplyNotifications(); assertEquals(0, writes) }
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun changingTheSelectedValueWritesOnceAndSettles() {
        var options by mutableStateOf(colorOptions(Color.Blue, Color.Red))
        lateinit var property: ParamProperty<Color>
        var compositions = 0
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            val delegate = param<Color>(options)
            var choice: Color by delegate
            SideEffect { property = delegate; compositions++ }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("surface")
        driver.assertObject(Color.Red, "surface")
        var writes = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> if (property.state in changed) writes++ }
        try {
            val before = compositions
            composeRule.runOnIdle { options = colorOptions(Color.Green, Color.Yellow) }
            driver.assertObject(Color.Yellow, "surface")
            composeRule.runOnIdle {
                Snapshot.sendApplyNotifications()
                assertEquals(1, writes)
                assertTrue(compositions - before in 1..2,
                    "The update must settle within two declaration compositions, got ${compositions - before}")
            }
            val settled = compositions
            composeRule.mainClock.advanceTimeBy(320)
            composeRule.runOnIdle { assertEquals(settled, compositions) }
        } finally {
            observer.dispose()
        }
    }

    @Test
    fun delegateReadsDoNotSearchOrCompareOptions() {
        val driver = ParamTestDriver<CountingValue>(composeRule)
        driver.setContent {
            var choice: CountingValue by param(listOf(CountingValue(1) named "One", CountingValue(2) named "Two"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Two")
        composeRule.runOnIdle {
            val expected = driver.value()
            val before = CountingValue.comparisons
            repeat(1000) { assertSame(expected, driver.value()) }
            assertEquals(before, CountingValue.comparisons)
        }
    }

    private fun colorDriver(options: () -> List<ParamOption<Color>>): ParamTestDriver<Color> {
        val driver = ParamTestDriver<Color>(composeRule)
        driver.setContent {
            var choice: Color by param(options())
            driver.Observe(choice, { choice }, { choice = it })
        }
        return driver
    }

    private fun colorOptions(background: Color, surface: Color) = listOf(
        ParamOption(background, "background"), ParamOption(surface, "surface"),
    )

    private class CountingValue(private val number: Int) {
        override fun equals(other: Any?): Boolean {
            comparisons++
            return other is CountingValue && other.number == number
        }
        override fun hashCode(): Int = number
        override fun toString(): String = number.toString()
        companion object { var comparisons: Int = 0 }
    }
}
