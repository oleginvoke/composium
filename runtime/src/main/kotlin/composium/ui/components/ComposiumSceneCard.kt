package oleginvoke.com.composium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailPreviewHorizontalAlignment
import oleginvoke.com.composium.scene_thumbnail.SceneThumbnailState
import oleginvoke.com.composium.scene_thumbnail.calculateSceneThumbnailReadyPreviewSize
import oleginvoke.com.composium.scene_thumbnail.sceneThumbnailCardLayout
import oleginvoke.com.composium.ui.theme.Tokens
import oleginvoke.com.composium.ui.theme.pressScale

@Composable
internal fun ComposiumSceneCard(
    name: String,
    group: String?,
    thumbnailState: SceneThumbnailState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val layout = remember { sceneThumbnailCardLayout() }
    val previewWidth = if (compact) SceneThumbnailPreviewCompactWidth else SceneThumbnailPreviewWidth
    val previewHeight = if (compact) SceneThumbnailPreviewCompactHeight else SceneThumbnailPreviewHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .shadow(
                elevation = if (pressed) 3.dp else 1.dp,
                shape = Tokens.shapes.medium,
                ambientColor = Tokens.colors.scrim.copy(alpha = 0.08f),
                spotColor = Tokens.colors.scrim.copy(alpha = 0.10f),
            )
            .clip(Tokens.shapes.medium)
            .background(Tokens.colors.surface.copy(alpha = if (pressed) 0.94f else 0.98f))
            .border(
                width = 1.dp,
                color = Tokens.colors.outlineVariant.copy(alpha = 0.78f),
                shape = Tokens.shapes.medium,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = previewHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SceneThumbnailPreview(
                state = thumbnailState,
                compact = compact,
                horizontalAlignment = layout.previewHorizontalAlignment,
                modifier = Modifier
                    .size(width = previewWidth, height = previewHeight)
                    .background(Tokens.colors.surfaceVariant.copy(alpha = 0.42f))
                    .drawThumbnailRightBorder(
                        color = Tokens.colors.outlineVariant.copy(alpha = 0.52f),
                    )
                    .padding(
                        6.dp
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (compact) 12.dp else 14.dp,
                        end = if (compact) 10.dp else 12.dp,
                        top = if (compact) 10.dp else 12.dp,
                        bottom = if (compact) 10.dp else 12.dp,
                    ),
            ) {
                ComposiumText(
                    text = name,
                    style = if (compact) Tokens.typography.titleMedium else Tokens.typography.titleLarge,
                    color = Tokens.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!group.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    ComposiumText(
                        text = formatSceneGroupPath(group),
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(if (group.isNullOrBlank()) 4.dp else 5.dp))
                ComposiumText(
                    text = "Scene",
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SceneThumbnailPreview(
    state: SceneThumbnailState?,
    compact: Boolean,
    horizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is SceneThumbnailState.Ready -> {
            BoxWithConstraints(
                modifier = modifier,
                contentAlignment = horizontalAlignment.toComposeAlignment(),
            ) {
                val maxPreviewHeight = if (compact) {
                    SceneThumbnailPreviewCompactMaxHeight
                } else {
                    SceneThumbnailPreviewMaxHeight
                }
                val previewSize = calculateSceneThumbnailReadyPreviewSize(
                    imageWidthPx = state.image.width,
                    imageHeightPx = state.image.height,
                    maxWidthDp = maxWidth.value,
                    maxHeightDp = minOf(maxHeight.value, maxPreviewHeight.value),
                    captureScale = state.captureScale,
                )
                Box(
                    modifier = Modifier
                        .size(
                            width = previewSize.widthDp.dp,
                            height = previewSize.heightDp.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = state.image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        is SceneThumbnailState.Failed -> {
            SceneThumbnailPlaceholder(
                text = "Preview unavailable",
                detail = state.reason,
                horizontalAlignment = horizontalAlignment,
                modifier = modifier,
            )
        }

        SceneThumbnailState.Capturing,
        SceneThumbnailState.Pending,
        null,
        -> {
            SceneThumbnailPlaceholder(
                text = "Preparing preview",
                horizontalAlignment = horizontalAlignment,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SceneThumbnailPlaceholder(
    text: String,
    detail: String? = null,
    horizontalAlignment: SceneThumbnailPreviewHorizontalAlignment,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = horizontalAlignment.toComposeAlignment(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalAlignment = horizontalAlignment.toComposeHorizontalAlignment(),
        ) {
            ComposiumText(
                text = text,
                style = Tokens.typography.labelSmall,
                color = Tokens.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                ComposiumText(
                    text = detail,
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colors.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val SceneThumbnailPreviewMaxHeight = 180.dp
private val SceneThumbnailPreviewCompactMaxHeight = 150.dp
private val SceneThumbnailPreviewWidth = 124.dp
private val SceneThumbnailPreviewHeight = 88.dp
private val SceneThumbnailPreviewCompactWidth = 112.dp
private val SceneThumbnailPreviewCompactHeight = 76.dp

private fun Modifier.drawThumbnailRightBorder(color: Color): Modifier =
    drawBehind {
        val strokeWidth = 1.dp.toPx()
        val x = size.width - strokeWidth / 2f
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth,
        )
    }

private fun SceneThumbnailPreviewHorizontalAlignment.toComposeAlignment(): Alignment =
    when (this) {
        SceneThumbnailPreviewHorizontalAlignment.Start -> Alignment.CenterStart
        SceneThumbnailPreviewHorizontalAlignment.Center -> Alignment.Center
    }

private fun SceneThumbnailPreviewHorizontalAlignment.toComposeHorizontalAlignment(): Alignment.Horizontal =
    when (this) {
        SceneThumbnailPreviewHorizontalAlignment.Start -> Alignment.Start
        SceneThumbnailPreviewHorizontalAlignment.Center -> Alignment.CenterHorizontally
    }

private fun formatSceneGroupPath(group: String): String =
    group.split("/")
        .map { segment -> segment.trim() }
        .filter { segment -> segment.isNotEmpty() }
        .joinToString(" / ")
        .ifEmpty { group }
