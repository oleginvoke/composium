package oleginvoke.com.composium.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oleginvoke.com.composium.ui.theme.Tokens
import kotlin.math.roundToInt

private const val DragDismissThresholdDp = 120f

@Composable
internal fun ComposiumModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Tokens.colors.surface,
    sheetWindowInsets: WindowInsets? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val dragDismissThresholdPx = with(density) { DragDismissThresholdDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Tokens.colors.scrim.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismissRequest,
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    dragOffset.snapTo(
                                        (dragOffset.value + dragAmount).coerceAtLeast(0f),
                                    )
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (dragOffset.value > dragDismissThresholdPx) {
                                        onDismissRequest()
                                        dragOffset.snapTo(0f)
                                    } else {
                                        dragOffset.animateTo(0f, tween(220))
                                    }
                                }
                            },
                        )
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                    .background(containerColor),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (sheetWindowInsets != null) {
                                Modifier.windowInsetsPadding(sheetWindowInsets.only(WindowInsetsSides.Bottom))
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(Tokens.shapes.pill)
                                .background(Tokens.colors.outlineVariant)
                                .align(Alignment.Center),
                        )
                        ComposiumIconButton(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onClick = onDismissRequest,
                            size = 26.dp,
                        ) {
                            ComposiumIcon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Tokens.colors.onSurface,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 0.dp),
                    ) {
                        content()
                    }
                }
            }
        }
    }

    LaunchedEffect(visible) {
        if (!visible) dragOffset.snapTo(0f)
    }
}
