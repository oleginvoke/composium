package composium.compatibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompatibilitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun thumbnailRunsOnAPrivateDisplay() {
        val catalogDisplay = rule.activity.window.decorView.display.displayId
        rule.waitUntil(timeoutMillis = 10_000) {
            sceneDisplayId?.let { it != catalogDisplay } == true
        }
    }

    @Test fun generatedRegistryAndSceneControlsWork() {
        if (BuildConfig.USE_KSP) {
            val registry = Class.forName("oleginvoke.com.composium.generated.GeneratedSceneRegistry")
            val scenes = registry.getDeclaredMethod("collectAll").invoke(registry.getDeclaredField("INSTANCE").get(null)) as List<*>
            assertTrue(scenes.isNotEmpty())
        }
        rule.onNodeWithText("Compatibility scene").performClick()
        rule.onNodeWithText("Scene rendered").assertExists()
        rule.onNodeWithContentDescription("Open properties").performClick()
        // Inferred sealed options exercise the runtime's kotlin-reflect baseline.
        rule.onNodeWithText("Second").performClick()
        rule.onNodeWithText("Environment").performClick()
        rule.onNodeWithTag("font_scale_edit").performClick()
        rule.onNode(hasSetTextAction() and isFocused()).performTextReplacement("1.1375")
        rule.onNodeWithTag("font_scale_apply").performClick()
        rule.onNodeWithTag("font_scale_value", useUnmergedTree = true)
            .assertContentDescriptionEquals("Font size: 1.1375 times")
        rule.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasTestTag("font_scale_card")))
            .performScrollToNode(hasTestTag("display_scale_edit"))
        rule.onNodeWithTag("display_scale_edit").performClick()
        rule.onNode(hasSetTextAction() and isFocused()).performTextReplacement("1.4e-45")
        rule.onNodeWithTag("display_scale_apply").performClick()
        rule.onNodeWithTag("display_scale_reset").performClick()
        rule.onNodeWithContentDescription("Close properties").performClick()
        rule.onNodeWithText("Scene rendered").assertExists()
    }
}
