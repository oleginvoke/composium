package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.ComposiumGradients
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.pressScale

@Composable
internal fun ComposiumThemeToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val width = 64.dp
    val height = 32.dp
    val thumbSize = 26.dp
    val edgePadding = 3.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (isDark) width - thumbSize - edgePadding else edgePadding,
        animationSpec = Motion.springBouncy(),
        label = "theme_thumb_offset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (isDark) {
            Tokens.colors.primaryContainer.copy(alpha = 0.36f)
        } else {
            Tokens.colors.surfaceVariant
        },
        animationSpec = Motion.tweenStandard(),
        label = "theme_track_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDark) {
            Tokens.colors.primary.copy(alpha = 0.55f)
        } else {
            Tokens.colors.outlineVariant
        },
        animationSpec = Motion.tweenStandard(),
        label = "theme_border_color",
    )
    val sunAlpha by animateFloatAsState(
        targetValue = if (isDark) 0.38f else 0f,
        animationSpec = Motion.tweenStandard(),
        label = "theme_sun_alpha",
    )
    val moonAlpha by animateFloatAsState(
        targetValue = if (isDark) 0f else 0.38f,
        animationSpec = Motion.tweenStandard(),
        label = "theme_moon_alpha",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 360f else 0f,
        animationSpec = Motion.springGentle(),
        label = "theme_thumb_rotation",
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .pressScale(interactionSource)
            .clip(Tokens.shapes.pill)
            .background(trackColor)
            .border(width = 1.dp, color = borderColor, shape = Tokens.shapes.pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onToggle(!isDark) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .graphicsLayer { alpha = sunAlpha },
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.WbSunny,
                contentDescription = null,
                tint = Tokens.colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .graphicsLayer { alpha = moonAlpha },
        ) {
            ComposiumIcon(
                imageVector = Icons.Outlined.NightsStay,
                contentDescription = null,
                tint = Tokens.colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(Tokens.shapes.pill)
                .background(if (isDark) ComposiumGradients.accent else ComposiumGradients.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (isDark) {
                ComposiumIcon(
                    imageVector = Icons.Outlined.NightsStay,
                    contentDescription = "Switch to light theme",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation),
                )
            } else {
                ComposiumIcon(
                    imageVector = Icons.Outlined.WbSunny,
                    contentDescription = "Switch to dark theme",
                    tint = Tokens.colors.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation),
                )
            }
        }
    }
}
