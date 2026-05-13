package oleginvoke.com.composium.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.R
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.pressScale

@Composable
internal fun ComposiumSceneCard(
    name: String,
    group: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val iconTint by animateColorAsState(
        targetValue = if (pressed) {
            Tokens.colors.primary
        } else {
            Tokens.colors.primary.copy(alpha = 0.88f)
        },
        animationSpec = Motion.tweenStandard(),
        label = "scene_card_icon_tint",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(Tokens.shapes.medium)
            .background(Tokens.colors.background.copy(alpha = if (pressed) 0.9f else 0f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposiumIcon(
                painter = painterResource(R.drawable.scene_icon),
                contentDescription = null,
                tint = Tokens.colors.secondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!group.isNullOrBlank()) {
                    ComposiumText(
                        text = group,
                        style = Tokens.typography.labelSmall,
                        color = Tokens.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                ComposiumText(
                    text = name,
                    style = Tokens.typography.titleLarge,
                    color = Tokens.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
internal fun ComposiumSceneRow(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val accentAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0.72f,
        animationSpec = Motion.tweenStandard(),
        label = "scene_row_accent",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(Tokens.shapes.medium)
            .background(Tokens.colors.background.copy(alpha = if (pressed) 0.9f else 0f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComposiumIcon(
            painter = painterResource(R.drawable.scene_icon),
            contentDescription = null,
            tint = Tokens.colors.secondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(10.dp))
        ComposiumText(
            text = name,
            style = Tokens.typography.titleMedium,
            color = Tokens.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
