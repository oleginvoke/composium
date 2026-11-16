package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cached
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import oleginvoke.com.composium.ui.components.EnvironmentScalePresets
import oleginvoke.com.composium.ui.components.EnvironmentScaleRange
import oleginvoke.com.composium.ui.components.scaleFraction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneSystemSettings
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.components.ComposiumScaleSlider
import oleginvoke.com.composium.ui.components.ComposiumSwitch
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.components.TextFieldDefaults
import oleginvoke.com.composium.ui.components.ComposiumTextField
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun EnvironmentPanel(
    preview: SceneSystemSettings,
    isDarkThemeEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val stableDensity = systemDefaultDisplayDensity(density.density)
    val scales = EnvironmentScalePresets

    val items = buildEnvironmentItems(
        preview = preview,
        isDarkThemeEnabled = isDarkThemeEnabled,
        onThemeChange = onThemeChange,
        fontScales = scales,
        displayScales = scales,
        systemDisplayScale = density.density / stableDensity,
        systemFontScale = density.fontScale,
    )

    LazyColumn(
        modifier = modifier.dismissScaleKeyboardOnBackgroundTap(),
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
    systemFontScale: Float = 1f,
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
            systemValue = systemFontScale,
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
internal fun ScaleSettingCard(item: EnvironmentItem.Scale) {
    val shownValue = item.selectedValue ?: item.systemValue
    var editing by rememberSaveable(item.key) { mutableStateOf(false) }
    var draft by rememberSaveable(item.key, item.selectedValue, item.systemValue) {
        mutableStateOf(formatScaleValue(shownValue))
    }
    val parsed = parseScaleValue(draft)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var editorHadFocus by remember(editing) { mutableStateOf(false) }
    val commit = {
        if (parsed != null) {
            item.onValueSelected(parsed)
            editing = false
            focusManager.clearFocus()
        }
    }
    val choose: (Float) -> Unit = { value ->
        item.onValueSelected(value)
        draft = formatScaleValue(value)
        editing = false
        focusManager.clearFocus()
    }
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    // Keep a little breathing room while only the empty slot edges overlap. The slider (28 dp),
    // preset targets (44 x 36 dp) keep their sizes. Edit uses Compose minimum touch expansion.
    // The editor shares the same header height and spacing; off-scale help stays in place.
    val systemInteraction = remember { MutableInteractionSource() }
    val systemPressed by systemInteraction.collectIsPressedAsState()
    val systemFeedback by animateColorAsState(
        Tokens.colors.primary.copy(alpha = if (systemPressed) 0.16f else 0f),
        tween(140), label = "systemFeedback",
    )
    val editInteraction = remember { MutableInteractionSource() }
    val editPressed by editInteraction.collectIsPressedAsState()
    val editFeedback by animateColorAsState(
        Tokens.colors.primary.copy(alpha = if (editPressed) 0.16f else 0f),
        tween(140), label = "editFeedback",
    )
    val controlHeight = with(LocalDensity.current) { Tokens.typography.titleMedium.lineHeight.toDp() } + 8.dp
    val textMeasurer = rememberTextMeasurer()
    // Keep the common multiplier/1.23... slot stable; only exceptional long values need more room.
    val valueStyle = Tokens.typography.titleMedium
    val displayText = "\u00d7${formatScaleDisplayValue(shownValue)}"
    val valueWidth = with(LocalDensity.current) {
        listOf("\u00d78.88\u2026", displayText)
            .maxOf { textMeasurer.measure(it, valueStyle, maxLines = 1).size.width }.toDp() + 1.dp
    }
    val compact = shownValue in EnvironmentScaleRange
    Column(
        Modifier.fillMaxWidth().dismissScaleKeyboardOnBackgroundTap().testTag("${item.key}_card"),
        verticalArrangement = Arrangement.spacedBy(if (compact) (-4).dp else 0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(controlHeight).testTag("${item.key}_header"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (editing) 8.dp else 4.dp),
        ) {
            ComposiumText(item.title, modifier = Modifier.weight(1f), style = Tokens.typography.titleMedium, color = Tokens.colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (editing) 8.dp else 0.dp),
            ) {
                if (!editing && shownValue != item.systemValue) {
                    Box(
                        Modifier.width(32.dp).height(controlHeight).testTag("${item.key}_reset")
                            .clickable(role = Role.Button, interactionSource = systemInteraction, indication = null) {
                                item.onUseSystem()
                                draft = formatScaleValue(item.systemValue)
                                editing = false
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(Modifier.size(controlHeight).clip(RoundedCornerShape(6.dp)).background(systemFeedback),
                            contentAlignment = Alignment.Center) {
                            ComposiumIcon(Icons.Default.Cached, "Reset ${item.title} to system value", Modifier.size(20.dp).testTag("${item.key}_reset_icon"), Tokens.colors.primary)
                        }
                    }
                }
                if (editing) {
                    ComposiumTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.width(96.dp).height(controlHeight)
                            .onFocusChanged {
                                if (it.isFocused) editorHadFocus = true
                                else if (editorHadFocus) {
                                    editing = false
                                    draft = formatScaleValue(shownValue)
                                }
                            }
                            .focusRequester(focusRequester)
                            .testTag("${item.key}_input")
                            .semantics {
                                contentDescription = "${item.title} value"
                                if (parsed == null) error("Enter a number greater than 0")
                            },
                        initialCursorAtEnd = true,
                        singleLine = true,
                        showClearButton = false,
                        textStyle = Tokens.typography.bodyMedium,
                        colors = TextFieldDefaults.colors(
                            focusedBorderColor = if (parsed == null) Tokens.colors.error else Tokens.colors.primary,
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                    )
                    ComposiumIconButton(
                        onClick = { commit() },
                        enabled = parsed != null,
                        modifier = Modifier.testTag("${item.key}_apply"),
                        size = controlHeight,
                    ) {
                        ComposiumIcon(Icons.Default.Check, "Apply ${item.title}", Modifier.size(20.dp), Tokens.colors.primary)
                    }
                } else {
                    Box(
                        modifier = Modifier.height(controlHeight).width(valueWidth + 6.dp).testTag("${item.key}_edit")
                            .semantics { contentDescription = "Edit ${item.title}" }
                            .clickable(role = Role.Button, onClickLabel = "Edit ${item.title}", interactionSource = editInteraction, indication = null) {
                                draft = formatScaleValue(shownValue); editing = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(controlHeight).testTag("${item.key}_edit_feedback")
                                .clip(RoundedCornerShape(8.dp)).background(editFeedback)
                                .padding(start = 2.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                displayText,
                                modifier = Modifier.weight(1f).testTag("${item.key}_value")
                                    .semantics { contentDescription = "${item.title}: ${formatScaleValue(shownValue)} times" },
                                style = Tokens.typography.titleMedium.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    color = Tokens.colors.primary,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        if (shownValue !in EnvironmentScaleRange) {
            ComposiumText(
                "Outside slider range (0.5\u20132)",
                style = Tokens.typography.bodySmall,
                color = Tokens.colors.onSurfaceVariant,
            )
        }
        val presets = item.values.filter { it.isFinite() && it > 0f }.distinct().sorted()
        // The scrubber is a convenient range, not a restriction on exact input.
        val range = EnvironmentScaleRange
        Spacer(Modifier.height(if (compact) 8.dp else 4.dp))
        ComposiumScaleSlider(
            value = shownValue,
            range = range,
            systemValue = item.systemValue,
            onValueChange = choose,
            modifier = Modifier.testTag("${item.key}_slider")
                .semantics { contentDescription = item.title },
        )
        Layout(
            modifier = Modifier.fillMaxWidth().testTag("${item.key}_presets"),
            content = {
                presets.filter { it in range }.forEach { value ->
                    val fraction = scaleFraction(value, range)
                    // Touch targets keep their full width inside the card. Only the
                    // visual label centers on its tick, including both endpoints.
                    val labelOffset = ((2f * fraction - 1f) * 9f).dp
                    ScalePresetLabel(
                        value = value,
                        selected = shownValue == value,
                        labelOffset = labelOffset,
                        tag = "${item.key}_preset_$value",
                        onClick = { choose(value) },
                    )
                }
            },
        ) { measurables, constraints ->
            val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val width = constraints.maxWidth
            val inset = 22.dp.toPx() // Hit target inset, independent of the visible track.
            val values = presets.filter { it in range }
            val centers = values.map { inset + scaleFraction(it, range) * (width - 2 * inset) }
            // Keep every label anchored to its tick. On narrow screens stagger the
            // intermediate presets rather than shrinking or overlapping touch targets.
            val stagger = children.indices.drop(1).any { index ->
                centers[index] - centers[index - 1] < (children[index].width + children[index - 1].width) / 2f
            }
            val rowHeight = children.maxOfOrNull { it.height } ?: 0
            layout(width, rowHeight * if (stagger) 2 else 1) {
                children.forEachIndexed { index, child ->
                    val row = if (stagger) index % 2 else 0
                    child.placeRelative((centers[index] - child.width / 2f).roundToInt(), row * rowHeight)
                }
            }
        }
    }
}

@Composable
private fun ScalePresetLabel(
    value: Float,
    selected: Boolean,
    labelOffset: androidx.compose.ui.unit.Dp,
    tag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val emphasis by animateFloatAsState(
        if (pressed) 1f else if (selected) 0.55f else 0f,
        tween(180), label = "presetUnderline",
    )
    val ink by animateColorAsState(
        if (selected || pressed) Tokens.colors.primary else Tokens.colors.onSurfaceVariant,
        tween(160), label = "presetInk",
    )
    Box(
        modifier = Modifier.testTag(tag).heightIn(min = 36.dp).width(44.dp)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val child = measurable.measure(constraints)
                val shift = labelOffset.roundToPx()
                layout(child.width, child.height) { child.placeRelative(shift, 0) }
            }
                .drawBehind {
                    if (emphasis > 0f) {
                        val half = 11.dp.toPx() * emphasis
                        drawLine(ink, Offset(size.width / 2f - half, size.height - 1.dp.toPx()),
                            Offset(size.width / 2f + half, size.height - 1.dp.toPx()),
                            2.dp.toPx(), StrokeCap.Round)
                    }
                }
                .padding(vertical = 4.dp),
        ) {
            ComposiumText(
                text = formatScaleValue(value),
                modifier = Modifier.testTag("${tag}_label"),
                style = Tokens.typography.bodySmall,
                color = ink,
            )
        }
    }
}

// Preserve the actual Float value instead of rounding it to the nearest preset or hundredth.
internal fun formatScaleValue(value: Float): String = value.toString().removeSuffix(".0")

// Presentation only: exact Float text is retained by the editor and state.
internal fun formatScaleDisplayValue(value: Float): String {
    val exact = BigDecimal(value.toString())
    val truncated = exact.setScale(2, RoundingMode.DOWN)
    val hasMore = exact.compareTo(truncated) != 0
    val decimal = truncated.toPlainString()
    if (truncated.signum() != 0 && decimal.length <= 6) {
        return decimal + if (hasMore) "\u2026" else ""
    }
    val scientific = String.format(Locale.ROOT, "%.2e", value.toDouble()).split('e')
    val mantissa = scientific[0]
    val compact = "${mantissa}e${scientific[1].toInt()}"
    return compact + if (compact.toFloatOrNull() != value) "\u2026" else ""
}

internal fun parseScaleValue(text: String): Float? = text.trim().replace(',', '.')
    .toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

// Child controls consume their taps first. Empty-space taps dismiss without changing the draft.
internal fun Modifier.dismissScaleKeyboardOnBackgroundTap(): Modifier = composed {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(focus, keyboard) {
        detectTapGestures(onTap = { focus.clearFocus(); keyboard?.hide() })
    }
}
