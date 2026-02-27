package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 20.dp,
    cornerRadius: Dp = 6.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val pressed by interactionSource.collectIsPressedAsState()

    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "checkProgress",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled && checked -> Tokens.colors.surfaceVariant.copy(alpha = 0.55f)
            checked -> Tokens.colors.primary
            else -> Color.Transparent
        },
        label = "containerColor",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> (if (checked) Tokens.colors.primary else Tokens.colors.outlineVariant)
                .copy(alpha = 0.45f)
            checked -> Tokens.colors.primary
            else -> Tokens.colors.outline
        },
        label = "borderColor",
    )
    val checkColor by animateColorAsState(
        targetValue = if (enabled) Tokens.colors.onPrimary else Tokens.colors.onSurfaceVariant.copy(alpha = 0.6f),
        label = "checkColor",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(Tokens.shapes.extraSmall)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ),
    ) {
        val strokeWidth = this.size.minDimension * 0.1f
        val radiusPx = cornerRadius.toPx().coerceAtMost(this.size.minDimension / 2f)

        drawRoundRect(
            color = containerColor,
            size = Size(this.size.width, this.size.height),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
        )
        drawRoundRect(
            color = borderColor,
            size = Size(this.size.width, this.size.height),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = strokeWidth),
        )

        if (checkProgress > 0f) {
            val start = Offset(x = this.size.width * 0.26f, y = this.size.height * 0.54f)
            val mid = Offset(x = this.size.width * 0.44f, y = this.size.height * 0.72f)
            val endX = if (layoutDirection == LayoutDirection.Ltr) this.size.width * 0.74f else this.size.width * 0.14f
            val end = Offset(x = endX, y = this.size.height * 0.30f)

            val firstPartProgress = (checkProgress / 0.45f).coerceIn(0f, 1f)
            val secondPartProgress = ((checkProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)

            if (firstPartProgress > 0f) {
                drawLine(
                    color = checkColor,
                    start = start,
                    end = Offset(
                        x = start.x + (mid.x - start.x) * firstPartProgress,
                        y = start.y + (mid.y - start.y) * firstPartProgress,
                    ),
                    strokeWidth = strokeWidth * 1.0f,
                    cap = StrokeCap.Round,
                )
            }
            if (secondPartProgress > 0f) {
                drawLine(
                    color = checkColor,
                    start = mid,
                    end = Offset(
                        x = mid.x + (end.x - mid.x) * secondPartProgress,
                        y = mid.y + (end.y - mid.y) * secondPartProgress,
                    ),
                    strokeWidth = strokeWidth * 1.0f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
