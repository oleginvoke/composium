package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumSurface
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens

private val SceneFloatingToolSize = 48.dp
private val SceneFloatingHandleWidth = 28.dp
private val SceneFloatingHandleHeight = 18.dp

@Composable
internal fun SceneFloatingTools(
    controlsLayout: SceneInspectorLayoutMode,
    isMinimized: Boolean,
    isDarkTheme: Boolean,
    isEyedropperVisible: Boolean,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleEyedropper: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onMinimize: () -> Unit,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsButtonState = calculateSceneSettingsButtonState(controlsLayout)
    val eyedropperButtonState = calculateSceneEyedropperButtonState(isEyedropperVisible)

    ComposiumSurface(
        modifier = modifier,
        color = Tokens.colors.surface,
        shape = Tokens.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = Tokens.colors.outlineVariant.copy(alpha = 0.8f),
        ),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            SceneFloatingToolsHandle(
                isMinimized = isMinimized,
                onClick = if (isMinimized) onShow else onMinimize,
            )
            AnimatedVisibility(
                visible = !isMinimized,
                enter = fadeIn(Motion.tweenStandard()) +
                    expandVertically(
                        animationSpec = Motion.tweenStandard(),
                        expandFrom = Alignment.Top,
                    ),
                exit = fadeOut(Motion.tweenFast()) +
                    shrinkVertically(
                        animationSpec = Motion.tweenStandard(),
                        shrinkTowards = Alignment.Top,
                    ),
            ) {
                Column {
                    Row {
                        SceneToolActionButton(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                        SceneToolActionButton(
                            imageVector = settingsButtonState.icon.imageVector(),
                            contentDescription = settingsButtonState.contentDescription,
                            onClick = onToggleControls,
                            active = settingsButtonState.active,
                        )
                    }
                    Row {
                        SceneToolActionButton(
                            imageVector = Icons.Outlined.Colorize,
                            contentDescription = eyedropperButtonState.contentDescription,
                            onClick = onToggleEyedropper,
                            active = eyedropperButtonState.active,
                        )
                        SceneToolActionButton(
                            imageVector = if (isDarkTheme) {
                                Icons.Outlined.NightsStay
                            } else {
                                Icons.Outlined.WbSunny
                            },
                            contentDescription = if (isDarkTheme) {
                                "Switch to light theme"
                            } else {
                                "Switch to dark theme"
                            },
                            onClick = { onThemeChange(!isDarkTheme) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneFloatingToolsHandle(
    isMinimized: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(SceneFloatingToolSize)
            .semantics {
                contentDescription = floatingToolsToggleContentDescription(isMinimized)
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(SceneFloatingHandleWidth)
                .height(SceneFloatingHandleHeight)
                .clip(Tokens.shapes.pill)
                .background(Tokens.colors.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = Tokens.colors.outlineVariant.copy(alpha = 0.8f),
                    shape = Tokens.shapes.pill,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ComposiumIcon(
                imageVector = if (isMinimized) {
                    Icons.Outlined.ExpandMore
                } else {
                    Icons.Outlined.ExpandLess
                },
                contentDescription = null,
                tint = Tokens.colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
