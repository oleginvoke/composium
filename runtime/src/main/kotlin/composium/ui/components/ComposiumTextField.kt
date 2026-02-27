package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    showClearButton: Boolean = true,
    clearIcon: ImageVector = Icons.Filled.Close,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = TextStyle(fontSize = 16.sp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors(),
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    borderWidth: Dp = 1.dp,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        !enabled -> colors.disabledBorderColor
        isFocused -> colors.focusedBorderColor
        else -> colors.unfocusedBorderColor
    }

    val resolvedTextColor = if (enabled) colors.textColor else colors.disabledTextColor
    val mergedTextStyle = textStyle.merge(TextStyle(color = resolvedTextColor))

    val selectionColors = remember(colors.selectionHandleColor, colors.selectionBackgroundColor) {
        TextSelectionColors(
            handleColor = colors.selectionHandleColor,
            backgroundColor = colors.selectionBackgroundColor,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalTextSelectionColors provides selectionColors,
    ) {
        Box(
            modifier = modifier
                .background(color = colors.containerColor, shape = shape)
                .border(BorderStroke(borderWidth, borderColor), shape)
                .clip(shape)
                .padding(contentPadding),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                readOnly = readOnly,
                textStyle = mergedTextStyle,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                singleLine = singleLine,
                minLines = minLines,
                maxLines = maxLines,
                cursorBrush = SolidColor(colors.cursorColor),
                decorationBox = { inner ->
                    val canClear = showClearButton && enabled && !readOnly && value.isNotEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                                BasicText(
                                    text = placeholder,
                                    style = mergedTextStyle.copy(color = colors.placeholderColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }

                        if (canClear) {
                            ClearTextButton(
                                icon = clearIcon,
                                contentDescription = "Clear",
                                tint = colors.placeholderColor,
                                onClick = { onValueChange("") },
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ClearTextButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Immutable
internal data class TextFieldColors(
    val textColor: Color,
    val disabledTextColor: Color,
    val placeholderColor: Color,
    val containerColor: Color,
    val cursorColor: Color,
    val focusedBorderColor: Color,
    val unfocusedBorderColor: Color,
    val disabledBorderColor: Color,
    val selectionHandleColor: Color,
    val selectionBackgroundColor: Color,
)

internal object TextFieldDefaults {

    @Composable
    fun colors(
        textColor: Color = Tokens.colors.onSurface,
        disabledTextColor: Color = Tokens.colors.onSurfaceVariant.copy(alpha = 0.6f),
        placeholderColor: Color = Tokens.colors.onSurfaceVariant,
        containerColor: Color = Tokens.colors.surface,
        cursorColor: Color = Tokens.colors.primary,
        focusedBorderColor: Color = Tokens.colors.primary,
        unfocusedBorderColor: Color = Tokens.colors.outline,
        disabledBorderColor: Color = Tokens.colors.outlineVariant.copy(alpha = 0.7f),
        selectionHandleColor: Color = Tokens.colors.primary,
        selectionBackgroundColor: Color = Tokens.colors.primary.copy(alpha = 0.35f),
    ): TextFieldColors {
        return TextFieldColors(
            textColor = textColor,
            disabledTextColor = disabledTextColor,
            placeholderColor = placeholderColor,
            containerColor = containerColor,
            cursorColor = cursorColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            selectionHandleColor = selectionHandleColor,
            selectionBackgroundColor = selectionBackgroundColor,
        )
    }
}
