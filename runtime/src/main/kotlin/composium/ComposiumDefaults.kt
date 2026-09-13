package oleginvoke.com.composium

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

/** Default values used by [ComposiumScreen]. */
object ComposiumDefaults {
    /** System bar and display cutout insets. Parent-consumed insets are excluded by the screen. */
    val contentWindowInsets: WindowInsets
        @Composable
        get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)
}
