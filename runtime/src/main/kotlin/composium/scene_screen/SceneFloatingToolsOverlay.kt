package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SceneFloatingToolsScreenMargin = 12.dp

@Composable
internal fun SceneFloatingToolsOverlay(
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
    position: MutableState<Offset?>,
    contentWindowInsets: WindowInsets?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = AbsoluteAlignment.TopLeft,
    ) {
        // ComposiumHostScreen already consumes the horizontal system insets around SceneScreen.
        // Top and bottom remain unconsumed so scene content can draw edge to edge there.
        val safeInsets = SceneFloatingToolsSafeInsets(
            top = contentWindowInsets?.getTop(density) ?: 0,
            bottom = contentWindowInsets?.getBottom(density) ?: 0,
        )
        val placementBounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(width = constraints.maxWidth, height = constraints.maxHeight),
            toolsSizePx = with(density) {
                IntSize(SceneFloatingToolsWidth.roundToPx(), SceneFloatingToolsHeight.roundToPx())
            },
            safeInsets = safeInsets,
            marginPx = with(density) { SceneFloatingToolsScreenMargin.roundToPx() },
        )
        val displayedPosition = placementBounds.clamp(
            position.value ?: placementBounds.topRightOffset,
        )

        SideEffect {
            if (position.value != displayedPosition) {
                position.value = displayedPosition
            }
        }

        SceneFloatingTools(
            controlsLayout = controlsLayout,
            isMinimized = isMinimized,
            isDarkTheme = isDarkTheme,
            isEyedropperVisible = isEyedropperVisible,
            onBack = onBack,
            onToggleControls = onToggleControls,
            onToggleEyedropper = onToggleEyedropper,
            onThemeChange = onThemeChange,
            onMinimize = onMinimize,
            onShow = onShow,
            onDrag = { dragAmount ->
                val currentPosition = placementBounds.clamp(
                    position.value ?: placementBounds.topRightOffset,
                )
                position.value = placementBounds.clamp(currentPosition + dragAmount)
            },
            modifier = Modifier.absoluteOffset {
                IntOffset(
                    x = displayedPosition.x.roundToInt(),
                    y = displayedPosition.y.roundToInt(),
                )
            },
        )
    }
}
