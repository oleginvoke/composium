package oleginvoke.com.composium.scene_screen

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneSystemSettings
import oleginvoke.com.composium.ui.components.ComposiumDiscreteSlider
import oleginvoke.com.composium.ui.components.ComposiumHorizontalDivider
import oleginvoke.com.composium.ui.components.ComposiumModalBottomSheet
import oleginvoke.com.composium.ui.components.ComposiumSwitch
import oleginvoke.com.composium.ui.components.ComposiumTab
import oleginvoke.com.composium.ui.components.ComposiumTabRow
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.theme.Tokens
import kotlin.math.abs

@Composable
internal fun ControlsSheet(
    uiState: ControlsSheetUiState,
    paramsState: SceneParamsState,
    paramsCallbacks: SceneParamsCallbacks,
    preview: SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    onDismiss: () -> Unit,
    onTabSelected: (ControlsSheetTab) -> Unit,
    onThemeChange: (isDarkTheme: Boolean) -> Unit,
    sheetWindowInsets: WindowInsets? = null,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).toInt().dp

    ComposiumModalBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        containerColor = Tokens.colors.surface,
        sheetWindowInsets = sheetWindowInsets,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .wrapContentHeight(),
        ) {
            ComposiumTabRow(
                containerColor = Tokens.colors.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ComposiumTab(
                        selected = uiState.selectedTab == ControlsSheetTab.Properties,
                        onClick = { onTabSelected(ControlsSheetTab.Properties) },
                        text = "Properties",
                    )
                    ComposiumTab(
                        selected = uiState.selectedTab == ControlsSheetTab.SystemParams,
                        onClick = { onTabSelected(ControlsSheetTab.SystemParams) },
                        text = "System params",
                    )
                }
            }

            when (uiState.selectedTab) {
                ControlsSheetTab.Properties -> {
                    ComposiumHorizontalDivider(color = Tokens.colors.outlineVariant)
                    if (paramsState.params.isEmpty()) {
                        ComposiumText(
                            text = "No properties for this scene.",
                            style = Tokens.typography.bodySmall,
                            color = Tokens.colors.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    } else {
                        ControlsPanel(
                            state = paramsState,
                            callbacks = paramsCallbacks,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                        )
                    }
                    Spacer(Modifier.padding(bottom = 8.dp))
                }

                ControlsSheetTab.SystemParams -> {
                    ComposiumHorizontalDivider(color = Tokens.colors.outlineVariant)
                    PreviewPanel(
                        preview = preview,
                        isDarkThemeEnabled = isDarkThemeEnabled,
                        onThemeChange = onThemeChange,
                    )
                    Spacer(Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    preview: SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    onThemeChange: (isDarkTheme: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val stableDensity = remember { DisplayMetrics.DENSITY_DEVICE_STABLE / 160f }
    val systemFontScale = density.fontScale
    val systemDisplayScale = density.density / stableDensity

    val fontScales = remember {
        systemFloatArray("config_fontScale")
            ?: listOf(0.85f, 0.93f, 1.00f, 1.08f, 1.15f, 1.23f, 1.30f)
    }
    val displayScales = remember {
        systemFloatArray("screenZoomRatios", "config_screenZoomRatios", "config_screenZoomRatiosDefault")
            ?: listOf(0.85f, 0.93f, 1.00f, 1.08f, 1.15f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        SettingSwitchRow(
            title = "Dark theme",
            subtitle = "Matches Composium top bar toggle",
            checked = isDarkThemeEnabled,
            onCheckedChange = onThemeChange,
        )

        SettingSwitchRow(
            title = "RTL layout",
            subtitle = "Layout direction preview for this scene",
            checked = preview.rtl,
            onCheckedChange = { checked -> preview.rtl = checked },
        )

        Spacer(Modifier.height(6.dp))
        ComposiumHorizontalDivider(color = Tokens.colors.outlineVariant)
        Spacer(Modifier.height(10.dp))

        SettingScaleSliderRow(
            title = "Font size",
            subtitle = "Mimics Android \"Font size\" slider (fontScale)",
            values = fontScales,
            selectedValue = preview.fontScaleOverride,
            systemValue = systemFontScale,
            onUseSystem = { preview.fontScaleOverride = null },
            onValueSelected = { selected -> preview.fontScaleOverride = selected },
        )

        Spacer(Modifier.height(12.dp))

        SettingScaleSliderRow(
            title = "Display size",
            subtitle = "Mimics Android \"Display size\" slider (density multiplier)",
            values = displayScales,
            selectedValue = preview.displayScaleOverride,
            systemValue = systemDisplayScale,
            onUseSystem = { preview.displayScaleOverride = null },
            onValueSelected = { selected -> preview.displayScaleOverride = selected },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            ComposiumText(
                text = title,
                style = Tokens.typography.titleMedium,
                color = Tokens.colors.onSurface,
            )
            ComposiumText(
                text = subtitle,
                style = Tokens.typography.bodySmall,
                color = Tokens.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        ComposiumSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingScaleSliderRow(
    title: String,
    subtitle: String,
    values: List<Float>,
    selectedValue: Float?,
    systemValue: Float,
    onUseSystem: () -> Unit,
    onValueSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun nearestIndex(value: Float): Int {
        var bestIndex = 0
        var bestDistance = Float.POSITIVE_INFINITY
        values.forEachIndexed { index, candidate ->
            val distance = abs(candidate - value)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    val resolvedSelectedValue = selectedValue ?: systemValue
    val selectedIndex = nearestIndex(resolvedSelectedValue)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                ComposiumText(
                    text = title,
                    style = Tokens.typography.titleMedium,
                    color = Tokens.colors.onSurface,
                )
                ComposiumText(
                    text = subtitle,
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val shownValue = values[selectedIndex]
            ComposiumText(
                text = if (selectedValue == null) {
                    "System x${"%.2f".format(shownValue)}"
                } else {
                    "x${"%.2f".format(shownValue)}"
                },
                style = Tokens.typography.labelSmall,
                color = Tokens.colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(Tokens.shapes.pill)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))

        ComposiumDiscreteSlider(
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index -> onValueSelected(values[index]) },
            steps = values.size,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(6.dp))
        if (selectedValue != null) {
            ComposiumText(
                text = "Use system (x${"%.2f".format(systemValue)})",
                style = Tokens.typography.bodySmall,
                color = Tokens.colors.primary,
                modifier = Modifier
                    .clip(Tokens.shapes.pill)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(onClick = onUseSystem),
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            values.forEach { value ->
                ComposiumText(
                    text = "x${"%.2f".format(value)}",
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun systemFloatArray(vararg names: String): List<Float>? {
    val resources = Resources.getSystem()
    for (name in names) {
        val id = resources.getIdentifier(name, "array", "android")
        if (id == 0) continue

        val typedArray = resources.obtainTypedArray(id)
        try {
            val values = ArrayList<Float>(typedArray.length())
            for (index in 0 until typedArray.length()) {
                val value = typedArray.getFloat(index, Float.NaN)
                if (!value.isNaN()) values.add(value)
            }
            val cleaned = values
                .filter { it > 0f }
                .distinct()
                .sorted()

            if (cleaned.size >= 2) return cleaned
        } finally {
            typedArray.recycle()
        }
    }
    return null
}
