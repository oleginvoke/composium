package composium.compatibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric 4.14 retains Android 21 support, which newer Robolectric releases dropped. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 23])
class LegacyAndroidTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun catalogSceneReflectionAndEnvironmentWorkOnOldAndroid() {
        rule.onNodeWithText("Compatibility scene").performClick()
        rule.onNodeWithText("Scene rendered").assertExists()
        rule.onNodeWithContentDescription("Open properties").performClick()
        rule.onNodeWithText("Second").performClick()
        rule.onNodeWithText("Environment").performClick()
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
