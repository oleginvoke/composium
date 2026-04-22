package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumTabRow(
    modifier: Modifier = Modifier,
    containerColor: Color = Tokens.colors.surface,
    tabs: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Tokens.shapes.large)
            .background(containerColor)
            .border(1.dp, Tokens.colors.outlineVariant, Tokens.shapes.large)
            .padding(4.dp),
    ) {
        tabs()
    }
}

@Composable
internal fun ComposiumTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Tokens.colors.inverseSurface else Color.Transparent,
        animationSpec = tween(200),
        label = "tab_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Tokens.colors.inverseOnSurface else Tokens.colors.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tab_fg",
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(Tokens.shapes.pill)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumText(
            text = text,
            style = Tokens.typography.titleMedium,
            color = fg,
            maxLines = 1,
        )
    }
}
