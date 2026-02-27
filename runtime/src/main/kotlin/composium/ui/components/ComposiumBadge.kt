package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    compact: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(Tokens.shapes.pill)
            .background(containerColor)
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 1.dp else 2.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumText(
            text = text,
            style = if (compact) Tokens.typography.labelSmall else Tokens.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}
