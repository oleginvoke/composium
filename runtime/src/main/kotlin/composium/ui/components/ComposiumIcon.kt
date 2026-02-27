
package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
internal fun ComposiumIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint != null) ColorFilter.tint(tint) else null,
    )
}

@Composable
internal fun ComposiumIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint != null) ColorFilter.tint(tint) else null,
    )
}
