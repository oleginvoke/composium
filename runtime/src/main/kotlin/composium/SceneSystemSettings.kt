package oleginvoke.com.composium

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver

@Stable
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

internal val SceneSystemSettingsSaver = listSaver<SceneSystemSettings, Any>(
    save = { listOf(it.rtl, it.fontScaleOverride ?: 0f, it.displayScaleOverride ?: 0f) },
    restore = { saved ->
        SceneSystemSettings().apply {
            rtl = saved[0] as Boolean
            fontScaleOverride = (saved[1] as Float).takeIf { it > 0f }
            displayScaleOverride = (saved[2] as Float).takeIf { it > 0f }
        }
    },
)
