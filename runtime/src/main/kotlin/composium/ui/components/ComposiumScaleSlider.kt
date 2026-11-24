package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.theme.Tokens
import kotlin.math.round

internal val EnvironmentScalePresets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
internal val EnvironmentScaleRange = 0.5f..2f
internal val ScaleTrackInset = 13.dp

internal fun scaleFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float =
    ((value.toDouble() - range.start) / (range.endInclusive.toDouble() - range.start)).toFloat().coerceIn(0f, 1f)

/** Continuous control. Presets and exact input are independent of its track. */
@Composable
internal fun ComposiumScaleSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    systemValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val change by rememberUpdatedState(onValueChange)
    val active = Tokens.colors.primary
    val inactive = Tokens.colors.outlineVariant
    val surface = Tokens.colors.surface
    Canvas(
        modifier.fillMaxWidth().height(28.dp)
            .semantics {
                stateDescription = "${value}x"
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range), range)
                setProgress { requested ->
                    if (!requested.isFinite()) false else {
                        change(requested.coerceIn(range)); true
                    }
                }
            }
            .pointerInput(range, rtl) {
                fun update(x: Float) {
                    val padding = ScaleTrackInset.toPx()
                    var fraction = ((x - padding) / (size.width - 2 * padding).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    if (rtl) fraction = 1f - fraction
                    change((round((range.start.toDouble() + fraction * (range.endInclusive.toDouble() - range.start)) * 1000.0) / 1000.0).toFloat().coerceIn(range))
                }
                detectTapGestures { update(it.x) }
            }
            .pointerInput(range, rtl) {
                detectHorizontalDragGestures { event, _ ->
                    event.consume()
                    val padding = ScaleTrackInset.toPx()
                    var fraction = ((event.position.x - padding) / (size.width - 2 * padding).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    if (rtl) fraction = 1f - fraction
                    change((round((range.start.toDouble() + fraction * (range.endInclusive.toDouble() - range.start)) * 1000.0) / 1000.0).toFloat().coerceIn(range))
                }
            },
    ) {
        val inset = ScaleTrackInset.toPx()
        fun x(v: Float): Float {
            var fraction = scaleFraction(v, range)
            if (rtl) fraction = 1f - fraction
            return inset + fraction * (size.width - 2 * inset)
        }
        val y = size.height / 2f
        val thumb = x(value)
        drawLine(inactive.copy(alpha = 0.65f), Offset(inset, y), Offset(size.width - inset, y), 6.dp.toPx(), StrokeCap.Round)
        drawLine(active, Offset(x(range.start), y), Offset(thumb, y), 6.dp.toPx(), StrokeCap.Round)
        EnvironmentScalePresets.forEach { preset ->
            drawCircle(if (preset <= value) surface.copy(alpha = 0.65f) else active.copy(alpha = 0.35f), 2.dp.toPx(), Offset(x(preset), y))
        }
        if (systemValue in range && systemValue !in EnvironmentScalePresets) {
            drawCircle(surface, 2.dp.toPx(), Offset(x(systemValue), y))
        }
        drawCircle(active.copy(alpha = 0.12f), 13.dp.toPx(), Offset(thumb, y))
        drawCircle(active, 9.dp.toPx(), Offset(thumb, y))
        drawCircle(surface, 3.dp.toPx(), Offset(thumb, y))
    }
}
