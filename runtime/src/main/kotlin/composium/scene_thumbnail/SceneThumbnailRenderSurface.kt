package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.SceneEntry
import oleginvoke.com.composium.SceneScope
import oleginvoke.com.composium.thumbnailContent

@Composable
internal fun SceneThumbnailRenderSurface(
    sceneEntry: SceneEntry,
    sceneScope: SceneScope,
    captureScale: Float = DefaultSceneThumbnailCaptureScale,
    modifier: Modifier = Modifier,
) {
    require(captureScale > 0f) { "captureScale must be > 0" }

    SideEffect {
        sceneScope.internalInnerPadding = PaddingValues(0.dp)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        androidx.compose.ui.platform.LocalDensity provides Density(density = captureScale, fontScale = 1f),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            with(sceneScope) {
                sceneEntry.scene.thumbnailContent().invoke(this)
            }
        }
    }
}
