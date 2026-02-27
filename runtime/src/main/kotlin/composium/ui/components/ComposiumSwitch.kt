package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun ComposiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    defaultWidth: Dp = 40.dp,
    defaultHeight: Dp = 20.dp,
    padding: Dp = 1.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> (if (checked) Tokens.colors.primary else Tokens.colors.outlineVariant)
                .copy(alpha = 0.45f)
            checked -> Tokens.colors.primary
            else -> Tokens.colors.outlineVariant
        },
        label = "trackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (!enabled) {
            Tokens.colors.surface.copy(alpha = 0.45f)
        } else {
            Tokens.colors.surface
        },
        label = "thumbColor"
    )

    Layout(
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange
            )
            .drawBehind {
                val radius = size.height / 2f
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = CornerRadius(radius, radius)
                )
            },
        content = {
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .background(thumbColor, CircleShape)
            )
        }
    ) { measurables, constraints ->
        val density = this

        val defaultW = with(density) { defaultWidth.roundToPx() }
        val defaultH = with(density) { defaultHeight.roundToPx() }

        val w = constraints.constrainWidth(defaultW)
        val h = constraints.constrainHeight(defaultH)

        val pad = with(density) { padding.roundToPx() }.coerceIn(0, h / 3)

        val thumbSize = (h - 2 * pad).coerceAtLeast(1)

        val childConstraints = Constraints.fixed(thumbSize, thumbSize)
        val placeable = measurables.first().measure(childConstraints)

        val travel = (w - 2 * pad - thumbSize).coerceAtLeast(0)
        val x = (pad + (travel * progress)).toInt()
        val y = ((h - thumbSize) / 2f).toInt()

        layout(w, h) {
            placeable.placeRelative(x, y)
        }
    }
}
