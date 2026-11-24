package oleginvoke.com.composium.scene_screen

import android.os.Build
import android.util.DisplayMetrics
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Display zoom was introduced in API 24; earlier devices use their current density as default. */
internal fun systemDefaultDisplayDensity(currentDensity: Float): Float =
    if (Build.VERSION.SDK_INT >= 24) DisplayMetrics.DENSITY_DEVICE_STABLE / 160f else currentDensity

internal fun resolvePreviewDensity(defaultDensity: Float, scale: Float): Float =
    (defaultDensity.toDouble() * scale.toDouble())
        .coerceIn(Float.MIN_VALUE.toDouble(), Float.MAX_VALUE.toDouble()).toFloat()

/**
 * Divide in Double and convert each edge independently. Float division can overflow even
 * for a valid positive scale; multiplying a zero edge by that infinity then produces NaN.
 * Keep the exact scale in state and saturate only results outside Float's representable range.
 */
internal fun rescaleSceneContentPadding(
    padding: PaddingValues,
    layoutDirection: LayoutDirection,
    hostDensity: Float,
    previewDensity: Float,
): PaddingValues {
    fun rescale(value: Float) = (value.toDouble() * hostDensity.toDouble() / previewDensity.toDouble())
        .coerceAtMost(Float.MAX_VALUE.toDouble()).toFloat().dp
    return PaddingValues.Absolute(
        left = rescale(padding.calculateLeftPadding(layoutDirection).value),
        top = rescale(padding.calculateTopPadding().value),
        right = rescale(padding.calculateRightPadding(layoutDirection).value),
        bottom = rescale(padding.calculateBottomPadding().value),
    )
}
