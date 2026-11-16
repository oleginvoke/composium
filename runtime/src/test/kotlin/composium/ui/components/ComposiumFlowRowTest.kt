package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComposiumFlowRowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun wrappingAndSpacingMatchComposeInLtr() = compare(LayoutDirection.Ltr)
    @Test fun wrappingAndSpacingMatchComposeInRtl() = compare(LayoutDirection.Rtl)

    @OptIn(ExperimentalLayoutApi::class)
    private fun compare(direction: LayoutDirection) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Column(Modifier.width(160.dp)) {
                    FlowRow(
                        modifier = Modifier.testTag("reference"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(3) { Box(Modifier.size(72.dp, 24.dp).testTag("reference_$it")) }
                    }
                    ComposiumFlowRow(6.dp, 6.dp, Modifier.testTag("compatible")) {
                        repeat(3) { Box(Modifier.size(72.dp, 24.dp).testTag("compatible_$it")) }
                    }
                }
            }
        }
        val reference = rule.onNodeWithTag("reference").fetchSemanticsNode().boundsInRoot
        val compatible = rule.onNodeWithTag("compatible").fetchSemanticsNode().boundsInRoot
        assertEquals(reference.size, compatible.size)
        repeat(3) {
            val expected = rule.onNodeWithTag("reference_$it").fetchSemanticsNode().boundsInRoot
            val actual = rule.onNodeWithTag("compatible_$it").fetchSemanticsNode().boundsInRoot
            assertEquals(expected.topLeft - reference.topLeft, actual.topLeft - compatible.topLeft)
            assertEquals(expected.size, actual.size)
        }
    }
}
