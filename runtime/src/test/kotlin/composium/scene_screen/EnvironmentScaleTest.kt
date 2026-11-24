package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import oleginvoke.com.composium.ui.components.EnvironmentScaleRange
import oleginvoke.com.composium.ui.components.EnvironmentScalePresets
import oleginvoke.com.composium.ui.components.scaleFraction
import androidx.compose.ui.test.junit4.createComposeRule
import oleginvoke.com.composium.SceneSystemSettings
import oleginvoke.com.composium.ui.theme.ComposiumTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnvironmentScaleTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun parserAcceptsCustomValuesAndDecimalComma() {
        assertEquals(1.1375f, parseScaleValue("1.1375"))
        assertEquals(1.1375f, parseScaleValue(" 1,1375 "))
        assertEquals(0.05f, parseScaleValue("0.05"))
        assertEquals(12f, parseScaleValue("12"))
        listOf("", ".", "0", "-1", "NaN", "Infinity", "1e99", "1,2.3").forEach {
            assertNull(it, parseScaleValue(it))
        }
        assertEquals("1.1375", formatScaleValue(1.1375f))
    }

    @Test fun environmentUsesActualSystemValuesWithoutSnapping() {
        val preview = SceneSystemSettings()
        val scales = buildEnvironmentItems(preview, false, {}, listOf(1f), listOf(1f), 1.1375f, 1.3f)
            .filterIsInstance<EnvironmentItem.Scale>()
        assertEquals(1.3f, scales[0].systemValue)
        assertEquals(1.1375f, scales[1].systemValue)
        scales[0].onValueSelected(1.1375f)
        assertEquals(1.1375f, preview.fontScaleOverride)
        scales[0].onUseSystem()
        assertNull(preview.fontScaleOverride)
    }

    @Test fun exactEntryPresetsAndSystemResetStaySynchronized() {
        var selected by mutableStateOf<Float?>(null)
        var system by mutableStateOf(1.1375f)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", listOf(1f, 1.3f),
                    selected, system, { selected = null }, { selected = it }))
            }
        }
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        val input = composeRule.onNode(hasSetTextAction())
        input.assertTextEquals("1.1375")
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(6)))
        composeRule.onNodeWithTag("font_scale_reset").assertDoesNotExist()
        input.performTextReplacement("1,2345")
        composeRule.runOnIdle { assertNull(selected) }
        composeRule.onNodeWithTag("font_scale_apply").performClick()
        composeRule.runOnIdle { assertEquals(1.2345f, selected) }
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        input.assertTextEquals("1.2345")
        input.performTextReplacement("0")
        composeRule.onNodeWithTag("font_scale_apply").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(1.2345f, selected) }
        composeRule.onNodeWithTag("font_scale_preset_1.3").performClick()
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        input.assertTextEquals("1.3")
        composeRule.onNodeWithTag("font_scale_preset_1.3").assertIsSelected()
        input.performTextReplacement(".")
        composeRule.onNodeWithTag("font_scale_reset").assertDoesNotExist()
        composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        composeRule.onNodeWithTag("font_scale_reset").performClick()
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        input.assertTextEquals("1.1375")
        composeRule.runOnIdle { assertNull(selected); system = 1.1875f }
        input.assertTextEquals("1.1875")
        input.performTextReplacement("1.222")
        input.performImeAction()
        composeRule.runOnIdle { assertEquals(1.222f, selected) }
        composeRule.onNodeWithTag("font_scale_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1.1375f) }
        composeRule.runOnIdle { assertEquals(1.1375f, selected) }
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        input.assertTextEquals("1.1375")
    }
    @Test fun presetsMatchTrackPositionsAndOutOfRangeInputDoesNotChangeTrack() {
        var selected by mutableStateOf<Float?>(null)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1.1375f, { selected = null }, { selected = it }))
                }
            }
        }
        val card = composeRule.onNodeWithTag("font_scale_card").fetchSemanticsNode().boundsInRoot
        val header = composeRule.onNodeWithTag("font_scale_header").fetchSemanticsNode().boundsInRoot
        val presetArea = composeRule.onNodeWithTag("font_scale_presets").fetchSemanticsNode().boundsInRoot
        val slider = composeRule.onNodeWithTag("font_scale_slider")
        slider.assertHeightIsEqualTo(28.dp)
        val track = slider.fetchSemanticsNode().boundsInRoot
        val fourDp = track.height / 28f * 4f
        assertEquals(header.bottom, track.top, 1f)
        assertEquals(track.bottom - fourDp, presetArea.top, 1f)
        assertEquals(header.height + track.height + presetArea.height - fourDp, card.height, 1f)
        composeRule.onNodeWithTag("font_scale_reset").assertDoesNotExist()
        val labels = EnvironmentScalePresets.map {
            composeRule.onNodeWithTag("font_scale_preset_$it").fetchSemanticsNode().boundsInRoot
        }
        val inset = labels.first().width / 2f
        labels.forEachIndexed { index, bounds ->
            val expected = track.left + inset + (track.width - 2 * inset) * scaleFraction(EnvironmentScalePresets[index], EnvironmentScaleRange)
            assertEquals(expected, bounds.center.x, 1f)
        }
        val dp = track.height / 28f
        // Include the complete 13 dp halo, not only the opaque 9 dp thumb.
        assertEquals(header.left, track.left, 0.5f)
        assertEquals(header.right, track.right, 0.5f)
        assertEquals(13f, oleginvoke.com.composium.ui.components.ScaleTrackInset.value, 0f)
        val visualLabels = EnvironmentScalePresets.map {
            composeRule.onNodeWithTag("font_scale_preset_${it}_label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        }
        val editTarget = composeRule.onNodeWithTag("font_scale_edit").fetchSemanticsNode().boundsInRoot
        val editFeedback = composeRule.onNodeWithTag("font_scale_edit_feedback", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(editFeedback.height, editTarget.height, 0.5f)
        assertEquals(editFeedback.width, editTarget.width, 0.5f)
        assertEquals(header.bottom, editTarget.bottom, 0.5f)
        visualLabels.forEachIndexed { index, bounds ->
            val expected = track.left + 13 * dp + (track.width - 26 * dp) * scaleFraction(EnvironmentScalePresets[index], EnvironmentScaleRange)
            assertEquals(expected, bounds.center.x, 1.5f)
        }
        listOf("3.75", "0.125", "1000").forEach { exact ->
            composeRule.onNodeWithTag("font_scale_edit").performClick()
            val input = composeRule.onNode(hasSetTextAction())
            input.performTextReplacement(exact)
            input.performImeAction()
            composeRule.runOnIdle { assertEquals(exact.toFloat(), selected) }
            slider.assertRangeInfoEquals(ProgressBarRangeInfo(exact.toFloat().coerceIn(EnvironmentScaleRange), EnvironmentScaleRange))
            // Multiple movement events must survive recomposition, even after out-of-range input.
            slider.performTouchInput { swipeLeft(durationMillis = 500) }
            composeRule.runOnIdle { assertTrue(selected!! in 0.5f..0.8f) }
            slider.performTouchInput { swipeRight(durationMillis = 500) }
            composeRule.runOnIdle { assertTrue(selected!! in 1.7f..2f) }
        }
        composeRule.onNodeWithTag("font_scale_preset_1.5").performClick()
        composeRule.runOnIdle { assertEquals(1.5f, selected) }
    }

    @Test fun trackFractionIsStableForExtremeAndOffScaleValues() {
        assertEquals(0f, scaleFraction(Float.MIN_VALUE, EnvironmentScaleRange))
        assertEquals(1f, scaleFraction(Float.MAX_VALUE, EnvironmentScaleRange))
        assertEquals(0.5f, scaleFraction(1.25f, EnvironmentScaleRange))
        assertEquals(1f / 3f, scaleFraction(1f, EnvironmentScaleRange))
    }

    @Test fun intermediatePresetsRemainAlignedAndDoNotOverlapOnNarrowScreens() {
        var selected by mutableStateOf<Float?>(null)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(240.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1f, { selected = null }, { selected = it }))
                }
            }
        }
        assertEquals(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f), EnvironmentScalePresets)
        val track = composeRule.onNodeWithTag("font_scale_slider").fetchSemanticsNode().boundsInRoot
        val labels = EnvironmentScalePresets.map {
            composeRule.onNodeWithTag("font_scale_preset_$it").fetchSemanticsNode().boundsInRoot
        }
        val inset = labels.first().width / 2f
        labels.forEachIndexed { index, bounds ->
            val expected = track.left + inset + (track.width - 2 * inset) * scaleFraction(EnvironmentScalePresets[index], EnvironmentScaleRange)
            assertEquals(expected, bounds.center.x, 1f)
            labels.drop(index + 1).forEach { other -> assertFalse(bounds.overlaps(other)) }
        }
        val dp = track.height / 28f
        EnvironmentScalePresets.forEach { value ->
            val label = composeRule.onNodeWithTag("font_scale_preset_${value}_label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val expected = track.left + 13 * dp + (track.width - 26 * dp) * scaleFraction(value, EnvironmentScaleRange)
            assertEquals(expected, label.center.x, 1.5f)
        }
        assertTrue(labels[1].top > labels[0].top)
        composeRule.onNodeWithTag("font_scale_preset_0.75").performClick()
        composeRule.runOnIdle { assertEquals(0.75f, selected) }
        composeRule.onNodeWithTag("font_scale_preset_1.25").performClick()
        composeRule.runOnIdle { assertEquals(1.25f, selected) }
    }

    @Test fun everyPresetSelectsOnReleaseAndCancelledPressDoesNotSelect() {
        var selected by mutableStateOf<Float?>(null)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                    selected, 1f, { selected = null }, { selected = it }))
            }
        }
        val first = composeRule.onNodeWithTag("font_scale_preset_0.75")
        first.performTouchInput { down(center) }
        composeRule.runOnIdle { assertNull(selected) }
        first.performTouchInput { cancel() }
        composeRule.runOnIdle { assertNull(selected) }
        EnvironmentScalePresets.forEach { value ->
            val preset = composeRule.onNodeWithTag("font_scale_preset_$value")
            preset.performClick()
            preset.assertIsSelected()
            composeRule.runOnIdle { assertEquals(value, selected) }
        }
    }

    @Test fun effectiveValueDrivesSelectionAndConditionalReset() {
        var selected by mutableStateOf<Float?>(null)
        var system by mutableStateOf(1f)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(240.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, system, { selected = null }, { selected = it }))
                }
            }
        }
        composeRule.onNodeWithTag("font_scale_preset_1.0").assertIsSelected()
        composeRule.onNodeWithTag("font_scale_reset").assertDoesNotExist()
        composeRule.runOnIdle { system = 1.0001f }
        EnvironmentScalePresets.forEach { composeRule.onNodeWithTag("font_scale_preset_$it").assertIsNotSelected() }
        composeRule.onNodeWithTag("font_scale_preset_1.5").performClick()
        val reset = composeRule.onNodeWithTag("font_scale_reset")
        reset.assertExists()
        val edit = composeRule.onNodeWithTag("font_scale_edit").fetchSemanticsNode().boundsInRoot
        val resetBounds = reset.fetchSemanticsNode().boundsInRoot
        assertTrue(resetBounds.right <= edit.left)
        assertEquals(edit.center.y, resetBounds.center.y, 1f)
        reset.performClick()
        composeRule.runOnIdle { assertNull(selected) }
        reset.assertDoesNotExist()
        composeRule.runOnIdle { selected = system }
        reset.assertDoesNotExist()
    }

    @Test fun emptySpaceCancelsDraftAndDoesNotSwallowChildClicks() {
        var selected by mutableStateOf<Float?>(1.5f)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1f, { selected = null }, { selected = it }))
                }
            }
        }
        composeRule.onNodeWithTag("font_scale_edit").performClick()
        composeRule.onNodeWithTag("font_scale_reset").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("1.2345")
        composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1.5f, selected) }
        composeRule.onNodeWithTag("font_scale_reset").performTouchInput { click() }
        composeRule.onNodeWithTag("font_scale_preset_1.0").assertIsSelected()
        composeRule.runOnIdle { assertNull(selected) }
        composeRule.onNodeWithTag("font_scale_edit").performTouchInput { click() }
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        composeRule.onNodeWithTag("font_scale_preset_1.75").performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1.75f, selected) }
    }

    @Test fun openingEditorKeepsCardAndSliderGeometryStable() {
        var selected by mutableStateOf<Float?>(null)
        var width by mutableStateOf(360.dp)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(width).padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Display size", EnvironmentScalePresets,
                        selected, 1f, { selected = null }, { selected = it }))
                }
            }
        }
        for (w in listOf(360.dp, 240.dp)) {
            for (value in listOf(null, 1.5f, 3.75f)) {
                composeRule.runOnIdle { width = w; selected = value }
                fun bounds(tag: String) = composeRule.onNodeWithTag("font_scale_$tag").fetchSemanticsNode().boundsInRoot
                val before = listOf("card", "header", "slider", "presets").associateWith { bounds(it) }
                composeRule.onNodeWithTag("font_scale_edit").performClick()
                before.forEach { (tag, rect) -> assertEquals("$w $value $tag", rect, bounds(tag)) }
                composeRule.onNode(hasSetTextAction()).performTextReplacement(".")
                before.forEach { (tag, rect) -> assertEquals("invalid $w $value $tag", rect, bounds(tag)) }
                composeRule.onNodeWithTag("font_scale_apply").assertIsNotEnabled()
                composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
            }
        }
    }

    @Test fun resetPositionIsIndependentOfValueLength() {
        var selected by mutableStateOf<Float?>(0.5f)
        var width by mutableStateOf(360.dp)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(width).padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1f, { selected = null }, { selected = it }))
                }
            }
        }
        for (w in listOf(360.dp, 240.dp)) {
            composeRule.runOnIdle { width = w; selected = 0.5f }
            val expected = composeRule.onNodeWithTag("font_scale_reset").fetchSemanticsNode().boundsInRoot
            for (value in listOf(2f, 1.25f, 1.1375f, 1000f, Float.MAX_VALUE)) {
                composeRule.runOnIdle { selected = value }
                if (value <= 2f) assertEquals(expected, composeRule.onNodeWithTag("font_scale_reset").fetchSemanticsNode().boundsInRoot)
                composeRule.onNodeWithTag("font_scale_edit").performClick()
                composeRule.onNode(hasSetTextAction()).assertTextEquals(formatScaleValue(value))
                composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
            }
        }
    }

    @Test fun joinedResetAndEditHaveSeparateReliableHitAreas() {
        var selected by mutableStateOf<Float?>(1.25f)
        var width by mutableStateOf(360.dp)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(width).padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1f, { selected = null }, { selected = it }))
                }
            }
        }
        for (w in listOf(360.dp, 240.dp)) {
            composeRule.runOnIdle { width = w; selected = 1.25f }
            val reset = composeRule.onNodeWithTag("font_scale_reset")
            val edit = composeRule.onNodeWithTag("font_scale_edit")
            val rb = reset.fetchSemanticsNode().boundsInRoot
            val eb = edit.fetchSemanticsNode().boundsInRoot
            assertEquals(rb.right, eb.left, 1f)
            reset.performTouchInput { click(androidx.compose.ui.geometry.Offset(right - 1f, centerY)) }
            composeRule.runOnIdle { assertNull(selected); selected = 1.25f }
            edit.performTouchInput { click(androidx.compose.ui.geometry.Offset(1f, centerY)) }
            composeRule.onNode(hasSetTextAction()).assertIsFocused()
            composeRule.runOnIdle { assertEquals(1.25f, selected) }
            reset.assertDoesNotExist()
            composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
        }
    }

    @Test fun displayRoundingDoesNotChangeExactValueOrIconGeometry() {
        assertEquals("1.13\u2026", formatScaleDisplayValue(1.1375f))
        assertEquals("1.00\u2026", formatScaleDisplayValue(1.00001f))
        assertEquals("1.20", formatScaleDisplayValue(1.2f))
        assertEquals("1.00", formatScaleDisplayValue(1f))
        assertEquals("2.00", formatScaleDisplayValue(2f))
        assertEquals("1.00e-4", formatScaleDisplayValue(0.0001f))
        assertEquals("1.25", formatScaleDisplayValue(1.25f))
        assertEquals("1.99\u2026", formatScaleDisplayValue(1.9999f))
        assertEquals("3.40e38\u2026", formatScaleDisplayValue(Float.MAX_VALUE))
        assertEquals("1.40e-45", formatScaleDisplayValue(Float.MIN_VALUE))
        var selected by mutableStateOf<Float?>(1.25f)
        composeRule.setContent {
            ComposiumTheme(darkTheme = false) {
                Box(Modifier.width(240.dp).padding(14.dp)) {
                    ScaleSettingCard(EnvironmentItem.Scale("font_scale", "Font size", EnvironmentScalePresets,
                        selected, 1.5f, { selected = null }, { selected = it }))
                }
            }
        }
        fun bounds(tag: String) = composeRule.onNodeWithTag("font_scale_$tag", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reset = bounds("reset")
        val edit = bounds("edit")
        composeRule.onNodeWithTag("font_scale_edit_icon", useUnmergedTree = true).assertDoesNotExist()
        val resetIcon = bounds("reset_icon")
        assertEquals(edit.center.y, resetIcon.center.y, 0.5f)
        var expectedFont: androidx.compose.ui.unit.TextUnit? = null
        val valueBounds = bounds("value")
        for (value in listOf(1f, 2f, 1.2f, 1.25f, 1.1375f, 1.9999f, 99.999f, 99999f, Float.MAX_VALUE, Float.MIN_VALUE)) {
            composeRule.runOnIdle { selected = value }
            if (value in 0.5f..2f) assertEquals(reset, bounds("reset"))
            assertEquals(edit.right, bounds("edit").right, 0.5f)
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            composeRule.onNodeWithTag("font_scale_value", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals("\u00d7${formatScaleDisplayValue(value)}", layout.layoutInput.text.text)
            if (expectedFont == null) expectedFont = layout.layoutInput.style.fontSize
            assertEquals(expectedFont, layout.layoutInput.style.fontSize)
            assertEquals(valueBounds.height, bounds("value").height, 0.5f)
            assertTrue(bounds("value").left <= edit.right)
            assertTrue(bounds("value").left + layout.getLineRight(0) <= edit.right)
            if (value in 0.5f..2f) assertEquals(resetIcon, bounds("reset_icon"))
            assertEquals(androidx.compose.ui.text.style.TextAlign.Start, layout.layoutInput.style.textAlign)
            if (value in 0.5f..2f) assertEquals(valueBounds.left, bounds("value").left, 0.5f)
            assertEquals(0f, layout.getLineLeft(0), 0.5f)
            assertFalse("Horizontal overflow for $value", layout.didOverflowWidth)
            assertFalse("Ellipsis for $value", layout.isLineEllipsized(0))
            assertEquals("Missing digits for $value", layout.layoutInput.text.length, layout.getLineEnd(0))
            composeRule.onNodeWithTag("font_scale_edit").performClick()
            composeRule.onNode(hasSetTextAction()).assertTextEquals(formatScaleValue(value))
            composeRule.onNodeWithTag("font_scale_card").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 2f)) }
            composeRule.runOnIdle { assertEquals(value, selected) }
        }
    }

}
