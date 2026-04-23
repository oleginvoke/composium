package oleginvoke.com.composium.scene_screen

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneSystemSettings
import oleginvoke.com.composium.ui.components.ComposiumBadge
import oleginvoke.com.composium.ui.components.ComposiumDiscreteSlider
import oleginvoke.com.composium.ui.components.ComposiumSwitch
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.theme.Tokens
import kotlin.math.abs

@Composable
internal fun EnvironmentPanel(
    preview: SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val stableDensity = remember { DisplayMetrics.DENSITY_DEVICE_STABLE / 160f }
    val fontScales = remember {
        systemFloatArray("config_fontScale")
            ?: listOf(0.85f, 0.93f, 1.00f, 1.08f, 1.15f, 1.23f, 1.30f)
    }
    val displayScales = remember {
        systemFloatArray("screenZoomRatios", "config_screenZoomRatios", "config_screenZoomRatiosDefault")
            ?: listOf(0.85f, 0.93f, 1.00f, 1.08f, 1.15f)
    }

    val items = buildEnvironmentItems(
        preview = preview,
        isDarkThemeEnabled = isDarkThemeEnabled,
        onThemeChange = onThemeChange,
        fontScales = fontScales,
        displayScales = displayScales,
        systemDisplayScale = density.density / stableDensity,
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.key }) { item ->
            when (item) {
                is EnvironmentItem.Toggle -> {
                    EnvironmentIslandCard {
                        ToggleSettingRow(item = item)
                    }
                }

                is EnvironmentItem.Scale -> {
                    EnvironmentIslandCard {
                        ScaleSettingCard(item = item)
                    }
                }
            }
        }
    }
}

internal fun buildEnvironmentItems(
    preview: SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    fontScales: List<Float>,
    displayScales: List<Float>,
    systemDisplayScale: Float,
): List<EnvironmentItem> {
    return listOf(
        EnvironmentItem.Toggle(
            key = "dark_theme",
            title = "Dark theme",
            checked = isDarkThemeEnabled,
            onCheckedChange = onThemeChange,
        ),
        EnvironmentItem.Toggle(
            key = "rtl",
            title = "RTL layout",
            checked = preview.rtl,
            onCheckedChange = { checked -> preview.rtl = checked },
        ),
        EnvironmentItem.Scale(
            key = "font_scale",
            title = "Font size",
            values = fontScales,
            selectedValue = preview.fontScaleOverride,
            systemValue = 1f,
            onUseSystem = { preview.fontScaleOverride = null },
            onValueSelected = { selected -> preview.fontScaleOverride = selected },
        ),
        EnvironmentItem.Scale(
            key = "display_scale",
            title = "Display size",
            values = displayScales,
            selectedValue = preview.displayScaleOverride,
            systemValue = systemDisplayScale,
            onUseSystem = { preview.displayScaleOverride = null },
            onValueSelected = { selected -> preview.displayScaleOverride = selected },
        ),
    )
}

internal sealed interface EnvironmentItem {
    val key: String
    val title: String

    data class Toggle(
        override val key: String,
        override val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : EnvironmentItem

    data class Scale(
        override val key: String,
        override val title: String,
        val values: List<Float>,
        val selectedValue: Float?,
        val systemValue: Float,
        val onUseSystem: () -> Unit,
        val onValueSelected: (Float) -> Unit,
    ) : EnvironmentItem
}

@Composable
private fun EnvironmentIslandCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = Tokens.shapes.large,
                ambientColor = Tokens.colors.scrim.copy(alpha = 0.22f),
                spotColor = Tokens.colors.scrim.copy(alpha = 0.28f),
            )
            .clip(Tokens.shapes.large)
            .background(Tokens.colors.surface.copy(alpha = 0.94f))
            .border(1.dp, Tokens.colors.outlineVariant.copy(alpha = 0.76f), Tokens.shapes.large)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}

@Composable
private fun ToggleSettingRow(item: EnvironmentItem.Toggle) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ComposiumText(
            text = item.title,
            style = Tokens.typography.titleMedium,
            color = Tokens.colors.onSurface,
        )
        ComposiumSwitch(
            checked = item.checked,
            onCheckedChange = item.onCheckedChange,
        )
    }
}

@Composable
private fun ScaleSettingCard(item: EnvironmentItem.Scale) {
    val selectedIndex = nearestIndex(
        values = item.values,
        value = item.selectedValue ?: item.systemValue,
    )
    val shownValue = item.values[selectedIndex]

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ComposiumText(
                modifier = Modifier.fillMaxWidth().weight(1f),
                text = item.title,
                style = Tokens.typography.titleMedium,
                color = Tokens.colors.onSurface,
            )
            if (item.selectedValue != null) {
                ComposiumText(
                    text = "Reset",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colors.primary,
                    modifier = Modifier
                        .clip(Tokens.shapes.pill)
                        .clickable(onClick = item.onUseSystem)
                        .padding(horizontal = 6.dp),
                )
            }
            ComposiumBadge(
                text = if (item.selectedValue == null) {
                    "System x${"%.2f".format(shownValue)}"
                } else {
                    "x${"%.2f".format(shownValue)}"
                },
                containerColor = Tokens.colors.secondaryContainer,
                contentColor = Tokens.colors.onSecondaryContainer,
                compact = true,
            )
        }

        Spacer(Modifier.padding(top = 10.dp))

        ComposiumDiscreteSlider(
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index -> item.onValueSelected(item.values[index]) },
            steps = item.values.size,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            item.values.forEach { value ->
                ComposiumText(
                    text = "x${"%.2f".format(value)}",
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun nearestIndex(values: List<Float>, value: Float): Int {
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
