package oleginvoke.com.composium.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.min

internal object Motion {
    fun <T> tweenFast(): TweenSpec<T> = tween(durationMillis = 140)

    fun <T> tweenStandard(): TweenSpec<T> = tween(durationMillis = 220)

    fun <T> springSnappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> springGentle(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

internal fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.985f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.tweenFast(),
        label = "press_scale",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

internal fun Modifier.staggeredAppear(index: Int): Modifier = composed {
    var entered by remember { mutableStateOf(false) }
    val delay = min(index, 6) * 28

    LaunchedEffect(Unit) {
        entered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 260, delayMillis = delay),
        label = "stagger_alpha",
    )
    val offset by animateIntOffsetAsState(
        targetValue = if (entered) IntOffset.Zero else IntOffset(0, 12),
        animationSpec = tween(durationMillis = 280, delayMillis = delay),
        label = "stagger_offset",
    )

    graphicsLayer {
        this.alpha = alpha
        translationY = offset.y.toFloat()
    }
}
