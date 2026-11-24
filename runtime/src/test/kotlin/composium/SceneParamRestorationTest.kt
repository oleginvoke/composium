package oleginvoke.com.composium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import oleginvoke.com.composium.host_screen.ComposiumHostScreen
import oleginvoke.com.composium.scene_screen.SceneScreen
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 35])
class SceneParamRestorationTest {
    @get:Rule val composeRule = createComposeRule()
    private lateinit var scope: SceneScope
    private val restoration = StateRestorationTester(composeRule)
    private val registeredKeys = mutableSetOf<SceneKey>()

    @After
    fun removeScenes() {
        val entries = ReflectionHelpers.getField<MutableList<SceneEntry>>(ComposiumRuntime, "_scenes")
        val registry = ReflectionHelpers.getField<MutableMap<SceneKey, Scene>>(ComposiumRuntime, "registeredScenesById")
        entries.removeAll { it.id in registeredKeys }
        registeredKeys.forEach(registry::remove)
    }

    @Test
    fun booleanStringAndNullableHistoryRestoreBeforeTheFirstSceneFrame() {
        val frames = mutableListOf<String>()
        render {
            val enabled: Boolean by param(true)
            val text: String by param("Default")
            val optionalFlag: Boolean? by param(true)
            val optionalText: String? by param(null)
            val frame = "$enabled|$text|$optionalFlag|$optionalText"
            BasicText(frame, Modifier.testTag("values"))
            SideEffect { frames += frame }
        }
        composeRule.runOnIdle {
            scope.paramsCallbacks.onBooleanParamChange("enabled", false)
            scope.paramsCallbacks.onStringParamChange("text", "")
            scope.paramsCallbacks.onBooleanParamChange("optionalFlag", false)
            scope.paramsCallbacks.onStringParamChange("optionalText", "Configured")
            scope.paramsCallbacks.onParamActivationChange("optionalFlag", false)
            scope.paramsCallbacks.onParamActivationChange("optionalText", false)
        }
        composeRule.waitForIdle()
        frames.clear()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("values").assertTextEquals("false||null|null")
        assertEquals("false||null|null", frames.first(), "No frame with defaults after restoration")
        composeRule.runOnIdle {
            scope.paramsCallbacks.onParamActivationChange("optionalFlag", true)
            scope.paramsCallbacks.onParamActivationChange("optionalText", true)
        }
        composeRule.onNodeWithTag("values").assertTextEquals("false||false|Configured")
    }

    @Test
    fun inferredEnumAndSealedSelectionsRestore() {
        render {
            val tone: Tone by param(Tone.First)
            val size: TestSize by param(TestSize.Small)
            BasicText("$tone|$size", Modifier.testTag("values"))
        }
        select("tone", "Second")
        select("size", "Large")
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("Second|Large")
        assertSelection("tone", "Second")
        assertSelection("size", "Large")
    }

    @Test
    fun arbitraryObjectChoicesUseNamesAndNewOptionInstancesAfterRestoration() {
        var generation = 1
        lateinit var readChoice: () -> Choice
        render {
            val choices = remember(generation) { listOf(
                Choice("First", generation) named "First",
                Choice("Second", generation) named "Second",
            ) }
            val choice: Choice by param(choices)
            BasicText("${choice.label}|${choice.generation}", Modifier.testTag("values"))
            SideEffect { readChoice = { choice } }
        }
        select("choice", "Second")
        val before = readChoice()
        generation = 2
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("Second|2")
        assertSelection("choice", "Second")
        composeRule.runOnIdle {
            val selected = (scope.params.single() as ObjectParamDescriptor).options.single { it.name == "Second" }
            assertSame(selected.value, readChoice())
            assertNotSame(before, readChoice())
        }
    }

    @Test
    fun equalNamedChoicesAndDisabledObjectHistoryRemainDistinct() {
        var generation = 1
        render {
            val equal: Int by param(listOf(8 named "First", 8 named "Second"))
            val choices = remember(generation) { listOf(
                Choice("First", generation) named "First",
                Choice("Second", generation) named "Second",
            ) }
            val optional: Choice? by param(choices)
            BasicText("$equal|${optional?.label}|${optional?.generation}", Modifier.testTag("values"))
        }
        select("equal", "Second")
        select("optional", "Second")
        composeRule.runOnIdle { scope.paramsCallbacks.onParamActivationChange("optional", false) }
        generation = 2
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("8|null|null")
        assertSelection("equal", "Second")
        composeRule.runOnIdle { scope.paramsCallbacks.onParamActivationChange("optional", true) }
        composeRule.onNodeWithTag("values").assertTextEquals("8|Second|2")
        assertSelection("optional", "Second")
    }

    @Test
    fun parametersRestoreByNameWhenConfigurationChangesDeclarationOrder() {
        var reversed = false
        render {
            Column {
                val names = if (reversed) listOf("Second", "First") else listOf("First", "Second")
                names.forEach { name -> NamedParameter(name) }
            }
        }
        composeRule.runOnIdle {
            scope.paramsCallbacks.onStringParamChange("First", "Configured first")
            scope.paramsCallbacks.onStringParamChange("Second", "Configured second")
        }
        reversed = true
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("First").assertTextEquals("Configured first")
        composeRule.onNodeWithTag("Second").assertTextEquals("Configured second")
    }

    @Test
    fun valueAssignedBySceneCodeAlsoRestoresWhenItIsOutsideTheListedChoices() {
        lateinit var assign: (Int) -> Unit
        render {
            var choice: Int by param(listOf(1 named "First", 2 named "Second"))
            BasicText(choice.toString(), Modifier.testTag("values"))
            SideEffect { assign = { choice = it } }
        }
        composeRule.runOnIdle { assign(99) }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("99")
    }

    @Test
    fun missingChoiceFallsBackAndReturnsWhenThatOptionIsAvailableAgain() {
        var includeSecond = true
        val revision = mutableIntStateOf(0)
        render {
            revision.intValue
            val choice: Int by param(buildList {
                add(1 named "First")
                if (includeSecond) add(2 named "Second")
            })
            BasicText(choice.toString(), Modifier.testTag("values"))
        }
        select("choice", "Second")
        includeSecond = false
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("1")
        composeRule.runOnIdle {
            includeSecond = true
            revision.intValue++
        }
        composeRule.onNodeWithTag("values").assertTextEquals("2")
        assertSelection("choice", "Second")
    }

    @Test
    fun openSceneRestoresParametersTogetherWithItsDestinationAndResetsOnExplicitExit() {
        val scene = Scene(null, "Parameter restoration host fixture", thumbnail = null) {
            scope = this
            val caption: String by param("Default")
            BasicText(caption, Modifier.testTag("values"))
        }
        registeredKeys += SceneEntry(scene).id
        ComposiumRuntime.register(scene)
        restoration.setContent {
            ComposiumTheme(darkTheme = false) { ComposiumHostScreen() }
        }
        openScene(scene.name)
        composeRule.runOnIdle { scope.paramsCallbacks.onStringParamChange("caption", "Configured") }
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithTag("values").assertTextEquals("Configured")

        composeRule.runOnIdle { scope.host.closeScene() }
        composeRule.waitForIdle()
        openScene(scene.name)
        composeRule.onNodeWithTag("values").assertTextEquals("Default")
    }

    private fun render(content: @Composable SceneScope.() -> Unit) {
        val entry = SceneEntry(Scene(null, "Parameter restoration", thumbnail = null) {
            scope = this
            content()
        })
        restoration.setContent {
            ComposiumTheme(darkTheme = false) { SceneScreen(entry, onBack = {}) }
        }
    }

    private fun select(name: String, option: String) = composeRule.runOnIdle {
        val descriptor = scope.params.single { it.name == name } as ObjectParamDescriptor
        scope.paramsCallbacks.onObjectParamChange(name, descriptor.options.single { it.name == option })
    }

    private fun assertSelection(name: String, option: String) = composeRule.runOnIdle {
        assertEquals(option, (scope.params.single { it.name == name } as ObjectParamDescriptor).selectedOption.name)
    }

    private fun openScene(name: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(name))
        composeRule.onNodeWithText(name).performClick()
        composeRule.waitForIdle()
    }

    @Composable
    private fun SceneScope.NamedParameter(name: String) {
        val value: String by param("Default $name", name = name)
        BasicText(value, Modifier.testTag(name))
    }

    private enum class Tone { First, Second }

    private sealed interface TestSize {
        data object Small : TestSize
        data object Large : TestSize
    }

    private class Choice(val label: String, val generation: Int)
}
