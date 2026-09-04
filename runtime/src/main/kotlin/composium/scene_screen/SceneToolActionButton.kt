package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumIconButton
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens

private val SceneToolActionSize = 48.dp

@Composable
internal fun SceneToolActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val containerColor by animateColorAsState(
        // The light-theme primaryContainer token bakes in alpha 0.83 (0xD3B5D9E8). Override
        // to fully opaque so the active settings button is solid like the other top bar
        // elements rather than letting preview content bleed through.
        targetValue = if (active) {
            Tokens.colors.primaryContainer
        } else {
            Tokens.colors.surface
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) {
            Color.Transparent
        } else {
            Tokens.colors.outlineVariant.copy(alpha = 0.8f)
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_border",
    )
    val tint by animateColorAsState(
        targetValue = if (active) Tokens.colors.primary else Tokens.colors.onSurface,
        animationSpec = Motion.tweenStandard(),
        label = "scene_top_bar_button_tint",
    )

    Box(
        modifier = modifier
            .size(SceneToolActionSize)
            .clip(Tokens.shapes.pill)
            .background(containerColor)
            .border(1.dp, borderColor, Tokens.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumIconButton(
            onClick = onClick,
            enabled = enabled,
            size = SceneToolActionSize,
        ) {
            AnimatedContent(
                targetState = imageVector,
                transitionSpec = {
                    fadeIn(animationSpec = Motion.tweenStandard()) togetherWith
                        fadeOut(animationSpec = Motion.tweenStandard())
                },
                label = "scene_top_bar_button_icon",
            ) { targetIcon ->
                ComposiumIcon(
                    imageVector = targetIcon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

internal fun SceneSettingsButtonIcon.imageVector(): ImageVector {
    return when (this) {
        SceneSettingsButtonIcon.Settings -> Icons.Outlined.Tune
        SceneSettingsButtonIcon.Expand -> Icons.Outlined.CropFree
    }
}
