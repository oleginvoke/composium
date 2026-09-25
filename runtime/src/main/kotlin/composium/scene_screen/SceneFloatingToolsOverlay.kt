package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.FloatingToolsPosition

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
    settingsPosition: MutableState<Offset?>,
    morePosition: MutableState<Offset?>,
    contentWindowInsets: WindowInsets,
    initialPosition: FloatingToolsPosition,
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
            top = contentWindowInsets.getTop(density),
            bottom = contentWindowInsets.getBottom(density),
        )
        val initialPairBounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(width = constraints.maxWidth, height = constraints.maxHeight),
            toolsSizePx = with(density) {
                IntSize(SceneFloatingToolsWidth.roundToPx(), SceneFloatingToolsHeight.roundToPx())
            },
            safeInsets = safeInsets,
            marginPx = with(density) { SceneFloatingToolsScreenMargin.roundToPx() },
        )
        val settingsBounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(constraints.maxWidth, constraints.maxHeight),
            toolsSizePx = with(density) { IntSize(SceneFloatingToolSize.roundToPx(), SceneFloatingToolSize.roundToPx()) },
            safeInsets = safeInsets,
            marginPx = with(density) { SceneFloatingToolsScreenMargin.roundToPx() },
        )
        val moreBounds = calculateSceneFloatingToolsPlacementBounds(
            containerSize = IntSize(constraints.maxWidth, constraints.maxHeight),
            toolsSizePx = with(density) { IntSize(SceneFloatingMoreSize.roundToPx(), SceneFloatingMoreSize.roundToPx()) },
            safeInsets = safeInsets,
            marginPx = with(density) { SceneFloatingToolsScreenMargin.roundToPx() },
        )
        val initialSettings = initialPairBounds.initialOffset(initialPosition)
        val initialMore = initialSettings + Offset(with(density) {
            (SceneFloatingToolSize - SceneFloatingMoreSize).toPx() / 2f
        }, with(density) {
            (SceneFloatingToolSize + SceneFloatingToolsGap).toPx()
        })
        val displayedSettings = settingsBounds.clamp(settingsPosition.value ?: initialSettings)
        val displayedMore = moreBounds.clamp(morePosition.value ?: initialMore)

        SideEffect {
            // Until the user drags, keep following the default anchor as insets settle or change.
            if (settingsPosition.value != null && settingsPosition.value != displayedSettings) settingsPosition.value = displayedSettings
            if (morePosition.value != null && morePosition.value != displayedMore) morePosition.value = displayedMore
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
            settingsPosition = displayedSettings,
            morePosition = displayedMore,
            onSettingsDrag = { amount ->
                settingsPosition.value = settingsBounds.clamp(settingsBounds.clamp(settingsPosition.value ?: initialSettings) + amount)
            },
            onMoreDrag = { amount ->
                morePosition.value = moreBounds.clamp(moreBounds.clamp(morePosition.value ?: initialMore) + amount)
            },
            modifier = Modifier,
        )
    }
}
