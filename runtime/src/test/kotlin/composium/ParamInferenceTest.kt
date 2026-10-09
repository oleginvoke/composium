package oleginvoke.com.composium

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParamInferenceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inferredEnumsUseDeclarationOrderAndEnumConstantNames() {
        val driver = ParamTestDriver<TestTone>(composeRule)
        driver.setContent {
            var choice: TestTone by param(TestTone.Alpha)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestTone.Alpha, "Alpha", typeName = "Enum",
            options = listOf(TestTone.Zulu to "Zulu", TestTone.Alpha to "Alpha"))
        driver.select("Zulu")
        driver.assertObject(TestTone.Zulu, "Zulu")
    }

    @Test
    fun namedEnumDefaultWithoutExplicitOptionsCurrentlyUsesTheEnumConstantName() {
        val driver = ParamTestDriver<TestTone>(composeRule)
        driver.setContent {
            var choice: TestTone by param(TestTone.Alpha named "Default alias")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestTone.Alpha, "Alpha", typeName = "Enum")
    }

    @Test
    fun explicitEnumOptionsUseTheirLabelsAndOrderInsteadOfInference() {
        val driver = ParamTestDriver<TestTone>(composeRule)
        driver.setContent {
            var choice: TestTone by param(listOf(TestTone.Alpha named "First alias", TestTone.Zulu named "Second alias"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestTone.Alpha, "First alias", typeName = "Enum",
            options = listOf(TestTone.Alpha to "First alias", TestTone.Zulu to "Second alias"))
        driver.select("Second alias")
        driver.assertObject(TestTone.Zulu, "Second alias", typeName = "Enum")
    }

    @Test
    fun enumDefaultMissingFromExplicitOptionsCurrentlyUsesTheClassNameForItsInsertedLabel() {
        val driver = ParamTestDriver<TestTone>(composeRule)
        driver.setContent {
            var choice: TestTone by param(default = TestTone.Alpha, options = listOf(TestTone.Zulu named "Zed"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestTone.Alpha, "TestTone", typeName = "Enum",
            options = listOf(TestTone.Alpha to "TestTone", TestTone.Zulu to "Zed"))
    }

    @Test
    fun nullableEnumWithNullDefaultStartsInactiveAndRestoresTheFirstConstant() {
        val driver = ParamTestDriver<TestTone?>(composeRule)
        driver.setContent {
            var choice: TestTone? by param(null)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Zulu", selectedValue = TestTone.Zulu, isActive = false, typeName = "Enum?")
        driver.activate(true)
        driver.assertObject(TestTone.Zulu, "Zulu", isActive = true)
    }

    @Test
    fun inferredSealedObjectsUseNaturalNameOrdering() {
        val driver = ParamTestDriver<TestSize>(composeRule)
        driver.setContent {
            var choice: TestSize by param(TestSize.Item10)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestSize.Item10, "Item10", options = listOf(
            TestSize.Item2 to "Item2", TestSize.Item10 to "Item10",
        ))
        driver.select("Item2")
        driver.assertObject(TestSize.Item2, "Item2")
    }

    @Test
    fun sealedOptionsCanBeReorderedWithoutChangingTheDefaultSelection() {
        val driver = ParamTestDriver<TestSize>(composeRule)
        driver.setContent {
            var choice: TestSize by param(TestSize.Item2) { it.reversed() }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestSize.Item2, "Item2", options = listOf(
            TestSize.Item10 to "Item10", TestSize.Item2 to "Item2",
        ))
    }

    @Test
    fun namedSealedDefaultUsesItsLabelInTheInferredOptions() {
        val driver = ParamTestDriver<TestSize>(composeRule)
        driver.setContent {
            var choice: TestSize by param(TestSize.Item10 named "Preferred")
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestSize.Item10, "Preferred", options = listOf(
            TestSize.Item2 to "Item2", TestSize.Item10 to "Preferred",
        ))
    }

    @Test
    fun aSealedDefaultRemovedByTheOrderTransformCurrentlyRemainsTheDelegatedValue() {
        val driver = ParamTestDriver<TestSize>(composeRule)
        driver.setContent {
            var choice: TestSize by param(TestSize.Item10) { listOf(null, TestSize.Item2) }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestSize.Item10, "Item10", options = listOf(TestSize.Item2 to "Item2"))
    }

    @Test
    fun explicitSealedOptionsSuppressAutomaticDiscovery() {
        val driver = ParamTestDriver<TestSize>(composeRule)
        driver.setContent {
            var choice: TestSize by param(listOf(TestSize.Item10 named "Only choice"))
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestSize.Item10, "Only choice", options = listOf(TestSize.Item10 to "Only choice"))
    }

    @Test
    fun nullableSealedNullDefaultRestoresTheFirstNaturallyOrderedObject() {
        val driver = ParamTestDriver<TestSize?>(composeRule)
        driver.setContent {
            var choice: TestSize? by param(null)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(null, "Item2", selectedValue = TestSize.Item2, isActive = false)
        driver.activate(true)
        driver.assertObject(TestSize.Item2, "Item2", isActive = true)
    }

    @Test
    fun aDeclaredNestedSealedRootExcludesObjectsFromOtherBranches() {
        val driver = ParamTestDriver<TestFamily.Sizes>(composeRule)
        driver.setContent {
            var choice: TestFamily.Sizes by param(TestFamily.Sizes.Large)
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestFamily.Sizes.Large, "Large", options = listOf(
            TestFamily.Sizes.Large to "Large", TestFamily.Sizes.Small to "Small",
        ))
    }

    @Test
    fun enumOrderTransformCurrentlyDoesNotChangeDeclarationOrder() {
        val driver = ParamTestDriver<TestTone>(composeRule)
        driver.setContent {
            var choice: TestTone by param(TestTone.Alpha) { it.reversed() }
            driver.Observe(choice, { choice }, { choice = it })
        }
        driver.assertObject(TestTone.Alpha, "Alpha", options = listOf(TestTone.Zulu to "Zulu", TestTone.Alpha to "Alpha"))
    }

    enum class TestTone { Zulu, Alpha }

    sealed interface TestSize {
        data object Item10 : TestSize
        data object Item2 : TestSize
    }

    sealed interface TestFamily {
        sealed interface Sizes : TestFamily {
            data object Small : Sizes
            data object Large : Sizes
        }
        data object Other : TestFamily
    }
}
