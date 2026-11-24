package oleginvoke.com.composium.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * Wrap the parameter chips using stable layout APIs. Compose 1.7's experimental
 * FlowRow overload includes FlowRowOverflow, whose binary API was removed in 1.12.
 */
@Composable
internal fun ComposiumFlowRow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val horizontalGap = horizontalSpacing.roundToPx()
        val verticalGap = verticalSpacing.roundToPx()
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rowStarts = mutableListOf(0)
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        var rowWidth = 0
        var rowHeight = 0
        children.forEachIndexed { index, child ->
            val gap = if (index == rowStarts.last()) 0 else horizontalGap
            if (index > rowStarts.last() && rowWidth.toLong() + gap + child.width > constraints.maxWidth) {
                rowWidths += rowWidth
                rowHeights += rowHeight
                rowStarts += index
                rowWidth = child.width
                rowHeight = child.height
            } else {
                rowWidth += gap + child.width
                rowHeight = maxOf(rowHeight, child.height)
            }
        }
        rowWidths += rowWidth
        rowHeights += rowHeight
        val width = constraints.constrainWidth(rowWidths.maxOrNull() ?: 0)
        val height = constraints.constrainHeight(rowHeights.sum() + verticalGap * (rowHeights.size - 1))
        layout(width, height) {
            var y = 0
            rowStarts.forEachIndexed { row, start ->
                val end = rowStarts.getOrNull(row + 1) ?: children.size
                var x = 0
                for (index in start until end) {
                    val child = children[index]
                    child.placeRelative(x, y)
                    x += child.width + horizontalGap
                }
                y += rowHeights[row] + verticalGap
            }
        }
    }
}
