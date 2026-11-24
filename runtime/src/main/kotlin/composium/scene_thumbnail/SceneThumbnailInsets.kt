package oleginvoke.com.composium.scene_thumbnail

import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
internal fun thumbnailInsets(
    view: View,
    origin: () -> IntOffset,
): (SceneThumbnailKey) -> WindowInsetsCompat {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val status = if (Build.VERSION.SDK_INT < 23) WindowInsets.statusBars else WindowInsets(0)
    val navigation = if (Build.VERSION.SDK_INT < 23) WindowInsets.navigationBars else WindowInsets(0)
    fun WindowInsets.toInsets() = Insets.of(
        getLeft(density, direction), getTop(density), getRight(density, direction), getBottom(density),
    )
    return { key ->
        // getRootWindowInsets is unavailable on API 21–22. Compose receives the host's inset dispatch there.
        val source = ViewCompat.getRootWindowInsets(view) ?: WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), status.toInsets())
            .setInsets(WindowInsetsCompat.Type.navigationBars(), navigation.toInsets())
            .build()
        val offset = origin()
        val clipped = source.inset(
            offset.x.coerceAtLeast(0), offset.y.coerceAtLeast(0),
            (view.rootView.width - offset.x - key.viewportWidthPx).coerceAtLeast(0),
            (view.rootView.height - offset.y - key.viewportHeightPx).coerceAtLeast(0),
        )
        WindowInsetsCompat.Builder(clipped)
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
            .setVisible(WindowInsetsCompat.Type.ime(), false)
            .build()
    }
}
