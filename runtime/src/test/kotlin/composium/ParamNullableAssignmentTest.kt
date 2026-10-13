package oleginvoke.com.composium

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
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
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamNullableAssignmentTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun programmaticChoiceThenNullRestoresTheLatestAssignedChoice() {
        val driver = intChoices()
        driver.assign(24)
        driver.assign(null)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun severalWritesBeforeRecompositionPreserveTheLastNonNullChoice() {
        val driver = intChoices()
        composeRule.runOnIdle {
            driver.assignNow(24)
            driver.assignNow(null)
        }
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun falseIsAnActiveNonNullBooleanValue() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(null)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(false)
        assertBoolean(driver, false, true)
        driver.assign(null)
        assertBoolean(driver, null, false, preview = false)
        driver.activate(true)
        assertBoolean(driver, false, true)
    }

    @Test
    fun anEmptyStringIsAnActiveNonNullStringValue() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param(null)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign("")
        assertString(driver, "", true)
        driver.assign(null)
        assertString(driver, null, false, preview = "")
        driver.activate(true)
        assertString(driver, "", true)
    }

    @Test
    fun aProgrammaticStringIsRememberedAcrossNull() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param("initial")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign("updated")
        driver.assign(null)
        driver.activate(false)
        driver.activate(true)
        assertString(driver, "updated", true)
    }

    @Test
    fun aProgrammaticBooleanIsRememberedAcrossNull() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(false)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(true)
        driver.assign(null)
        driver.activate(false)
        driver.activate(true)
        assertBoolean(driver, true, true)
    }

    @Test
    fun aFreeObjectValueIsRememberedAcrossNull() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(16 named "Spacing")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        driver.assign(null)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Spacing", options = emptyList(), isActive = true)
    }

    @Test
    fun primitiveHistoryPreviewUpdatesEvenWhenBatchedWritesFinishAtNull() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param(null)
            driver.ObserveInChild({ choice }, { choice = it })
        }
        composeRule.runOnIdle {
            driver.assignNow("updated")
            driver.assignNow(null)
        }
        assertString(driver, null, false, preview = "updated")
        driver.activate(true)
        assertString(driver, "updated", true)
    }

    @Test
    fun changedOptionsAndBatchedWritesRestoreTheNewAssignedRole() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        val driver = colorChoices { options }
        composeRule.runOnIdle {
            options = colors(Color.Green, Color.Yellow)
            driver.assignNow(Color.Yellow)
            driver.assignNow(null)
        }
        driver.assertObject(null, "background", selectedValue = Color.Green, isActive = false)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun nullDuringAThemeChangeKeepsThePreviouslyClickedRole() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        val driver = colorChoices { options }
        driver.activate(true)
        driver.select("surface")
        composeRule.runOnIdle {
            options = colors(Color.Red, Color.Yellow)
            driver.assignNow(null)
        }
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun immediateActivationAfterBatchedWritesKeepsThePendingAssignedRole() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        val driver = colorChoices { options }
        composeRule.runOnIdle {
            options = colors(Color.Green, Color.Yellow)
            driver.assignNow(Color.Yellow)
            driver.assignNow(null)
            driver.scope.paramsCallbacks.onParamActivationChange("choice", true)
        }
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun immediateDeactivationKeepsThePendingAssignedRoleForRestoration() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        val driver = colorChoices { options }
        composeRule.runOnIdle {
            options = colors(Color.Green, Color.Yellow)
            driver.assignNow(Color.Yellow)
            driver.scope.paramsCallbacks.onParamActivationChange("choice", false)
        }
        driver.assertObject(null, "background", selectedValue = Color.Green, isActive = false)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun emptyChoicesWithoutAFallbackCannotBeActivated() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = emptyList())
            driver.Observe(choice, { choice }, { choice = it })
        }
        repeat(2) {
            driver.activate(true)
            driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
        }
    }

    @Test
    fun removalOfAllChoicesDeactivatesAndCannotRestoreARemovedValue() {
        var options by mutableStateOf(listOf(ParamOption(8, "Small"), ParamOption(24, "Large")))
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(default = null, options = options)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assign(24)
        composeRule.runOnIdle { options = emptyList() }
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
        driver.activate(true)
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
    }

    @Test
    fun writesBeforeFirstRegistrationPreserveRestorationHistory() {
        val driver = ParamTestDriver<Int?>(composeRule)
        var firstDeclaration = true
        driver.setContent {
            var choice: Int? by param(default = 8, options = listOf(8 named "Small", 24 named "Large"))
            if (firstDeclaration) {
                firstDeclaration = false
                choice = 24
                choice = null
            }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun inferredChoicesResolveHistoryAssignedBeforeFirstRegistration() {
        val driver = ParamTestDriver<InferredChoice?>(composeRule)
        var firstDeclaration = true
        driver.setContent {
            var choice: InferredChoice? by param(null)
            if (firstDeclaration) {
                firstDeclaration = false
                choice = InferredChoice.Second
                choice = null
            }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.activate(true)
        driver.assertObject(InferredChoice.Second, "Second", isActive = true)
    }

    @Test
    fun discardedSnapshotRollsBackValueActivityAndHistoryTogether() {
        val driver = intChoices()
        driver.select("Large")
        driver.assertObject(24, "Large", isActive = true)
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle {
                snapshot.enter {
                    driver.assignNow(8)
                    driver.assignNow(null)
                }
            }
        } finally {
            snapshot.dispose()
        }
        driver.assertObject(24, "Large", isActive = true)
        driver.assign(null)
        driver.activate(false)
        driver.activate(true)
        driver.assertObject(24, "Large", isActive = true)
    }

    @Test
    fun delayedSnapshotKeepsBatchedHistoryAcrossAnUnrelatedComposition() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        var tick by mutableStateOf(0)
        val driver = ParamTestDriver<Color?>(composeRule)
        driver.setContent {
            @Suppress("UNUSED_VARIABLE") val observedTick = tick
            var choice: Color? by param(default = null, options = options)
            driver.ObserveInChild({ choice }, { choice = it })
        }
        driver.activate(true)
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle {
                snapshot.enter {
                    options = colors(Color.Green, Color.Yellow)
                    driver.assignNow(Color.Yellow)
                    driver.assignNow(null)
                }
            }
            composeRule.runOnIdle { tick++ }
            driver.assertObject(Color.Blue, "background", isActive = true)
            composeRule.runOnIdle { snapshot.apply().check() }
        } finally {
            snapshot.dispose()
        }
        driver.assertObject(null, "background", selectedValue = Color.Green, isActive = false)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun equalValuedChoicesKeepTheClickedNameAcrossANullAssignment() {
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            var choice: Int? by param(listOf(8 named "First", 8 named "Second"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.select("Second")
        driver.assign(8)
        driver.assign(null)
        driver.activate(true)
        driver.assertObject(8, "Second", isActive = true)
    }

    @Test
    fun repeatedNullSideEffectWritesSettle() {
        composeRule.mainClock.autoAdvance = false
        val driver = ParamTestDriver<Int?>(composeRule)
        var compositions = 0
        driver.setContent {
            var choice: Int? by param(default = 8, options = listOf(8 named "Small", 24 named "Large"))
            SideEffect { compositions++; choice = null }
            driver.Observe(choice, { choice }, { choice = it })
        }
        repeat(8) { composeRule.mainClock.advanceTimeByFrame() }
        composeRule.runOnIdle { assertTrue(compositions <= 3, "compositions=$compositions") }
        driver.assertObject(null, "Small", selectedValue = 8, isActive = false)
    }

    @Test
    fun aUiChoiceSupersedesBatchedAssignmentsAndTheirRestorationHistory() {
        var options by mutableStateOf(colors(Color.Blue, Color.Red))
        val driver = colorChoices { options }
        driver.activate(true)
        composeRule.runOnIdle {
            val surface = (driver.scope.params.single() as ObjectParamDescriptor).options.single { it.name == "surface" }
            options = colors(Color.Green, Color.Yellow)
            driver.assignNow(Color.Green)
            driver.assignNow(null)
            driver.scope.paramsCallbacks.onObjectParamChange("choice", surface)
        }
        driver.assertObject(Color.Yellow, "surface", isActive = true)
        driver.assign(null)
        driver.activate(true)
        driver.assertObject(Color.Yellow, "surface", isActive = true)
    }

    @Test
    fun stringSnapshotRollbackDoesNotLeakTheAssignedHistory() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param("initial")
            driver.ObserveInChild({ choice }, { choice = it })
        }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle {
                snapshot.enter { driver.assignNow("updated"); driver.assignNow(null) }
            }
        } finally { snapshot.dispose() }
        assertString(driver, "initial", true)
        driver.assign(null)
        assertString(driver, null, false, preview = "initial")
        driver.activate(true)
        assertString(driver, "initial", true)
    }

    @Test
    fun booleanSnapshotRollbackDoesNotLeakTheAssignedHistory() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(false)
            driver.Observe(choice, { choice }, { choice = it })
        }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle {
                snapshot.enter { driver.assignNow(true); driver.assignNow(null) }
            }
        } finally { snapshot.dispose() }
        assertBoolean(driver, false, true)
        driver.assign(null)
        driver.activate(true)
        assertBoolean(driver, false, true)
    }

    @Test
    fun aDelayedFreeObjectSnapshotPreservesItsHistoryAcrossAnUnrelatedComposition() {
        var tick by mutableStateOf(0)
        val driver = ParamTestDriver<Int?>(composeRule)
        driver.setContent {
            @Suppress("UNUSED_VARIABLE") val observedTick = tick
            var choice: Int? by param(16 named "Spacing")
            driver.ObserveInChild({ choice }, { choice = it })
        }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            composeRule.runOnIdle { snapshot.enter { driver.assignNow(24); driver.assignNow(null) } }
            composeRule.runOnIdle { tick++ }
            driver.assertObject(16, "Spacing", options = emptyList(), isActive = true)
            composeRule.runOnIdle { snapshot.apply().check() }
        } finally { snapshot.dispose() }
        driver.assertObject(null, "Not selected", options = emptyList(), isActive = false)
        driver.activate(true)
        driver.assertObject(24, "Spacing", options = emptyList(), isActive = true)
    }

    @Test
    fun aUiStringEditSupersedesPendingProgrammaticRestoration() {
        val driver = ParamTestDriver<String?>(composeRule)
        driver.setContent {
            var choice: String? by param("initial")
            driver.Observe(choice, { choice }, { choice = it })
        }
        composeRule.runOnIdle {
            driver.assignNow("code")
            driver.assignNow(null)
            driver.scope.paramsCallbacks.onStringParamChange("choice", "ui")
            driver.scope.paramsCallbacks.onParamActivationChange("choice", false)
            driver.scope.paramsCallbacks.onParamActivationChange("choice", true)
        }
        assertString(driver, "ui", true)
    }

    @Test
    fun aUiBooleanEditSupersedesPendingProgrammaticRestoration() {
        val driver = ParamTestDriver<Boolean?>(composeRule)
        driver.setContent {
            var choice: Boolean? by param(false)
            driver.Observe(choice, { choice }, { choice = it })
        }
        composeRule.runOnIdle {
            driver.assignNow(true)
            driver.assignNow(null)
            driver.scope.paramsCallbacks.onBooleanParamChange("choice", false)
            driver.scope.paramsCallbacks.onParamActivationChange("choice", false)
            driver.scope.paramsCallbacks.onParamActivationChange("choice", true)
        }
        assertBoolean(driver, false, true)
    }

    private fun intChoices(): ParamTestDriver<Int?> = ParamTestDriver<Int?>(composeRule).also { driver ->
        driver.setContent {
            var choice: Int? by param(default = 8, options = listOf(8 named "Small", 24 named "Large"))
            driver.Observe(choice, { choice }, { choice = it })
        }
    }

    private fun colorChoices(options: () -> List<ParamOption<Color>>): ParamTestDriver<Color?> =
        ParamTestDriver<Color?>(composeRule).also { driver ->
            driver.setContent {
                var choice: Color? by param(default = null, options = options())
                driver.Observe(choice, { choice }, { choice = it })
            }
        }

    private fun colors(background: Color, surface: Color): List<ParamOption<Color>> = listOf(
        ParamOption(background, "background"), ParamOption(surface, "surface"),
    )

    private fun assertBoolean(driver: ParamTestDriver<Boolean?>, value: Boolean?, active: Boolean, preview: Boolean = value ?: false) {
        driver.assertValue(value)
        composeRule.runOnIdle {
            val descriptor = driver.scope.params.single() as BooleanParamDescriptor
            assertEquals(active, descriptor.activation?.isActive)
            assertEquals(preview, descriptor.value)
        }
    }

    private fun assertString(driver: ParamTestDriver<String?>, value: String?, active: Boolean, preview: String = value.orEmpty()) {
        driver.assertValue(value)
        composeRule.runOnIdle {
            val descriptor = driver.scope.params.single() as StringParamDescriptor
            assertEquals(active, descriptor.activation?.isActive)
            assertEquals(preview, descriptor.value)
        }
    }

    private enum class InferredChoice { First, Second }
}
