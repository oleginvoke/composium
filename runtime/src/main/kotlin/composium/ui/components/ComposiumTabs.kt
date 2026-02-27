package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
            .background(containerColor),
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
        targetValue = if (selected) Tokens.colors.primaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "tab_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Tokens.colors.onPrimaryContainer else Tokens.colors.onSurface,
        animationSpec = tween(200),
        label = "tab_fg",
    )
    Box(
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(Tokens.shapes.medium)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
