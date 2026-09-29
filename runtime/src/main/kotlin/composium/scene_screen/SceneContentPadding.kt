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
    spaceBelowPreview: Dp,
): PaddingValues = PaddingValues(
    top = statusBarInset + if (tools == SceneTools.TopBar) topBarHeight else 0.dp,
    // Only the part of the system inset that intersects the actual preview needs padding.
    bottom = (navigationBarInset - spaceBelowPreview).coerceIn(0.dp, navigationBarInset),
)
