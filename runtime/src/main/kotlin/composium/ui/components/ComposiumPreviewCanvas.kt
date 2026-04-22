package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumPreviewCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val primary = Tokens.colors.primary
    val secondary = Tokens.colors.secondary
    val gridTint = Tokens.colors.outlineVariant.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.background)
            .drawWithCache {
                val cell = 24.dp.toPx()
                val lineColor = gridTint
                val softCellColor = lineColor.copy(alpha = 0.08f)
                val verticalGradient = Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    endY = size.height * 0.4f,
                )
                val radialGradient = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.34f),
                    radius = size.minDimension * 0.75f,
                )

                onDrawBehind {
                    var y = 0f
                    var row = 0
                    while (y < size.height + cell) {
                        var x = 0f
                        var col = 0
                        while (x < size.width + cell) {
                            if ((row + col) % 2 == 0) {
                                drawRect(
                                    color = softCellColor,
                                    topLeft = Offset(x, y),
                                    size = Size(cell, cell),
                                )
                            }
                            x += cell
                            col += 1
                        }
                        y += cell
                        row += 1
                    }

                    var vertical = 0f
                    while (vertical < size.width + cell) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.16f),
                            start = Offset(vertical, 0f),
                            end = Offset(vertical, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                        vertical += cell
                    }

                    var horizontal = 0f
                    while (horizontal < size.height + cell) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.16f),
                            start = Offset(0f, horizontal),
                            end = Offset(size.width, horizontal),
                            strokeWidth = 1.dp.toPx(),
                        )
                        horizontal += cell
                    }

                    drawRect(brush = verticalGradient)

                    drawRect(brush = radialGradient)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
