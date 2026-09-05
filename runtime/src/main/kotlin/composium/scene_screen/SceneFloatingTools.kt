package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumSurface
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens

private val SceneFloatingToolsSize = 104.dp
private val SceneFloatingToolCellSize = 52.dp
private val SceneFloatingEyeTouchSize = 48.dp
private val SceneFloatingEyeSize = 40.dp

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
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    val settingsButtonState = calculateSceneSettingsButtonState(controlsLayout)
    val eyedropperButtonState = calculateSceneEyedropperButtonState(isEyedropperVisible)

    Box(
        modifier = modifier.size(SceneFloatingToolsSize),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = !isMinimized,
            enter = fadeIn(Motion.tweenStandard()) +
                scaleIn(
                    animationSpec = Motion.springSnappy(),
                    initialScale = 0.72f,
                    transformOrigin = TransformOrigin.Center,
                ),
            exit = fadeOut(Motion.tweenFast()) +
                scaleOut(
                    animationSpec = Motion.tweenFast(),
                    targetScale = 0.72f,
                    transformOrigin = TransformOrigin.Center,
                ),
        ) {
            SceneFloatingToolsGrid(
                settingsButtonState = settingsButtonState,
                eyedropperButtonState = eyedropperButtonState,
                isDarkTheme = isDarkTheme,
                onBack = onBack,
                onToggleControls = onToggleControls,
                onToggleEyedropper = onToggleEyedropper,
                onThemeChange = onThemeChange,
            )
        }

        SceneFloatingToolsEye(
            isMinimized = isMinimized,
            onClick = if (isMinimized) onShow else onMinimize,
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f),
        )
    }
}

@Composable
private fun SceneFloatingToolsGrid(
    settingsButtonState: SceneSettingsButtonState,
    eyedropperButtonState: SceneEyedropperButtonState,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleEyedropper: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
) {
    val dividerColor = Tokens.colors.outlineVariant.copy(alpha = 0.8f)
    ComposiumSurface(
        modifier = Modifier.size(SceneFloatingToolsSize),
        color = Tokens.colors.surface,
        shape = Tokens.shapes.medium,
        border = BorderStroke(width = 1.dp, color = dividerColor),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column {
                Row {
                    SceneFloatingToolCell(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                    SceneFloatingToolCell(
                        imageVector = settingsButtonState.icon.imageVector(),
                        contentDescription = settingsButtonState.contentDescription,
                        onClick = onToggleControls,
                        active = settingsButtonState.active,
                    )
                }
                Row {
                    SceneFloatingToolCell(
                        imageVector = Icons.Outlined.Colorize,
                        contentDescription = eyedropperButtonState.contentDescription,
                        onClick = onToggleEyedropper,
                        active = eyedropperButtonState.active,
                    )
                    SceneFloatingToolCell(
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
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(dividerColor),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(dividerColor),
            )
        }
    }
}

@Composable
private fun SceneFloatingToolCell(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    SceneToolActionButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        active = active,
        size = SceneFloatingToolCellSize,
        containerShape = RectangleShape,
        showBorder = false,
    )
}

@Composable
private fun SceneFloatingToolsEye(
    isMinimized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(SceneFloatingEyeTouchSize)
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
                .size(SceneFloatingEyeSize)
                .shadow(
                    elevation = 3.dp,
                    shape = Tokens.shapes.pill,
                    clip = false,
                )
                .clip(Tokens.shapes.pill)
                .background(Tokens.colors.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = Tokens.colors.outlineVariant,
                    shape = Tokens.shapes.pill,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isMinimized,
                transitionSpec = {
                    (fadeIn(Motion.tweenStandard()) + scaleIn(initialScale = 0.7f)) togetherWith
                        (fadeOut(Motion.tweenFast()) + scaleOut(targetScale = 0.7f))
                },
                label = "scene_floating_tools_eye",
            ) { minimized ->
                ComposiumIcon(
                    imageVector = if (minimized) {
                        Icons.Outlined.Visibility
                    } else {
                        Icons.Outlined.VisibilityOff
                    },
                    contentDescription = null,
                    tint = Tokens.colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
