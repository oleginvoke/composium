package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumChip(
    text: String,
    modifier: Modifier = Modifier,
    colors: ComposiumChipColors = ComposiumChipDefaults.colors(),
    shape: Shape = RoundedCornerShape(4.dp),
    selected: Boolean = false,
) {
    Row(
        modifier = modifier
            .border(
                shape = shape,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) colors.selectedBorderColor else colors.unselectedBorderColor,
                ),
            )
            .background(
                color = if (selected) colors.selectedBackgroundColor else colors.unselectedBackgroundColor,
                shape = shape,
            )
            .clip(shape)
            .height(20.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val onSurfaceVariant = Tokens.colors.onSurfaceVariant
        BasicText(
            modifier = Modifier,
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ColorProducer { onSurfaceVariant },
        )
    }
}

@Immutable
internal data class ComposiumChipColors(
    val selectedBackgroundColor: Color,
    val selectedBorderColor: Color,
    val unselectedBackgroundColor: Color,
    val unselectedBorderColor: Color,
)

internal object ComposiumChipDefaults {

    @Composable
    fun colors(
        selectedBackgroundColor: Color = Tokens.colors.primaryContainer,
        selectedBorderColor: Color = Tokens.colors.primaryContainer,
        unselectedBackgroundColor: Color = Tokens.colors.secondaryContainer.copy(alpha = 0.2f),
        unselectedBorderColor: Color = Tokens.colors.outline,
    ): ComposiumChipColors {
        return ComposiumChipColors(
            selectedBackgroundColor = selectedBackgroundColor,
            selectedBorderColor = selectedBorderColor,
            unselectedBackgroundColor = unselectedBackgroundColor,
            unselectedBorderColor = unselectedBorderColor,
        )
    }
}
