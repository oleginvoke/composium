package oleginvoke.com.composium.scene_screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneTools

internal fun calculateSceneContentPadding(
    tools: SceneTools,
    statusBarInset: Dp,
    navigationBarInset: Dp,
    topBarHeight: Dp,
    inspectorLayoutMode: SceneInspectorLayoutMode,
): PaddingValues = PaddingValues(
    top = statusBarInset + if (tools == SceneTools.TopBar) topBarHeight else 0.dp,
    bottom = if (inspectorLayoutMode == SceneInspectorLayoutMode.Closed) {
        navigationBarInset
    } else {
        0.dp
    },
)
