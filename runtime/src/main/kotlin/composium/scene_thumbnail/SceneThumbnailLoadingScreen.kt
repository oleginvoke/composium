package oleginvoke.com.composium.scene_thumbnail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.ui.components.ComposiumText
import oleginvoke.com.composium.ui.theme.Tokens

@Composable
internal fun SceneThumbnailLoadingScreen(
    readyCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "thumbnail_loading")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "thumbnail_loading_pulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Tokens.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(Tokens.shapes.pill)
                    .background(Tokens.colors.primaryContainer)
                    .border(1.dp, Tokens.colors.outlineVariant, Tokens.shapes.pill),
            )
            Spacer(Modifier.height(18.dp))
            ComposiumText(
                text = "Preparing scene previews",
                style = Tokens.typography.titleLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                color = Tokens.colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            ComposiumText(
                text = "$readyCount of $totalCount ready",
                style = Tokens.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                color = Tokens.colors.onSurfaceVariant,
            )
        }
    }
}
