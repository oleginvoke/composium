package oleginvoke.com.composium.main_screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.Scene
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneKey
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainScreenThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lastVisibleSceneIsReportedByKeyWithoutCountingTheMetadataRow() {
        val scenes = listOf("First", "Middle", "Last").map {
            SceneEntry(Scene(null, it, thumbnail = {}, content = {}))
        }
        var visibleIds = emptyList<SceneKey>()
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.requiredSize(320.dp, 140.dp)) {
                    MainScreen(
                        scenes = scenes,
                        onSceneSelected = {},
                        contentWindowInsets = WindowInsets(0),
                        onVisibleSceneIdsChanged = { visibleIds = it },
                    )
                }
            }
        }
        // One metadata row precedes the three cards. The viewport fits only the last card.
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(3).performTouchInput { swipeUp() }
        val viewportHeight = composeRule.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot.height
        assertTrue(viewportHeight <= with(composeRule.density) { 140.dp.toPx() })
        composeRule.runOnIdle { assertEquals(listOf(scenes.last().id), visibleIds) }
    }

    @Test
    fun disabledThumbnailRemovesPreviewSpaceButKeepsBadgeAndNavigation() {
        val automatic = SceneEntry(Scene(null, "Automatic", content = {}))
        val disabled = SceneEntry(Scene(
            group = null,
            name = "Disabled",
            thumbnail = null,
            badge = { BasicText("Badge") },
            content = {},
        ))
        var selected: SceneKey? = null
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                MainScreen(
                    scenes = listOf(disabled, automatic),
                    onSceneSelected = { selected = it },
                    contentWindowInsets = WindowInsets(0),
                )
            }
        }

        val disabledCard = composeRule.onNode(hasText("Disabled") and hasClickAction())
        val automaticCard = composeRule.onNode(hasText("Automatic") and hasClickAction())
        val disabledHeight = disabledCard.fetchSemanticsNode().boundsInRoot.height
        val automaticHeight = automaticCard.fetchSemanticsNode().boundsInRoot.height
        assertTrue(disabledHeight < automaticHeight, "Disabled preview must not reserve space")
        val titleTop = composeRule.onNodeWithText("Disabled", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.top
        val cardTop = disabledCard.fetchSemanticsNode().boundsInRoot.top
        assertTrue(
            titleTop - cardTop < with(composeRule.density) { 24.dp.toPx() },
            "Title must be at the top of the card, without a preview area above it",
        )
        composeRule.onNodeWithText("Badge", useUnmergedTree = true).assertIsDisplayed()
        val titleBounds = composeRule.onNodeWithText("Disabled", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val badgeBounds = composeRule.onNodeWithText("Badge", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(badgeBounds.left >= titleBounds.right, "Badge must be beside the title")
        assertEquals(titleBounds.center.y, badgeBounds.center.y, absoluteTolerance = 1f)
        disabledCard.performClick()
        composeRule.runOnIdle { assertEquals(disabled.id, selected) }
    }
}
