package oleginvoke.com.composium

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SceneParamRegistrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun duplicateExplicitNamesFailEvenWhenValuesMatch() {
        val scope = SceneScope()
        val failure = assertFailsWith<IllegalStateException> {
            composeRule.setContent {
                with(scope) {
                    val title: String by param("Same", name = "Text")
                    val subtitle: String by param("Same", name = "Text")
                    BasicText("$title / $subtitle")
                }
            }
            composeRule.waitForIdle()
        }

        assertTrue(failure.message.orEmpty().contains("Text"))
        assertTrue(failure.message.orEmpty().contains("unique"))
    }

    @Test
    fun repeatedHelperWithInferredNameFails() {
        val scope = SceneScope()
        val failure = assertFailsWith<IllegalStateException> {
            composeRule.setContent {
                scope.NamedText()
                scope.NamedText()
            }
            composeRule.waitForIdle()
        }

        assertTrue(failure.message.orEmpty().contains("text"))
    }

    @Test
    fun differentTypesCannotShareAName() {
        val scope = SceneScope()
        assertFailsWith<IllegalStateException> {
            composeRule.setContent {
                with(scope) {
                    val text: String by param("Hello", name = "Value")
                    val enabled: Boolean by param(true, name = "Value")
                    BasicText("$text / $enabled")
                }
            }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun renamingToAnOccupiedNameFails() {
        val scope = SceneScope()
        val name = mutableStateOf("Title")
        composeRule.setContent {
            with(scope) {
                val title: String by param("Title", name = name.value)
                val subtitle: String by param("Subtitle", name = "Subtitle")
                BasicText("$title / $subtitle")
            }
        }

        val failure = assertFailsWith<IllegalStateException> {
            composeRule.runOnIdle { name.value = "Subtitle" }
            composeRule.waitForIdle()
        }
        assertTrue(failure.message.orEmpty().contains("Subtitle"))
    }

    @Test
    fun recompositionAndDefaultChangesKeepTheSameParameterEditable() {
        val scope = SceneScope()
        val default = mutableStateOf("Initial")
        composeRule.setContent {
            with(scope) {
                val text: String by param(default.value)
                BasicText(text)
            }
        }
        composeRule.runOnIdle { scope.paramsCallbacks.onStringParamChange("text", "Edited") }
        composeRule.onNodeWithText("Edited").assertExists()
        composeRule.runOnIdle { default.value = "New default" }
        composeRule.onNodeWithText("Edited").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, scope.params.size)
            scope.paramsCallbacks.onStringParamChange("text", "Still editable")
        }
        composeRule.onNodeWithText("Still editable").assertExists()
    }

    @Test
    fun optionsChangesUpdateTheBindingWithoutAFalseDuplicate() {
        val scope = SceneScope()
        val includeThird = mutableStateOf(false)
        composeRule.setContent {
            with(scope) {
                val choice: String by param(
                    default = "A",
                    options = (if (includeThird.value) listOf("A", "B", "C") else listOf("A", "B"))
                        .toParamOptions(),
                )
                BasicText(choice)
            }
        }
        composeRule.runOnIdle {
            scope.paramsCallbacks.onObjectParamChange("choice", ParamOption<Any>("B", "B"))
        }
        composeRule.onNodeWithText("B").assertExists()
        composeRule.runOnIdle { includeThird.value = true }
        composeRule.runOnIdle {
            assertEquals(1, scope.params.size)
            assertEquals(listOf("A", "B", "C"), (scope.params.single() as ObjectParamDescriptor).options.map { it.value })
            scope.paramsCallbacks.onObjectParamChange("choice", ParamOption<Any>("C", "C"))
        }
        composeRule.onNodeWithText("C").assertExists()
    }

    @Test
    fun renamingPreservesValueAndReleasesTheOldName() {
        val scope = SceneScope()
        val name = mutableStateOf("Old")
        composeRule.setContent {
            with(scope) {
                val text: String by param("Initial", name = name.value)
                BasicText("Renamed: $text")
                if (name.value == "New") {
                    val other: String by param("Other", name = "Old")
                    BasicText("Other: $other")
                }
            }
        }
        composeRule.runOnIdle { scope.paramsCallbacks.onStringParamChange("Old", "Edited") }
        composeRule.onNodeWithText("Renamed: Edited").assertExists()
        composeRule.runOnIdle { name.value = "New" }
        composeRule.runOnIdle {
            assertEquals(setOf("New", "Old"), scope.params.map { it.name }.toSet())
            scope.paramsCallbacks.onStringParamChange("Old", "Replacement")
            scope.paramsCallbacks.onStringParamChange("New", "Preserved")
        }
        composeRule.onNodeWithText("Other: Replacement").assertExists()
        composeRule.onNodeWithText("Renamed: Preserved").assertExists()
    }

    @Test
    fun conditionalReplacementCanReuseTheDisposedParametersName() {
        val scope = SceneScope()
        val first = mutableStateOf(true)
        composeRule.setContent {
            with(scope) {
                if (first.value) {
                    val text: String by param("First", name = "Text")
                    BasicText(text)
                } else {
                    val text: String by param("Second", name = "Text")
                    BasicText(text)
                }
            }
        }
        composeRule.runOnIdle { first.value = false }
        composeRule.onNodeWithText("Second").assertExists()
        composeRule.runOnIdle {
            assertEquals(1, scope.params.size)
            scope.paramsCallbacks.onStringParamChange("Text", "Edited second")
        }
        composeRule.onNodeWithText("Edited second").assertExists()
        composeRule.runOnIdle { first.value = true }
        composeRule.onNodeWithText("First").assertExists()
        composeRule.runOnIdle { assertEquals(1, scope.params.size) }
    }

    @Test
    fun oldNameCanBeReusedBeforeTheRenamedDeclaration() {
        val scope = SceneScope()
        val name = mutableStateOf("Old")
        composeRule.setContent {
            with(scope) {
                if (name.value == "New") {
                    val other: String by param("Other", name = "Old")
                    BasicText("Other: $other")
                }
                val text: String by param("Initial", name = name.value)
                BasicText("Renamed: $text")
            }
        }
        composeRule.runOnIdle { scope.paramsCallbacks.onStringParamChange("Old", "Edited") }
        composeRule.onNodeWithText("Renamed: Edited").assertExists()
        composeRule.runOnIdle { name.value = "New" }
        composeRule.runOnIdle {
            assertEquals(setOf("New", "Old"), scope.params.map { it.name }.toSet())
            scope.paramsCallbacks.onStringParamChange("Old", "Replacement")
            scope.paramsCallbacks.onStringParamChange("New", "Preserved")
        }
        composeRule.onNodeWithText("Other: Replacement").assertExists()
        composeRule.onNodeWithText("Renamed: Preserved").assertExists()
    }

    @Test
    fun twoParametersCanExchangeNamesInTheSameCompositionUpdate() {
        val scope = SceneScope()
        val swapped = mutableStateOf(false)
        composeRule.setContent {
            with(scope) {
                val first: String by param("First", name = if (swapped.value) "B" else "A")
                val second: String by param("Second", name = if (swapped.value) "A" else "B")
                BasicText("$first / $second")
            }
        }
        composeRule.runOnIdle { swapped.value = true }
        composeRule.runOnIdle {
            assertEquals(2, scope.params.size)
            scope.paramsCallbacks.onStringParamChange("A", "Edited second")
            scope.paramsCallbacks.onStringParamChange("B", "Edited first")
        }
        composeRule.onNodeWithText("Edited first / Edited second").assertExists()
    }

    @Test
    fun differentScopesMayUseTheSameName() {
        val firstScope = SceneScope()
        val secondScope = SceneScope()
        composeRule.setContent {
            firstScope.NamedText()
            secondScope.NamedText()
        }
        composeRule.runOnIdle {
            firstScope.paramsCallbacks.onStringParamChange("text", "First scope")
            secondScope.paramsCallbacks.onStringParamChange("text", "Second scope")
        }
        composeRule.onNodeWithText("First scope").assertTextEquals("First scope")
        composeRule.onNodeWithText("Second scope").assertTextEquals("Second scope")
    }

    @Composable
    private fun SceneScope.NamedText() {
        val text: String by param("Initial")
        BasicText(text)
    }
}
