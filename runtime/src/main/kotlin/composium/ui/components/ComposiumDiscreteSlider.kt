package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.roundToInt
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumDiscreteSlider(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 32.dp,
    trackHeight: Dp = 4.dp,
    tickRadius: Dp = 3.dp,
    thumbRadius: Dp = 9.dp,
    activeColor: Color = Tokens.colors.primary,
    inactiveColor: Color = Tokens.colors.outlineVariant,
    thumbColor: Color = Tokens.colors.surface,
) {
    require(steps >= 2) { "steps must be >= 2" }
    val clampedSelected = selectedIndex.coerceIn(0, steps - 1)

    val density = LocalDensity.current
    val trackH = with(density) { trackHeight.toPx() }
    val tickR = with(density) { tickRadius.toPx() }
    val thumbR = with(density) { thumbRadius.toPx() }

    fun indexFromX(x: Float, width: Float, padding: Float): Int {
        val available = (width - 2 * padding).coerceAtLeast(1f)
        val t = ((x - padding) / available).coerceIn(0f, 1f)
        return (t * (steps - 1)).roundToInt().coerceIn(0, steps - 1)
    }

    val pointerModifier = if (!enabled) Modifier else Modifier
        .pointerInput(steps) {
            detectTapGestures { pos ->
                val i = indexFromX(pos.x, size.width.toFloat(), padding = thumbR)
                onSelectedIndexChange(i)
            }
        }
        .pointerInput(steps) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    val i = indexFromX(change.position.x, size.width.toFloat(), padding = thumbR)
                    onSelectedIndexChange(i)
                },
            )
        }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(pointerModifier)
            .semantics(mergeDescendants = true) {},
    ) {
        val w = size.width
        val h = size.height

        val left = thumbR
        val right = w - thumbR
        val centerY = h / 2f

        val stepX = (right - left) / (steps - 1).toFloat()
        fun xAt(i: Int): Float = left + stepX * i

        val activeX = xAt(clampedSelected)

        // Track (inactive)
        drawLine(
            color = inactiveColor,
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = trackH,
            cap = StrokeCap.Round,
        )
        // Track (active)
        drawLine(
            color = if (enabled) activeColor else activeColor.copy(alpha = 0.5f),
            start = Offset(left, centerY),
            end = Offset(activeX, centerY),
            strokeWidth = trackH,
            cap = StrokeCap.Round,
        )

        // Ticks
        for (i in 0 until steps) {
            val x = xAt(i)
            val isActive = i <= clampedSelected
            val c = when {
                !enabled -> (if (isActive) activeColor else inactiveColor).copy(alpha = 0.5f)
                isActive -> activeColor
                else -> inactiveColor
            }
            drawCircle(
                color = c,
                radius = tickR,
                center = Offset(x, centerY),
            )
        }

        // Thumb
        val thumbBorder = if (enabled) activeColor else activeColor.copy(alpha = 0.5f)
        drawCircle(
            color = thumbColor,
            radius = thumbR,
            center = Offset(activeX, centerY),
        )
        drawCircle(
            color = thumbBorder,
            radius = thumbR,
            center = Offset(activeX, centerY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 2.dp.toPx() }),
        )
    }
}

