package oleginvoke.com.composium

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import kotlin.test.assertEquals

/** Exercises the public delegate and the descriptor consumed by the inspector independently. */
internal class ParamTestDriver<T>(
    private val rule: ComposeContentTestRule,
    val scope: SceneScope = SceneScope(),
) {
    private lateinit var readValue: () -> T
    private lateinit var writeValue: (T) -> Unit

    fun setContent(content: @Composable SceneScope.() -> Unit) {
        rule.setContent { scope.content() }
    }

    @Composable
    fun Observe(value: T, read: () -> T, write: (T) -> Unit) {
        BasicText(value.toString(), Modifier.testTag("param-value"))
        SideEffect {
            readValue = read
            writeValue = write
        }
    }

    /** Deliberately reads in a child composition to characterize skipped declaration recomposition. */
    @Composable
    fun ObserveInChild(read: () -> T, write: (T) -> Unit) {
        Observe(read(), read, write)
    }

    fun value(): T = readValue()

    fun assertValue(value: T) {
        rule.onNodeWithTag("param-value").assertTextEquals(value.toString())
        rule.runOnIdle { assertEquals(value, readValue(), "delegated value") }
    }

    fun assign(value: T) {
        rule.runOnIdle { writeValue(value) }
    }

    fun select(optionName: String, paramName: String = "choice") {
        rule.runOnIdle {
            val descriptor = scope.params.single { it.name == paramName } as ObjectParamDescriptor
            scope.paramsCallbacks.onObjectParamChange(
                paramName,
                descriptor.options.single { it.name == optionName },
            )
        }
    }

    fun activate(isActive: Boolean, paramName: String = "choice") {
        rule.runOnIdle { scope.paramsCallbacks.onParamActivationChange(paramName, isActive) }
    }

    fun assertObject(
        value: T,
        selectedName: String,
        options: List<Pair<Any?, String>>? = null,
        isActive: Boolean? = null,
        selectedValue: Any? = value,
        paramName: String = "choice",
        typeName: String? = null,
    ) {
        assertValue(value)
        rule.runOnIdle {
            val descriptor = scope.params.single { it.name == paramName } as ObjectParamDescriptor
            assertEquals(selectedName, descriptor.selectedOption.name, "inspector selection")
            assertEquals(selectedValue, descriptor.selectedOption.value, "inspector value")
            assertEquals(isActive, descriptor.activation?.isActive, "activation")
            if (options != null) {
                assertEquals(options, descriptor.options.map { it.value to it.name }, "inspector options")
            }
            if (typeName != null) assertEquals(typeName, descriptor.typeName)
        }
    }
}
