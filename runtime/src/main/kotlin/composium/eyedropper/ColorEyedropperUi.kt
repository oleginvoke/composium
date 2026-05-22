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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
internal fun ColorEyedropperCursor(
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(metrics.cursorDiameter)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            )
            .border(
                BorderStroke(1.dp, colors.cursorContrastOutline),
                CircleShape,
            )
            .padding(1.dp)
            .border(
                BorderStroke(metrics.cursorOutlineWidth, colors.cursorOutline),
                CircleShape,
            )
    )
}

@Composable
internal fun ColorEyedropperLens(
    lens: ColorEyedropperPixelLens,
    onNudge: (ColorEyedropperNudgeDirection) -> Unit,
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val lensContentDiameter = with(density) {
        calculateColorEyedropperLensContentDiameter(
            lensSizePx = metrics.lensDiameter.toPx(),
            ringWidthPx = metrics.lensInnerPadding.toPx(),
        ).toDp()
    }

    Box(
        modifier = modifier
            .size(metrics.lensDiameter)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            )
            .clip(CircleShape)
            .background(colors.lensContainer)
            .border(
                BorderStroke(metrics.lensOutlineWidth, colors.lensOutline),
                CircleShape,
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(lensContentDiameter),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawColorEyedropperLens(
                    lens = lens,
                    metrics = metrics,
                    colors = colors,
                )
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                drawLensContentOutline(
                    metrics = metrics,
                    colors = colors,
                )
            }
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawLensDirectionMarkers(
                metrics = metrics,
                colors = colors,
                ringWidthPx = metrics.lensInnerPadding.toPx(),
            )
        }
        ColorEyedropperLensNudgeButton(
            direction = ColorEyedropperNudgeDirection.Top,
            alignment = Alignment.TopCenter,
            metrics = metrics,
            onNudge = onNudge,
        )
        ColorEyedropperLensNudgeButton(
            direction = ColorEyedropperNudgeDirection.Bottom,
            alignment = Alignment.BottomCenter,
            metrics = metrics,
            onNudge = onNudge,
        )
        ColorEyedropperLensNudgeButton(
            direction = ColorEyedropperNudgeDirection.Start,
            alignment = Alignment.CenterStart,
            metrics = metrics,
            onNudge = onNudge,
        )
        ColorEyedropperLensNudgeButton(
            direction = ColorEyedropperNudgeDirection.End,
            alignment = Alignment.CenterEnd,
            metrics = metrics,
            onNudge = onNudge,
        )
    }
}

@Composable
private fun BoxScope.ColorEyedropperLensNudgeButton(
    direction: ColorEyedropperNudgeDirection,
    alignment: Alignment,
    metrics: ColorEyedropperMetrics,
    onNudge: (ColorEyedropperNudgeDirection) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .align(alignment)
            .size(metrics.lensArrowTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onNudge(direction) },
            ),
    )
}

@Composable
internal fun ColorEyedropperIsland(
    values: List<ColorEyedropperValue>,
    onValueClick: (ColorEyedropperValue) -> Unit,
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

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
            .widthIn(max = metrics.lensDiameter)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        values.forEach { value ->
            ColorValueRow(
                value = value,
                onClick = { onValueClick(value) },
                colors = colors,
            )
        }
    }
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
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = value.label,
            modifier = Modifier.width(30.dp),
            style = TextStyle(
                color = colors.islandSecondaryContent,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 11.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            text = value.text,
            style = TextStyle(
                color = colors.islandContent,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontFamily = FontFamily.Monospace,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun DrawScope.drawColorEyedropperLens(
    lens: ColorEyedropperPixelLens,
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
) {
    drawColorEyedropperPixelPatch(
        lens = lens,
        colors = colors,
        gridStrokePx = metrics.lensGridStroke.toPx().coerceAtLeast(1f),
        centerStrokePx = metrics.lensCenterStroke.toPx().coerceAtLeast(2f),
        showGrid = true,
        showCenter = true,
    )
}

private fun DrawScope.drawLensContentOutline(
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
) {
    val strokeWidth = metrics.lensContentOutlineWidth.toPx().coerceAtLeast(1f)
    val radius = size.minDimension / 2f - strokeWidth / 2f
    if (radius <= 0f) return

    drawCircle(
        color = colors.lensContentOutline,
        radius = radius,
        center = Offset(size.width / 2f, size.height / 2f),
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.drawColorEyedropperPixelPatch(
    lens: ColorEyedropperPixelLens,
    colors: ColorEyedropperColors,
    gridStrokePx: Float,
    centerStrokePx: Float,
    showGrid: Boolean,
    showCenter: Boolean,
) {
    if (lens.side <= 0 || lens.pixels.isEmpty()) return

    val contentSize = min(size.width, size.height)
    if (contentSize <= 0f) return

    val topLeft = Offset(
        x = (size.width - contentSize) / 2f,
        y = (size.height - contentSize) / 2f,
    )
    val cellSize = contentSize / lens.side
    val clip = Path().apply {
        addOval(Rect(offset = topLeft, size = Size(contentSize, contentSize)))
    }

    clipPath(clip) {
        lens.pixels.forEachIndexed { index, pixel ->
            val row = index / lens.side
            val column = index % lens.side
            drawRect(
                color = Color(pixel),
                topLeft = Offset(
                    x = topLeft.x + column * cellSize,
                    y = topLeft.y + row * cellSize,
                ),
                size = Size(
                    width = cellSize + 0.5f,
                    height = cellSize + 0.5f,
                ),
            )
        }

        if (showGrid) {
            for (line in 1 until lens.side) {
                val x = topLeft.x + line * cellSize
                val y = topLeft.y + line * cellSize
                drawLine(
                    color = colors.lensGrid,
                    start = Offset(x, topLeft.y),
                    end = Offset(x, topLeft.y + contentSize),
                    strokeWidth = gridStrokePx,
                )
                drawLine(
                    color = colors.lensGrid,
                    start = Offset(topLeft.x, y),
                    end = Offset(topLeft.x + contentSize, y),
                    strokeWidth = gridStrokePx,
                )
            }
        }

        if (showCenter) {
            val centerColumn = lens.centerIndex % lens.side
            val centerRow = lens.centerIndex / lens.side
            val centerTopLeft = Offset(
                x = topLeft.x + centerColumn * cellSize,
                y = topLeft.y + centerRow * cellSize,
            )
            val centerSize = Size(cellSize, cellSize)
            drawRect(
                color = colors.lensCenterContrast,
                topLeft = centerTopLeft,
                size = centerSize,
                style = Stroke(width = centerStrokePx + 2f),
            )
            drawRect(
                color = colors.lensCenterOutline,
                topLeft = centerTopLeft,
                size = centerSize,
                style = Stroke(width = centerStrokePx),
            )
        }
    }
}

private fun DrawScope.drawLensDirectionMarkers(
    metrics: ColorEyedropperMetrics,
    colors: ColorEyedropperColors,
    ringWidthPx: Float,
) {
    val markerColor = colors.lensArrow
    val width = min(9.dp.toPx(), ringWidthPx * 0.76f).coerceAtLeast(5f)
    val height = min(7.dp.toPx(), ringWidthPx * 0.54f).coerceAtLeast(4f)
    val strokeWidth = metrics.lensChevronStroke.toPx().coerceAtLeast(1f)

    ColorEyedropperNudgeDirection.entries.forEach { direction ->
        drawLensDirectionMarker(
            center = calculateColorEyedropperLensArrowCenter(
                lensSizePx = size.minDimension,
                ringWidthPx = ringWidthPx,
                direction = direction,
            ),
            direction = direction,
            width = width,
            height = height,
            strokeWidth = strokeWidth,
            color = markerColor,
        )
    }
}

internal fun calculateColorEyedropperLensContentDiameter(
    lensSizePx: Float,
    ringWidthPx: Float,
): Float = (lensSizePx - ringWidthPx * 2f).coerceAtLeast(1f)

internal fun calculateColorEyedropperLensArrowCenter(
    lensSizePx: Float,
    ringWidthPx: Float,
    direction: ColorEyedropperNudgeDirection,
): Offset {
    val ringCenter = (ringWidthPx / 2f).coerceAtLeast(0f)
    val center = lensSizePx / 2f
    return when (direction) {
        ColorEyedropperNudgeDirection.Top -> Offset(x = center, y = ringCenter)
        ColorEyedropperNudgeDirection.Bottom -> Offset(x = center, y = lensSizePx - ringCenter)
        ColorEyedropperNudgeDirection.Start -> Offset(x = ringCenter, y = center)
        ColorEyedropperNudgeDirection.End -> Offset(x = lensSizePx - ringCenter, y = center)
    }
}

private fun DrawScope.drawLensDirectionMarker(
    center: Offset,
    direction: ColorEyedropperNudgeDirection,
    width: Float,
    height: Float,
    strokeWidth: Float,
    color: Color,
) {
    val points = when (direction) {
        ColorEyedropperNudgeDirection.Top -> listOf(
            Offset(center.x - width / 2f, center.y + height / 2f),
            Offset(center.x, center.y - height / 2f),
            Offset(center.x + width / 2f, center.y + height / 2f),
        )

        ColorEyedropperNudgeDirection.Bottom -> listOf(
            Offset(center.x - width / 2f, center.y - height / 2f),
            Offset(center.x, center.y + height / 2f),
            Offset(center.x + width / 2f, center.y - height / 2f),
        )

        ColorEyedropperNudgeDirection.Start -> listOf(
            Offset(center.x + height / 2f, center.y - width / 2f),
            Offset(center.x - height / 2f, center.y),
            Offset(center.x + height / 2f, center.y + width / 2f),
        )

        ColorEyedropperNudgeDirection.End -> listOf(
            Offset(center.x - height / 2f, center.y - width / 2f),
            Offset(center.x + height / 2f, center.y),
            Offset(center.x - height / 2f, center.y + width / 2f),
        )
    }

    drawLine(
        color = color,
        start = points[0],
        end = points[1],
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = points[1],
        end = points[2],
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}
