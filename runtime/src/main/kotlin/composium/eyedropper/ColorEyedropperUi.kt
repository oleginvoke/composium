package oleginvoke.com.composium.eyedropper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ColorEyedropperCrosshair(
    target: Offset,
    metrics: ColorEyedropperMetrics,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx().coerceAtLeast(1f)
        val arm = metrics.crosshairArm.toPx()
        val gap = metrics.crosshairGap.toPx()
        val dash = metrics.crosshairDash.toPx()
        val dashGap = metrics.crosshairDashGap.toPx()

        drawAlternatingLine(
            start = Offset(target.x - arm, target.y),
            end = Offset(target.x - gap, target.y),
            dash = dash,
            gap = dashGap,
            strokeWidth = strokeWidth,
        )
        drawAlternatingLine(
            start = Offset(target.x + gap, target.y),
            end = Offset(target.x + arm, target.y),
            dash = dash,
            gap = dashGap,
            strokeWidth = strokeWidth,
        )
        drawAlternatingLine(
            start = Offset(target.x, target.y - arm),
            end = Offset(target.x, target.y - gap),
            dash = dash,
            gap = dashGap,
            strokeWidth = strokeWidth,
        )
        drawAlternatingLine(
            start = Offset(target.x, target.y + gap),
            end = Offset(target.x, target.y + arm),
            dash = dash,
            gap = dashGap,
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
internal fun ColorEyedropperIsland(
    color: Color,
    values: List<ColorEyedropperValue>,
    onValueClick: (ColorEyedropperValue) -> Unit,
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            )
            .clip(shape)
            .background(colors.islandContainer)
            .border(BorderStroke(1.dp, colors.islandOutline), shape)
            .defaultMinSize(minWidth = metrics.islandMinWidth)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(
                color = color,
                size = metrics.swatchSize,
                outline = colors.swatchOutline,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                BasicText(
                    text = "Eyedropper",
                    style = TextStyle(
                        color = colors.islandContent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                BasicText(
                    text = ColorEyedropperFormat.HEX.format(color),
                    style = TextStyle(
                        color = colors.islandSecondaryContent,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            values.forEach { value ->
                ColorValueRow(
                    value = value,
                    onClick = { onValueClick(value) },
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    size: Dp,
    outline: Color,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(BorderStroke(2.dp, outline), CircleShape),
    )
}

@Composable
private fun ColorValueRow(
    value: ColorEyedropperValue,
    onClick: () -> Unit,
    colors: ColorEyedropperColors,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (pressed) colors.rowPressedContainer else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = value.label,
            modifier = Modifier.width(48.dp),
            style = TextStyle(
                color = colors.islandSecondaryContent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 12.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            text = value.text,
            style = TextStyle(
                color = colors.islandContent,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun DrawScope.drawAlternatingLine(
    start: Offset,
    end: Offset,
    dash: Float,
    gap: Float,
    strokeWidth: Float,
) {
    val delta = end - start
    val length = delta.getDistance()
    if (length <= 0f) return

    val direction = delta / length
    var position = 0f
    var index = 0
    while (position < length) {
        val next = (position + dash).coerceAtMost(length)
        val color = if (index % 2 == 0) Color.White else Color.Black
        val shadowColor = if (index % 2 == 0) Color.Black else Color.White
        val segmentStart = start + direction * position
        val segmentEnd = start + direction * next
        drawLine(
            color = shadowColor.copy(alpha = 0.72f),
            start = segmentStart,
            end = segmentEnd,
            strokeWidth = strokeWidth + 1f,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = color,
            start = segmentStart,
            end = segmentEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square,
        )
        position = next + gap
        index += 1
    }
}
