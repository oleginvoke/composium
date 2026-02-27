package oleginvoke.com.composium

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class SceneSystemSettings {
    var rtl by mutableStateOf(false)
    /** Null = use system font scale. */
    var fontScaleOverride: Float? by mutableStateOf(null)
    /**
     * Null = use system display size.
     * Value is a multiplier relative to "default" display size.
     */
    var displayScaleOverride: Float? by mutableStateOf(null)

    fun resetToSystem() {
        rtl = false
        fontScaleOverride = null
        displayScaleOverride = null
    }
}
