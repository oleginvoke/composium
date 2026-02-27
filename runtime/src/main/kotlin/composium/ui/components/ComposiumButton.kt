
package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Tokens.colors.primary,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.45f)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun ComposiumOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    border: BorderStroke = BorderStroke(1.dp, Tokens.colors.outline),
    containerColor: Color = Tokens.colors.surface,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.45f)
            .background(containerColor)
            .border(border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
