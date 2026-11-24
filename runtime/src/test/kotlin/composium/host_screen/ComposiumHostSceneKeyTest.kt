package oleginvoke.com.composium.host_screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ComposiumRuntime
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposiumHostSceneKeyTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openSceneAndItsSaveableContentSurviveActivityStateRestoration() {
        ComposiumRuntime.register(counterScene("Restoration::group", "Still open"))
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            ComposiumTheme(darkTheme = false) {
                ComposiumHostScreen(contentWindowInsets = WindowInsets(0))
            }
        }
        clickCatalogItem("Restoration::group")
        clickCatalogItem("Still open")
        composeRule.onNodeWithTag("counter").performClick().performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("counter").assertTextEquals("2")
        closeScene()
        clickCatalogItem("Still open")
        composeRule.onNodeWithTag("counter").assertTextEquals("2")
    }

    @Test
    fun explicitlyReopeningSceneResetsToolsToTheirDefaults() {
        lateinit var scope: SceneScope
        ComposiumRuntime.register(Scene("Revisit tools", "Revisit counter", thumbnail = null) {
            scope = this
            Box(Modifier.size(48.dp))
        })
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) { ComposiumHostScreen() }
        }
        clickCatalogItem("Revisit tools")
        clickCatalogItem("Revisit counter")
        composeRule.onNodeWithContentDescription("Open properties").performClick()
        composeRule.onNodeWithText("Environment").performClick()
        composeRule.runOnIdle {
            scope.preview.rtl = true
            scope.host.closeScene()
        }
        composeRule.waitForIdle()
        clickCatalogItem("Revisit counter")
        composeRule.onNodeWithContentDescription("Open properties").assertExists()
        composeRule.runOnIdle { org.junit.Assert.assertFalse(scope.preview.rtl) }
    }

    @Test
    fun separatorScenesNavigateAndRestoreTheirOwnState() {
        ComposiumRuntime.registerAll(
            counterScene("Host key scenes::Primary", "Key counter"),
            counterScene("Host key scenes", "Primary::Key counter"),
        )
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            ComposiumTheme(darkTheme = false) {
                ComposiumHostScreen(contentWindowInsets = WindowInsets(0))
            }
        }

        clickCatalogItem("Host key scenes::Primary")
        clickCatalogItem("Key counter")
        composeRule.onNodeWithTag("counter").assertTextEquals("0").performClick()
        closeScene()

        clickCatalogItem("Host key scenes")
        clickCatalogItem("Primary::Key counter")
        composeRule.onNodeWithTag("counter").assertTextEquals("0").performClick().performClick()
        closeScene()

        restoration.emulateSavedInstanceStateRestore()

        clickCatalogItem("Key counter")
        composeRule.onNodeWithTag("counter").assertTextEquals("1")
        closeScene()
        clickCatalogItem("Primary::Key counter")
        composeRule.onNodeWithTag("counter").assertTextEquals("2")
    }

    private fun clickCatalogItem(name: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(name))
        composeRule.onNodeWithText(name).performClick()
        composeRule.waitForIdle()
    }

    private fun closeScene() {
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
    }

    private fun counterScene(group: String, name: String) = Scene(
        group = group,
        name = name,
        thumbnail = {},
    ) { contentPadding ->
        var count by rememberSaveable { mutableIntStateOf(0) }
        Box(Modifier.padding(contentPadding)) {
            Box(Modifier.size(48.dp).testTag("counter").clickable { count++ }) {
                BasicText(count.toString())
            }
        }
    }
}
