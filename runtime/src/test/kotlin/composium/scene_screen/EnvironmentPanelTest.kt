package oleginvoke.com.composium.scene_screen

import oleginvoke.com.composium.SceneSystemSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvironmentPanelTest {

    @Test
    fun `buildEnvironmentItems reflects latest preview state after mutations`() {
        val preview = SceneSystemSettings()
        var darkThemeEnabled = false

        val initialItems = buildEnvironmentItems(
            preview = preview,
            isDarkThemeEnabled = darkThemeEnabled,
            onThemeChange = { darkThemeEnabled = it },
            fontScales = listOf(0.85f, 1.00f, 1.15f),
            displayScales = listOf(0.85f, 1.00f, 1.08f),
            systemDisplayScale = 1f,
        )

        val initialRtl = initialItems[1] as EnvironmentItem.Toggle
        val initialFont = initialItems[2] as EnvironmentItem.Scale
        val initialDisplay = initialItems[3] as EnvironmentItem.Scale

        assertFalse(initialRtl.checked)
        assertNull(initialFont.selectedValue)
        assertNull(initialDisplay.selectedValue)

        preview.rtl = true
        preview.fontScaleOverride = 1.15f
        preview.displayScaleOverride = 1.08f

        val updatedItems = buildEnvironmentItems(
            preview = preview,
            isDarkThemeEnabled = darkThemeEnabled,
            onThemeChange = { darkThemeEnabled = it },
            fontScales = listOf(0.85f, 1.00f, 1.15f),
            displayScales = listOf(0.85f, 1.00f, 1.08f),
            systemDisplayScale = 1f,
        )

        val updatedRtl = updatedItems[1] as EnvironmentItem.Toggle
        val updatedFont = updatedItems[2] as EnvironmentItem.Scale
        val updatedDisplay = updatedItems[3] as EnvironmentItem.Scale

        assertTrue(updatedRtl.checked)
        assertEquals(1.15f, updatedFont.selectedValue)
        assertEquals(1.08f, updatedDisplay.selectedValue)
    }

    @Test
    fun `buildEnvironmentItems callbacks mutate preview and theme state`() {
        val preview = SceneSystemSettings()
        var darkThemeEnabled = false

        val items = buildEnvironmentItems(
            preview = preview,
            isDarkThemeEnabled = darkThemeEnabled,
            onThemeChange = { darkThemeEnabled = it },
            fontScales = listOf(0.85f, 1.00f, 1.15f),
            displayScales = listOf(0.85f, 1.00f, 1.08f),
            systemDisplayScale = 1f,
        )

        (items[0] as EnvironmentItem.Toggle).onCheckedChange(true)
        (items[1] as EnvironmentItem.Toggle).onCheckedChange(true)
        (items[2] as EnvironmentItem.Scale).onValueSelected(1.15f)
        (items[3] as EnvironmentItem.Scale).onUseSystem()

        assertTrue(darkThemeEnabled)
        assertTrue(preview.rtl)
        assertEquals(1.15f, preview.fontScaleOverride)
        assertNull(preview.displayScaleOverride)
    }
}
