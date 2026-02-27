package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Простой Surface без зависимостей на Material.
 * По сути: background + clip (+ optional border).
 */
@Composable
internal fun ComposiumSurface(
    modifier: Modifier = Modifier,
    color: Color,
    shape: Shape,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    var m = modifier
        .clip(shape)
        .background(color = color, shape = shape)

    if (border != null) {
        m = m.border(border = border, shape = shape)
    }

    Box(modifier = m) {
        content()
    }
}
