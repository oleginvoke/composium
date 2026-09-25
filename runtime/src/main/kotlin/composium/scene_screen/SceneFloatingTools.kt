package oleginvoke.com.composium.scene_screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal val SceneFloatingToolSize = 48.dp
internal val SceneFloatingMoreSize = 44.dp
internal val SceneFloatingToolsGap = 8.dp
internal val SceneFloatingToolsWidth = SceneFloatingToolSize
internal val SceneFloatingToolsHeight = SceneFloatingToolSize + SceneFloatingToolsGap + SceneFloatingMoreSize
private val ActionSize = 48.dp
private val ActionRadius = 54.dp
// Unit vectors spaced by 60 degrees along the left/lower arc.
private val ActionDirections = listOf(
    Offset(-sqrt(3f) / 2f, -0.5f),
    Offset(-sqrt(3f) / 2f, 0.5f),
    Offset(0f, 1f),
)

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
    onSettingsDrag: (Offset) -> Unit = {},
    onMoreDrag: (Offset) -> Unit = {},
    settingsPosition: Offset? = null,
    morePosition: Offset? = null,
    modifier: Modifier = Modifier,
) {
    val settings = calculateSceneSettingsButtonState(controlsLayout)
    val eyedropper = calculateSceneEyedropperButtonState(isEyedropperVisible)
    val progress by animateFloatAsState(
        targetValue = if (isMinimized) 0f else 1f,
        animationSpec = tween(if (isMinimized) 180 else 280, easing = FastOutSlowInEasing),
        label = "floating_actions_expansion",
    )
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = AbsoluteAlignment.TopLeft) {
        val buttonPx = with(density) { SceneFloatingToolSize.toPx() }
        val morePx = with(density) { SceneFloatingMoreSize.toPx() }
        val defaultSettings = Offset(
            (constraints.maxWidth - buttonPx - with(density) { 12.dp.toPx() }).coerceAtLeast(0f),
            with(density) { 12.dp.toPx() },
        )
        val settingsOffset = settingsPosition ?: defaultSettings
        val moreOffset = morePosition ?: (defaultSettings + Offset((buttonPx - morePx) / 2f, with(density) {
            (SceneFloatingToolSize + SceneFloatingToolsGap).toPx()
        }))
        if (!isMinimized || progress > 0f) {
            // Local vectors relative to the ellipsis, never clamped or mirrored.
            val actions = listOf(
                Triple(Icons.Outlined.Colorize, eyedropper.contentDescription, ActionDirections[0]),
                Triple(if (isDarkTheme) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny,
                    if (isDarkTheme) "Switch to light theme" else "Switch to dark theme", ActionDirections[1]),
                Triple(Icons.AutoMirrored.Filled.ArrowBack, "Back", ActionDirections[2]),
            )
            actions.forEachIndexed { index, (icon, description, vector) ->
                FloatingButton(
                    imageVector = icon,
                    description = description,
                    active = index == 0 && eyedropper.active,
                    enabled = !isMinimized && progress >= 0.99f,
                    size = ActionSize,
                    onClick = when (index) {
                        0 -> onToggleEyedropper
                        1 -> { { onThemeChange(!isDarkTheme) } }
                        else -> onBack
                    },
                    modifier = Modifier.absoluteOffset {
                        val inset = (morePx - with(density) { ActionSize.toPx() }) / 2f
                        val position = moreOffset + Offset(inset, inset) + vector * with(density) { ActionRadius.toPx() } * progress
                        IntOffset(position.x.roundToInt(), position.y.roundToInt())
                    }.graphicsLayer {
                        alpha = progress
                        scaleX = 0.65f + 0.35f * progress
                        scaleY = scaleX
                    },
                )
            }
        }
        FloatingButton(
            imageVector = Icons.Outlined.Tune,
            description = settings.contentDescription,
            active = settings.active,
            onClick = onToggleControls,
            onDrag = onSettingsDrag,
            modifier = Modifier.absoluteOffset { settingsOffset.toIntOffset() },
        )
        // Main anchors are above the fan; overlap between the two anchors is allowed.
        FloatingButton(
            imageVector = Icons.Outlined.MoreVert,
            description = floatingToolsToggleContentDescription(isMinimized),
            active = !isMinimized,
            onClick = if (isMinimized) onShow else onMinimize,
            onDrag = onMoreDrag,
            size = SceneFloatingMoreSize,
            modifier = Modifier.absoluteOffset { moreOffset.toIntOffset() },
        )
    }
}

private fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloatingButton(
    imageVector: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = SceneFloatingToolSize,
    onDrag: ((Offset) -> Unit)? = null,
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    SceneToolActionButton(
        imageVector = imageVector,
        contentDescription = description,
        onClick = onClick,
        active = active,
        enabled = enabled,
        size = size,
        modifier = modifier.then(if (onDrag != null) Modifier.pointerInput(Unit) {
                detectDragGestures(
                    orientationLock = null,
                    onDragStart = { down, change, overSlop ->
                        currentOnDrag?.invoke(change.position - down.position - overSlop)
                    },
                    shouldAwaitTouchSlop = { true },
                    onDrag = { change, amount ->
                        change.consume()
                        currentOnDrag?.invoke(amount)
                    },
                )
            } else Modifier),
    )
}
