package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.components.ComposiumIcon
import oleginvoke.com.composium.ui.components.ComposiumSurface
import oleginvoke.com.composium.ui.theme.Motion
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.pressScale

private val SceneFloatingToolsPadding = 8.dp
private val SceneFloatingToolSize = 48.dp
private val SceneFloatingEyeHeight = 56.dp
private val SceneFloatingToolSpacing = 6.dp
internal val SceneFloatingToolsWidth = SceneFloatingToolSize + SceneFloatingToolsPadding * 2
internal val SceneFloatingToolsHeight =
    SceneFloatingToolSize * 4 + SceneFloatingEyeHeight + SceneFloatingToolSpacing * 4 + SceneFloatingToolsPadding * 2
private val SceneFloatingToolsCollapsedHeight = SceneFloatingEyeHeight + SceneFloatingToolsPadding * 2

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
    onDrag: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    val settingsButtonState = calculateSceneSettingsButtonState(controlsLayout)
    val eyedropperButtonState = calculateSceneEyedropperButtonState(isEyedropperVisible)
    val capsuleHeight by animateDpAsState(
        targetValue = if (isMinimized) SceneFloatingToolsCollapsedHeight else SceneFloatingToolsHeight,
        animationSpec = Motion.springSnappy(),
        label = "scene_floating_tools_height",
    )

    // Keep the expanded footprint as the drag anchor: collapsing never moves the eye.
    Box(
        modifier = modifier.size(width = SceneFloatingToolsWidth, height = SceneFloatingToolsHeight),
        contentAlignment = Alignment.Center,
    ) {
        ComposiumSurface(
            modifier = Modifier
                .size(width = SceneFloatingToolsWidth, height = capsuleHeight)
                .pointerInput(Unit) {
                    // Gaps belong to the toolbar, not to the scene underneath it.
                    detectTapGestures(onTap = {})
                },
            color = Tokens.colors.surface,
            shape = Tokens.shapes.pill,
            border = BorderStroke(1.dp, Tokens.colors.outlineVariant),
        ) {
            // The surface clips the full-height actions as it contracts towards the eye.
            Box(
                Modifier.requiredSize(width = SceneFloatingToolsWidth, height = SceneFloatingToolsHeight),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = !isMinimized,
                    enter = fadeIn(Motion.tweenStandard()) +
                        scaleIn(animationSpec = Motion.springSnappy(), initialScale = 0.85f),
                    exit = fadeOut(Motion.tweenFast()) +
                        scaleOut(animationSpec = Motion.tweenFast(), targetScale = 0.85f),
                ) {
                    Column(
                        modifier = Modifier.padding(SceneFloatingToolsPadding),
                        verticalArrangement = Arrangement.spacedBy(SceneFloatingToolSpacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SceneFloatingToolAction(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                            enabled = !isMinimized,
                        )
                        SceneFloatingToolAction(
                            imageVector = settingsButtonState.icon.imageVector(),
                            contentDescription = settingsButtonState.contentDescription,
                            onClick = onToggleControls,
                            active = settingsButtonState.active,
                            enabled = !isMinimized,
                        )
                        Spacer(Modifier.height(SceneFloatingEyeHeight))
                        SceneFloatingToolAction(
                            imageVector = Icons.Outlined.Colorize,
                            contentDescription = eyedropperButtonState.contentDescription,
                            onClick = onToggleEyedropper,
                            active = eyedropperButtonState.active,
                            enabled = !isMinimized,
                        )
                        SceneFloatingToolAction(
                            imageVector = if (isDarkTheme) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny,
                            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                            onClick = { onThemeChange(!isDarkTheme) },
                            enabled = !isMinimized,
                        )
                    }
                }
            }
        }
        SceneFloatingToolsEye(
            isMinimized = isMinimized,
            onClick = if (isMinimized) onShow else onMinimize,
            onDrag = onDrag,
        )
    }
}

@Composable
private fun SceneFloatingToolAction(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    SceneToolActionButton(
        imageVector = imageVector,
        contentDescription = contentDescription,
        onClick = onClick,
        active = active,
        enabled = enabled,
        size = SceneFloatingToolSize,
        showBorder = false,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SceneFloatingToolsEye(
    isMinimized: Boolean,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .size(width = SceneFloatingToolSize, height = SceneFloatingEyeHeight)
            .clip(Tokens.shapes.pill)
            .background(Tokens.colors.primaryContainer)
            .semantics {
                contentDescription = floatingToolsToggleContentDescription(isMinimized)
                role = Role.Button
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    orientationLock = null,
                    onDragStart = { down, slopTriggerChange, overSlopOffset ->
                        currentOnDrag(slopTriggerChange.position - down.position - overSlopOffset)
                    },
                    shouldAwaitTouchSlop = { true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount)
                    },
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
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
                imageVector = if (minimized) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = Tokens.colors.onPrimaryContainer,
                modifier = Modifier
                    .size(24.dp)
                    .pressScale(interactionSource, pressedScale = 0.9f),
            )
        }
    }
}
